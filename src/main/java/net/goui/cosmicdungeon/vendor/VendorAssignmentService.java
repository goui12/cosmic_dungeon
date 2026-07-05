package net.goui.cosmicdungeon.vendor;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

import javax.annotation.Nullable;

public final class VendorAssignmentService {
    private static final String TAG_VENDOR_PROFILE_ID = "cosmicdungeon.vendor_profile_id";
    private static final TextColor FRIENDLY_VENDOR_NAME_COLOR = TextColor.fromRgb(0x39FF14);

    private VendorAssignmentService() {}

    @Nullable
    public static ResourceLocation getProfileId(Entity entity) {
        String raw = entity.getPersistentData().getStringOr(TAG_VENDOR_PROFILE_ID, "");
        if (raw.isEmpty()) return null;
        return ResourceLocation.tryParse(raw);
    }

    public static boolean hasAssignedProfile(Entity entity) {
        return getProfileId(entity) != null;
    }

    public static boolean assignProfile(Entity entity, ResourceLocation profileId) {
        if (profileId == null) return false;
        VendorProfile profile = VendorProfileManager.INSTANCE.get(profileId);
        if (profile == null) return false;
        entity.getPersistentData().putString(TAG_VENDOR_PROFILE_ID, profileId.toString());
        entity.setCustomName(friendlyVendorName(profile.displayName()));
        entity.setCustomNameVisible(true);
        entity.setInvulnerable(true);
        if (entity instanceof Mob mob) {
            mob.setPersistenceRequired();
            mob.setNoAi(true);
        }
        return true;
    }

    public static void clearProfile(Entity entity) {
        entity.getPersistentData().remove(TAG_VENDOR_PROFILE_ID);
        if (entity instanceof Mob mob) {
            mob.setNoAi(false);
        }
        entity.setInvulnerable(false);
    }

    private static Component friendlyVendorName(String displayName) {
        return Component.literal(displayName).withStyle(Style.EMPTY.withColor(FRIENDLY_VENDOR_NAME_COLOR).withBold(true));
    }
}
