package com.overyourhead.craftandfind.common.menu;

import com.overyourhead.craftandfind.CraftAndFindMod;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

/**
 * Stores a monotonically increasing generation for every destroyed workbench
 * position in one dimension. A player grid saved against an older generation
 * can never reappear after the workbench is broken and placed again.
 */
public final class WorkbenchGridVersionData extends SavedData {
    private static final String DATA_NAME = CraftAndFindMod.MOD_ID + "_workbench_grid_versions";
    private static final String VERSIONS_KEY = "Versions";

    private final Map<Long, Integer> versions = new HashMap<>();

    public WorkbenchGridVersionData() {
    }

    public static WorkbenchGridVersionData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                WorkbenchGridVersionData::load,
                WorkbenchGridVersionData::new,
                DATA_NAME
        );
    }

    public int version(BlockPos workbenchPos) {
        return versions.getOrDefault(workbenchPos.asLong(), 0);
    }

    public void invalidate(BlockPos workbenchPos) {
        long packedPos = workbenchPos.asLong();
        versions.put(packedPos, version(workbenchPos) + 1);
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag versionsTag = new CompoundTag();
        for (Map.Entry<Long, Integer> entry : versions.entrySet()) {
            versionsTag.putInt(Long.toString(entry.getKey()), entry.getValue());
        }
        tag.put(VERSIONS_KEY, versionsTag);
        return tag;
    }

    private static WorkbenchGridVersionData load(CompoundTag tag) {
        WorkbenchGridVersionData data = new WorkbenchGridVersionData();
        if (!tag.contains(VERSIONS_KEY, Tag.TAG_COMPOUND)) {
            return data;
        }

        CompoundTag versionsTag = tag.getCompound(VERSIONS_KEY);
        for (String key : versionsTag.getAllKeys()) {
            try {
                data.versions.put(Long.parseLong(key), versionsTag.getInt(key));
            } catch (NumberFormatException ignored) {
                // Ignore malformed data rather than making the world unloadable.
            }
        }
        return data;
    }
}
