package com.overyourhead.craftandfind.mixin.client;

import com.overyourhead.craftandfind.client.gui.StorageWorkbenchScreen;
import com.overyourhead.craftandfind.client.gui.workbench.WorkbenchTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.recipebook.RecipeBookTabButton;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Fully owns category-tab rendering on the custom workbench. This avoids the
 * legacy 1.20.1 atlas path while keeping vanilla icon rendering and animation.
 */
@Mixin(RecipeBookTabButton.class)
public abstract class RecipeBookTabButtonTextureMixin {
    @Shadow private float animationTime;

    @Invoker("renderIcon")
    protected abstract void craftandfind$renderIcon(GuiGraphics graphics, ItemRenderer itemRenderer);

    @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
    private void craftandfind$drawRecipeCategoryFrame(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick,
            CallbackInfo ci
    ) {
        if (!(Minecraft.getInstance().screen instanceof StorageWorkbenchScreen)) {
            return;
        }

        RecipeBookTabButton button = (RecipeBookTabButton) (Object) this;
        boolean selected = button.isStateTriggered();
        ResourceLocation texture = selected
                ? WorkbenchTextures.CATEGORY_TAB_SELECTED_TEXTURE
                : WorkbenchTextures.CATEGORY_TAB_TEXTURE;

        boolean animated = animationTime > 0.0F;
        if (animated) {
            float scaleY = 1.0F + 0.1F * (float) Math.sin(animationTime / 15.0F * Math.PI);
            graphics.pose().pushPose();
            graphics.pose().translate(button.getX() + 8.0F, button.getY() + 12.0F, 0.0F);
            graphics.pose().scale(1.0F, scaleY, 1.0F);
            graphics.pose().translate(-(button.getX() + 8.0F), -(button.getY() + 12.0F), 0.0F);
        }

        int drawX = button.getX() - (selected ? 2 : 0);
        graphics.blit(
                texture,
                drawX,
                button.getY(),
                button.getWidth(),
                button.getHeight(),
                0.0F,
                0.0F,
                25,
                25,
                25,
                25
        );
        craftandfind$renderIcon(graphics, Minecraft.getInstance().getItemRenderer());

        if (animated) {
            graphics.pose().popPose();
            animationTime -= partialTick;
        }

        ci.cancel();
    }
}
