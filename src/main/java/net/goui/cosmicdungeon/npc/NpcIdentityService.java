package net.goui.cosmicdungeon.npc;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.npc.tamsin.TamsinData;
import net.goui.cosmicdungeon.vendor.VendorAssignmentService;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import java.util.UUID;

/** Explicit developer placement wins; stale chunks cannot resurrect the replaced identity. */
@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID)
public final class NpcIdentityService {
    public static final String TAMSIN = "tamsin";
    private NpcIdentityService() {}

    private static String role(Entity entity) {
        if (!(entity.level() instanceof ServerLevel level) || !(entity instanceof LivingEntity)
                || entity instanceof Player || entity.getPersistentData().contains("cosmicdungeon_d1_watson_run")) return null;
        boolean tamsin = TamsinData.get(level.getServer()).binding(entity.getUUID()) != null;
        boolean vendor = VendorAssignmentService.hasAssignedProfile(entity);
        if (tamsin && vendor) return null; // Conflicting authored roles still require explicit review.
        if (tamsin) return TAMSIN;
        var profile = VendorAssignmentService.getProfileId(entity);
        return profile == null ? null : "vendor:" + profile;
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void joined(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        String role = role(event.getEntity());
        if (role == null) return;
        if (!NpcIdentityData.get(level.getServer()).admit(role, event.getEntity().getUUID())) {
            event.setCanceled(true);
            event.getEntity().discard();
        }
    }
    /** For an already loaded, explicitly assigned NPC. No entity/world scan or chunk load. */
    public static void placed(Entity entity) {
        if (!(entity.level() instanceof ServerLevel level) || level.getEntity(entity.getUUID()) != entity) return;
        String role = role(entity);
        if (role == null) return;
        String previous = NpcIdentityData.get(level.getServer()).replace(role, entity.getUUID());
        retire(level.getServer(), role, previous, entity.getUUID());
    }
    /** The old NPC is retained if any mod refuses insertion of the new one. */
    public static boolean spawn(Entity entity) {
        if (!(entity.level() instanceof ServerLevel level)) return false;
        String role = role(entity);
        if (role == null) return false;
        var data = NpcIdentityData.get(level.getServer());
        String previous = data.owner(role);
        boolean added = false;
        try {
            added = data.install(role, entity.getUUID(), () -> level.addFreshEntity(entity));
            if (added) retire(level.getServer(), role, previous, entity.getUUID());
            return added;
        } finally {
            if (!added) entity.discard();
        }
    }
    public static void cleared(Entity entity) {
        if (!(entity.level() instanceof ServerLevel level)) return;
        String role = role(entity);
        if (role != null) {
            var data = NpcIdentityData.get(level.getServer());
            data.admit(role, entity.getUUID()); // Legacy unbind also leaves a retirement marker.
            data.clear(role, entity.getUUID());
        }
    }
    private static void retire(MinecraftServer server, String role, String previous, UUID replacement) {
        if (previous == null || previous.isEmpty() || previous.equals(replacement.toString())) return;
        UUID old = UUID.fromString(previous);
        for (var level : server.getAllLevels()) {
            Entity entity = level.getEntity(old); // Indexed lookup, including another loaded dimension.
            if (entity != null && role.equals(role(entity))) entity.discard();
        }
        // An unloaded old entity is excluded by the durable owner on its next join.
    }
}
