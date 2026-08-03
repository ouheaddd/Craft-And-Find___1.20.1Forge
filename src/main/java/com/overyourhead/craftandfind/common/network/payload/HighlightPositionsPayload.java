package com.overyourhead.craftandfind.common.network.payload;

import com.overyourhead.craftandfind.common.storage.StorageHighlightTarget;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record HighlightPositionsPayload(ItemStack stack, List<StorageHighlightTarget> targets) {
    private static final int MAX_TARGETS = 512;

    public HighlightPositionsPayload {
        stack = singleCopy(stack);

        Map<TargetKey, StorageHighlightTarget> unique = new LinkedHashMap<>();
        for (StorageHighlightTarget target : targets) {
            if (target.count() <= 0 || unique.size() >= MAX_TARGETS) {
                continue;
            }
            unique.putIfAbsent(new TargetKey(target.minPos(), target.maxPos()), target);
        }
        targets = List.copyOf(unique.values());
    }

    public static void encode(HighlightPositionsPayload payload, FriendlyByteBuf buffer) {
        buffer.writeItem(payload.stack());
        int size = Math.min(payload.targets().size(), MAX_TARGETS);
        buffer.writeVarInt(size);
        for (int index = 0; index < size; index++) {
            StorageHighlightTarget target = payload.targets().get(index);
            buffer.writeBlockPos(target.minPos());
            buffer.writeBlockPos(target.maxPos());
            buffer.writeVarInt(target.count());
        }
    }

    public static HighlightPositionsPayload decode(FriendlyByteBuf buffer) {
        ItemStack stack = singleCopy(buffer.readItem());
        int size = readBoundedSize(buffer, MAX_TARGETS, "highlight targets");
        List<StorageHighlightTarget> targets = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            BlockPos minPos = buffer.readBlockPos();
            BlockPos maxPos = buffer.readBlockPos();
            int count = buffer.readVarInt();
            if (count < 0) {
                throw new IllegalArgumentException("Invalid highlight item count: " + count);
            }
            targets.add(new StorageHighlightTarget(minPos, maxPos, count));
        }
        return new HighlightPositionsPayload(stack, targets);
    }

    private static int readBoundedSize(FriendlyByteBuf buffer, int maximum, String valueName) {
        int size = buffer.readVarInt();
        if (size < 0 || size > maximum) {
            throw new IllegalArgumentException("Invalid " + valueName + " count: " + size);
        }
        return size;
    }

    private static ItemStack singleCopy(ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack copy = stack.copy();
        copy.setCount(1);
        return copy;
    }

    private record TargetKey(BlockPos minPos, BlockPos maxPos) {
    }
}
