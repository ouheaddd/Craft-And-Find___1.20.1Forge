package com.overyourhead.craftandfind.common.network.payload;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** Tells the client which unavailable recipe should be drawn as ghost items. */
public record GhostRecipePayload(int containerId, ResourceLocation recipeId) {
    public static void encode(GhostRecipePayload payload, FriendlyByteBuf buffer) {
        buffer.writeVarInt(payload.containerId());
        buffer.writeResourceLocation(payload.recipeId());
    }

    public static GhostRecipePayload decode(FriendlyByteBuf buffer) {
        return new GhostRecipePayload(buffer.readVarInt(), buffer.readResourceLocation());
    }
}
