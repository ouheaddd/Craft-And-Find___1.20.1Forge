package com.overyourhead.craftandfind.common.network;

import com.overyourhead.craftandfind.CraftAndFindMod;
import com.overyourhead.craftandfind.client.network.ClientPayloadHandler;
import com.overyourhead.craftandfind.common.menu.StorageWorkbenchMenu;
import com.overyourhead.craftandfind.common.network.payload.GhostRecipePayload;
import com.overyourhead.craftandfind.common.network.payload.HighlightPositionsPayload;
import com.overyourhead.craftandfind.common.network.payload.HighlightRequestPayload;
import com.overyourhead.craftandfind.common.network.payload.StorageSnapshotPayload;
import com.overyourhead.craftandfind.config.CraftAndFindServerConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

public final class CraftAndFindNetwork {
    private static final String PROTOCOL_VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(CraftAndFindMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );
    private static int nextMessageId;

    private CraftAndFindNetwork() {
    }

    public static void register() {
        CHANNEL.messageBuilder(HighlightRequestPayload.class, nextMessageId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(HighlightRequestPayload::encode)
                .decoder(HighlightRequestPayload::decode)
                .consumerMainThread(CraftAndFindNetwork::handleHighlightRequest)
                .add();

        CHANNEL.messageBuilder(StorageSnapshotPayload.class, nextMessageId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(StorageSnapshotPayload::encode)
                .decoder(StorageSnapshotPayload::decode)
                .consumerMainThread(CraftAndFindNetwork::handleStorageSnapshot)
                .add();

        CHANNEL.messageBuilder(HighlightPositionsPayload.class, nextMessageId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(HighlightPositionsPayload::encode)
                .decoder(HighlightPositionsPayload::decode)
                .consumerMainThread(CraftAndFindNetwork::handleHighlightPositions)
                .add();

        CHANNEL.messageBuilder(GhostRecipePayload.class, nextMessageId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(GhostRecipePayload::encode)
                .decoder(GhostRecipePayload::decode)
                .consumerMainThread(CraftAndFindNetwork::handleGhostRecipe)
                .add();
    }

    public static void sendToServer(Object payload) {
        CHANNEL.sendToServer(payload);
    }

    public static void sendToPlayer(ServerPlayer player, Object payload) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), payload);
    }

    private static void handleHighlightRequest(
            HighlightRequestPayload payload,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        try {
            ServerPlayer player = context.getSender();
            if (player == null || !(player.containerMenu instanceof StorageWorkbenchMenu menu)) {
                return;
            }
            if (menu.containerId != payload.containerId() || payload.stack().isEmpty()) {
                return;
            }

            var allTargets = menu.refreshStorage().highlightTargets(payload.stack(), player.position());
            int targetLimit = Math.min(allTargets.size(), CraftAndFindServerConfig.maxHighlightedContainers());
            sendToPlayer(player, new HighlightPositionsPayload(payload.stack(), allTargets.subList(0, targetLimit)));
        } finally {
            context.setPacketHandled(true);
        }
    }

    private static void handleStorageSnapshot(
            StorageSnapshotPayload payload,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPayloadHandler.handleStorageSnapshot(payload));
        context.setPacketHandled(true);
    }

    private static void handleHighlightPositions(
            HighlightPositionsPayload payload,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPayloadHandler.handleHighlightPositions(payload));
        context.setPacketHandled(true);
    }

    private static void handleGhostRecipe(
            GhostRecipePayload payload,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPayloadHandler.handleGhostRecipe(payload));
        context.setPacketHandled(true);
    }
}
