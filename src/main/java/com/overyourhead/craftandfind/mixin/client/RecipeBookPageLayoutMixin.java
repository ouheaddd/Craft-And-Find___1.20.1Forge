package com.overyourhead.craftandfind.mixin.client;

import com.overyourhead.craftandfind.client.gui.StorageWorkbenchScreen;
import com.overyourhead.craftandfind.client.gui.workbench.WorkbenchLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.StateSwitchingButton;
import net.minecraft.client.gui.screens.recipebook.RecipeBookPage;
import net.minecraft.client.gui.screens.recipebook.RecipeButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Fine-tunes the recipe-grid and pager spacing inside the themed recipe book.
 */
@Mixin(RecipeBookPage.class)
public abstract class RecipeBookPageLayoutMixin {
    @Shadow private StateSwitchingButton forwardButton;
    @Shadow private StateSwitchingButton backButton;

    @Unique
    private Map<AbstractWidget, int[]> craftandfind$recipePositions;
    @Unique
    private Map<AbstractWidget, int[]> craftandfind$pagerPositions;

    @Inject(method = "init", at = @At("TAIL"))
    private void craftandfind$captureInitialLayout(CallbackInfo ci) {
        craftandfind$recipePositions = new IdentityHashMap<>();
        craftandfind$pagerPositions = new IdentityHashMap<>();
        craftandfind$applyLayout();
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void craftandfind$refreshLayout(
            GuiGraphics graphics,
            int x,
            int y,
            int mouseX,
            int mouseY,
            float partialTick,
            CallbackInfo ci
    ) {
        craftandfind$applyLayout();
    }

    @ModifyConstant(
            method = "render",
            constant = @Constant(intValue = 141),
            require = 0
    )
    private int craftandfind$movePageLabelDown(int vanillaYOffset) {
        return craftandfind$ownScreen()
                ? vanillaYOffset + WorkbenchLayout.RECIPE_BOOK_PAGE_TEXT_Y_OFFSET
                : vanillaYOffset;
    }

    @Unique
    private void craftandfind$applyLayout() {
        if (!craftandfind$ownScreen()) {
            return;
        }

        if (craftandfind$recipePositions == null) {
            craftandfind$recipePositions = new IdentityHashMap<>();
        }
        if (craftandfind$pagerPositions == null) {
            craftandfind$pagerPositions = new IdentityHashMap<>();
        }

        RecipeBookPage self = (RecipeBookPage) (Object) this;
        List<RecipeButton> recipeButtons = craftandfind$findRecipeButtons(self);
        List<AbstractWidget> pagerButtons = craftandfind$findPagerButtons(self);
        craftandfind$capturePositionsIfNeeded(recipeButtons, pagerButtons);
        craftandfind$applyPositions(recipeButtons, pagerButtons);
    }

    @Unique
    private void craftandfind$capturePositionsIfNeeded(
            List<RecipeButton> recipeButtons,
            List<AbstractWidget> pagerButtons
    ) {
        if (craftandfind$recipePositions.size() != recipeButtons.size()) {
            craftandfind$recipePositions.clear();
            for (RecipeButton button : recipeButtons) {
                craftandfind$recipePositions.put(button, new int[]{button.getX(), button.getY()});
            }
        }

        if (craftandfind$pagerPositions.size() != pagerButtons.size()) {
            craftandfind$pagerPositions.clear();
            for (AbstractWidget button : pagerButtons) {
                craftandfind$pagerPositions.put(button, new int[]{button.getX(), button.getY()});
            }
        }
    }

    @Unique
    private void craftandfind$applyPositions(
            List<RecipeButton> recipeButtons,
            List<AbstractWidget> pagerButtons
    ) {
        for (RecipeButton button : recipeButtons) {
            int[] position = craftandfind$recipePositions.get(button);
            if (position != null) {
                button.setX(position[0]);
                button.setY(position[1] + WorkbenchLayout.RECIPE_BOOK_BUTTONS_Y_OFFSET);
            }
        }

        for (AbstractWidget button : pagerButtons) {
            int[] position = craftandfind$pagerPositions.get(button);
            if (position != null) {
                // The old button was 12 pixels wide. Moving the 20-pixel back
                // button left by eight preserves its original inner/right edge.
                int xOffset = button == backButton ? -8 : 0;
                button.setX(position[0] + xOffset);
                button.setY(position[1] + WorkbenchLayout.RECIPE_BOOK_PAGE_CONTROLS_Y_OFFSET);
                if (button == backButton || button == forwardButton) {
                    button.setWidth(20);
                    button.setHeight(18);
                }
            }
        }
    }

    @Unique
    private static boolean craftandfind$ownScreen() {
        return Minecraft.getInstance().screen instanceof StorageWorkbenchScreen;
    }

    @Unique
    private static List<RecipeButton> craftandfind$findRecipeButtons(Object owner) {
        Set<RecipeButton> buttons = new LinkedHashSet<>();
        Class<?> type = owner.getClass();
        while (type != null) {
            for (Field field : type.getDeclaredFields()) {
                field.setAccessible(true);
                try {
                    Object value = field.get(owner);
                    if (value instanceof RecipeButton recipeButton) {
                        buttons.add(recipeButton);
                    } else if (value instanceof List<?> list) {
                        for (Object element : list) {
                            if (element instanceof RecipeButton recipeButton) {
                                buttons.add(recipeButton);
                            }
                        }
                    }
                } catch (IllegalAccessException ignored) {
                }
            }
            type = type.getSuperclass();
        }
        return new ArrayList<>(buttons);
    }

    @Unique
    private static List<AbstractWidget> craftandfind$findPagerButtons(Object owner) {
        Set<AbstractWidget> buttons = new LinkedHashSet<>();
        Class<?> type = owner.getClass();
        while (type != null) {
            for (Field field : type.getDeclaredFields()) {
                field.setAccessible(true);
                try {
                    Object value = field.get(owner);
                    if (value instanceof AbstractWidget widget && !(widget instanceof RecipeButton)) {
                        buttons.add(widget);
                    }
                } catch (IllegalAccessException ignored) {
                }
            }
            type = type.getSuperclass();
        }
        return new ArrayList<>(buttons);
    }
}
