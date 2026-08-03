package com.overyourhead.craftandfind.core;

import com.overyourhead.craftandfind.CraftAndFindMod;
import com.overyourhead.craftandfind.common.block.StorageWorkbenchBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, CraftAndFindMod.MOD_ID);

    public static final RegistryObject<StorageWorkbenchBlock> STORAGE_WORKBENCH = BLOCKS.register(
            "storage_workbench",
            () -> new StorageWorkbenchBlock(
                    BlockBehaviour.Properties.copy(Blocks.CRAFTING_TABLE)
                            .noOcclusion()
                            .pushReaction(PushReaction.BLOCK)
            )
    );

    private ModBlocks() {
    }
}
