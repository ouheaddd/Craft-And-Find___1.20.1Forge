package com.overyourhead.craftandfind;

import com.overyourhead.craftandfind.common.event.CommonEvents;
import com.overyourhead.craftandfind.common.network.CraftAndFindNetwork;
import com.overyourhead.craftandfind.config.CraftAndFindClientConfig;
import com.overyourhead.craftandfind.config.CraftAndFindServerConfig;
import com.overyourhead.craftandfind.core.ModBlocks;
import com.overyourhead.craftandfind.core.ModItems;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(CraftAndFindMod.MOD_ID)
public final class CraftAndFindMod {
    public static final String MOD_ID = "craftandfind";

    public CraftAndFindMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);

        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, CraftAndFindClientConfig.SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, CraftAndFindServerConfig.SPEC);

        modBus.addListener(this::addCreativeTabContents);
        MinecraftForge.EVENT_BUS.addListener(CommonEvents::onPlayerClone);

        CraftAndFindNetwork.register();
    }

    private void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (!CreativeModeTabs.FUNCTIONAL_BLOCKS.equals(event.getTabKey())) {
            return;
        }

        event.getEntries().putAfter(
                Blocks.ENCHANTING_TABLE.asItem().getDefaultInstance(),
                ModItems.STORAGE_WORKBENCH_ITEM.get().getDefaultInstance(),
                CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS
        );
    }
}
