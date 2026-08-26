package com.overyourhead.craftandfind.compat.jei;

import com.overyourhead.craftandfind.client.ClientStorageState;
import com.overyourhead.craftandfind.common.storage.StorageItemEntry;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.recipe.transfer.IRecipeTransferInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public final class StorageWorkbenchJeiTransferHandler
        implements IRecipeTransferHandler<CraftingMenu, CraftingRecipe> {
    private static final int FIRST_INPUT_SLOT = 1;
    private static final int INPUT_SLOT_COUNT = 9;
    private static final int FIRST_PLAYER_SLOT = 10;
    private static final int PLAYER_SLOT_COUNT = 36;

    private final IRecipeTransferHandlerHelper helper;
    private final IRecipeTransferHandler<CraftingMenu, CraftingRecipe> vanillaFallback;

    public StorageWorkbenchJeiTransferHandler(IRecipeTransferHandlerHelper helper) {
        this.helper = helper;

        IRecipeTransferInfo<CraftingMenu, CraftingRecipe> vanillaInfo = helper.createBasicRecipeTransferInfo(
                CraftingMenu.class,
                MenuType.CRAFTING,
                RecipeTypes.CRAFTING,
                FIRST_INPUT_SLOT,
                INPUT_SLOT_COUNT,
                FIRST_PLAYER_SLOT,
                PLAYER_SLOT_COUNT
        );
        this.vanillaFallback = helper.createUnregisteredRecipeTransferHandler(vanillaInfo);
    }

    @Override
    public Class<? extends CraftingMenu> getContainerClass() {
        return CraftingMenu.class;
    }

    @Override
    public Optional<MenuType<CraftingMenu>> getMenuType() {
        return Optional.of(MenuType.CRAFTING);
    }

    @Override
    public RecipeType<CraftingRecipe> getRecipeType() {
        return RecipeTypes.CRAFTING;
    }

    @Override
    @Nullable
    public IRecipeTransferError transferRecipe(
            CraftingMenu container,
            CraftingRecipe recipe,
            IRecipeSlotsView recipeSlots,
            Player player,
            boolean maxTransfer,
            boolean doTransfer
    ) {
        if (!isStorageWorkbench(container)) {
            return vanillaFallback.transferRecipe(
                    container,
                    recipe,
                    recipeSlots,
                    player,
                    maxTransfer,
                    doTransfer
            );
        }

        List<IRecipeSlotView> missingSlots = findMissingIngredientSlots(container, recipeSlots, player);
        if (!missingSlots.isEmpty()) {
            // Using JEI's missing-slot error is important here instead of a plain tooltip:
            // JEI will paint the recipe slots that cannot be satisfied red, just like vanilla JEI.
            return helper.createUserErrorForMissingSlots(
                    Component.literal("Missing ingredients in inventory or nearby storage"),
                    missingSlots
            );
        }

        if (!doTransfer) {
            return null;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gameMode == null) {
            return helper.createInternalError();
        }

        // Use the same vanilla recipe-book packet the workbench already handles.
        // The server-side menu is StorageWorkbenchMenu, so its handlePlacement
        // implementation pulls missing ingredients from NearbyStorage.
        minecraft.gameMode.handlePlaceRecipe(container.containerId, recipe, maxTransfer);
        return null;
    }

    private static boolean isStorageWorkbench(CraftingMenu container) {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null
                && minecraft.player.containerMenu == container
                && ClientStorageState.isActive(container.containerId);
    }

    /**
     * Finds recipe inputs that cannot be satisfied by the player's normal inventory,
     * the current crafting grid, or Craft & Find's nearby-storage snapshot.
     *
     * This uses a small bipartite matching pass instead of a greedy check, so recipes
     * with tag/alternative ingredients do not get incorrect red slots just because
     * one ingredient can satisfy more than one recipe position.
     */
    private static List<IRecipeSlotView> findMissingIngredientSlots(
            CraftingMenu container,
            IRecipeSlotsView recipeSlots,
            Player player
    ) {
        List<IRecipeSlotView> inputSlots = recipeSlots.getSlotViews(RecipeIngredientRole.INPUT)
                .stream()
                .filter(slot -> !slot.isEmpty())
                .toList();

        if (inputSlots.isEmpty()) {
            return List.of();
        }

        List<ItemStack> availableUnits = collectAvailableUnits(container, player, inputSlots.size());
        int[] unitOwners = new int[availableUnits.size()];
        Arrays.fill(unitOwners, -1);

        for (int inputIndex = 0; inputIndex < inputSlots.size(); inputIndex++) {
            boolean[] visitedUnits = new boolean[availableUnits.size()];
            tryAssignInput(inputIndex, inputSlots, availableUnits, unitOwners, visitedUnits);
        }

        boolean[] matchedInputs = new boolean[inputSlots.size()];
        for (int owner : unitOwners) {
            if (owner >= 0) {
                matchedInputs[owner] = true;
            }
        }

        List<IRecipeSlotView> missing = new ArrayList<>();
        for (int inputIndex = 0; inputIndex < inputSlots.size(); inputIndex++) {
            if (!matchedInputs[inputIndex]) {
                missing.add(inputSlots.get(inputIndex));
            }
        }
        return missing;
    }

    private static List<ItemStack> collectAvailableUnits(
            CraftingMenu container,
            Player player,
            int maxNeededPerStack
    ) {
        List<ItemStack> units = new ArrayList<>();

        // JEI's normal crafting transfer uses the 36 main inventory/hotbar slots.
        for (ItemStack stack : player.getInventory().items) {
            addUnits(units, stack, stack.getCount(), maxNeededPerStack);
        }

        // Existing ingredients in the 3x3 crafting grid are also reusable.
        for (int slotIndex = FIRST_INPUT_SLOT; slotIndex < FIRST_INPUT_SLOT + INPUT_SLOT_COUNT; slotIndex++) {
            ItemStack stack = container.getSlot(slotIndex).getItem();
            addUnits(units, stack, stack.getCount(), maxNeededPerStack);
        }

        // Finally include the exact same nearby-storage snapshot that the workbench UI uses.
        for (StorageItemEntry entry : ClientStorageState.entries(container.containerId)) {
            addUnits(units, entry.stack(), entry.count(), maxNeededPerStack);
        }

        return units;
    }

    private static void addUnits(
            List<ItemStack> units,
            ItemStack stack,
            int count,
            int maxNeededPerStack
    ) {
        if (stack.isEmpty() || count <= 0) {
            return;
        }

        // A crafting grid needs at most one unit per occupied recipe slot, so there is
        // no reason to expand a stack of 64 into 64 temporary entries.
        int unitCount = Math.min(count, maxNeededPerStack);
        for (int i = 0; i < unitCount; i++) {
            ItemStack unit = stack.copy();
            unit.setCount(1);
            units.add(unit);
        }
    }

    private static boolean tryAssignInput(
            int inputIndex,
            List<IRecipeSlotView> inputSlots,
            List<ItemStack> availableUnits,
            int[] unitOwners,
            boolean[] visitedUnits
    ) {
        IRecipeSlotView inputSlot = inputSlots.get(inputIndex);

        for (int unitIndex = 0; unitIndex < availableUnits.size(); unitIndex++) {
            if (visitedUnits[unitIndex]) {
                continue;
            }

            ItemStack available = availableUnits.get(unitIndex);
            if (!slotAccepts(inputSlot, available)) {
                continue;
            }

            visitedUnits[unitIndex] = true;
            int previousOwner = unitOwners[unitIndex];
            if (previousOwner == -1
                    || tryAssignInput(previousOwner, inputSlots, availableUnits, unitOwners, visitedUnits)) {
                unitOwners[unitIndex] = inputIndex;
                return true;
            }
        }

        return false;
    }

    private static boolean slotAccepts(IRecipeSlotView inputSlot, ItemStack available) {
        return inputSlot.getItemStacks()
                .anyMatch(candidate -> ItemStack.isSameItemSameTags(candidate, available));
    }
}
