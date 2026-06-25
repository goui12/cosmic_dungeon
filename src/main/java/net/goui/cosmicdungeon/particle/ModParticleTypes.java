package net.goui.cosmicdungeon.particle;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModParticleTypes {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, CosmicDungeonMod.MOD_ID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> DRAGOON_LIGHTNING =
            PARTICLE_TYPES.register("dragoon_lightning_particle", () -> new SimpleParticleType(false));

    private ModParticleTypes() {}

    public static void register(IEventBus modEventBus) {
        PARTICLE_TYPES.register(modEventBus);
    }
}
