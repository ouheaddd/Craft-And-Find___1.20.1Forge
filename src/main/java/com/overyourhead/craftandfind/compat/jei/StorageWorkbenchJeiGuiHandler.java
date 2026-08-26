package com.overyourhead.craftandfind.compat.jei;

import com.overyourhead.craftandfind.client.gui.StorageWorkbenchScreen;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.runtime.IClickableIngredient;
import mezz.jei.api.runtime.IIngredientManager;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

public final class StorageWorkbenchJeiGuiHandler implements IGuiContainerHandler<StorageWorkbenchScreen> {
    private final IIngredientManager ingredientManager;

    public StorageWorkbenchJeiGuiHandler(IIngredientManager ingredientManager) {
        this.ingredientManager = ingredientManager;
    }

    @Override
    public List<Rect2i> getGuiExtraAreas(StorageWorkbenchScreen screen) {
        if (!screen.isStoragePanelOpen()) {
            return List.of();
        }
        return List.of(screen.storagePanelArea());
    }

    @Override
    public Optional<IClickableIngredient<?>> getClickableIngredientUnderMouse(
            StorageWorkbenchScreen screen,
            double mouseX,
            double mouseY
    ) {
        ItemStack stack = screen.storageItemUnderMouse(mouseX, mouseY);
        if (stack.isEmpty()) {
            return Optional.empty();
        }

        Rect2i mouseArea = new Rect2i((int) mouseX, (int) mouseY, 1, 1);
        return ingredientManager
                .createClickableIngredient(VanillaTypes.ITEM_STACK, stack, mouseArea, true)
                .map(ingredient -> (IClickableIngredient<?>) ingredient);
    }
}
