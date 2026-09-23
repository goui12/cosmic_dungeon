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
    public static final DeferredHolder<MobEffect, MobEffect> MENDING_STING = MOB_EFFECTS.register("mending_sting", () -> new D1TunedMobEffect("theurgist","mending_sting","heal",13458603));
    public static final DeferredHolder<MobEffect, MobEffect> VERDANT_JOLT = MOB_EFFECTS.register("verdant_jolt", () -> new D1TunedMobEffect("theurgist","verdant_jolt","heal",7391352));
    public static final DeferredHolder<MobEffect, MobEffect> TREE_VIPER = MOB_EFFECTS.register("tree_viper", () -> new D1TunedMobEffect("venefex","tree_viper","poison",6192150));
    public static final DeferredHolder<MobEffect, MobEffect> BUSHMASTER = MOB_EFFECTS.register("bushmaster", () -> new D1TunedMobEffect("venefex","bushmaster","poison",4680484));
    public static final DeferredHolder<MobEffect, MobEffect> FER_DE_LANCE = MOB_EFFECTS.register("fer_de_lance", () -> new D1TunedMobEffect("venefex","fer_de_lance","poison",7972400));
    public static final DeferredHolder<MobEffect, MobEffect> PESTIS = MOB_EFFECTS.register("pestis", () -> new D1TunedMobEffect("venefex","pestis","weak",4738376));
    public static final DeferredHolder<MobEffect, MobEffect> BLACK_BUBO = MOB_EFFECTS.register("black_bubo", () -> new D1TunedMobEffect("venefex","black_bubo","weak",2368548));
    public static final DeferredHolder<MobEffect, MobEffect> VAPOURS = MOB_EFFECTS.register("vapours", () -> new D1TunedMobEffect("venefex","vapours","slow",7833719));
    public static final DeferredHolder<MobEffect, MobEffect> MELANCHOLIA = MOB_EFFECTS.register("melancholia", () -> new D1TunedMobEffect("venefex","melancholia","slow",5859697));
    public static final DeferredHolder<MobEffect, MobEffect> DEATHLY_STUPOR = MOB_EFFECTS.register("deathly_stupor", () -> new D1TunedMobEffect("venefex","deathly_stupor","slow",3225928));
    public static void register(IEventBus eventBus) { MOB_EFFECTS.register(eventBus); }
}
