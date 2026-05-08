package net.minecraft.world.entity.npc;

import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryFixedCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

public final class VillagerType {
    public static final ResourceKey<VillagerType> DESERT = createKey("desert");
    public static final ResourceKey<VillagerType> JUNGLE = createKey("jungle");
    public static final ResourceKey<VillagerType> PLAINS = createKey("plains");
    public static final ResourceKey<VillagerType> SAVANNA = createKey("savanna");
    public static final ResourceKey<VillagerType> SNOW = createKey("snow");
    public static final ResourceKey<VillagerType> SWAMP = createKey("swamp");
    public static final ResourceKey<VillagerType> TAIGA = createKey("taiga");
    public static final Codec<Holder<VillagerType>> CODEC = RegistryFixedCodec.create(Registries.VILLAGER_TYPE);
    public static final StreamCodec<RegistryFriendlyByteBuf, Holder<VillagerType>> STREAM_CODEC = ByteBufCodecs.holderRegistry(Registries.VILLAGER_TYPE);
    /** Neo: use the {@link net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps#VILLAGER_TYPES data map} instead as this field will be ignored in future versions */
    @Deprecated
    private static final Map<ResourceKey<Biome>, ResourceKey<VillagerType>> BY_BIOME = Util.make(Maps.newHashMap(), p_35834_ -> {
        p_35834_.put(Biomes.BADLANDS, DESERT);
        p_35834_.put(Biomes.DESERT, DESERT);
        p_35834_.put(Biomes.ERODED_BADLANDS, DESERT);
        p_35834_.put(Biomes.WOODED_BADLANDS, DESERT);
        p_35834_.put(Biomes.BAMBOO_JUNGLE, JUNGLE);
        p_35834_.put(Biomes.JUNGLE, JUNGLE);
        p_35834_.put(Biomes.SPARSE_JUNGLE, JUNGLE);
        p_35834_.put(Biomes.SAVANNA_PLATEAU, SAVANNA);
        p_35834_.put(Biomes.SAVANNA, SAVANNA);
        p_35834_.put(Biomes.WINDSWEPT_SAVANNA, SAVANNA);
        p_35834_.put(Biomes.DEEP_FROZEN_OCEAN, SNOW);
        p_35834_.put(Biomes.FROZEN_OCEAN, SNOW);
        p_35834_.put(Biomes.FROZEN_RIVER, SNOW);
        p_35834_.put(Biomes.ICE_SPIKES, SNOW);
        p_35834_.put(Biomes.SNOWY_BEACH, SNOW);
        p_35834_.put(Biomes.SNOWY_TAIGA, SNOW);
        p_35834_.put(Biomes.SNOWY_PLAINS, SNOW);
        p_35834_.put(Biomes.GROVE, SNOW);
        p_35834_.put(Biomes.SNOWY_SLOPES, SNOW);
        p_35834_.put(Biomes.FROZEN_PEAKS, SNOW);
        p_35834_.put(Biomes.JAGGED_PEAKS, SNOW);
        p_35834_.put(Biomes.SWAMP, SWAMP);
        p_35834_.put(Biomes.MANGROVE_SWAMP, SWAMP);
        p_35834_.put(Biomes.OLD_GROWTH_SPRUCE_TAIGA, TAIGA);
        p_35834_.put(Biomes.OLD_GROWTH_PINE_TAIGA, TAIGA);
        p_35834_.put(Biomes.WINDSWEPT_GRAVELLY_HILLS, TAIGA);
        p_35834_.put(Biomes.WINDSWEPT_HILLS, TAIGA);
        p_35834_.put(Biomes.TAIGA, TAIGA);
        p_35834_.put(Biomes.WINDSWEPT_FOREST, TAIGA);
    });

    private static ResourceKey<VillagerType> createKey(String name) {
        return ResourceKey.create(Registries.VILLAGER_TYPE, ResourceLocation.withDefaultNamespace(name));
    }

    private static VillagerType register(Registry<VillagerType> registry, ResourceKey<VillagerType> key) {
        return Registry.register(registry, key, new VillagerType());
    }

    public static VillagerType bootstrap(Registry<VillagerType> registry) {
        register(registry, DESERT);
        register(registry, JUNGLE);
        register(registry, PLAINS);
        register(registry, SAVANNA);
        register(registry, SNOW);
        register(registry, SWAMP);
        return register(registry, TAIGA);
    }

    public static ResourceKey<VillagerType> byBiome(Holder<Biome> biome) {
        var fromDataMap = biome.getData(net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps.VILLAGER_TYPES);
        if (fromDataMap != null) return fromDataMap.type();
        // TODO - 1.22: remove fallback
        return biome.unwrapKey().map(BY_BIOME::get).orElse(PLAINS);
    }
}
