package net.goui.cosmicdungeon.economy;
import java.util.*;
import net.goui.cosmicdungeon.Config;
import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.block.entity.ClassLockedChestBlockEntity;
import net.goui.cosmicdungeon.network.CurrencyBalancePayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.PacketDistributor;
/** At most one bounded account poll per player per configured interval; no world/entity/container scans. */
@EventBusSubscriber(modid=CosmicDungeonMod.MOD_ID)
public final class BalanceDisplaySync {
    private BalanceDisplaySync(){}
    private static final Map<UUID,BalanceDisplayPoll> VIEWERS=new HashMap<>();
    private static void send(ServerPlayer player,AbstractContainerMenu menu,boolean force) {
        var poll=VIEWERS.computeIfAbsent(player.getUUID(),key->new BalanceDisplayPoll());
        long tick=player.level().getServer().overworld().getGameTime();
        if(!poll.due(tick,Config.MENU_BALANCE_POLL_TICKS.get(),force))return;
        long balance=CurrencyService.getBalanceTrace(player),available=CurrencyService.getAvailableTrace(player);
        int chestId=menu instanceof ChestMenu chest && chest.getContainer() instanceof ClassLockedChestBlockEntity
                ?menu.containerId:-1;
        if(poll.changed(balance,available,chestId,force))
            PacketDistributor.sendToPlayer(player,new CurrencyBalancePayload(player.getUUID(),poll.revision(),balance,available,chestId));
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) {
        if(event.getEntity() instanceof ServerPlayer player)send(player,player.containerMenu,false);
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if(event.getEntity() instanceof ServerPlayer player)send(player,player.containerMenu,true);
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {VIEWERS.remove(event.getEntity().getUUID());}
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if(event.getEntity() instanceof ServerPlayer player)send(player,player.containerMenu,true);
    }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if(event.getEntity() instanceof ServerPlayer player)send(player,player.containerMenu,true);
    }
    @SubscribeEvent public static void open(PlayerContainerEvent.Open event) {
        if(event.getEntity() instanceof ServerPlayer player)send(player,event.getContainer(),true);
    }
    @SubscribeEvent public static void close(PlayerContainerEvent.Close event) {
        if(event.getEntity() instanceof ServerPlayer player)send(player,player.inventoryMenu,true);
    }
    @SubscribeEvent public static void stop(ServerStoppedEvent event){VIEWERS.clear();}
}
