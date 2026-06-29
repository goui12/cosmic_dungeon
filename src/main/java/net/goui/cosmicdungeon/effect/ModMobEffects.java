package net.goui.cosmicdungeon.effect;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMobEffects {
    private ModMobEffects() {}
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, CosmicDungeonMod.MOD_ID);
    public static final DeferredHolder<MobEffect, MobEffect> TELEPORT_COOLDOWN = MOB_EFFECTS.register("teleport_cooldown", TeleportCooldownMobEffect::new);
    public static void register(IEventBus eventBus) { MOB_EFFECTS.register(eventBus); }
}
