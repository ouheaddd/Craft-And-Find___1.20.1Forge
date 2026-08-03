package com.overyourhead.craftandfind.mixin.client;

import com.overyourhead.craftandfind.client.gui.StorageWorkbenchScreen;
import com.overyourhead.craftandfind.client.gui.workbench.WorkbenchTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.StateSwitchingButton;
import net.minecraft.client.gui.screens.recipebook.RecipeBookTabButton;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces only the vanilla tab frame. RecipeBookTabButton itself continues
 * afterwards and renders/scales the category icon with its normal animation.
 */
@Mixin(StateSwitchingButton.class)
public abstract class RecipeBookTabButtonTextureMixin {
    @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
    private void craftandfind$drawRecipeCategoryFrame(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick,
            CallbackInfo ci
    ) {
        Object self = this;
        if (!(self instanceof RecipeBookTabButton)
                || !(Minecraft.getInstance().screen instanceof StorageWorkbenchScreen)) {
            return;
        }

        StateSwitchingButton button = (StateSwitchingButton) self;
        ResourceLocation texture = button.isStateTriggered()
                ? WorkbenchTextures.CATEGORY_TAB_SELECTED_TEXTURE
                : WorkbenchTextures.CATEGORY_TAB_TEXTURE;

        // These are standalone 25x25 PNGs, not a 256x256 legacy atlas.
        graphics.blit(texture, button.getX(), button.getY(), 0, 0, 25, 25, 25, 25);
        ci.cancel();
    }
}
