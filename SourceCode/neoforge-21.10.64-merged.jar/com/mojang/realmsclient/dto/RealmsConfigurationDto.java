package com.mojang.realmsclient.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import javax.annotation.Nullable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record RealmsConfigurationDto(
    @SerializedName("options") RealmsSlotUpdateDto options,
    @SerializedName("settings") List<RealmsSetting> settings,
    @Nullable @SerializedName("regionSelectionPreference") RegionSelectionPreferenceDto regionSelectionPreference,
    @Nullable @SerializedName("description") RealmsDescriptionDto description
) implements ReflectionBasedSerialization {
}
