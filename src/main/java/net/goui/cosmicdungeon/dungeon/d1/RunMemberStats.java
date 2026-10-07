package net.goui.cosmicdungeon.dungeon.d1;

import java.util.UUID;
import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.auth.AccessPolicy;
import net.goui.cosmicdungeon.dungeon.DungeonKillCredit;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Enemy;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Event-driven current-run readings in the existing extensible run store; never lifetime statistics. */
@EventBusSubscriber(modid=CosmicDungeonMod.MOD_ID)
public final class RunMemberStats {
    private static final String DEATH_RECORDED="cosmicdungeon_inspection_death_recorded";
    private RunMemberStats() {}
    private static String key(String metric,UUID member){return "inspection:"+metric+":"+member;}
    public static double value(D1RunData data,long run,UUID member,String metric) {
        var values=data.values(run,key(metric,member));
        if(values.isEmpty())return 0;
        try { double value=Double.parseDouble(values.getFirst());return Double.isFinite(value)&&value>=0?value:0; }
        catch(NumberFormatException invalid){return 0;}
    }
    public static void add(D1RunData data,long run,UUID member,String metric,double amount) {
        if(!Double.isFinite(amount)||amount<=0)return;
        double next=Math.min(1.0E15,value(data,run,member,metric)+amount);
        data.setValue(run,key(metric,member),Double.toString(next));
    }
    @SubscribeEvent public static void damaged(LivingDamageEvent.Post event) {
        if(!(event.getEntity() instanceof Enemy)||!(event.getEntity().level() instanceof ServerLevel level)
                ||event.getNewDamage()<=0)return;
        UUID owner=DungeonKillCredit.controller(event.getSource().getEntity());
        if(owner==null)return;
        var player=level.getServer().getPlayerList().getPlayer(owner);
        var run=D1Members.run(level).orElse(null);
        if(player==null||run==null||!D1Members.inside(player,run))return;
        add(D1RunData.get(level.getServer()),run.runId(),owner,"damage",event.getNewDamage());
    }
    /** Called after native heal actually sets health, so cancellation and overheal never inflate totals. */
    public static void healed(ServerPlayer player,float before,float after) {
        var run=D1Members.run(player.level()).orElse(null);
        if(run!=null&&D1Members.inside(player,run))
            add(D1RunData.get(player.level().getServer()),run.runId(),player.getUUID(),"healing_received",Math.max(0,after-before));
    }
    /** Called only at the existing uncancelled native death tail. Duplicate death callbacks are ignored. */
    public static void died(ServerPlayer player) {
        if(!player.isDeadOrDying()||player.isSpectator()||AccessPolicy.isDeveloper(player)
                ||player.getPersistentData().getBooleanOr(DEATH_RECORDED,false))return;
        var run=D1Members.run(player.level()).filter(r->r.containsPlayer(player.getUUID())&&!r.isCompletionExited(player.getUUID())).orElse(null);
        if(run==null)return;
        player.getPersistentData().putBoolean(DEATH_RECORDED,true);
        add(D1RunData.get(player.level().getServer()),run.runId(),player.getUUID(),"deaths",1);
    }
    @SubscribeEvent public static void respawned(PlayerEvent.PlayerRespawnEvent event) {
        event.getEntity().getPersistentData().remove(DEATH_RECORDED);
    }
}
