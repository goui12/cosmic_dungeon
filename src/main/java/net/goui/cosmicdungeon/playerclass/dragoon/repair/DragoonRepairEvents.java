package net.goui.cosmicdungeon.playerclass.dragoon.repair;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID)
public final class DragoonRepairEvents {
    private DragoonRepairEvents() {}
    @SubscribeEvent(priority=net.neoforged.bus.api.EventPriority.HIGHEST) public static void onLogin(PlayerEvent.PlayerLoggedInEvent e){ if(e.getEntity() instanceof ServerPlayer p) RepairCustody.claim(p); }
    @SubscribeEvent public static void onRespawn(PlayerEvent.PlayerRespawnEvent e){if(e.getEntity() instanceof ServerPlayer p)RepairCustody.claim(p);}
    @SubscribeEvent public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e){ if(e.getEntity() instanceof ServerPlayer sp) DragoonRepairSessionData.handleLogout(sp); }
    @SubscribeEvent public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent e){ if(e.getEntity() instanceof ServerPlayer sp) DragoonRepairSessionData.cancel(sp, "dimension changed"); }
    @SubscribeEvent public static void onDeath(LivingDeathEvent e){ if(e.getEntity() instanceof ServerPlayer sp) DragoonRepairSessionData.cancel(sp, "player died"); }
    @SubscribeEvent public static void onTick(ServerTickEvent.Post e){ MinecraftServer s=e.getServer(); if(s!=null && DragoonRepairSessionData.needsTick(s)) DragoonRepairSessionData.tick(s); }
    @SubscribeEvent public static void onServerStarted(net.neoforged.neoforge.event.server.ServerStartedEvent e){
        var data=net.goui.cosmicdungeon.economy.PlayerCurrencyData.get(e.getServer());
        // Menus do not survive a restart. RESERVED guarantees no fee was committed.
        // Terminal COMMITTED decisions are left intact; participant login/claim reconciles their item plans.
        if(data.cancelReservations("dragoon_repair",System.currentTimeMillis(),"server restarted before repair commit")>0
                &&!data.flushVerified())
            com.mojang.logging.LogUtils.getLogger().error("Repair reservation release awaits a verified currency save");
    }
    @SubscribeEvent public static void onServerStopping(ServerStoppingEvent e){ DragoonRepairSessionData.clearAll(); }
}
