package net.minecraft.world.level.storage;

import com.mojang.serialization.Dynamic;
import com.mojang.serialization.OptionalDynamic;
import net.minecraft.SharedConstants;

public class LevelVersion {
    private final int levelDataVersion;
    private final long lastPlayed;
    private final String minecraftVersionName;
    private final DataVersion minecraftVersion;
    private final boolean snapshot;

    private LevelVersion(int levelDataVersion, long lastPlayed, String minecraftVersionName, int minecraftVersion, String series, boolean snapshot) {
        this.levelDataVersion = levelDataVersion;
        this.lastPlayed = lastPlayed;
        this.minecraftVersionName = minecraftVersionName;
        this.minecraftVersion = new DataVersion(minecraftVersion, series);
        this.snapshot = snapshot;
    }

    public static LevelVersion parse(Dynamic<?> nbt) {
        int i = nbt.get("version").asInt(0);
        long j = nbt.get("LastPlayed").asLong(0L);
        OptionalDynamic<?> optionaldynamic = nbt.get("Version");
        return optionaldynamic.result().isPresent()
            ? new LevelVersion(
                i,
                j,
                optionaldynamic.get("Name").asString(SharedConstants.getCurrentVersion().name()),
                optionaldynamic.get("Id").asInt(SharedConstants.getCurrentVersion().dataVersion().version()),
                optionaldynamic.get("Series").asString("main"),
                optionaldynamic.get("Snapshot").asBoolean(!SharedConstants.getCurrentVersion().stable())
            )
            : new LevelVersion(i, j, "", 0, "main", false);
    }

    public int levelDataVersion() {
        return this.levelDataVersion;
    }

    public long lastPlayed() {
        return this.lastPlayed;
    }

    public String minecraftVersionName() {
        return this.minecraftVersionName;
    }

    public DataVersion minecraftVersion() {
        return this.minecraftVersion;
    }

    public boolean snapshot() {
        return this.snapshot;
    }
}
