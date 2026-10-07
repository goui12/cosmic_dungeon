package net.goui.cosmicdungeon.npc.tamsin;

import java.util.*;
import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.dungeon.*;
import net.goui.cosmicdungeon.dungeon.d1.*;
import net.goui.cosmicdungeon.network.*;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.goui.cosmicdungeon.playerclass.ore.SatchelApi;
import net.goui.cosmicdungeon.playerclass.metalmancer.MetalmancerActions;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/** Server-thread, same-active-run access. No client-selected player or run bypasses membership checks. */
@EventBusSubscriber(modid=CosmicDungeonMod.MOD_ID)
public final class PartyInspectionService {
    private static final Map<UUID,Long> LAST=new HashMap<>();
    private PartyInspectionService() {}
    public static boolean authorized(ServerPlayer viewer,DungeonRunRegistryData.RunRecord run,PartyInspectionPayloads.Request request) {
        return run!=null && run.runId()==request.run() && run.stateEnum()==DungeonRunState.ACTIVE
                && !D1RunData.get(viewer.level().getServer()).sealed(run.runId())
                && run.containsPlayer(viewer.getUUID()) && !run.isCompletionExited(viewer.getUUID())
                && run.containsPlayer(request.subject()) && !run.isCompletionExited(request.subject())
                && run.containsDimension(viewer.level().dimension()) && viewer.containerMenu==viewer.inventoryMenu
                && viewer.connection!=null && !viewer.hasDisconnected();
    }
    public static PartyInspectionPayloads.View snapshot(ServerPlayer viewer,PartyInspectionPayloads.Request request) {
        var server=viewer.level().getServer();
        var run=DungeonRunRegistryData.get(server).findRunForPlayer(viewer.getUUID()).orElse(null);
        if(!authorized(viewer,run,request))return PartyInspectionPayloads.View.denied(request);
        var subject=server.getPlayerList().getPlayer(request.subject());
        var identity=D1PartyHudService.identity(server,request.subject());
        var vitals=PartyVitalsSnapshot.capture(subject,subject!=null&&run.containsDimension(subject.level().dimension()));
        var data=D1RunData.get(server);
        var readings=new ArrayList<PartyInspectionPayloads.Reading>();
        readings.add(new PartyInspectionPayloads.Reading("Run kills",Integer.toString(data.count(run.runId(),"kills:"+request.subject()))));
        for(String metric:List.of("damage","healing_received","deaths")) {
            String label=switch(metric){case "damage"->"Damage dealt to hostiles (HP)";case "healing_received"->"Healing received (HP)";default->"Run deaths";};
            readings.add(new PartyInspectionPayloads.Reading(label,String.format(Locale.ROOT,"%.1f",
                    RunMemberStats.value(data,run.runId(),request.subject(),metric))));
        }
        if(vitals.state().equals("ACTIVE")) {
            if(ClassData.getClassId(subject).equals("metalmancer")) {
                readings.add(new PartyInspectionPayloads.Reading("Ore",SatchelApi.get(subject)+" / "+SatchelApi.capacity(subject)));
                readings.addAll(MetalmancerActions.inspectionCooldowns(subject));
            } else readings.add(new PartyInspectionPayloads.Reading("Ability cooldowns","No timed class action"));
            var shown=new HashSet<net.minecraft.resources.ResourceLocation>();
            int extra=0;
            for(int i=0;i<subject.getInventory().getContainerSize();i++) {
                var stack=subject.getInventory().getItem(i);
                if(stack.isEmpty()||!subject.getCooldowns().isOnCooldown(stack))continue;
                var group=subject.getCooldowns().getCooldownGroup(stack);
                if(!shown.add(group))continue;
                if(readings.size()>=22){extra++;continue;}
                String label="Item: "+stack.getHoverName().getString();
                if(label.length()>48)label=label.substring(0,48);
                readings.add(new PartyInspectionPayloads.Reading(label,
                        (int)Math.ceil(subject.getCooldowns().getCooldownPercent(stack,0)*100)+"% cooldown remaining"));
            }
            if(extra>0)readings.add(new PartyInspectionPayloads.Reading("Other active item cooldowns",Integer.toString(extra)));
        } else readings.add(new PartyInspectionPayloads.Reading("Resources / cooldowns","Unavailable while "+vitals.state().toLowerCase(Locale.ROOT)));
        return new PartyInspectionPayloads.View(request,true,identity.name(),identity.classId(),vitals,readings);
    }
    public static void request(ServerPlayer player,PartyInspectionPayloads.Request request) {
        long now=player.level().getServer().overworld().getGameTime();
        long last=LAST.getOrDefault(player.getUUID(),Long.MIN_VALUE/2);
        if(now>=last && now-last<10)return;
        LAST.put(player.getUUID(),now);
        ModNetwork.sendTo(player,snapshot(player,request));
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){LAST.remove(event.getEntity().getUUID());}
    @SubscribeEvent public static void stopped(ServerStoppedEvent event){LAST.clear();}
}
