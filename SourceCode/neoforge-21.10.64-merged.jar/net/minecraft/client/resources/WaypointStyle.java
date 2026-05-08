package net.minecraft.client.resources;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record WaypointStyle(int nearDistance, int farDistance, List<ResourceLocation> sprites, List<ResourceLocation> spriteLocations) {
    @VisibleForTesting
    public static final String ICON_LOCATION_PREFIX = "hud/locator_bar_dot/";
    public static final int DEFAULT_NEAR_DISTANCE = 128;
    public static final int DEFAULT_FAR_DISTANCE = 332;
    private static final Codec<Integer> DISTANCE_CODEC = Codec.intRange(0, 60000000);
    public static final Codec<WaypointStyle> CODEC = RecordCodecBuilder.<WaypointStyle>create(
            p_419703_ -> p_419703_.group(
                    DISTANCE_CODEC.optionalFieldOf("near_distance", 128).forGetter(WaypointStyle::nearDistance),
                    DISTANCE_CODEC.optionalFieldOf("far_distance", 332).forGetter(WaypointStyle::farDistance),
                    ExtraCodecs.nonEmptyList(ResourceLocation.CODEC.listOf()).fieldOf("sprites").forGetter(WaypointStyle::sprites)
                )
                .apply(p_419703_, WaypointStyle::new)
        )
        .validate(WaypointStyle::validate);

    public WaypointStyle(int p_419759_, int p_419537_, List<ResourceLocation> p_420059_) {
        this(p_419759_, p_419537_, p_420059_, p_420059_.stream().map(p_420007_ -> p_420007_.withPrefix("hud/locator_bar_dot/")).toList());
    }

    @VisibleForTesting
    public DataResult<WaypointStyle> validate() {
        if (this.sprites.isEmpty()) {
            return DataResult.error(() -> "Must have at least one sprite icon");
        } else if (this.nearDistance <= 0) {
            return DataResult.error(() -> "Near distance (" + this.nearDistance + ") must be greater than zero");
        } else {
            return this.nearDistance >= this.farDistance
                ? DataResult.error(() -> "Far distance (" + this.farDistance + ") cannot be closer or equal to near distance (" + this.nearDistance + ")")
                : DataResult.success(this);
        }
    }

    public ResourceLocation sprite(float distance) {
        if (distance < this.nearDistance) {
            return this.spriteLocations.getFirst();
        } else if (distance >= this.farDistance) {
            return this.spriteLocations.getLast();
        } else if (this.spriteLocations.size() == 1) {
            return this.spriteLocations.getFirst();
        } else if (this.spriteLocations.size() == 3) {
            return this.spriteLocations.get(1);
        } else {
            int i = Mth.lerpInt((distance - this.nearDistance) / (this.farDistance - this.nearDistance), 1, this.spriteLocations.size() - 1);
            return this.spriteLocations.get(i);
        }
    }
}
