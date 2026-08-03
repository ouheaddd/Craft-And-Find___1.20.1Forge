package com.overyourhead.craftandfind.common.network.payload;

import com.overyourhead.craftandfind.common.storage.StorageItemEntry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public record StorageSnapshotPayload(int containerId, List<StorageItemEntry> entries) {
    private static final int MAX_ENTRIES = 2048;

    public StorageSnapshotPayload {
        entries = List.copyOf(entries);
    }

    public static void encode(StorageSnapshotPayload payload, FriendlyByteBuf buffer) {
        buffer.writeVarInt(payload.containerId());
        int size = Math.min(payload.entries().size(), MAX_ENTRIES);
        buffer.writeVarInt(size);
        for (int index = 0; index < size; index++) {
            StorageItemEntry entry = payload.entries().get(index);
            buffer.writeItem(entry.stack());
            buffer.writeVarInt(entry.count());
        }
    }

    public static StorageSnapshotPayload decode(FriendlyByteBuf buffer) {
        int containerId = buffer.readVarInt();
        int size = readBoundedSize(buffer, MAX_ENTRIES, "storage entries");
        List<StorageItemEntry> entries = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            ItemStack stack = buffer.readItem();
            int count = buffer.readVarInt();
            if (!stack.isEmpty() && count > 0) {
                entries.add(new StorageItemEntry(stack, count));
            }
        }
        return new StorageSnapshotPayload(containerId, entries);
    }

    private static int readBoundedSize(FriendlyByteBuf buffer, int maximum, String valueName) {
        int size = buffer.readVarInt();
        if (size < 0 || size > maximum) {
            throw new IllegalArgumentException("Invalid " + valueName + " count: " + size);
        }
        return size;
    }
}
