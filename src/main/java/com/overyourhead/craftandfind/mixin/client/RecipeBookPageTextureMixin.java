package com.overyourhead.craftandfind.mixin.client;

import com.overyourhead.craftandfind.client.gui.StorageWorkbenchScreen;
import com.overyourhead.craftandfind.client.gui.workbench.WorkbenchTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.StateSwitchingButton;
import net.minecraft.client.gui.screens.recipebook.RecipeBookPage;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws Craft & Find's old-atlas-compatible next/previous page controls. */
@Mixin(RecipeBookPage.class)
public abstract class RecipeBookPageTextureMixin {
    @Shadow private StateSwitchingButton forwardButton;
    @Shadow private StateSwitchingButton backButton;

    @Inject(method = "render", at = @At("TAIL"))
    private void craftandfind$drawPageButtons(
            GuiGraphics graphics,
            int x,
            int y,
            int mouseX,
            int mouseY,
            float partialTick,
            CallbackInfo ci
    ) {
        if (!(Minecraft.getInstance().screen instanceof StorageWorkbenchScreen)) {
            return;
        }

        craftandfind$drawPageButton(
                graphics,
                backButton,
                WorkbenchTextures.PAGE_PREVIOUS_TEXTURE,
                WorkbenchTextures.PAGE_PREVIOUS_HOVERED_TEXTURE
        );
        craftandfind$drawPageButton(
                graphics,
                forwardButton,
                WorkbenchTextures.PAGE_NEXT_TEXTURE,
                WorkbenchTextures.PAGE_NEXT_HOVERED_TEXTURE
        );
    }

    private static void craftandfind$drawPageButton(
            GuiGraphics graphics,
            StateSwitchingButton button,
            ResourceLocation normal,
            ResourceLocation hovered
    ) {
        if (button == null || !button.visible) {
            return;
        }

        ResourceLocation texture = button.isHoveredOrFocused() ? hovered : normal;
        graphics.blit(texture, button.getX(), button.getY(), 0, 0, 20, 18, 20, 18);
    }
}
