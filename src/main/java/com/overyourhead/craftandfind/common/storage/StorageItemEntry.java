package com.overyourhead.craftandfind.common.storage;

import net.minecraft.world.item.ItemStack;

public record StorageItemEntry(ItemStack stack, int count) {
    public StorageItemEntry {
        if (stack.isEmpty()) {
            stack = ItemStack.EMPTY;
        } else {
            stack = stack.copy();
            stack.setCount(1);
        }
        count = Math.max(0, count);
    }
}
