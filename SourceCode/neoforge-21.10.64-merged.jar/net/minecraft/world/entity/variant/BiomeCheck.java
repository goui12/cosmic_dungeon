package net.minecraft.world.entity.variant;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;

public record BiomeCheck(HolderSet<Biome> requiredBiomes) implements SpawnCondition {
    public static final MapCodec<BiomeCheck> MAP_CODEC = RecordCodecBuilder.mapCodec(
        p_399647_ -> p_399647_.group(RegistryCodecs.homogeneousList(Registries.BIOME).fieldOf("biomes").forGetter(BiomeCheck::requiredBiomes))
            .apply(p_399647_, BiomeCheck::new)
    );

    public boolean test(SpawnContext p_400182_) {
        return this.requiredBiomes.contains(p_400182_.biome());
    }

    @Override
    public MapCodec<BiomeCheck> codec() {
        return MAP_CODEC;
    }
}
