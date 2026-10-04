package net.goui.cosmicdungeon.mercenary;
import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.dungeon.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import java.util.UUID;
/** Only admitted run contracts may restore entities; cleanup visits known UUIDs, never scans. */
@EventBusSubscriber(modid=CosmicDungeonMod.MOD_ID)
public final class MercenaryLifecycle {
    private MercenaryLifecycle(){}
    public static boolean admitted(DungeonRunRegistryData.RunRecord run,MercenaryContract contract,UUID entityId,String dimension){
        return run!=null&&contract!=null&&contract.id().equals(entityId)
            &&run.stateEnum()==DungeonRunState.ACTIVE&&run.dungeonId().equals("dungeon_1")
            &&run.dungeonDimensionIds().contains(dimension)&&run.containsPlayer(contract.hirer())
            &&!run.isCompletionExited(contract.hirer())&&run.mercenaries().contains(contract);
    }
    @SubscribeEvent public static void joined(EntityJoinLevelEvent event){
        if(!(event.getLevel() instanceof ServerLevel level)||!(event.getEntity() instanceof MercenaryEntity entity))return;
        var run=DungeonRunRegistryData.get(level.getServer()).getRun(entity.runId()).orElse(null);
        var contract=entity.contract();
        boolean valid=admitted(run,contract,entity.getUUID(),level.dimension().location().toString());
        if(valid){
            var recovery=PendingDungeonRecoveryData.get(level.getServer());
            var plan=recovery.handoff(contract.hirer());
            valid=recovery.completed(contract.hirer())<entity.runId()
                &&!(plan!=null&&plan.run()==entity.runId()&&plan.kind().equals("cleanup"));
        }
        if(!valid){event.setCanceled(true);entity.discard();}
    }
    public static void dismiss(MinecraftServer server,long runId,UUID hirer){
        var run=DungeonRunRegistryData.get(server).getRun(runId).orElse(null);if(run==null)return;
        for(var contract:run.mercenaries())if(contract.hirer().equals(hirer))
            for(String dimension:run.dungeonDimensionIds()){
                var level=net.goui.cosmicdungeon.block.custom.ClassSelectorTeleportUtil.resolveLevel(server,dimension);
                if(level!=null&&level.getEntity(contract.id()) instanceof MercenaryEntity entity)entity.discard();
            }
    }
}
