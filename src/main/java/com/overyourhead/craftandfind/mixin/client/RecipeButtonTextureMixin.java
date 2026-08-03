package com.overyourhead.craftandfind.mixin.client;

import com.overyourhead.craftandfind.client.gui.StorageWorkbenchScreen;
import com.overyourhead.craftandfind.client.gui.workbench.WorkbenchTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.recipebook.RecipeButton;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Minecraft 1.20.1 draws recipe cells from the old recipe_book.png atlas.
 * Keep vanilla's state calculation and replace only the resulting background
 * quad, so item animation, tooltips and click behavior remain untouched.
 */
@Mixin(RecipeButton.class)
public abstract class RecipeButtonTextureMixin {
    @Redirect(
            method = "renderWidget",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIII)V"
            ),
            require = 0
    )
    private void craftandfind$drawRecipeCell(
            GuiGraphics graphics,
            ResourceLocation vanillaTexture,
            int x,
            int y,
            int u,
            int v,
            int width,
            int height
    ) {
        if (!(Minecraft.getInstance().screen instanceof StorageWorkbenchScreen)
                || width != 25
                || height != 25) {
            graphics.blit(vanillaTexture, x, y, u, v, width, height);
            return;
        }

        // Vanilla 1.20.1 uses the X half for craftability and the Y half for
        // single/multiple recipe states. Reading the already-computed atlas
        // coordinates avoids duplicating recipe-book logic in this mixin.
        boolean craftable = u < 54;
        boolean multiple = v >= 231;
        ResourceLocation texture;
        if (craftable) {
            texture = multiple
                    ? WorkbenchTextures.RECIPE_SLOT_MANY_AVAILABLE_TEXTURE
                    : WorkbenchTextures.RECIPE_SLOT_AVAILABLE_TEXTURE;
        } else {
            texture = multiple
                    ? WorkbenchTextures.RECIPE_SLOT_MANY_UNAVAILABLE_TEXTURE
                    : WorkbenchTextures.RECIPE_SLOT_UNAVAILABLE_TEXTURE;
        }

        graphics.blit(texture, x, y, 0, 0, 25, 25, 25, 25);
    }
}
