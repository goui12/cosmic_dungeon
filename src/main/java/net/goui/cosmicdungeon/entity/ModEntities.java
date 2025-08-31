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

    public static final DeferredHolder<EntityType<?>, EntityType<MagmaGlobEntity>> MAGMA_GLOB =
            ENTITIES.register("magma_glob", () -> {
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, "magma_glob");
                ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, id);
                return EntityType.Builder
                        .of(MagmaGlobEntity::new, MobCategory.MONSTER)
                        .sized(1.0f, 1.0f)
                        .build(key);
            });

    public static final DeferredHolder<EntityType<?>, EntityType<StoneWardenEntity>> STONE_WARDEN =
            ENTITIES.register("stone_warden", () -> {
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, "stone_warden");
                ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, id);
                return EntityType.Builder
                        .of(StoneWardenEntity::new, MobCategory.MONSTER)
                        .sized(2.8f, 9.0f) // big hitbox
                        .build(key);
            });

    /** Goblin Ambusher (sneaky, smaller hitbox). */
    public static final DeferredHolder<EntityType<?>, EntityType<GoblinAmbusherEntity>> GOBLIN_AMBUSHER =
            ENTITIES.register("goblin_ambusher", () -> {
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, "goblin_ambusher");
                ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, id);
                return EntityType.Builder
                        .of(GoblinAmbusherEntity::new, MobCategory.MONSTER)
                        .sized(0.6f, 1.3f) // tweak if your model needs different bounds
                        .build(key);
            });

    private ModEntities() {}

    /** Call this once from your mod constructor: ModEntities.register(modEventBus); */
    public static void register(IEventBus modEventBus) {
        ENTITIES.register(modEventBus);
    }
}
