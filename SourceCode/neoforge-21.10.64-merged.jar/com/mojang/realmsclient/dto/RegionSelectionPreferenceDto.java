package com.mojang.realmsclient.dto;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.mojang.logging.LogUtils;
import javax.annotation.Nullable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RegionSelectionPreferenceDto extends ValueObject implements ReflectionBasedSerialization {
    public static final RegionSelectionPreferenceDto DEFAULT = new RegionSelectionPreferenceDto(RegionSelectionPreference.AUTOMATIC_OWNER, null);
    private static final Logger LOGGER = LogUtils.getLogger();
    @SerializedName("regionSelectionPreference")
    @JsonAdapter(RegionSelectionPreference.RegionSelectionPreferenceJsonAdapter.class)
    public RegionSelectionPreference regionSelectionPreference;
    @SerializedName("preferredRegion")
    @JsonAdapter(RealmsRegion.RealmsRegionJsonAdapter.class)
    @Nullable
    public RealmsRegion preferredRegion;

    public RegionSelectionPreferenceDto(RegionSelectionPreference regionSelectionPreference, @Nullable RealmsRegion preferredRegion) {
        this.regionSelectionPreference = regionSelectionPreference;
        this.preferredRegion = preferredRegion;
    }

    private RegionSelectionPreferenceDto() {
    }

    public static RegionSelectionPreferenceDto parse(GuardedSerializer serializer, String json) {
        try {
            RegionSelectionPreferenceDto regionselectionpreferencedto = serializer.fromJson(json, RegionSelectionPreferenceDto.class);
            if (regionselectionpreferencedto == null) {
                LOGGER.error("Could not parse RegionSelectionPreference: {}", json);
                return new RegionSelectionPreferenceDto();
            } else {
                return regionselectionpreferencedto;
            }
        } catch (Exception exception) {
            LOGGER.error("Could not parse RegionSelectionPreference: {}", exception.getMessage());
            return new RegionSelectionPreferenceDto();
        }
    }

    public RegionSelectionPreferenceDto clone() {
        return new RegionSelectionPreferenceDto(this.regionSelectionPreference, this.preferredRegion);
    }
}
