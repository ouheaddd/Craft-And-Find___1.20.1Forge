package com.overyourhead.craftandfind.core;

import com.overyourhead.craftandfind.CraftAndFindMod;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, CraftAndFindMod.MOD_ID);

    public static final RegistryObject<BlockItem> STORAGE_WORKBENCH_ITEM = ITEMS.register(
            "storage_workbench",
            () -> new BlockItem(ModBlocks.STORAGE_WORKBENCH.get(), new Item.Properties())
    );

    private ModItems() {
    }
}
