package net.goui.cosmicdungeon.playerclass.api;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.TridentItem;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Central server-side policy for class-attuned equipment use and wear restrictions. */
public final class ClassItemEquipmentGuard {
    private ClassItemEquipmentGuard() {}

    private static final long DENIAL_MESSAGE_COOLDOWN_TICKS = 15L;
    private static final Map<UUID, Long> LAST_DENIAL_TICK = new HashMap<>();

    public static boolean hasClassAttunement(ItemStack stack) {
        return getRequiredClass(stack) != null;
    }

    public static String getRequiredClass(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        String attuned = ClassItemUtil.getClassAttunement(stack);
        if (attuned != null) return attuned;
        if (stack.getItem() instanceof ClassBoundItem bound) {
            String classId = ClassItemUtil.normalizeClassId(bound.requiredClassId());
            return ClassItemUtil.isPlayableClass(classId) ? classId : null;
        }
        return null;
    }

    public static String getPlayerClass(ServerPlayer player) {
        return player == null ? ClassKeys.CLASS_ID_NONE : ClassNbtUtil.getClassId(player);
    }

    public static boolean isGuardedEquipment(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (stack.getItem() instanceof BannerItem) return false;
        if (isWearableEquipment(stack)) return true;
        if (stack.has(DataComponents.BLOCKS_ATTACKS)) return true;
        if (stack.has(DataComponents.TOOL)) return true;
        if (stack.has(DataComponents.WEAPON)) return true;
        Item item = stack.getItem();
        return item instanceof AxeItem
                || item instanceof ShovelItem
                || item instanceof HoeItem
                || item instanceof BowItem
                || item instanceof CrossbowItem
                || item instanceof TridentItem
                || item instanceof ShieldItem
                || item instanceof MaceItem;
    }

    public static boolean isWearableEquipment(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.has(DataComponents.EQUIPPABLE) && !(stack.getItem() instanceof BannerItem);
    }

    public static boolean canUse(ServerPlayer player, ItemStack stack) {
        return can(player, stack, false);
    }

    public static boolean canWear(ServerPlayer player, ItemStack stack) {
        return can(player, stack, true);
    }

    private static boolean can(ServerPlayer player, ItemStack stack, boolean wearing) {
        String required = getRequiredClass(stack);
        if (required == null) return true;
        if (wearing) {
            if (!isWearableEquipment(stack)) return true;
        } else if (!isGuardedEquipment(stack) && !(stack.getItem() instanceof ClassBoundItem)) {
            return true;
        }
        return required.equals(getPlayerClass(player));
    }

    public static void denyUse(ServerPlayer player, ItemStack stack) {
        deny(player, stack, "use");
    }

    public static void denyWear(ServerPlayer player, ItemStack stack) {
        deny(player, stack, "wear");
    }

    private static void deny(ServerPlayer player, ItemStack stack, String verb) {
        if (player == null || player.level().isClientSide()) return;
        String required = getRequiredClass(stack);
        if (required == null) return;
        long now = player.level().getGameTime();
        Long last = LAST_DENIAL_TICK.get(player.getUUID());
        if (last != null && now - last < DENIAL_MESSAGE_COOLDOWN_TICKS) return;
        LAST_DENIAL_TICK.put(player.getUUID(), now);
        String display = ClassItemUtil.displayNameForClass(required);
        player.sendSystemMessage(Component.literal("Only a " + display + " can " + verb + " that!"));
    }
}
