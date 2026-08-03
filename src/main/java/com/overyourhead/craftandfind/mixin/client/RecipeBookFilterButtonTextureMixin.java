package com.overyourhead.craftandfind.mixin.client;

import com.overyourhead.craftandfind.client.gui.StorageWorkbenchScreen;
import com.overyourhead.craftandfind.client.gui.workbench.WorkbenchLayout;
import com.overyourhead.craftandfind.client.gui.workbench.WorkbenchTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.StateSwitchingButton;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws the recipe filter with the original Craft & Find 20x18 sprites. */
@Mixin(StateSwitchingButton.class)
public abstract class RecipeBookFilterButtonTextureMixin {
    @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
    private void craftandfind$renderRecipeFilter(
            GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci
    ) {
        StateSwitchingButton self = (StateSwitchingButton) (Object) this;
        if (!(Minecraft.getInstance().screen instanceof StorageWorkbenchScreen)
                || self.getWidth() != WorkbenchLayout.SORT_BUTTON_WIDTH
                || self.getHeight() != WorkbenchLayout.SORT_BUTTON_HEIGHT) {
            return;
        }

        // 1.20.1's state meaning is opposite to the 1.21 WidgetSprites order:
        // triggered means "show all", whose source art is the red disabled filter.
        ResourceLocation texture;
        if (self.isStateTriggered()) {
            texture = self.isHoveredOrFocused()
                    ? WorkbenchTextures.RECIPE_FILTER_BUTTON_HOVERED
                    : WorkbenchTextures.RECIPE_FILTER_BUTTON;
        } else {
            texture = WorkbenchTextures.RECIPE_FILTER_BUTTON_SELECTED;
        }

        graphics.blit(
                texture,
                self.getX(), self.getY(),
                0, 0,
                WorkbenchLayout.SORT_BUTTON_WIDTH,
                WorkbenchLayout.SORT_BUTTON_HEIGHT,
                WorkbenchLayout.SORT_BUTTON_WIDTH,
                WorkbenchLayout.SORT_BUTTON_HEIGHT
        );
        ci.cancel();
    }
}
