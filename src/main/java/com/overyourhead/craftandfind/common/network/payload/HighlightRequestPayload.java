package com.overyourhead.craftandfind.common.network.payload;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

public record HighlightRequestPayload(int containerId, ItemStack stack) {
    public HighlightRequestPayload {
        stack = singleCopy(stack);
    }

    public static void encode(HighlightRequestPayload payload, FriendlyByteBuf buffer) {
        buffer.writeVarInt(payload.containerId());
        buffer.writeItem(payload.stack());
    }

    public static HighlightRequestPayload decode(FriendlyByteBuf buffer) {
        return new HighlightRequestPayload(buffer.readVarInt(), buffer.readItem());
    }

    private static ItemStack singleCopy(ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack copy = stack.copy();
        copy.setCount(1);
        return copy;
    }
}
