package com.overyourhead.craftandfind.common.recipe;

import com.overyourhead.craftandfind.common.storage.NearbyStorage;
import net.minecraft.recipebook.ServerPlaceRecipe;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;

/**
 * Storage-aware recipe placer that deliberately bypasses vanilla recipe-book
 * ownership. NERB keeps that book empty, but Craft & Find already validates the
 * requested recipe in StorageWorkbenchMenu before this class is invoked.
 */
public final class NearbyServerPlaceRecipe extends ServerPlaceRecipe<CraftingContainer> {
    private final NearbyStorage storage;

    public NearbyServerPlaceRecipe(CraftingMenu menu, NearbyStorage storage) {
        super(menu);
        this.storage = storage;
    }

    @Override
    public void recipeClicked(
            ServerPlayer player,
            @Nullable Recipe<CraftingContainer> recipe,
            boolean placeAll
    ) {
        if (recipe == null) {
            return;
        }

        this.inventory = player.getInventory();
        this.stackedContents.clear();
        this.inventory.fillStackedContents(this.stackedContents);
        this.menu.fillCraftSlotsStackedContents(this.stackedContents);

        if (!this.stackedContents.canCraft(recipe, null)) {
            this.inventory.setChanged();
            return;
        }

        this.handleRecipeClicked(recipe, placeAll);
        this.inventory.setChanged();
    }

    @Override
    public void addItemToSlot(
            Iterator<Integer> ingredients,
            int slotIndex,
            int maxAmount,
            int x,
            int y
    ) {
        if (!ingredients.hasNext()) {
            return;
        }

        ItemStack wanted = StackedContents.fromStackingIndex(ingredients.next());
        if (wanted.isEmpty()) {
            return;
        }

        Slot target = this.menu.getSlot(slotIndex);
        for (int placed = 0; placed < maxAmount; placed++) {
            int before = target.getItem().getCount();
            this.moveItemToGrid(target, wanted);
            int afterInventoryMove = target.getItem().getCount();

            if (afterInventoryMove > before) {
                continue;
            }

            int remaining = storage.moveToCraftingSlot(target, wanted, 1);
            if (remaining > 0 || target.getItem().getCount() <= before) {
                return;
            }
        }
    }
}
