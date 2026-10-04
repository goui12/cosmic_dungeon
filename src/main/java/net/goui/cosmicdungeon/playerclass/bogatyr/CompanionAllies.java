package net.goui.cosmicdungeon.playerclass.bogatyr;

import net.goui.cosmicdungeon.entity.MetalmancerGolemEntity;
import net.goui.cosmicdungeon.npc.tamsin.TamsinData;
import net.goui.cosmicdungeon.vendor.VendorAssignmentService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.npc.Npc;
import net.minecraft.world.entity.player.Player;

/** Party membership is intentionally irrelevant: pets must not attack other friendly players/NPCs. */
public final class CompanionAllies {
    private CompanionAllies() {}
    public static boolean friendlyType(Class<? extends Entity> type) {
        return Player.class.isAssignableFrom(type) || Npc.class.isAssignableFrom(type)
                || Allay.class.isAssignableFrom(type) || IronGolem.class.isAssignableFrom(type)
                || SnowGolem.class.isAssignableFrom(type);
    }
    public static boolean friendly(Entity target) {
        if (target == null) return false;
        if (friendlyType(target.getClass())) return true;
        if (target instanceof OwnableEntity owned && owned.getOwnerReference() != null) return true;
        if (target instanceof MetalmancerGolemEntity golem && golem.getOwnerId() != null) return true;
        if (target.getPersistentData().contains("cosmicdungeon_d1_watson_run")
                || VendorAssignmentService.hasAssignedProfile(target)) return true;
        return target.level() instanceof ServerLevel level
                && TamsinData.get(level.getServer()).binding(target.getUUID()) != null;
    }
}
