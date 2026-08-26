package com.overyourhead.craftandfind.compat.emi;

import com.overyourhead.craftandfind.client.gui.StorageWorkbenchScreen;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.handler.EmiRecipeHandler;
import dev.emi.emi.api.widget.Bounds;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.MenuType;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

@EmiEntrypoint
public final class CraftAndFindEmiPlugin implements EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        registry.addStackProvider(StorageWorkbenchScreen.class, new StorageWorkbenchEmiStackProvider());

        registry.addExclusionArea(StorageWorkbenchScreen.class, (screen, consumer) -> {
            Rect2i area;
            if (screen.isStoragePanelOpen()) {
                area = screen.storagePanelArea();
            } else if (screen.isRecipePanelOpen()) {
                area = screen.recipePanelArea();
            } else {
                return;
            }
            consumer.accept(new Bounds(area.getX(), area.getY(), area.getWidth(), area.getHeight()));
        });

        StorageWorkbenchEmiRecipeHandler handler = new StorageWorkbenchEmiRecipeHandler();
        registry.addRecipeHandler(MenuType.CRAFTING, handler);

        // EMI's vanilla crafting handler is registered first for MenuType.CRAFTING.
        // The Storage Workbench deliberately uses that vanilla menu type, so put our
        // narrow handler in front. It reports support only while a Craft & Find
        // storage snapshot is active; normal crafting tables immediately fall
        // through to EMI's own handler.
        prioritizeCraftingHandler(handler);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void prioritizeCraftingHandler(EmiRecipeHandler<CraftingMenu> handler) {
        try {
            Class<?> fillerClass = Class.forName("dev.emi.emi.registry.EmiRecipeFiller");
            Field handlersField = fillerClass.getField("handlers");
            Object rawMap = handlersField.get(null);
            if (!(rawMap instanceof Map map)) {
                return;
            }

            Object rawList = map.get(MenuType.CRAFTING);
            if (!(rawList instanceof List list)) {
                return;
            }

            if (list.remove(handler)) {
                list.add(0, handler);
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // The stack provider/exclusion integration still works if a future EMI
            // version changes its internal handler list. Recipe fill then falls back
            // to EMI's vanilla behavior instead of crashing Craft & Find.
        }
    }
}
