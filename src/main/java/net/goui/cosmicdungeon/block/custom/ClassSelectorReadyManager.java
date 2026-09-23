package net.goui.cosmicdungeon.block.custom;

import net.goui.cosmicdungeon.npc.tamsin.D1PartyService;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Compatibility integration points; there is no shared selector ready pool. */
public final class ClassSelectorReadyManager {
    private ClassSelectorReadyManager() {}
    public static void clear() { D1PartyService.clear(); }
    public static void withdraw(ServerPlayer player) { D1PartyService.withdraw(player); }
    public static void tick(MinecraftServer server) { D1PartyService.tick(server); }
}
