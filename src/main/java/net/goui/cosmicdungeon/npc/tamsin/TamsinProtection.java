package net.goui.cosmicdungeon.npc.tamsin;

import java.util.UUID;
import java.util.function.Predicate;
import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.npc.NpcIdentityData;
import net.goui.cosmicdungeon.npc.NpcIdentityService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityInvulnerabilityCheckEvent;

/** Keeps Tamsin immune even when a damage source bypasses vanilla's Invulnerable flag. */
@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID)
public final class TamsinProtection {
    private static final TamsinProtection INSTANCE = new TamsinProtection();
    private final Predicate<Entity> protectedNpc;

    private TamsinProtection() {
        protectedNpc = this::isProtectedNpc;
    }

    TamsinProtection(Predicate<Entity> protectedNpc) {
        this.protectedNpc = protectedNpc;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void checkInvulnerability(EntityInvulnerabilityCheckEvent event) {
        INSTANCE.protect(event);
    }

    void protect(EntityInvulnerabilityCheckEvent event) {
        // Preserve all pre-existing immunity; no source whitelist can leave Creative/void/kill gaps.
        if (!event.isInvulnerable() && protectedNpc.test(event.getEntity())) event.setInvulnerable(true);
    }

    private boolean isProtectedNpc(Entity entity) {
        if (!(entity instanceof LivingEntity) || entity instanceof Player || entity.isRemoved()
                || !(entity.level() instanceof ServerLevel level)) return false;
        var data = TamsinData.get(level.getServer());
        UUID id = entity.getUUID();
        if (data.binding(id) == null) return false;
        String owner = NpcIdentityData.get(level.getServer()).owner(NpcIdentityService.TAMSIN);
        return matchesBinding(data, id, owner);
    }

    boolean matchesBinding(TamsinData data, UUID npc, String owner) {
        // A legacy binding is valid before identity adoption; cleared/replaced identities are not.
        return data.binding(npc) != null && (owner == null || owner.equals(npc.toString()));
    }
}
