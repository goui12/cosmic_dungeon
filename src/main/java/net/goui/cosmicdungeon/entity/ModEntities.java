package net.goui.cosmicdungeon.entity;

import net.goui.cosmicdungeon.CosmicDungeonMod;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, CosmicDungeonMod.MOD_ID);

    // -------------------------
    // MAGMA GLOB
    // -------------------------
    public static final DeferredHolder<EntityType<?>, EntityType<MagmaGlobEntity>> MAGMA_GLOB =
            ENTITIES.register("magma_glob", () -> {
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, "magma_glob");
                ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, id);
                return EntityType.Builder
                        .of(MagmaGlobEntity::new, MobCategory.MONSTER)
                        .sized(1.0f, 1.0f)
                        .build(key);
            });

    // -------------------------
    // STONE WARDEN
    // -------------------------
    public static final DeferredHolder<EntityType<?>, EntityType<StoneWardenEntity>> STONE_WARDEN =
            ENTITIES.register("stone_warden", () -> {
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, "stone_warden");
                ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, id);
                return EntityType.Builder
                        .of(StoneWardenEntity::new, MobCategory.MONSTER)
                        .sized(2.8f, 9.0f)
                        .build(key);
            });

    // -------------------------
    // GOBLIN AMBUSHER
    // -------------------------
    public static final DeferredHolder<EntityType<?>, EntityType<GoblinAmbusherEntity>> GOBLIN_AMBUSHER =
            ENTITIES.register("goblin_ambusher", () -> {
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, "goblin_ambusher");
                ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, id);
                return EntityType.Builder
                        .of(GoblinAmbusherEntity::new, MobCategory.MONSTER)
                        .sized(0.7f, 1.6f)
                        .build(key);
            });

    // -------------------------
    // METALMANCER GOLEM
    // -------------------------
    public static final DeferredHolder<EntityType<?>, EntityType<MetalmancerGolemEntity>> METALMANCER_GOLEM =
            ENTITIES.register("metalmancer_golem", () -> {
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, "metalmancer_golem");
                ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, id);
                return EntityType.Builder
                        .of(MetalmancerGolemEntity::new, MobCategory.MISC)
                        .sized(0.9f, 2.4f)
                        .build(key);
            });

    // -------------------------
// CRYSTAL CREEPER
// -------------------------
    public static final DeferredHolder<EntityType<?>, EntityType<CrystalCreeperEntity>> CRYSTAL_CREEPER =
            ENTITIES.register("crystal_creeper", () -> {
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, "crystal_creeper");
                ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, id);
                return EntityType.Builder
                        .of(CrystalCreeperEntity::new, MobCategory.MONSTER)
                        .sized(0.6f, 1.7f)
                        .build(key);
            });


    private ModEntities() {}

    /** Call once in CosmicDungeonMod constructor. */
    public static void register(IEventBus modEventBus) {
        ENTITIES.register(modEventBus);
    }
}
