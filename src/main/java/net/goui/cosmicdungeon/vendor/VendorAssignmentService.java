package net.goui.cosmicdungeon.vendor;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.Villager;

import javax.annotation.Nullable;

public final class VendorAssignmentService {
    private static final String TAG_VENDOR_PROFILE_ID = "cosmicdungeon.vendor_profile_id";

    private VendorAssignmentService() {}

    @Nullable
    public static ResourceLocation getProfileId(Villager villager) {
        String raw = villager.getPersistentData().getStringOr(TAG_VENDOR_PROFILE_ID, "");
        if (raw.isEmpty()) return null;
        return ResourceLocation.tryParse(raw);
    }

    public static boolean hasAssignedProfile(Villager villager) {
        return getProfileId(villager) != null;
    }

    public static boolean assignProfile(Villager villager, ResourceLocation profileId) {
        if (profileId == null) return false;
        VendorProfile profile = VendorProfileManager.INSTANCE.get(profileId);
        if (profile == null) return false;
        villager.getPersistentData().putString(TAG_VENDOR_PROFILE_ID, profileId.toString());
        villager.setCustomName(net.minecraft.network.chat.Component.literal(profile.displayName()));
        villager.setCustomNameVisible(true);
        villager.setPersistenceRequired();
        villager.setNoAi(true);
        villager.setInvulnerable(true);
        return true;
    }

    public static void clearProfile(Villager villager) {
        villager.getPersistentData().remove(TAG_VENDOR_PROFILE_ID);
        villager.setNoAi(false);
        villager.setInvulnerable(false);
    }
}
