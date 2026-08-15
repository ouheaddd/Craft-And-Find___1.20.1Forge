package com.overyourhead.craftandfind.common.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A cached view of loaded storage positions inside the workbench radius.
 * Items are never stored here: every operation resolves the current block
 * entity / item handler and reads or moves stacks in the original storage.
 */
public final class NearbyStorage {
    private final Level level;
    private final BlockPos origin;
    private final List<StorageReference> storages;

    private NearbyStorage(Level level, BlockPos origin, List<StorageReference> storages) {
        this.level = level;
        this.origin = origin.immutable();
        this.storages = List.copyOf(storages);
    }

    public static NearbyStorage empty(BlockPos origin) {
        return new NearbyStorage(null, origin, List.of());
    }

    /**
     * Performs the relatively expensive radius scan. Only storage positions
     * are cached, so slot contents can still change immediately between scans.
     *
     * Vanilla {@link Container}s keep their old behavior. Modded block entity
     * inventories that expose Forge's item-handler capability are supported
     * as a fallback without a hard dependency on any specific storage mod.
     */
    public static NearbyStorage scan(Level level, BlockPos origin, int radius) {
        List<StorageReference> found = new ArrayList<>();
        BlockPos min = origin.offset(-radius, -radius, -radius);
        BlockPos max = origin.offset(radius, radius, radius);
        double radiusSquared = (double) radius * radius;

        for (BlockPos mutablePos : BlockPos.betweenClosed(min, max)) {
            BlockPos pos = mutablePos.immutable();
            if (pos.equals(origin)
                    || pos.distSqr(origin) > radiusSquared
                    || !level.hasChunkAt(pos)) {
                continue;
            }

            if (resolveStorage(level, pos) != null) {
                found.add(new StorageReference(pos, (int) pos.distSqr(origin)));
            }
        }

        return new NearbyStorage(level, origin, found);
    }

    /**
     * Builds the item list shown in the storage panel. Item components are part
     * of identity, so differently enchanted or otherwise component-bearing
     * stacks remain separate entries.
     */
    public List<StorageItemEntry> snapshot() {
        List<MutableEntry> totals = new ArrayList<>();

        for (SlotReference reference : currentSlots()) {
            ItemStack stack = reference.currentStack();
            if (stack.isEmpty()) {
                continue;
            }

            MutableEntry existing = null;
            for (MutableEntry candidate : totals) {
                if (ItemStack.isSameItemSameTags(candidate.stack, stack)) {
                    existing = candidate;
                    break;
                }
            }

            if (existing == null) {
                totals.add(new MutableEntry(copyWithCount(stack, 1), stack.getCount()));
            } else {
                existing.count = saturatedAdd(existing.count, stack.getCount());
            }
        }

        List<StorageItemEntry> result = new ArrayList<>(totals.size());
        for (MutableEntry total : totals) {
            result.add(new StorageItemEntry(total.stack, total.count));
        }
        result.sort(Comparator
                .comparing((StorageItemEntry value) -> value.stack().getHoverName().getString(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(value -> value.stack().getDescriptionId()));
        return List.copyOf(result);
    }

    /** Adds nearby storage stacks to vanilla's recipe availability counter. */
    public void account(StackedContents contents) {
        for (SlotReference reference : currentSlots()) {
            ItemStack stack = reference.currentStack();
            if (!stack.isEmpty()) {
                contents.accountStack(stack, stack.getCount());
            }
        }
    }

    /**
     * Returns as much of a crafting-grid stack as possible to nearby storage.
     * The returned stack is the remainder that no storage accepted.
     */
    public ItemStack insert(ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack remaining = stack.copy();
        for (ResolvedStorage resolved : currentStorages()) {
            remaining = resolved.storage().insert(remaining, false);
            if (remaining.isEmpty()) {
                return ItemStack.EMPTY;
            }
        }
        return remaining;
    }

    /**
     * Moves up to {@code remaining} matching items into a crafting-grid slot.
     * Started storage stacks are consumed first, then nearer storages.
     * Returns the number of items still missing after the operation.
     */
    public int moveToCraftingSlot(Slot target, ItemStack wanted, int remaining) {
        if (remaining <= 0 || wanted.isEmpty()) {
            return 0;
        }

        List<SlotReference> candidates = currentSlots().stream()
                .filter(reference -> {
                    ItemStack stack = reference.currentStack();
                    return !stack.isEmpty() && ItemStack.isSameItemSameTags(stack, wanted);
                })
                .sorted(Comparator
                        .comparingInt((SlotReference reference) -> isStartedStack(reference.currentStack()) ? 0 : 1)
                        .thenComparingInt(SlotReference::distanceSquared)
                        .thenComparingLong(reference -> reference.pos().asLong()))
                .toList();

        for (SlotReference reference : candidates) {
            if (remaining <= 0) {
                break;
            }

            ItemStack source = reference.currentStack();
            if (source.isEmpty() || !ItemStack.isSameItemSameTags(source, wanted)) {
                continue;
            }

            ItemStack targetStack = target.getItem();
            if (!targetStack.isEmpty() && !ItemStack.isSameItemSameTags(targetStack, wanted)) {
                break;
            }

            int targetCount = targetStack.isEmpty() ? 0 : targetStack.getCount();
            int room = Math.min(target.getMaxStackSize(wanted), wanted.getMaxStackSize()) - targetCount;
            if (room <= 0) {
                break;
            }

            int requested = Math.min(Math.min(source.getCount(), remaining), room);
            if (requested <= 0) {
                continue;
            }

            // IItemHandler implementations are allowed to reject or reduce an
            // extraction request. Simulate first so the crafting grid is only
            // changed after the source storage confirms the operation.
            ItemStack simulated = reference.extract(requested, true);
            if (simulated.isEmpty() || !ItemStack.isSameItemSameTags(simulated, wanted)) {
                continue;
            }

            int extractCount = Math.min(requested, simulated.getCount());
            ItemStack extracted = reference.extract(extractCount, false);
            if (extracted.isEmpty() || !ItemStack.isSameItemSameTags(extracted, wanted)) {
                continue;
            }

            int moved = Math.min(extractCount, extracted.getCount());
            if (moved <= 0) {
                continue;
            }

            if (targetStack.isEmpty()) {
                target.set(copyWithCount(wanted, moved));
            } else {
                targetStack.grow(moved);
                target.setChanged();
            }

            remaining -= moved;
        }

        return remaining;
    }

    /**
     * Returns every storage block holding the selected stack, together with its
     * exact amount. The storage with the largest amount is first; equal amounts
     * are resolved by distance to the requesting player.
     */
    public List<StorageHighlightTarget> highlightTargets(ItemStack wanted, Vec3 observerPosition) {
        if (wanted.isEmpty()) {
            return List.of();
        }

        Map<BlockPos, Integer> perBlockAmounts = new HashMap<>();
        for (SlotReference reference : currentSlots()) {
            ItemStack stack = reference.currentStack();
            if (stack.isEmpty() || !ItemStack.isSameItemSameTags(stack, wanted)) {
                continue;
            }

            perBlockAmounts.merge(reference.pos(), stack.getCount(), NearbyStorage::saturatedAdd);
        }

        Map<HighlightArea, Integer> amounts = new HashMap<>();
        for (Map.Entry<BlockPos, Integer> entry : perBlockAmounts.entrySet()) {
            amounts.merge(highlightArea(entry.getKey()), entry.getValue(), NearbyStorage::saturatedAdd);
        }

        return amounts.entrySet().stream()
                .map(entry -> new StorageHighlightTarget(
                        entry.getKey().minPos(),
                        entry.getKey().maxPos(),
                        entry.getValue()
                ))
                .sorted(Comparator
                        .comparingInt(StorageHighlightTarget::count)
                        .reversed()
                        .thenComparingDouble(target -> distanceSquared(target, observerPosition))
                        .thenComparingLong(target -> target.minPos().asLong())
                        .thenComparingLong(target -> target.maxPos().asLong()))
                .toList();
    }

    /** Resolves both halves of a live vanilla normal or trapped chest. */
    private HighlightArea highlightArea(BlockPos pos) {
        if (level == null || !level.hasChunkAt(pos)) {
            return HighlightArea.single(pos);
        }

        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof ChestBlock)
                || !state.hasProperty(ChestBlock.TYPE)
                || state.getValue(ChestBlock.TYPE) == ChestType.SINGLE) {
            return HighlightArea.single(pos);
        }

        BlockPos partnerPos = pos.relative(ChestBlock.getConnectedDirection(state));
        if (!level.hasChunkAt(partnerPos)) {
            return HighlightArea.single(pos);
        }

        BlockState partnerState = level.getBlockState(partnerPos);
        boolean validPartner = partnerState.getBlock() == state.getBlock()
                && partnerState.hasProperty(ChestBlock.TYPE)
                && partnerState.getValue(ChestBlock.TYPE) != ChestType.SINGLE
                && partnerPos.relative(ChestBlock.getConnectedDirection(partnerState)).equals(pos);
        return validPartner ? HighlightArea.of(pos, partnerPos) : HighlightArea.single(pos);
    }

    private static double distanceSquared(StorageHighlightTarget target, Vec3 observerPosition) {
        double x = target.centerX() - observerPosition.x;
        double y = target.centerY() - observerPosition.y;
        double z = target.centerZ() - observerPosition.z;
        return x * x + y * y + z * z;
    }

    /**
     * Resolves cached positions against the live world. Removed or unloaded
     * storages disappear immediately, while newly filled slots are visible
     * without waiting for the next radius scan.
     */
    private List<SlotReference> currentSlots() {
        List<SlotReference> result = new ArrayList<>();
        for (ResolvedStorage resolved : currentStorages()) {
            StorageAccess storage = resolved.storage();
            for (int slot = 0; slot < storage.getSlots(); slot++) {
                if (!storage.getStack(slot).isEmpty()) {
                    result.add(new SlotReference(
                            resolved.pos(),
                            storage,
                            slot,
                            resolved.distanceSquared()
                    ));
                }
            }
        }
        return result;
    }

    /**
     * Resolves cached storage positions and removes duplicate logical inventories.
     * Some multi-block/network storages expose the exact same handler from several
     * block positions; counting every position would duplicate their contents.
     */
    private List<ResolvedStorage> currentStorages() {
        if (level == null || storages.isEmpty()) {
            return List.of();
        }

        Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        List<ResolvedStorage> result = new ArrayList<>();
        for (StorageReference reference : storages) {
            StorageAccess storage = reference.currentStorage(level);
            if (storage == null || !seen.add(storage.identity())) {
                continue;
            }
            result.add(new ResolvedStorage(reference.pos(), storage, reference.distanceSquared()));
        }

        result.sort(Comparator
                .comparingInt(ResolvedStorage::distanceSquared)
                .thenComparingLong(value -> value.pos().asLong()));
        return result;
    }

    /**
     * Resolves a storage at a position without introducing a dependency on the
     * providing mod. Containers preserve the original Craft & Find behavior;
     * Forge item handlers are the generic compatibility fallback.
     */
    private static StorageAccess resolveStorage(Level level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) {
            return null;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) {
            return null;
        }

        // Prefer the physical Container view. In Forge 1.20.1 a vanilla double
        // chest capability represents the full 54-slot chest from *both* block
        // positions, so capability-first scanning counts the same inventory twice.
        if (blockEntity instanceof Container container) {
            return new ContainerStorageAccess(container);
        }

        // Non-Container modded storages still use the generic Forge capability.
        IItemHandler itemHandler = blockEntity
                .getCapability(ForgeCapabilities.ITEM_HANDLER, null)
                .resolve()
                .orElse(null);
        if (itemHandler != null) {
            return new ItemHandlerStorageAccess(itemHandler);
        }

        // Some mods expose automation access only from specific sides. Pick the
        // side that exposes the largest slot set instead of hard-coding a side.
        itemHandler = findSidedItemHandler(blockEntity);
        return itemHandler != null ? new ItemHandlerStorageAccess(itemHandler) : null;
    }

    private static IItemHandler findSidedItemHandler(BlockEntity blockEntity) {
        IItemHandler best = null;
        for (Direction direction : Direction.values()) {
            IItemHandler candidate = blockEntity
                    .getCapability(ForgeCapabilities.ITEM_HANDLER, direction)
                    .resolve()
                    .orElse(null);
            if (candidate != null && (best == null || candidate.getSlots() > best.getSlots())) {
                best = candidate;
            }
        }
        return best;
    }

    private static ItemStack copyWithCount(ItemStack stack, int count) {
        if (stack.isEmpty() || count <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack copy = stack.copy();
        copy.setCount(count);
        return copy;
    }

    private static boolean isStartedStack(ItemStack stack) {
        return stack.getCount() < stack.getMaxStackSize();
    }

    private static int saturatedAdd(int first, int second) {
        long result = (long) first + second;
        return result > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) result;
    }

    private interface StorageAccess {
        Object identity();

        int getSlots();

        ItemStack getStack(int slot);

        ItemStack extract(int slot, int amount, boolean simulate);

        ItemStack insert(ItemStack stack, boolean simulate);
    }

    private record ContainerStorageAccess(Container container) implements StorageAccess {
        @Override
        public Object identity() {
            return container;
        }

        @Override
        public int getSlots() {
            return container.getContainerSize();
        }

        @Override
        public ItemStack getStack(int slot) {
            return container.getItem(slot);
        }

        @Override
        public ItemStack extract(int slot, int amount, boolean simulate) {
            if (amount <= 0) {
                return ItemStack.EMPTY;
            }

            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }

            if (simulate) {
                return copyWithCount(stack, Math.min(amount, stack.getCount()));
            }

            ItemStack extracted = container.removeItem(slot, amount);
            if (!extracted.isEmpty()) {
                container.setChanged();
            }
            return extracted;
        }

        @Override
        public ItemStack insert(ItemStack stack, boolean simulate) {
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }

            ItemStack remaining = stack.copy();
            boolean changed = false;
            for (int pass = 0; pass < 2 && !remaining.isEmpty(); pass++) {
                for (int slot = 0; slot < container.getContainerSize() && !remaining.isEmpty(); slot++) {
                    ItemStack existing = container.getItem(slot);
                    boolean matching = !existing.isEmpty()
                            && ItemStack.isSameItemSameTags(existing, remaining);
                    if ((pass == 0 && !matching) || (pass == 1 && !existing.isEmpty())) {
                        continue;
                    }
                    if (!container.canPlaceItem(slot, remaining)) {
                        continue;
                    }

                    int limit = Math.min(container.getMaxStackSize(), remaining.getMaxStackSize());
                    int room = existing.isEmpty() ? limit : limit - existing.getCount();
                    if (room <= 0) {
                        continue;
                    }

                    int moved = Math.min(room, remaining.getCount());
                    if (!simulate) {
                        if (existing.isEmpty()) {
                            container.setItem(slot, copyWithCount(remaining, moved));
                        } else {
                            existing.grow(moved);
                        }
                        changed = true;
                    }
                    remaining.shrink(moved);
                }
            }

            if (changed) {
                container.setChanged();
            }
            return remaining;
        }
    }

    private record ItemHandlerStorageAccess(IItemHandler itemHandler) implements StorageAccess {
        @Override
        public Object identity() {
            return itemHandler;
        }

        @Override
        public int getSlots() {
            return itemHandler.getSlots();
        }

        @Override
        public ItemStack getStack(int slot) {
            return itemHandler.getStackInSlot(slot);
        }

        @Override
        public ItemStack extract(int slot, int amount, boolean simulate) {
            return itemHandler.extractItem(slot, amount, simulate);
        }

        @Override
        public ItemStack insert(ItemStack stack, boolean simulate) {
            ItemStack remaining = stack.copy();
            for (int slot = 0; slot < itemHandler.getSlots() && !remaining.isEmpty(); slot++) {
                remaining = itemHandler.insertItem(slot, remaining, simulate);
            }
            return remaining;
        }
    }

    private record HighlightArea(BlockPos minPos, BlockPos maxPos) {
        private HighlightArea {
            minPos = minPos.immutable();
            maxPos = maxPos.immutable();
        }

        static HighlightArea single(BlockPos pos) {
            return new HighlightArea(pos, pos);
        }

        static HighlightArea of(BlockPos first, BlockPos second) {
            return new HighlightArea(
                    new BlockPos(
                            Math.min(first.getX(), second.getX()),
                            Math.min(first.getY(), second.getY()),
                            Math.min(first.getZ(), second.getZ())
                    ),
                    new BlockPos(
                            Math.max(first.getX(), second.getX()),
                            Math.max(first.getY(), second.getY()),
                            Math.max(first.getZ(), second.getZ())
                    )
            );
        }
    }

    private record StorageReference(BlockPos pos, int distanceSquared) {
        StorageAccess currentStorage(Level level) {
            return resolveStorage(level, pos);
        }
    }

    private record ResolvedStorage(BlockPos pos, StorageAccess storage, int distanceSquared) {
    }

    private record SlotReference(BlockPos pos, StorageAccess storage, int slot, int distanceSquared) {
        ItemStack currentStack() {
            return storage.getStack(slot);
        }

        ItemStack extract(int amount, boolean simulate) {
            return storage.extract(slot, amount, simulate);
        }
    }

    private static final class MutableEntry {
        private final ItemStack stack;
        private int count;

        private MutableEntry(ItemStack stack, int count) {
            this.stack = stack;
            this.count = count;
        }
    }
}
