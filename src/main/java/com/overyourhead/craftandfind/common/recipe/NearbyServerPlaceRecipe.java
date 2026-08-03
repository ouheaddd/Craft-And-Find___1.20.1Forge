package com.overyourhead.craftandfind.common.recipe;

import com.overyourhead.craftandfind.common.storage.NearbyStorage;
import net.minecraft.recipebook.ServerPlaceRecipe;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.Iterator;

public final class NearbyServerPlaceRecipe extends ServerPlaceRecipe<CraftingContainer> {
    private final NearbyStorage storage;

    public NearbyServerPlaceRecipe(CraftingMenu menu, NearbyStorage storage) {
        super(menu);
        this.storage = storage;
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
