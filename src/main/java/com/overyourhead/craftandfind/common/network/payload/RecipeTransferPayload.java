package com.overyourhead.craftandfind.common.network.payload;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record RecipeTransferPayload(int containerId, ResourceLocation recipeId, boolean placeAll) {
    public static void encode(RecipeTransferPayload payload, FriendlyByteBuf buffer) {
        buffer.writeVarInt(payload.containerId());
        buffer.writeResourceLocation(payload.recipeId());
        buffer.writeBoolean(payload.placeAll());
    }

    public static RecipeTransferPayload decode(FriendlyByteBuf buffer) {
        return new RecipeTransferPayload(
                buffer.readVarInt(),
                buffer.readResourceLocation(),
                buffer.readBoolean()
        );
    }
}
