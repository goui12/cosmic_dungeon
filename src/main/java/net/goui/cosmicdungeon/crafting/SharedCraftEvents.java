package net.goui.cosmicdungeon.crafting;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.TriState;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.*;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

@EventBusSubscriber(modid = "cosmicdungeon")
public final class SharedCraftEvents {
    private SharedCraftEvents() {}
    @SubscribeEvent(priority = EventPriority.HIGHEST) public static void login(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) { SharedCrafting.forget(p); SharedCraftTransactions.reconcile(p); }
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) SharedCrafting.forget(p);
    }
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) { SharedCrafting.forget(p); SharedCraftTransactions.reconcile(p); }
    }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) SharedCrafting.forget(p);
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST) public static void death(LivingDeathEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && SharedCraftTransactions.blocked(p)) {
            e.setCanceled(true); p.setHealth(Math.max(1, p.getHealth()));
        }
    }
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void pickup(ItemEntityPickupEvent.Pre e) {
        if (e.getPlayer() instanceof ServerPlayer p && SharedCraftTransactions.blocked(p)) e.setCanPickup(TriState.FALSE);
    }
    @SubscribeEvent public static void stop(ServerStoppedEvent e) { SharedCrafting.stopped(); PotionCrafting.stopped(); SharedCraftTransactions.stopped(); }
}
