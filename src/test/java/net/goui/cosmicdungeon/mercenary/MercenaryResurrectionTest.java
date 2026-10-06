package net.goui.cosmicdungeon.mercenary;

import com.mojang.serialization.Codec;
import io.netty.buffer.Unpooled;
import java.util.*;
import net.goui.cosmicdungeon.client.screen.MercenaryHudLayout;
import net.goui.cosmicdungeon.dungeon.d1.D1RunData;
import net.goui.cosmicdungeon.network.PartyPayloads;
import net.minecraft.nbt.*;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class MercenaryResurrectionTest {
    @SuppressWarnings("unchecked") private Codec<D1RunData> codec() throws Exception{
        var field=D1RunData.class.getDeclaredField("CODEC");field.setAccessible(true);return (Codec<D1RunData>)field.get(null);
    }
    private D1RunData data() throws Exception{return codec().parse(NbtOps.INSTANCE,new CompoundTag()).getOrThrow();}
    private D1RunData reload(D1RunData data) throws Exception{
        return codec().parse(NbtOps.INSTANCE,codec().encodeStart(NbtOps.INSTANCE,data).getOrThrow()).getOrThrow();
    }
    private MercenaryResurrectionState.Death death(){
        return new MercenaryResurrectionState.Death(UUID.randomUUID(),"cosmicdungeon:d1_instance_1",new Vec3(12.125,68.75,-23.625),137.5f,-31.25f);
    }
    @Test void exactFractionalLocationAndRotationSurviveExistingRunCodec() throws Exception{
        var data=data();var owner=UUID.randomUUID();var death=death();
        data.setCount(1,"unrelated_objective",7);
        MercenaryResurrectionState.remember(data,1,owner,death);
        var restored=reload(data);
        assertEquals(death,MercenaryResurrectionState.death(restored,1,owner));
        assertEquals(7,restored.count(1,"unrelated_objective"));
        assertNull(MercenaryResurrectionState.death(restored,2,owner));
    }
    @Test void latestDeathIsSingleUseAndCannotSpendAnotherMercenaryOnReplay() throws Exception{
        var data=data();var owner=UUID.randomUUID();var merc=UUID.randomUUID();var first=death();var second=death();
        MercenaryResurrectionState.remember(data,1,owner,first);
        MercenaryResurrectionState.remember(data,1,owner,second);
        assertFalse(MercenaryResurrectionState.consume(data,1,owner,first.id(),merc,100));
        assertEquals(0,MercenaryResurrectionState.readyAt(data,1,merc));
        assertTrue(MercenaryResurrectionState.consume(data,1,owner,second.id(),merc,100));
        assertEquals(3700,MercenaryResurrectionState.readyAt(data,1,merc));
        assertFalse(MercenaryResurrectionState.consume(data,1,owner,second.id(),UUID.randomUUID(),100));
        assertNull(MercenaryResurrectionState.death(reload(data),1,owner));
    }
    @Test void cooldownPersistsAcrossReloadAndEndsExactlyAtThreeMinutes() throws Exception{
        var data=data();var owner=UUID.randomUUID();var merc=UUID.randomUUID();var first=death();var second=death();
        MercenaryResurrectionState.remember(data,1,owner,first);
        assertTrue(MercenaryResurrectionState.consume(data,1,owner,first.id(),merc,100));
        data=reload(data);
        MercenaryResurrectionState.remember(data,1,owner,second);
        assertEquals(180,MercenaryResurrectionState.seconds(MercenaryResurrectionState.readyAt(data,1,merc),100));
        assertEquals(1,MercenaryResurrectionState.seconds(3700,3699));
        assertFalse(MercenaryResurrectionState.consume(data,1,owner,second.id(),merc,3699));
        assertTrue(MercenaryResurrectionState.consume(data,1,owner,second.id(),merc,3700));
        assertEquals(0,MercenaryResurrectionState.readyAt(data,2,merc));
        data.clearRun(1);
        assertEquals(0,MercenaryResurrectionState.readyAt(reload(data),1,merc));
    }
    @Test void missingLegacyFieldsAreReadyAndMalformedRecordsFailClosed() throws Exception{
        var data=data();var owner=UUID.randomUUID();var merc=UUID.randomUUID();
        assertNull(MercenaryResurrectionState.death(data,1,owner));
        assertEquals(0,MercenaryResurrectionState.readyAt(data,1,merc));
        data.setValue(1,MercenaryResurrectionState.cooldownKey(merc),"broken");
        assertEquals(Long.MAX_VALUE,MercenaryResurrectionState.readyAt(reload(data),1,merc));
        assertNull(MercenaryResurrectionState.decode("{}"));
        assertNull(MercenaryResurrectionState.decode("x".repeat(1025)));
        assertThrows(IllegalArgumentException.class,()->new MercenaryResurrectionState.Death(UUID.randomUUID(),"bad dimension",Vec3.ZERO,0,0));
        assertThrows(IllegalArgumentException.class,()->new MercenaryResurrectionState.Death(UUID.randomUUID(),"minecraft:overworld",new Vec3(Double.NaN,0,0),0,0));
    }
    @Test void protectionExpiresAfterOneHundredTicksAndCannotSurviveClockRollback(){
        assertTrue(MercenaryResurrectionState.protectedAt(1100,1000));
        assertTrue(MercenaryResurrectionState.protectedAt(1100,1099));
        assertFalse(MercenaryResurrectionState.protectedAt(1100,1100));
        assertFalse(MercenaryResurrectionState.protectedAt(1100,999));
        assertFalse(MercenaryResurrectionState.protectedAt(0,0));
    }
    @Test void requestAndHudRoundTripWithBoundedOfferIdentity(){
        var merc=UUID.randomUUID();var death=UUID.randomUUID();
        var request=new PartyPayloads.Resurrect(42,merc,death);
        var row=new PartyPayloads.Mercenary("Edmund","Owner",20,20,-1,"ACTIVE",PartyPayloads.Recovery.NONE,
                List.of(new PartyPayloads.Skill("positive_potions",45),new PartyPayloads.Skill("negative_potions",0)),
                new PartyPayloads.Resurrection(42,merc.toString(),death.toString(),0));
        var buf=Unpooled.buffer();try{
            PartyPayloads.Resurrect.STREAM_CODEC.encode(buf,request);
            assertEquals(request,PartyPayloads.Resurrect.STREAM_CODEC.decode(buf));
            PartyPayloads.Mercenary.CODEC.encode(buf,row);
            assertEquals(row,PartyPayloads.Mercenary.CODEC.decode(buf));assertEquals(0,buf.readableBytes());
        }finally{buf.release();}
        assertTrue(MercenaryHudLayout.tooltip(row).contains("Resurrection: Ready"));
        assertTrue(row.resurrection().offered());
        assertThrows(IllegalArgumentException.class,()->new PartyPayloads.Resurrection(42,merc.toString(),death.toString(),1));
        assertThrows(IllegalArgumentException.class,()->new PartyPayloads.Resurrection(42,"not-an-id","",0));
        assertThrows(IllegalArgumentException.class,()->new PartyPayloads.Resurrect(0,merc,death));
    }
    @Test void hoverShowsCooldownEvenWhileRestingAndOmitsLockedAbility(){
        var merc=UUID.randomUUID();
        var base=new PartyPayloads.Mercenary("Edmund","Owner",0,0,500,"RESPAWNING");
        assertEquals(2,MercenaryHudLayout.tooltip(base).size());
        var row=new PartyPayloads.Mercenary("Edmund","Owner",0,0,500,"RESPAWNING",PartyPayloads.Recovery.NONE,List.of(),
                new PartyPayloads.Resurrection(42,merc.toString(),"",180));
        assertTrue(MercenaryHudLayout.tooltip(row).contains("Resurrection: 3:00"));
    }
}
