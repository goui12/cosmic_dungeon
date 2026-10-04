package net.goui.cosmicdungeon.mercenary;

import net.goui.cosmicdungeon.dungeon.*;
import net.goui.cosmicdungeon.dungeon.d1.D1Members;
import net.goui.cosmicdungeon.network.PartyPayloads;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

/** Resume the same retained entity. Never manufacture a replacement for an absent UUID. */
public final class MercenaryRespawns {
    private MercenaryRespawns(){}
    private static int nextPlayer;
    public static void remember(MercenaryEntity entity){
        if(!(entity.level() instanceof ServerLevel level)||entity.rest()==null)return;
        var data=DungeonRunRegistryData.get(level.getServer());
        var run=data.getRun(entity.runId()).orElse(null);
        if(MercenaryLifecycle.admitted(run,entity.contract(),entity.getUUID(),level.dimension().location().toString()))
            data.mercenaryRest(entity.runId(),entity.getUUID(),entity.rest());
    }
    public static void tick(MinecraftServer server){
        var players=server.getPlayerList().getPlayers();
        if(players.isEmpty())return;
        int start=Math.floorMod(nextPlayer,players.size()),count=Math.min(30,players.size());
        nextPlayer=(start+count)%players.size();
        for(int offset=0;offset<count;offset++){
            var player=players.get((start+offset)%players.size());
            var run=D1Members.run(player.level()).orElse(null);
            if(run==null||!D1Members.inside(player,run))continue;
            var recovery=PendingDungeonRecoveryData.get(server);
            var handoff=recovery.handoff(player.getUUID());
            if(recovery.completed(player.getUUID())>=run.runId()
                    ||handoff!=null&&handoff.run()==run.runId()&&handoff.kind().equals("cleanup"))continue;
            for(var contract:run.mercenaries()){
                if(!contract.hirer().equals(player.getUUID()))continue;
                var rest=run.mercenaryRests().get(contract.id());
                if(rest==null||!rest.due(server.overworld().getGameTime()))continue;
                var level=net.goui.cosmicdungeon.block.custom.ClassSelectorTeleportUtil.resolveLevel(server,rest.dimension());
                if(level==null||level!=player.level()||!run.containsDimension(level.dimension()))continue;
                var entity=level.getEntity(contract.id());
                if(entity==null){
                    // Share the existing bounded, expiring companion-load ticket budget.
                    var chunk=new ChunkPos(BlockPos.of(rest.position()));
                    if(!level.areEntitiesLoaded(chunk.toLong()))
                        net.goui.cosmicdungeon.playerclass.bogatyr.BogatyrRecovery.ready(server,contract.id(),level,rest.position());
                    continue;
                }
                if(!(entity instanceof MercenaryEntity mercenary)||mercenary.runId()!=run.runId()
                        ||!contract.equals(mercenary.contract()))continue;
                // Native and locator saves may finish in either order. The existing entity is authoritative.
                if(!mercenary.dormant()){
                    DungeonRunRegistryData.get(server).mercenaryRest(run.runId(),contract.id(),null);continue;
                }
                if(!rest.equals(mercenary.rest())){remember(mercenary);continue;}
                if(MercenaryBrain.teleport(mercenary,player,level)){
                    mercenary.resumeAfterRest();
                    DungeonRunRegistryData.get(server).mercenaryRest(run.runId(),contract.id(),null);
                }
            }
        }
    }
    public static PartyPayloads.Mercenary status(MinecraftServer server,DungeonRunRegistryData.RunRecord run,
                                                MercenaryContract contract,String owner){
        String name=contract.name();
        var rest=run.mercenaryRests().get(contract.id());
        for(String dimension:run.dungeonDimensionIds()){
            var level=net.goui.cosmicdungeon.block.custom.ClassSelectorTeleportUtil.resolveLevel(server,dimension);
            if(level==null)continue;
            if(level.getEntity(contract.id()) instanceof MercenaryEntity entity&&run.runId()==entity.runId()
                    &&contract.equals(entity.contract())){
                if(entity.rest()!=null)rest=entity.rest();
                else return new PartyPayloads.Mercenary(name,owner,entity.getHealth(),entity.getMaxHealth(),-1,"ACTIVE");
                break;
            }
        }
        return rest==null?new PartyPayloads.Mercenary(name,owner,0,0,-1,"UNLOADED")
                :new PartyPayloads.Mercenary(name,owner,0,0,rest.seconds(server.overworld().getGameTime()),"RESPAWNING");
    }
}
