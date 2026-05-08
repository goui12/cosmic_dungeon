package com.mojang.realmsclient.dto;

import com.google.gson.annotations.SerializedName;
import javax.annotation.Nullable;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.util.StringUtil;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RealmsWorldOptions extends ValueObject implements ReflectionBasedSerialization {
    @SerializedName("spawnProtection")
    public int spawnProtection = 0;
    @SerializedName("forceGameMode")
    public boolean forceGameMode = false;
    @SerializedName("difficulty")
    public int difficulty = 2;
    @SerializedName("gameMode")
    public int gameMode = 0;
    @SerializedName("slotName")
    private String slotName = "";
    @SerializedName("version")
    public String version = "";
    @SerializedName("compatibility")
    public RealmsServer.Compatibility compatibility = RealmsServer.Compatibility.UNVERIFIABLE;
    @SerializedName("worldTemplateId")
    public long templateId = -1L;
    @Nullable
    @SerializedName("worldTemplateImage")
    public String templateImage = null;
    public boolean empty;

    private RealmsWorldOptions() {
    }

    public RealmsWorldOptions(
        int spawnProtection, int difficulty, int gameMode, boolean forceGameMode, String slotName, String version, RealmsServer.Compatibility compatibility
    ) {
        this.spawnProtection = spawnProtection;
        this.difficulty = difficulty;
        this.gameMode = gameMode;
        this.forceGameMode = forceGameMode;
        this.slotName = slotName;
        this.version = version;
        this.compatibility = compatibility;
    }

    public static RealmsWorldOptions createDefaults() {
        return new RealmsWorldOptions();
    }

    public static RealmsWorldOptions createDefaultsWith(GameType gameMode, Difficulty difficulty, boolean hardcore, String version, String slotName) {
        RealmsWorldOptions realmsworldoptions = createDefaults();
        realmsworldoptions.difficulty = difficulty.getId();
        realmsworldoptions.gameMode = gameMode.getId();
        realmsworldoptions.slotName = slotName;
        realmsworldoptions.version = version;
        return realmsworldoptions;
    }

    public static RealmsWorldOptions createFromSettings(LevelSettings settings, String slotName) {
        return createDefaultsWith(settings.gameType(), settings.difficulty(), settings.hardcore(), slotName, settings.levelName());
    }

    public static RealmsWorldOptions createEmptyDefaults() {
        RealmsWorldOptions realmsworldoptions = createDefaults();
        realmsworldoptions.setEmpty(true);
        return realmsworldoptions;
    }

    public void setEmpty(boolean empty) {
        this.empty = empty;
    }

    public static RealmsWorldOptions parse(GuardedSerializer serializer, String json) {
        RealmsWorldOptions realmsworldoptions = serializer.fromJson(json, RealmsWorldOptions.class);
        if (realmsworldoptions == null) {
            return createDefaults();
        } else {
            finalize(realmsworldoptions);
            return realmsworldoptions;
        }
    }

    private static void finalize(RealmsWorldOptions options) {
        if (options.slotName == null) {
            options.slotName = "";
        }

        if (options.version == null) {
            options.version = "";
        }

        if (options.compatibility == null) {
            options.compatibility = RealmsServer.Compatibility.UNVERIFIABLE;
        }
    }

    public String getSlotName(int slotIndex) {
        if (StringUtil.isBlank(this.slotName)) {
            return this.empty ? I18n.get("mco.configure.world.slot.empty") : this.getDefaultSlotName(slotIndex);
        } else {
            return this.slotName;
        }
    }

    public String getDefaultSlotName(int slotIndex) {
        return I18n.get("mco.configure.world.slot", slotIndex);
    }

    public RealmsWorldOptions clone() {
        return new RealmsWorldOptions(this.spawnProtection, this.difficulty, this.gameMode, this.forceGameMode, this.slotName, this.version, this.compatibility);
    }
}
