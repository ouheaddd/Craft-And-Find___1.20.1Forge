package com.overyourhead.craftandfind.mixin.client;

import com.overyourhead.craftandfind.client.gui.StorageWorkbenchScreen;
import com.overyourhead.craftandfind.client.gui.workbench.WorkbenchLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookTabButton;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/** Keeps the category rail aligned with the custom recipe panel. */
@Mixin(RecipeBookComponent.class)
public abstract class RecipeBookTabsLayoutMixin {
    @Shadow @Final private List<RecipeBookTabButton> tabButtons;
    @Shadow private int xOffset;
    @Shadow private int width;
    @Shadow private int height;

    @Inject(method = "render", at = @At("HEAD"))
    private void craftandfind$layoutCategoryTabs(
            GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci
    ) {
        if (!(Minecraft.getInstance().screen instanceof StorageWorkbenchScreen)) return;

        if (tabButtons.isEmpty()) return;

        int panelX = (width - WorkbenchLayout.PANEL_WIDTH) / 2 - xOffset;
        int panelY = (height - WorkbenchLayout.PANEL_HEIGHT) / 2;

        // Vanilla 1.20.1 tabs are 35x27 and overlap the book by five pixels.
        // Keeping these bounds also preserves the category-icon scale/centering.
        int tabX = panelX - 30;
        int firstTabY = panelY + 3;

        for (int i = 0; i < tabButtons.size(); i++) {
            RecipeBookTabButton button = tabButtons.get(i);
            if (button == null) continue;
            button.setX(tabX);
            button.setY(firstTabY + i * 27);
            button.setWidth(35);
            button.setHeight(27);
            button.visible = true;
            button.active = true;
        }
    }
}
