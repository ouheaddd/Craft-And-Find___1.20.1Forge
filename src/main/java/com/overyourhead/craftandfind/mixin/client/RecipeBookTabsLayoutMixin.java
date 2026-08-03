package com.overyourhead.craftandfind.mixin.client;

import com.overyourhead.craftandfind.client.gui.StorageWorkbenchScreen;
import com.overyourhead.craftandfind.client.gui.workbench.WorkbenchLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookTabButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.List;

/** Applies the same 25x25 contiguous category rail used by the 1.21.1 build. */
@Mixin(RecipeBookComponent.class)
public abstract class RecipeBookTabsLayoutMixin {
    @Unique private Field craftandfind$tabButtonsField;

    @Inject(method = "render", at = @At("HEAD"))
    private void craftandfind$layoutCategoryTabs(
            GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci
    ) {
        if (!(Minecraft.getInstance().screen instanceof StorageWorkbenchScreen)) return;

        List<RecipeBookTabButton> buttons = craftandfind$getTabButtons();
        if (buttons == null || buttons.isEmpty()) return;

        Minecraft minecraft = Minecraft.getInstance();
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        int panelX = (screenWidth - WorkbenchLayout.PANEL_WIDTH) / 2 - 86;
        int panelY = (screenHeight - WorkbenchLayout.PANEL_HEIGHT) / 2;

        // Rightmost pixel touches the panel border; no artificial gap between tabs.
        int tabX = panelX - 24;
        int firstTabY = panelY + 3;

        for (int i = 0; i < buttons.size(); i++) {
            RecipeBookTabButton button = buttons.get(i);
            if (button == null) continue;
            button.setX(tabX);
            button.setY(firstTabY + i * 25);
            button.setWidth(25);
            button.setHeight(25);
            button.visible = true;
            button.active = true;
        }
    }

    @Unique
    @SuppressWarnings("unchecked")
    private List<RecipeBookTabButton> craftandfind$getTabButtons() {
        try {
            if (craftandfind$tabButtonsField == null) {
                Class<?> type = RecipeBookComponent.class;
                while (type != null && craftandfind$tabButtonsField == null) {
                    for (Field field : type.getDeclaredFields()) {
                        if (!List.class.isAssignableFrom(field.getType())) continue;
                        field.setAccessible(true);
                        Object value = field.get(this);
                        if (value instanceof List<?> list
                                && (list.isEmpty() || list.get(0) instanceof RecipeBookTabButton)) {
                            craftandfind$tabButtonsField = field;
                            break;
                        }
                    }
                    type = type.getSuperclass();
                }
            }
            if (craftandfind$tabButtonsField == null) return null;
            Object value = craftandfind$tabButtonsField.get(this);
            return value instanceof List<?> ? (List<RecipeBookTabButton>) value : null;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }
}
