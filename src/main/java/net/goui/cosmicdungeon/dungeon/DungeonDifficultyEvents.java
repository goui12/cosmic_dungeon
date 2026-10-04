package net.goui.cosmicdungeon.dungeon;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.Enemy;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.*;

/** Server-only, event-driven; at most ten registered instance leases are inspected. */
@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID)
public final class DungeonDifficultyEvents {
    private static final ResourceLocation HEALTH = ResourceLocation.fromNamespaceAndPath("cosmicdungeon", "instance_difficulty_health");
    private static final ThreadLocal<Boolean> BLINDNESS = ThreadLocal.withInitial(() -> false);
    private DungeonDifficultyEvents() {}
    public static DungeonDifficulty.Profile profile(ServerLevel level) {
        return DungeonRunRegistryData.get(level).difficultyForDimension(level.dimension());
    }
    private static DungeonDifficulty.Profile member(ServerPlayer player) {
        if (player.isSpectator() || net.goui.cosmicdungeon.auth.AccessPolicy.isDeveloper(player)) return null;
        var run = DungeonRunRegistryData.get(player.level().getServer()).findRunForPlayer(player.getUUID()).orElse(null);
        return run != null && run.stateEnum() == DungeonRunState.ACTIVE && !run.isCompletionExited(player.getUUID())
                && run.containsDimension(player.level().dimension()) ? run.difficulty() : null;
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void joined(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof LivingEntity mob)
                || !(mob instanceof Enemy)) return;
        var attribute = mob.getAttribute(Attributes.MAX_HEALTH);
        if (attribute == null) return;
        var settings = profile(level);
        double multiplier = settings == null ? 1 : settings.health();
        var previous = attribute.getModifier(HEALTH);
        if (previous == null && multiplier == 1 || previous != null && previous.amount() == multiplier - 1) return;
        float fraction = mob.getMaxHealth() <= 0 ? 1 : mob.getHealth() / mob.getMaxHealth();
        attribute.removeModifier(HEALTH);
        if (multiplier != 1) attribute.addPermanentModifier(new AttributeModifier(HEALTH, multiplier - 1,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        mob.setHealth(Math.min(mob.getMaxHealth(), mob.getMaxHealth() * fraction));
    }
    @SubscribeEvent
    public static void damage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)
                || !(event.getSource().getEntity() instanceof LivingEntity attacker)
                || !(attacker instanceof Enemy) || attacker.level() != level) return;
        var p = profile(level);
        if (p != null) event.setAmount((float)(event.getAmount() * p.damage()));
    }
    @SubscribeEvent
    public static void damaged(LivingDamageEvent.Post event) {
        if (event.getNewDamage() <= 0 || !(event.getEntity() instanceof ServerPlayer player)) return;
        var p = member(player);
        if (p == null || p.blindnessChance() <= 0 || player.getRandom().nextDouble() >= p.blindnessChance()) return;
        BLINDNESS.set(true);
        try { player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, p.blindnessTicks())); }
        finally { BLINDNESS.remove(); }
    }
    public static MobEffectInstance effect(LivingEntity entity, MobEffectInstance effect) {
        if (!(entity instanceof ServerPlayer player) || BLINDNESS.get()
                || effect.getEffect().value().getCategory() != MobEffectCategory.HARMFUL) return effect;
        var p = member(player);
        return p == null || p.harmfulDuration() == 1 ? effect : effect.withScaledDuration((float)p.harmfulDuration());
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void armor(ArmorHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var p = member(player);
        if (p == null || p.armorWear() == 1) return;
        for (var slot : event.getArmorMap().keySet())
            event.setNewDamage(slot, p.wear(event.getNewDamage(slot), player.getRandom().nextDouble()));
    }
}
