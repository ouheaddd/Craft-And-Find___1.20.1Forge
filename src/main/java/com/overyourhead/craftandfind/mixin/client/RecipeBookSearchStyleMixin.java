package com.overyourhead.craftandfind.mixin.client;

import com.overyourhead.craftandfind.client.gui.StorageWorkbenchScreen;
import com.overyourhead.craftandfind.client.gui.workbench.WorkbenchLayout;
import com.overyourhead.craftandfind.client.gui.workbench.WorkbenchTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StateSwitchingButton;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Aligns and themes the recipe-book search row without changing filter logic. */
@Mixin(RecipeBookComponent.class)
public abstract class RecipeBookSearchStyleMixin {
    @Shadow private EditBox searchBox;
    @Shadow protected StateSwitchingButton filterButton;
    @Shadow private int xOffset;
    @Shadow private int width;
    @Shadow private int height;

    @Inject(method = "render", at = @At("HEAD"))
    private void craftandfind$alignSearchRow(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick,
            CallbackInfo ci
    ) {
        if (!craftandfind$ownScreen() || searchBox == null || filterButton == null) {
            return;
        }

        int panelY = craftandfind$panelY();
        int rowX = craftandfind$rowX();

        searchBox.setBordered(false);
        searchBox.setX(rowX + WorkbenchLayout.SEARCH_TEXT_X);
        searchBox.setY(panelY + WorkbenchLayout.SEARCH_TEXT_Y
                + WorkbenchLayout.RECIPE_BOOK_SEARCH_TEXT_Y_OFFSET);
        searchBox.setWidth(WorkbenchLayout.SEARCH_TEXT_WIDTH);
        searchBox.setHeight(WorkbenchLayout.SEARCH_TEXT_HEIGHT);

        filterButton.setX(rowX + WorkbenchLayout.SORT_BUTTON_X);
        filterButton.setY(panelY + WorkbenchLayout.SORT_BUTTON_Y);
        filterButton.setWidth(WorkbenchLayout.SORT_BUTTON_WIDTH);
        filterButton.setHeight(WorkbenchLayout.SORT_BUTTON_HEIGHT);
    }

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/EditBox;render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"
            ),
            require = 0
    )
    private void craftandfind$renderThemedSearchBox(
            EditBox renderedSearchBox,
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        if (!craftandfind$ownScreen()) {
            renderedSearchBox.render(graphics, mouseX, mouseY, partialTick);
            return;
        }

        int panelY = craftandfind$panelY();
        int rowX = craftandfind$rowX();
        ResourceLocation background = renderedSearchBox.isFocused()
                ? WorkbenchTextures.RECIPE_SEARCH_BOX_FOCUSED
                : WorkbenchTextures.RECIPE_SEARCH_BOX;

        graphics.blit(background,
                rowX + WorkbenchLayout.SEARCH_BACKGROUND_X,
                panelY + WorkbenchLayout.SEARCH_BACKGROUND_Y,
                0, 0,
                WorkbenchLayout.SEARCH_BACKGROUND_WIDTH,
                WorkbenchLayout.SEARCH_BACKGROUND_HEIGHT,
                WorkbenchLayout.SEARCH_BACKGROUND_WIDTH,
                WorkbenchLayout.SEARCH_BACKGROUND_HEIGHT);
        graphics.blit(WorkbenchTextures.SEARCH_ICON,
                rowX + WorkbenchLayout.SEARCH_ICON_X,
                panelY + WorkbenchLayout.SEARCH_ICON_Y,
                0, 0, 16, 16, 16, 16);
        renderedSearchBox.render(graphics, mouseX, mouseY, partialTick);
    }

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/StateSwitchingButton;render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"
            ),
            require = 0
    )
    private void craftandfind$renderThemedFilterButton(
            StateSwitchingButton button,
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        if (!craftandfind$ownScreen() || button != filterButton) {
            button.render(graphics, mouseX, mouseY, partialTick);
            return;
        }

        // Vanilla semantics are preserved:
        // false = show all recipes (red cross), true = craftable only (green).
        ResourceLocation texture;
        if (button.isStateTriggered()) {
            texture = WorkbenchTextures.RECIPE_FILTER_BUTTON_SELECTED;
        } else if (button.isMouseOver(mouseX, mouseY)) {
            texture = WorkbenchTextures.RECIPE_FILTER_BUTTON_HOVERED;
        } else {
            texture = WorkbenchTextures.RECIPE_FILTER_BUTTON;
        }

        graphics.blit(texture, button.getX(), button.getY(), 0, 0,
                WorkbenchLayout.SORT_BUTTON_WIDTH,
                WorkbenchLayout.SORT_BUTTON_HEIGHT,
                WorkbenchLayout.SORT_BUTTON_WIDTH,
                WorkbenchLayout.SORT_BUTTON_HEIGHT);
    }

    @Unique
    private int craftandfind$rowX() {
        int panelX = (width - WorkbenchLayout.PANEL_WIDTH) / 2 - xOffset;
        return panelX + WorkbenchLayout.RECIPE_BOOK_ROW_X_OFFSET;
    }

    @Unique
    private int craftandfind$panelY() {
        return (height - WorkbenchLayout.PANEL_HEIGHT) / 2;
    }

    @Unique
    private static boolean craftandfind$ownScreen() {
        return Minecraft.getInstance().screen instanceof StorageWorkbenchScreen;
    }
}
