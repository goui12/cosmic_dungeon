package net.goui.cosmicdungeon.dungeon;

import java.util.UUID;
import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.auth.AccessPolicy;
import net.goui.cosmicdungeon.entity.MetalmancerGolemEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/** Actual player-caused damage, persisted on its victim; no death polling or deferred reward queue. */
@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID)
public final class DungeonKillCredit {
    public static final String LAST_PLAYER = "cosmicdungeon_last_damaging_player_v1";
    private DungeonKillCredit() {}
    public static UUID controller(Entity attacker) {
        if (attacker instanceof ServerPlayer player) return player.getUUID();
        if (attacker instanceof OwnableEntity owned && owned.getOwnerReference() != null)
            return owned.getOwnerReference().getUUID();
        if (attacker instanceof MetalmancerGolemEntity golem) return golem.getOwnerId();
        return null;
    }
    public static UUID remembered(CompoundTag data) {
        try { return UUID.fromString(data.getStringOr(LAST_PLAYER, "")); }
        catch (IllegalArgumentException invalid) { return null; }
    }
    public static UUID resolve(CompoundTag data, UUID directController, boolean administrativeHit) {
        return administrativeHit ? null : directController != null ? directController : remembered(data);
    }
    private static boolean administrative(ServerLevel level, UUID id) {
        ServerPlayer player = id == null ? null : level.getServer().getPlayerList().getPlayer(id);
        return player != null && (AccessPolicy.isDeveloper(player) || player.isSpectator());
    }
    public static UUID resolve(LivingEntity victim, DamageSource fatalSource) {
        if (!(victim.level() instanceof ServerLevel level)) return null;
        UUID direct = controller(fatalSource.getEntity());
        UUID result = resolve(victim.getPersistentData(), direct, administrative(level, direct));
        return administrative(level, result) ? null : result;
    }
    @SubscribeEvent public static void damaged(LivingDamageEvent.Post event) {
        if (event.getNewDamage() <= 0 || !(event.getEntity() instanceof Mob victim)
                || !(victim.level() instanceof ServerLevel level)) return;
        UUID player = controller(event.getSource().getEntity());
        if (player == null) return;
        if (administrative(level, player)) victim.getPersistentData().remove(LAST_PLAYER);
        else victim.getPersistentData().putString(LAST_PLAYER, player.toString());
    }
}
