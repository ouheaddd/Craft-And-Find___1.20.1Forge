package com.overyourhead.craftandfind.mixin.client;

import com.overyourhead.craftandfind.client.gui.StorageWorkbenchScreen;
import com.overyourhead.craftandfind.client.gui.workbench.WorkbenchLayout;
import com.overyourhead.craftandfind.client.gui.workbench.WorkbenchTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Replaces only the recipe-book panel draw call on the custom workbench. */
@Mixin(RecipeBookComponent.class)
public abstract class RecipeBookComponentTextureMixin {
    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIII)V",
                    ordinal = 0
            ),
            require = 0
    )
    private void craftandfind$drawOwnRecipePanel(
            GuiGraphics graphics,
            ResourceLocation vanillaTexture,
            int x,
            int y,
            int u,
            int v,
            int width,
            int height
    ) {
        if (!(Minecraft.getInstance().screen instanceof StorageWorkbenchScreen)) {
            graphics.blit(vanillaTexture, x, y, u, v, width, height);
            return;
        }

        // Use the vanilla destination position and preserve its atlas UV.
        // The recipe-panel artwork starts at (1, 1), so forcing (0, 0) drops
        // its final right column and bottom row.
        graphics.blit(
                WorkbenchTextures.RECIPE_PANEL,
                x,
                y,
                u,
                v,
                WorkbenchLayout.PANEL_WIDTH,
                WorkbenchLayout.PANEL_HEIGHT,
                256,
                256
        );
    }
}
