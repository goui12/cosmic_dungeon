// file: src/main/java/net/goui/cosmicdungeon/auth/AccessPolicy.java
package net.goui.cosmicdungeon.auth;

import net.goui.cosmicdungeon.block.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Canonical Cosmic Dungeon permission policy.
 *
 * Separation of concerns:
 * - use-protected devices: cannot be interacted with by dungeoneers
 * - break-protected devices: cannot be broken by dungeoneers
 */
public final class AccessPolicy {
    private AccessPolicy() {}

    /* -------------------- Denial message rate limit -------------------- */

    private static final Map<UUID, Long> NEXT_MSG_TICK = new ConcurrentHashMap<>();
    private static final long MSG_COOLDOWN_TICKS = 20; // 1 second

    public static void deny(ServerPlayer sp, String msg) {
        if (sp == null) return;
        long now = sp.level().getGameTime();
        long next = NEXT_MSG_TICK.getOrDefault(sp.getUUID(), 0L);
        if (now < next) return;

        NEXT_MSG_TICK.put(sp.getUUID(), now + MSG_COOLDOWN_TICKS);
        sp.displayClientMessage(Component.literal(msg).withStyle(ChatFormatting.RED), true);
    }

    /* -------------------- Rank predicates -------------------- */

    public static boolean isDeveloper(ServerPlayer sp) {
        return sp != null && Authority.isDeveloper(sp);
    }

    /**
     * Brigadier-friendly predicate:
     * - allows console / command block / rcon
     * - allows Developer
     * - shows failure message only to players
     */
    public static boolean requireDeveloperOrConsole(CommandSourceStack src) {
        if (src == null) return false;

        ServerPlayer p = src.getPlayer();
        if (p == null) return true; // console / CB / rcon

        if (isDeveloper(p)) return true;

        src.sendFailure(Component.literal("You do not have permission to use this command.")
                .withStyle(ChatFormatting.RED));
        return false;
    }

    /* -------------------- Device protection -------------------- */

    /**
     * Devices that cannot be USED (right-clicked) by dungeoneers.
     */
    public static boolean isUseProtectedDevice(Block b) {
        if (b == null) return false;

        return     b == ModBlocks.COSMIC_RIFT.get()
                || b == ModBlocks.COSMIC_RIFT_TILE.get()
                || b == ModBlocks.INFINITE_DISPENSER.get()
                || b == ModBlocks.REDSTONE_TRANSMITTER.get()
                || b == ModBlocks.REDSTONE_RECEIVER.get();
    }

    /**
     * Devices that cannot be BROKEN by dungeoneers.
     * (Includes class selector.)
     */
    public static boolean isBreakProtectedDevice(Block b) {
        if (b == null) return false;

        return     isUseProtectedDevice(b)
                || b == ModBlocks.CLASS_SELECTOR_BLOCK.get();
    }

    public static boolean canUseProtectedDevices(ServerPlayer sp) {
        return isDeveloper(sp);
    }

    public static boolean canBreakProtectedDevices(ServerPlayer sp) {
        return isDeveloper(sp);
    }

    /* -------------------- Class predicates -------------------- */

    public static boolean hasClass(ServerPlayer sp, String classId) {
        if (sp == null) return false;
        return net.goui.cosmicdungeon.playerclass.api.ClassNbtUtil.hasClass(sp, classId);
    }

    public static boolean requireClass(ServerPlayer sp, String classId, String denyMsg) {
        if (sp == null) return false;
        if (hasClass(sp, classId)) return true;
        deny(sp, denyMsg != null ? denyMsg : "You are not the correct class.");
        return false;
    }

    public static boolean requireAnyClass(ServerPlayer sp, java.util.Set<String> allowed, String denyMsg) {
        if (sp == null) return false;
        String current = net.goui.cosmicdungeon.playerclass.api.ClassNbtUtil.getClassId(sp);
        if (allowed != null && allowed.contains(current)) return true;
        deny(sp, denyMsg != null ? denyMsg : "You are not the correct class.");
        return false;
    }
}
