package com.mojang.realmsclient.dto;

import com.google.gson.annotations.SerializedName;
import javax.annotation.Nullable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class RealmsSlotUpdateDto implements ReflectionBasedSerialization {
    @SerializedName("slotId")
    public final int slotId;
    @SerializedName("spawnProtection")
    private final int spawnProtection;
    @SerializedName("forceGameMode")
    private final boolean forceGameMode;
    @SerializedName("difficulty")
    private final int difficulty;
    @SerializedName("gameMode")
    private final int gameMode;
    @SerializedName("slotName")
    private final String slotName;
    @SerializedName("version")
    private final String version;
    @SerializedName("compatibility")
    private final RealmsServer.Compatibility compatibility;
    @SerializedName("worldTemplateId")
    private final long templateId;
    @Nullable
    @SerializedName("worldTemplateImage")
    private final String templateImage;
    @SerializedName("hardcore")
    private final boolean hardcore;

    public RealmsSlotUpdateDto(int slotId, RealmsWorldOptions options, boolean hardcore) {
        this.slotId = slotId;
        this.spawnProtection = options.spawnProtection;
        this.forceGameMode = options.forceGameMode;
        this.difficulty = options.difficulty;
        this.gameMode = options.gameMode;
        this.slotName = options.getSlotName(slotId);
        this.version = options.version;
        this.compatibility = options.compatibility;
        this.templateId = options.templateId;
        this.templateImage = options.templateImage;
        this.hardcore = hardcore;
    }
}
