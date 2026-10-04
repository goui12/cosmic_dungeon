package net.goui.cosmicdungeon.leaderboard;
import net.goui.cosmicdungeon.dungeon.d1.D1LifetimeData;
import net.goui.cosmicdungeon.dungeon.DungeonKillCredit;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.monster.Enemy;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
@EventBusSubscriber(modid="cosmicdungeon")
public final class LifetimeEvents{
    private LifetimeEvents(){}
    public static void add(ServerPlayer player,String key){
        if(player instanceof net.neoforged.neoforge.common.util.FakePlayer)return;
        var data=D1LifetimeData.get(player.level().getServer());
        data.name(player.getUUID(),player.getGameProfile().name());data.activity(player.getUUID(),key,1);
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event){
        if(event.getEntity() instanceof ServerPlayer player)add(player,"logins");
    }
    @SubscribeEvent public static void travel(PlayerEvent.PlayerChangedDimensionEvent event){
        if(event.getEntity() instanceof ServerPlayer player)add(player,"dimension_changes");
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void death(LivingDeathEvent event){
        var victim=event.getEntity();
        if(!(victim instanceof Enemy)||!(victim.level() instanceof ServerLevel level))return;
        var owner=DungeonKillCredit.resolve(victim,event.getSource());
        if(owner==null||victim.getPersistentData().getBooleanOr("cosmicdungeon_lifetime_kill_recorded",false))return;
        victim.getPersistentData().putBoolean("cosmicdungeon_lifetime_kill_recorded",true);
        D1LifetimeData.get(level.getServer()).activity(owner,"hostile_kills",1);
    }
}
