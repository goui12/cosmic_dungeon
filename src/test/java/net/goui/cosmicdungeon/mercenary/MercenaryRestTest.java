package net.goui.cosmicdungeon.mercenary;
import net.goui.cosmicdungeon.dungeon.*;
import net.goui.cosmicdungeon.network.PartyPayloads;
import net.goui.cosmicdungeon.client.screen.*;
import net.minecraft.nbt.*;
import net.minecraft.core.BlockPos;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
final class MercenaryRestTest {
    private final UUID owner=UUID.randomUUID(),id=UUID.randomUUID();
    private final String dim="cosmicdungeon:d1_instance_1";
    private final MercenaryRest rest=new MercenaryRest(12100,dim,new BlockPos(-40,62,300).asLong(),16);
    private DungeonRunRegistryData.RunRecord run(){
        return new DungeonRunRegistryData.RunRecord(1,"dungeon_1","minecraft:overworld",0,List.of(dim),1,
            "ACTIVE","",0,List.of(owner),List.of(),List.of()).withMercenaries(
            List.of(new MercenaryContract(id,owner,"theurgist",2,500)));
    }
    @Test void tenMinutesUsesServerTicksAndRoundsRemainingSecondsUp(){
        assertEquals(12100,MercenaryRest.deadline(100));assertEquals(600,rest.seconds(100));
        assertEquals(600,rest.seconds(101));assertEquals(599,rest.seconds(120));
        assertEquals(1,rest.seconds(12099));assertFalse(rest.due(12099));
        assertEquals(0,rest.seconds(12100));assertTrue(rest.due(12100));assertEquals(0,rest.seconds(99999));
    }
    @Test void deadlineSaturatesAndCountdownCannotOverflow(){
        assertEquals(Long.MAX_VALUE,MercenaryRest.deadline(Long.MAX_VALUE));
        assertEquals(12000,MercenaryRest.deadline(-100));
        assertEquals(600,new MercenaryRest(Long.MAX_VALUE,dim,0,0).seconds(0));
        assertEquals(0,rest.seconds(Long.MAX_VALUE));
    }
    @Test void nativeRestRoundtripPreservesClockPositionAndFlags(){
        for(int flags=0;flags<64;flags++){
            var value=new MercenaryRest(12100,dim,rest.position(),flags);
            var tag=MercenaryRest.CODEC.encodeStart(NbtOps.INSTANCE,value).getOrThrow();
            var loaded=MercenaryRest.CODEC.parse(NbtOps.INSTANCE,tag).getOrThrow();
            assertEquals(value,loaded);assertEquals(600,loaded.seconds(100));
            for(int bit:List.of(1,2,4,8,16,32))assertEquals((flags&bit)!=0,loaded.flag(bit));
        }
    }
    @Test void malformedDeadlineAndFlagsFailCodec(){
        var tag=(CompoundTag)MercenaryRest.CODEC.encodeStart(NbtOps.INSTANCE,rest).getOrThrow();
        tag.putLong("until",-1);assertTrue(MercenaryRest.CODEC.parse(NbtOps.INSTANCE,tag).error().isPresent());
        tag.putLong("until",0);tag.putInt("flags",64);assertTrue(MercenaryRest.CODEC.parse(NbtOps.INSTANCE,tag).error().isPresent());
    }
    @Test void oldRunDefaultsToNoRestWithoutMigration(){
        var tag=(CompoundTag)DungeonRunRegistryData.RunRecord.CODEC.encodeStart(NbtOps.INSTANCE,run()).getOrThrow();
        tag.remove("mercenary_rests");
        assertTrue(DungeonRunRegistryData.RunRecord.CODEC.parse(NbtOps.INSTANCE,tag).getOrThrow().mercenaryRests().isEmpty());
    }
    @Test void runLocatorSurvivesNativeCodecAndTransitions(){
        var value=run().withMercenaryRests(Map.of(id,rest));
        var loaded=DungeonRunRegistryData.RunRecord.CODEC.parse(NbtOps.INSTANCE,
            DungeonRunRegistryData.RunRecord.CODEC.encodeStart(NbtOps.INSTANCE,value).getOrThrow()).getOrThrow();
        assertEquals(value,loaded);
        for(var changed:List.of(loaded.withCompletionExited(owner),loaded.withoutPlayer(owner),
                loaded.withState(DungeonRunState.RESETTING,DungeonResetReason.ABANDONED),
                loaded.withDifficulty(DungeonDifficulty.Profile.LEGACY),loaded.withInstance(1,List.of(dim)),
                loaded.withMercenaries(loaded.mercenaries())))assertEquals(Map.of(id,rest),changed.mercenaryRests());
        assertFalse(MercenaryLifecycle.admitted(loaded.withCompletionExited(owner),loaded.mercenaries().getFirst(),id,dim));
        assertFalse(MercenaryLifecycle.admitted(loaded.withoutPlayer(owner),loaded.mercenaries().getFirst(),id,dim));
    }
    @Test void locatorCannotReferenceForeignIdentityOrDimension(){
        assertThrows(IllegalArgumentException.class,()->run().withMercenaryRests(Map.of(UUID.randomUUID(),rest)));
        assertThrows(IllegalArgumentException.class,()->run().withMercenaryRests(Map.of(id,new MercenaryRest(12000,"minecraft:overworld",0,16))));
    }
    @Test void registryUpdatesAndRemovesOnlyExistingContractLocators(){
        DungeonRunRegistryData data;
        // Decode the actual SavedData codec so this exercises the same map updated by server lifecycle events.
        try{
            var field=DungeonRunRegistryData.class.getDeclaredField("CODEC");field.setAccessible(true);
            @SuppressWarnings("unchecked") var codec=(com.mojang.serialization.Codec<DungeonRunRegistryData>)field.get(null);
            var root=new CompoundTag();root.putLong("next_run_id",2);
            var runs=new ListTag();runs.add(DungeonRunRegistryData.RunRecord.CODEC.encodeStart(NbtOps.INSTANCE,run()).getOrThrow());
            root.put("runs",runs);data=codec.parse(NbtOps.INSTANCE,root).getOrThrow();
            data.mercenaryRest(1,UUID.randomUUID(),rest);assertTrue(data.getRun(1).orElseThrow().mercenaryRests().isEmpty());
            data.mercenaryRest(1,id,rest);data=codec.parse(NbtOps.INSTANCE,codec.encodeStart(NbtOps.INSTANCE,data).getOrThrow()).getOrThrow();
            assertEquals(rest,data.getRun(1).orElseThrow().mercenaryRests().get(id));
            data.mercenaryRest(1,id,null);data.mercenaryRest(1,id,null);
            assertTrue(data.getRun(1).orElseThrow().mercenaryRests().isEmpty());
        }catch(ReflectiveOperationException e){throw new AssertionError(e);}
    }
    private PartyPayloads.View view(List<PartyPayloads.Mercenary> hires){
        return new PartyPayloads.View(-1,new PartyPayloads.State(3,"ACTIVE",true,6,0,-1),
            List.of(new PartyPayloads.Member("Hirer","bogatyr",true,true)),new PartyPayloads.Invite("","",false,false),
            PartyPayloads.Recruitment.EMPTY,"HARD",PartyPayloads.Hire.NONE,hires);
    }
    private PartyPayloads.Mercenary active(){return new PartyPayloads.Mercenary("Mercenary","Hirer",7,20,-1,"ACTIVE");}
    private PartyPayloads.Mercenary waiting(int seconds){return new PartyPayloads.Mercenary("Mercenary","Hirer",0,0,seconds,"RESPAWNING");}
    @Test void nativePacketCarriesAllThreeStatusesAndFractionalHealth(){
        var value=view(List.of(active(),waiting(600),new PartyPayloads.Mercenary("Mercenary","Hirer",0,0,-1,"UNLOADED")));
        var buf=Unpooled.buffer();try{
            PartyPayloads.View.STREAM_CODEC.encode(buf,value);assertEquals(value,PartyPayloads.View.STREAM_CODEC.decode(buf));
            assertEquals(0,buf.readableBytes());
        }finally{buf.release();}
    }
    @Test void hudRejectsUnboundedOrNonFiniteRows(){
        assertThrows(IllegalArgumentException.class,()->view(List.of(active(),active(),active(),active())));
        assertThrows(IllegalArgumentException.class,()->new PartyPayloads.Mercenary("x","y",Float.NaN,20,-1,"ACTIVE"));
        assertThrows(IllegalArgumentException.class,()->new PartyPayloads.Mercenary("x","y",30,20,-1,"ACTIVE"));
        assertThrows(IllegalArgumentException.class,()->waiting(601));
        assertThrows(IllegalArgumentException.class,()->new PartyPayloads.Mercenary("x","y",0,0,0,"FORGED"));
    }
    @Test void oldPacketCallersDefaultToEmptyMercenaryStack(){
        var v=view(List.of());var old=new PartyPayloads.View(v.containerId(),v.state(),v.members(),v.invitation(),v.recruitment(),"HARD");
        assertEquals(v,old);assertEquals(0,MercenaryHudLayout.stackHeight(0));
    }
    @Test void maximumPartyAndMercenaryStackLeavesCurrencyInsideMinimumGuiHeight(){
        // Six total slots: one to three hires replace human rows, never duplicate them.
        for(int hires=1;hires<=3;hires++){
            var party=D1PartyHudLayout.forView(-1,6-hires);
            int currencyBottom=party.y()+party.height()+MercenaryHudLayout.stackHeight(hires)+8+29;
            assertTrue(currencyBottom<240);
            assertTrue(MercenaryHudLayout.stackHeight(hires)>=6+hires*26);
        }
        assertEquals(88,MercenaryHudLayout.stackHeight(99));
    }
    @Test void healthBarsAndCountdownLabelsCoverDeadAndReadyStates(){
        assertEquals(35,MercenaryHudLayout.healthWidth(active(),100));
        assertEquals(0,MercenaryHudLayout.healthWidth(waiting(1),100));
        assertEquals("Respawn 10:00",MercenaryHudLayout.status(waiting(600)));
        assertEquals("Respawn 0:01",MercenaryHudLayout.status(waiting(1)));
        assertEquals("Awaiting safe return",MercenaryHudLayout.status(waiting(0)));
        assertEquals("Health 7.0 / 20.0",MercenaryHudLayout.status(active()));
    }
}
