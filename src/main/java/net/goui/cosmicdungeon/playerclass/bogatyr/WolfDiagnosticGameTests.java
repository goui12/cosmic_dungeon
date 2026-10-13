package net.goui.cosmicdungeon.playerclass.bogatyr;

import java.util.*;
import net.goui.cosmicdungeon.dungeon.*;
import net.minecraft.core.UUIDUtil;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import static net.goui.cosmicdungeon.playerclass.bogatyr.WolfDiagnosticWindow.Event.*;

/** Native insertion and legacy disk admission paths, including callbacks NeoForge emits after rejection. */
public final class WolfDiagnosticGameTests {
    private WolfDiagnosticGameTests(){}
    @SuppressWarnings("unchecked")
    public static void admission(GameTestHelper helper){
        try(var f=new BogatyrCommandGameTests.Fixture(helper,Long.MAX_VALUE-3202)){
            var server=f.level.getServer();
            long fresh=WolfSpawnDiagnostics.count(server,ADDED_FRESH);
            var original=f.wolf(0,0,0,0);
            f.check(WolfSpawnDiagnostics.count(server,ADDED_FRESH)==fresh+1,"Actual native fresh insertion counted once");
            var image=BogatyrRecovery.image(original);
            original.setRemoved(Entity.RemovalReason.UNLOADED_TO_CHUNK);
            var paused=f.runs.get(f.run).withState(DungeonRunState.PAUSED,DungeonResetReason.MANUAL);f.runs.put(f.run,paused);
            long disk=WolfSpawnDiagnostics.count(server,ADDED_DISK);
            var loaded=f.readWolf(image);
            f.level.addLegacyChunkEntities(java.util.stream.Stream.of(loaded));
            f.check(f.level.getEntity(loaded.getUUID())==loaded&&WolfSpawnDiagnostics.count(server,ADDED_DISK)==disk+1,
                    "Native saved wolf in paused run is a disk addition, not a new spawn");
            var cloneImage=image.copy();var cloneId=UUID.randomUUID();cloneImage.store("UUID",UUIDUtil.CODEC,cloneId);
            var clone=f.readWolf(cloneImage);
            long rejected=WolfSpawnDiagnostics.count(server,REJECT_IDENTITY);
            disk=WolfSpawnDiagnostics.count(server,ADDED_DISK);
            try{
                f.level.addLegacyChunkEntities(java.util.stream.Stream.of(clone));
                f.check(f.level.getEntity(cloneId)==null&&WolfSpawnDiagnostics.count(server,REJECT_IDENTITY)>rejected,
                        "Copied bond is rejected with evidence");
                f.check(WolfSpawnDiagnostics.count(server,ADDED_DISK)==disk,"Canceled legacy callback cannot claim successful insertion");
            }finally{
                clone.discard(); // Retire the rejected callback while its fixture hold still protects the real bond.
                try{
                    var field=BogatyrCompanionData.class.getDeclaredField("identityHolds");field.setAccessible(true);
                    ((Set<String>)field.get(BogatyrCompanionData.get(server))).remove(f.level.dimension().location()+"|"+cloneId);
                }catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
            }
            // A third-party cancellation does not run our explicit rejection hook. Keep objects alive
            // to saturate pending metadata deterministically without depending on the garbage collector.
            var canceled=new HashSet<UUID>();
            java.util.function.Consumer<EntityJoinLevelEvent> veto=e->{if(canceled.contains(e.getEntity().getUUID()))e.setCanceled(true);};
            NeoForge.EVENT_BUS.addListener(veto);
            try{
                for(int i=0;i<260;i++){
                    var wolf=EntityType.WOLF.create(f.level,EntitySpawnReason.MOB_SUMMONED);wolf.setPos(f.origin);
                    canceled.add(wolf.getUUID());f.extras.add(wolf);
                    f.check(!f.level.addFreshEntity(wolf),"Third-party admission canceled");
                }
            }finally{NeoForge.EVENT_BUS.unregister(veto);}
            var nextImage=BogatyrRecovery.image(loaded);loaded.setRemoved(Entity.RemovalReason.UNLOADED_TO_CHUNK);
            var next=f.readWolf(nextImage);fresh=WolfSpawnDiagnostics.count(server,ADDED_FRESH);
            long unknown=WolfSpawnDiagnostics.count(server,ADDED_UNKNOWN);
            f.level.addLegacyChunkEntities(java.util.stream.Stream.of(next));
            f.check(f.level.getEntity(next.getUUID())==next,"Real disk admission remains unaffected by diagnostics capacity");
            f.check(WolfSpawnDiagnostics.count(server,ADDED_FRESH)==fresh&&WolfSpawnDiagnostics.count(server,ADDED_UNKNOWN)==unknown+1,
                    "Missing bounded metadata is explicitly unknown, never mislabeled as fresh");
        }
        helper.succeed();
    }
}
