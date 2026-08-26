package com.overyourhead.craftandfind.client.gui;

import com.overyourhead.craftandfind.client.ClientStorageState;
import com.overyourhead.craftandfind.client.RecipeViewerHooks;
import com.overyourhead.craftandfind.client.gui.workbench.WorkbenchLayout;
import com.overyourhead.craftandfind.client.gui.workbench.WorkbenchTextures;
import com.overyourhead.craftandfind.common.storage.StorageItemEntry;
import com.overyourhead.craftandfind.mixin.client.GhostIngredientAccessor;
import com.overyourhead.craftandfind.mixin.client.RecipeBookComponentAccessor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.recipebook.GhostRecipe;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class StorageWorkbenchScreen extends CraftingScreen {
    private static boolean rememberedRecipeTabActive;

    private StoragePanel storagePanel;
    private WorkbenchRecipePanel recipePanel;
    private WorkbenchIconButton compassButton;
    private WorkbenchIconButton customRecipeButton;
    private ImageButton vanillaRecipeBookButton;
    private boolean recipeTabActive = rememberedRecipeTabActive;
    private int lastMouseX;
    private int lastMouseY;
    private ItemStack ghostResult = ItemStack.EMPTY;
    private final List<GhostPreviewIngredient> ghostPreviewIngredients = new ArrayList<>();

    public StorageWorkbenchScreen(CraftingMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = WorkbenchLayout.MAIN_WIDTH;
        imageHeight = WorkbenchLayout.MAIN_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();

        for (GuiEventListener child : children()) {
            if (child instanceof ImageButton imageButton
                    && imageButton.getWidth() == 20
                    && imageButton.getHeight() == 18) {
                vanillaRecipeBookButton = imageButton;
                vanillaRecipeBookButton.visible = false;
                vanillaRecipeBookButton.active = false;
                break;
            }
        }

        // Craft & Find owns its recipe panel. Keep Minecraft's recipe book hidden
        // even if another mod removes, redirects or disables the vanilla book.
        setRecipeBookVisible(false);

        storagePanel = new StoragePanel(font, menu.containerId, this::onClose);
        addWidget(storagePanel.searchBox());
        recipePanel = new WorkbenchRecipePanel(minecraft, font, menu);
        addWidget(recipePanel.searchBox());

        compassButton = addRenderableWidget(new WorkbenchIconButton(
                0,
                0,
                WorkbenchTextures.COMPASS_ICON,
                Component.translatable("gui.craftandfind.open_storage"),
                () -> storagePanel != null && storagePanel.isOpen(),
                this::toggleStoragePanel
        ));
        customRecipeButton = addRenderableWidget(new WorkbenchIconButton(
                0,
                0,
                WorkbenchTextures.RECIPE_BOOK_ICON,
                Component.translatable("gui.craftandfind.open_recipes"),
                () -> recipePanel != null && recipePanel.isOpen(),
                this::toggleRecipeBook
        ));
        applyActiveTabState();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        blitPart(graphics, WorkbenchTextures.MAIN_PANEL, leftPos, topPos, imageWidth, imageHeight);
        blitPart(
                graphics,
                WorkbenchTextures.CRAFTING_ARROW,
                leftPos + WorkbenchLayout.CRAFTING_ARROW_X,
                topPos + WorkbenchLayout.CRAFTING_ARROW_Y,
                WorkbenchLayout.CRAFTING_ARROW_WIDTH,
                WorkbenchLayout.CRAFTING_ARROW_HEIGHT
        );

        for (int slotIndex = 0; slotIndex < menu.slots.size(); slotIndex++) {
            Slot slot = menu.slots.get(slotIndex);
            if (slotIndex == 0) {
                blitPart(
                        graphics,
                        WorkbenchTextures.RESULT_SLOT,
                        leftPos + slot.x
                                - WorkbenchLayout.RESULT_SLOT_OFFSET
                                + WorkbenchLayout.RESULT_SLOT_X_ADJUST,
                        topPos + slot.y
                                - WorkbenchLayout.RESULT_SLOT_OFFSET
                                + WorkbenchLayout.RESULT_SLOT_Y_ADJUST,
                        WorkbenchLayout.RESULT_SLOT_SIZE,
                        WorkbenchLayout.RESULT_SLOT_SIZE
                );
                continue;
            }

            ResourceLocation slotTexture = slotIndex <= 9
                    ? WorkbenchTextures.CRAFTING_SLOT
                    : WorkbenchTextures.INVENTORY_SLOT;
            blitPart(
                    graphics,
                    slotTexture,
                    leftPos + slot.x - WorkbenchLayout.NORMAL_SLOT_OFFSET,
                    topPos + slot.y - WorkbenchLayout.NORMAL_SLOT_OFFSET,
                    WorkbenchLayout.NORMAL_SLOT_SIZE,
                    WorkbenchLayout.NORMAL_SLOT_SIZE
            );
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(
                font,
                title,
                titleLabelX + WorkbenchLayout.CRAFTING_LABEL_X_OFFSET,
                titleLabelY,
                WorkbenchLayout.LABEL_COLOR,
                true
        );
        graphics.drawString(
                font,
                playerInventoryTitle,
                inventoryLabelX,
                inventoryLabelY,
                WorkbenchLayout.LABEL_COLOR,
                true
        );
    }

    private static void blitPart(
            GuiGraphics graphics,
            ResourceLocation texture,
            int x,
            int y,
            int width,
            int height
    ) {
        graphics.blit(texture, x, y, 0, 0, width, height, width, height);
    }

    @Override
    public void containerTick() {
        super.containerTick();
        if (recipePanel != null) {
            recipePanel.tick();
        }
        updateCustomPositions();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        updateCustomPositions();
        super.render(graphics, mouseX, mouseY, partialTick);
        renderPreciseGhostPreview(graphics);
        storagePanel.render(graphics, mouseX, mouseY, partialTick);
        recipePanel.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (storagePanel.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (recipePanel.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (storagePanel.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (storagePanel.mouseReleased(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (storagePanel.mouseScrolled(mouseX, mouseY, delta)) {
            return true;
        }
        if (recipePanel.mouseScrolled(mouseX, mouseY, delta)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }

        if (storagePanel.isOpen() && storagePanel.searchBox().isFocused()) {
            storagePanel.searchBox().keyPressed(keyCode, scanCode, modifiers);
            return true;
        }

        // Recipe tiles keep normal left-click crafting. JEI is invoked only from
        // its keyboard recipe/uses bindings (R/U by default).
        if (recipePanel != null && recipePanel.isOpen() && !recipePanel.searchBox().isFocused()) {
            ItemStack hoveredRecipe = recipePanel.itemUnderMouse(lastMouseX, lastMouseY);
            if (RecipeViewerHooks.handleRecipePanelKey(hoveredRecipe, keyCode, scanCode)) {
                return true;
            }
        }

        if (recipePanel != null && recipePanel.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (storagePanel.isOpen() && storagePanel.searchBox().charTyped(codePoint, modifiers)) {
            return true;
        }
        if (recipePanel != null && recipePanel.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public void onClose() {
        ClientStorageState.clear(menu.containerId);
        super.onClose();
    }

    @Override
    public void removed() {
        // JEI/EMI temporarily replace this screen when opening Recipes/Uses.
        // Keep the snapshot while the same crafting container remains active.
        if (minecraft == null
                || minecraft.player == null
                || minecraft.player.containerMenu.containerId != menu.containerId) {
            ClientStorageState.clear(menu.containerId);
        }
        super.removed();
    }

    public boolean isStoragePanelOpen() {
        return storagePanel != null && storagePanel.isOpen();
    }

    public Rect2i storagePanelArea() {
        if (storagePanel == null) {
            return new Rect2i(0, 0, 0, 0);
        }
        return new Rect2i(storagePanel.left(), storagePanel.top(), StoragePanel.WIDTH, StoragePanel.HEIGHT);
    }

    public boolean isRecipePanelOpen() {
        return recipePanel != null && recipePanel.isOpen();
    }

    public Rect2i recipePanelArea() {
        if (recipePanel == null) {
            return new Rect2i(0, 0, 0, 0);
        }
        return new Rect2i(recipePanel.left(), recipePanel.top(), WorkbenchRecipePanel.WIDTH, WorkbenchRecipePanel.HEIGHT);
    }

    public ItemStack storageItemUnderMouse(double mouseX, double mouseY) {
        if (storagePanel == null) {
            return ItemStack.EMPTY;
        }
        return storagePanel.itemUnderMouse(mouseX, mouseY);
    }

    public ItemStack recipeItemUnderMouse(double mouseX, double mouseY) {
        if (recipePanel == null) {
            return ItemStack.EMPTY;
        }
        return recipePanel.itemUnderMouse(mouseX, mouseY);
    }

    public void onStorageUpdated() {
        if (storagePanel != null) {
            storagePanel.onStorageUpdated();
        }
        if (recipePanel != null) {
            recipePanel.onStorageUpdated();
        }

        // Keep the hidden vanilla component valid only for calculating exact
        // shaped/shapeless ghost positions. Its recipe collections are not used.
        super.recipesUpdated();
    }

    @Override
    public void recipesUpdated() {
        super.recipesUpdated();
        if (recipePanel != null) {
            recipePanel.recipesUpdated();
        }
    }

    public void clearGhostRecipe() {
        ((RecipeBookComponentAccessor) (Object) getRecipeBookComponent())
                .craftandfind$getGhostRecipe()
                .clear();
        ghostResult = ItemStack.EMPTY;
        ghostPreviewIngredients.clear();
    }

    /** Draws a precise preview for an unavailable recipe. */
    public void showGhostRecipe(ResourceLocation recipeId) {
        if (minecraft == null || minecraft.level == null) {
            return;
        }

        clearGhostRecipe();

        // Never draw a ghost over persistent real ingredients.
        for (int slotIndex = 1; slotIndex <= 9; slotIndex++) {
            if (!menu.getSlot(slotIndex).getItem().isEmpty()) {
                return;
            }
        }

        minecraft.level.getRecipeManager().byKey(recipeId).ifPresent(recipe -> {
            ghostResult = recipe.getResultItem(minecraft.level.registryAccess()).copy();

            // Vanilla still knows the exact shaped/shapeless coordinates. Borrow
            // only that placement calculation, copy it, then clear vanilla's
            // all-red renderer and draw the preview ourselves.
            getRecipeBookComponent().setupGhostRecipe(recipe, menu.slots);
            GhostRecipe vanillaGhost = ((RecipeBookComponentAccessor) (Object) getRecipeBookComponent())
                    .craftandfind$getGhostRecipe();
            Slot resultSlot = menu.getSlot(0);

            for (int index = 0; index < vanillaGhost.size(); index++) {
                GhostRecipe.GhostIngredient ghost = vanillaGhost.get(index);
                if (ghost.getX() == resultSlot.x && ghost.getY() == resultSlot.y) {
                    continue;
                }

                Ingredient ingredient = ((GhostIngredientAccessor) (Object) ghost)
                        .craftandfind$getIngredient();
                if (ingredient.getItems().length > 0) {
                    ghostPreviewIngredients.add(new GhostPreviewIngredient(
                            ingredient,
                            ghost.getX(),
                            ghost.getY()
                    ));
                }
            }

            vanillaGhost.clear();
        });
    }

    /**
     * Only genuinely missing inputs are red. Available ingredients stay normal.
     * The output gets a red backing too because the preview itself is unavailable.
     */
    private void renderPreciseGhostPreview(GuiGraphics graphics) {
        if (ghostResult.isEmpty() && ghostPreviewIngredients.isEmpty()) {
            return;
        }

        ItemStack[] matchedStacks = matchAvailableGhostIngredients();
        for (int index = 0; index < ghostPreviewIngredients.size(); index++) {
            GhostPreviewIngredient preview = ghostPreviewIngredients.get(index);
            ItemStack display = matchedStacks[index];
            boolean missing = display.isEmpty();

            if (missing) {
                ItemStack[] choices = preview.ingredient().getItems();
                if (choices.length > 0) {
                    display = copyWithCount(choices[0], 1);
                }
            }
            if (display.isEmpty()) {
                continue;
            }

            int slotX = leftPos + preview.x();
            int slotY = topPos + preview.y();
            if (missing) {
                graphics.fill(slotX, slotY, slotX + 16, slotY + 16, 0x66FF0000);
            }
            graphics.renderItem(display, slotX, slotY);
        }

        if (!ghostResult.isEmpty()) {
            Slot resultSlot = menu.getSlot(0);
            int slotX = leftPos + resultSlot.x;
            int slotY = topPos + resultSlot.y;
            graphics.fill(slotX, slotY, slotX + 16, slotY + 16, 0x66FF0000);
            graphics.renderItem(ghostResult, slotX, slotY);
            graphics.renderItemDecorations(font, ghostResult, slotX, slotY, null);
        }
    }

    private ItemStack[] matchAvailableGhostIngredients() {
        int ingredientCount = ghostPreviewIngredients.size();
        ItemStack[] matched = new ItemStack[ingredientCount];
        Arrays.fill(matched, ItemStack.EMPTY);
        if (ingredientCount == 0 || minecraft == null || minecraft.player == null) {
            return matched;
        }

        List<ItemStack> units = new ArrayList<>();
        for (ItemStack stack : minecraft.player.getInventory().items) {
            addGhostUnits(units, stack, stack.getCount());
        }
        for (int slotIndex = 1; slotIndex <= 9 && slotIndex < menu.slots.size(); slotIndex++) {
            ItemStack stack = menu.getSlot(slotIndex).getItem();
            addGhostUnits(units, stack, stack.getCount());
        }
        for (StorageItemEntry entry : ClientStorageState.entries(menu.containerId)) {
            addGhostUnits(units, entry.stack(), entry.count());
        }

        int[] unitOwner = new int[units.size()];
        Arrays.fill(unitOwner, -1);
        int[] ingredientUnit = new int[ingredientCount];
        Arrays.fill(ingredientUnit, -1);

        for (int ingredientIndex = 0; ingredientIndex < ingredientCount; ingredientIndex++) {
            boolean[] visited = new boolean[units.size()];
            tryMatchGhostIngredient(ingredientIndex, units, unitOwner, ingredientUnit, visited);
        }

        for (int ingredientIndex = 0; ingredientIndex < ingredientCount; ingredientIndex++) {
            int unitIndex = ingredientUnit[ingredientIndex];
            if (unitIndex >= 0) {
                matched[ingredientIndex] = copyWithCount(units.get(unitIndex), 1);
            }
        }
        return matched;
    }

    private boolean tryMatchGhostIngredient(
            int ingredientIndex,
            List<ItemStack> units,
            int[] unitOwner,
            int[] ingredientUnit,
            boolean[] visited
    ) {
        Ingredient ingredient = ghostPreviewIngredients.get(ingredientIndex).ingredient();
        for (int unitIndex = 0; unitIndex < units.size(); unitIndex++) {
            if (visited[unitIndex] || !ingredient.test(units.get(unitIndex))) {
                continue;
            }
            visited[unitIndex] = true;

            int previousOwner = unitOwner[unitIndex];
            if (previousOwner == -1
                    || tryMatchGhostIngredient(previousOwner, units, unitOwner, ingredientUnit, visited)) {
                unitOwner[unitIndex] = ingredientIndex;
                ingredientUnit[ingredientIndex] = unitIndex;
                return true;
            }
        }
        return false;
    }

    private static void addGhostUnits(List<ItemStack> units, ItemStack stack, int count) {
        if (stack.isEmpty() || count <= 0) {
            return;
        }

        int amount = Math.min(count, 9);
        for (int index = 0; index < amount; index++) {
            units.add(copyWithCount(stack, 1));
        }
    }

    private static ItemStack copyWithCount(ItemStack stack, int count) {
        ItemStack copy = stack.copy();
        copy.setCount(count);
        return copy;
    }

    private record GhostPreviewIngredient(Ingredient ingredient, int x, int y) {
    }

    private void toggleStoragePanel() {
        if (!recipeTabActive && storagePanel.isOpen()) {
            return;
        }

        recipeTabActive = false;
        rememberedRecipeTabActive = false;
        applyActiveTabState();
    }

    private void toggleRecipeBook() {
        if (recipeTabActive && recipePanel != null && recipePanel.isOpen()) {
            return;
        }

        recipeTabActive = true;
        rememberedRecipeTabActive = true;
        applyActiveTabState();
    }

    private void applyActiveTabState() {
        // The vanilla component remains hidden permanently. It is retained only
        // as an internal shaped/shapeless ghost-position calculator.
        if (getRecipeBookComponent().isVisible()) {
            setRecipeBookVisible(false);
        }

        if (recipeTabActive) {
            storagePanel.setOpen(false);
            recipePanel.setOpen(true);
        } else {
            recipePanel.setOpen(false);
            storagePanel.setOpen(true);
        }

        leftPos = mainLeftForActiveTab();
        updateCustomPositions();
    }

    private void setRecipeBookVisible(boolean visible) {
        ((RecipeBookComponentAccessor) (Object) getRecipeBookComponent())
                .craftandfind$setVisible(visible);
    }

    private int mainLeftForActiveTab() {
        if (width < 379) {
            return (width - imageWidth) / 2;
        }
        if (!recipeTabActive) {
            return (width - imageWidth) / 2 + 77;
        }

        // Match the old themed recipe-book geometry exactly, including its panel gap.
        int recipePanelX = (width - WorkbenchLayout.PANEL_WIDTH) / 2 - 86;
        return recipePanelX + WorkbenchLayout.PANEL_WIDTH + WorkbenchLayout.PANEL_GAP;
    }

    private void updateCustomPositions() {
        if (storagePanel == null || recipePanel == null || compassButton == null || customRecipeButton == null) {
            return;
        }

        leftPos = mainLeftForActiveTab();

        int buttonX = leftPos + WorkbenchLayout.TAB_BUTTON_X;
        compassButton.setX(buttonX);
        compassButton.setY(topPos + WorkbenchLayout.TAB_COMPASS_Y);
        customRecipeButton.setX(buttonX);
        customRecipeButton.setY(topPos + WorkbenchLayout.TAB_RECIPE_Y);
        storagePanel.updatePosition(leftPos, width, height);
        recipePanel.updatePosition(leftPos, width, height);
    }
}
