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
import org.spongepowered.asm.mixin.injection.Redirect;

/** Replaces the vanilla pager render with Craft & Find's 20x18 controls. */
@Mixin(RecipeBookPage.class)
public abstract class RecipeBookPageTextureMixin {
    @Shadow private StateSwitchingButton forwardButton;
    @Shadow private StateSwitchingButton backButton;

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/StateSwitchingButton;render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"
            )
    )
    private void craftandfind$drawPageButton(
            StateSwitchingButton button,
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        if (!(Minecraft.getInstance().screen instanceof StorageWorkbenchScreen)) {
            button.render(graphics, mouseX, mouseY, partialTick);
            return;
        }

        if (button == null || !button.visible) {
            return;
        }

        ResourceLocation normal;
        ResourceLocation hovered;
        if (button == backButton) {
            normal = WorkbenchTextures.PAGE_PREVIOUS_TEXTURE;
            hovered = WorkbenchTextures.PAGE_PREVIOUS_HOVERED_TEXTURE;
        } else if (button == forwardButton) {
            normal = WorkbenchTextures.PAGE_NEXT_TEXTURE;
            hovered = WorkbenchTextures.PAGE_NEXT_HOVERED_TEXTURE;
        } else {
            button.render(graphics, mouseX, mouseY, partialTick);
            return;
        }

        // AbstractWidget#render normally refreshes the cached hovered flag.
        // This mixin intentionally replaces that render call, so calculate the
        // hover state from the current mouse coordinates instead.
        boolean isHovered = button.isMouseOver(mouseX, mouseY) || button.isFocused();
        ResourceLocation texture = isHovered ? hovered : normal;
        graphics.blit(texture, button.getX(), button.getY(), 0, 0, 20, 18, 20, 18);
    }
}
