package net.goui.cosmicdungeon.playerclass.dragoon.repair;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID)
public final class DragoonRepairEvents {
    private DragoonRepairEvents() {}
    @SubscribeEvent public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e){ if(e.getEntity() instanceof ServerPlayer sp) DragoonRepairSessionData.handleLogout(sp); }
    @SubscribeEvent public static void onTick(ServerTickEvent.Post e){ MinecraftServer s=e.getServer(); if(s!=null && s.getTickCount()%20==0) DragoonRepairSessionData.tick(s); }
}
