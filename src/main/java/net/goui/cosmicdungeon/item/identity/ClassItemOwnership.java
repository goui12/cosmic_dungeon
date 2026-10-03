package net.goui.cosmicdungeon.item.identity;

import java.util.UUID;
import net.goui.cosmicdungeon.auth.AccessPolicy;
import net.goui.cosmicdungeon.playerclass.api.ClassItemEquipmentGuard;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** First eligible inventory acquisition binds only ownership; authored item components stay intact. */
public final class ClassItemOwnership {
    public static final String KEY = "cosmicdungeon_attuned_owner_v1";
    private ClassItemOwnership() {}
    public static boolean attuned(ItemStack stack) {
        return ClassItemEquipmentGuard.getRequiredClass(stack) != null;
    }
    public static boolean present(ItemStack stack) {
        var data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.contains(KEY);
    }
    public static UUID owner(ItemStack stack) {
        var data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return null;
        try { return UUID.fromString(data.copyTag().getStringOr(KEY, "")); }
        catch (IllegalArgumentException invalid) { return null; }
    }
    public static boolean mayAcquire(ItemStack stack, UUID player, String playerClass) {
        if (present(stack)) return player.equals(owner(stack)); // Malformed owners fail closed.
        String required = ClassItemEquipmentGuard.getRequiredClass(stack);
        if (required != null) return required.equals(playerClass);
        return !net.goui.cosmicdungeon.playerclass.api.ClassItemUtil.hasAnyAttunementMetadata(stack);
    }
    public static boolean mayAcquire(ServerPlayer player, ItemStack stack) {
        return mayAcquire(stack, player.getUUID(), ClassItemEquipmentGuard.getPlayerClass(player));
    }
    public static boolean bind(ServerPlayer player, ItemStack stack) {
        return !AccessPolicy.isDeveloper(player)
                && bind(stack, player.getUUID(), ClassItemEquipmentGuard.getPlayerClass(player));
    }
    public static boolean bind(ItemStack stack, UUID player, String playerClass) {
        if (stack.isEmpty() || !attuned(stack) || present(stack) || !mayAcquire(stack, player, playerClass))
            return false;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString(KEY, player.toString()));
        return true;
    }
    /** Only for the original uncollected remainder of a temporary inventory insertion. */
    public static void unbindRemainder(ItemStack stack, UUID temporaryOwner) {
        if (!stack.isEmpty() && temporaryOwner.equals(owner(stack)))
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.remove(KEY));
    }
}
