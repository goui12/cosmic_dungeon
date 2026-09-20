package net.goui.cosmicdungeon.vendor;

import net.goui.cosmicdungeon.npc.tamsin.TamsinData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import javax.annotation.Nullable;

public final class VendorAssignmentService {
    private static final TextColor FRIENDLY_VENDOR_NAME_COLOR=TextColor.fromRgb(0x39FF14);
    private VendorAssignmentService(){}

    @Nullable public static ResourceLocation getProfileId(Entity entity) {
        String raw=entity.getPersistentData().getStringOr(VendorBindingRules.PROFILE,"");
        return raw.isEmpty()?null:ResourceLocation.tryParse(raw);
    }
    /** A malformed/unknown binding still occupies this entity and must not fall back to vanilla commerce. */
    public static boolean hasAssignedProfile(Entity entity) {
        return entity.getPersistentData().contains(VendorBindingRules.PROFILE);
    }
    public static boolean hasOtherRole(Entity entity) {
        return entity.getPersistentData().contains("cosmicdungeon_d1_watson_run")
                ||entity.level() instanceof ServerLevel level
                    &&TamsinData.get(level.getServer()).binding(entity.getUUID())!=null;
    }
    public static boolean assignProfile(Entity entity,ResourceLocation profileId) {
        if(!(entity instanceof Mob mob)||!(entity.level() instanceof ServerLevel)||!entity.isAlive()||profileId==null)return false;
        VendorProfile profile=VendorProfileManager.INSTANCE.get(profileId);
        if(profile==null||!VendorBindingRules.canAssign(entity.getPersistentData(),profileId.toString(),
                BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString(),hasOtherRole(entity)))return false;
        // Validate persisted identity before mutating the authored entity.
        net.goui.cosmicdungeon.npc.NpcIdentityData.get(((ServerLevel)entity.level()).getServer());
        var ops=entity.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        var name=entity.getCustomName();
        var encoded=name==null?null:ComponentSerialization.CODEC.encodeStart(ops,name).result().orElse(null);
        if(name!=null&&encoded==null)return false;
        VendorBindingRules.capture(entity.getPersistentData(),encoded,entity.isCustomNameVisible(),entity.isInvulnerable(),mob.isNoAi());
        entity.getPersistentData().putString(VendorBindingRules.PROFILE,profileId.toString());
        entity.setCustomName(Component.literal(profile.displayName()).withStyle(
                Style.EMPTY.withColor(FRIENDLY_VENDOR_NAME_COLOR).withBold(true)));
        entity.setCustomNameVisible(true);entity.setInvulnerable(true);
        mob.setPersistenceRequired();mob.setNoAi(true);
        net.goui.cosmicdungeon.npc.NpcIdentityService.placed(entity);
        return true;
    }
    /** Returns false without mutation if the retained original state cannot be decoded safely. */
    public static boolean clearProfile(Entity entity) {
        if(!(entity instanceof Mob mob)||!(entity.level() instanceof ServerLevel)
                ||!hasAssignedProfile(entity)||hasOtherRole(entity))return false;
        final VendorBindingRules.Before before;
        try {before=VendorBindingRules.before(entity.getPersistentData());}
        catch(IllegalArgumentException damaged){return false;}
        Component name=null;
        if(before!=null&&before.name()!=null) {
            name=ComponentSerialization.CODEC.parse(entity.registryAccess().createSerializationContext(NbtOps.INSTANCE),
                    before.name()).result().orElse(null);
            if(name==null)return false;
        }
        net.goui.cosmicdungeon.npc.NpcIdentityService.cleared(entity);
        if(before!=null) {
            entity.setCustomName(name);entity.setCustomNameVisible(before.visible());
            entity.setInvulnerable(before.invulnerable());mob.setNoAi(before.noAi());
        }
        // Legacy assignments have no prior-state proof. Keep their authored name/AI/protection.
        // Persistence deliberately remains enabled so unbinding cannot despawn an authored NPC.
        entity.getPersistentData().remove(VendorBindingRules.PROFILE);
        entity.getPersistentData().remove(VendorBindingRules.BEFORE);
        return true;
    }
}
