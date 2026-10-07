package net.goui.cosmicdungeon.network;
import io.netty.buffer.Unpooled;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
final class PartyVitalsTest {
    private static final List<PartyVitals.Effect> EFFECTS=List.of(new PartyVitals.Effect("minecraft:strength",1),new PartyVitals.Effect("minecraft:poison",0));
    @Test void rosterAndMercenaryRoundTripKeepHealthAndBothEffectKinds() {
        var health=new PartyVitals(7.5F,24,"ACTIVE",EFFECTS,0);
        var merc=new PartyPayloads.Mercenary("Edmund","Owner",7.5F,24,-1,"ACTIVE",PartyPayloads.Recovery.NONE,List.of(),PartyPayloads.Resurrection.LOCKED,"theurgist",health);
        var view=new PartyPayloads.View(-1,new PartyPayloads.State(1,"ACTIVE",true,4,0,-1),
                List.of(new PartyPayloads.Member("Self","bogatyr",false,true,false,health),
                        new PartyPayloads.Member("Friend","dragoon",false,false,false,PartyVitals.OFFLINE),
                        new PartyPayloads.Member("Dead","pyroclast",false,false,false,PartyVitals.DEAD)),
                new PartyPayloads.Invite("","",false,false),PartyPayloads.Recruitment.EMPTY,"HARD",PartyPayloads.Hire.NONE,List.of(merc));
        var buf=Unpooled.buffer();
        try { PartyPayloads.View.STREAM_CODEC.encode(buf,view);assertEquals(view,PartyPayloads.View.STREAM_CODEC.decode(buf));assertEquals(0,buf.readableBytes()); }
        finally {buf.release();}
    }
    @Test void staleHealthAndMalformedStatesCannotEnterUnavailableRows() {
        for(String state:List.of("DEAD","OFFLINE","UNLOADED")){
            assertThrows(IllegalArgumentException.class,()->new PartyVitals(5,20,state,List.of(),0));
            assertThrows(IllegalArgumentException.class,()->new PartyVitals(0,0,state,EFFECTS,0));
        }
        for(float n:new float[]{Float.NaN,Float.POSITIVE_INFINITY,-1})
            assertThrows(IllegalArgumentException.class,()->new PartyVitals(n,20,"ACTIVE",List.of(),0));
        assertThrows(IllegalArgumentException.class,()->new PartyVitals(21,20,"ACTIVE",List.of(),0));
        assertThrows(IllegalArgumentException.class,()->new PartyVitals(0,0,"invented",List.of(),0));
        assertThrows(IllegalArgumentException.class,()->new PartyVitals.Effect("bad space",0));
        assertThrows(IllegalArgumentException.class,()->new PartyVitals.Effect("minecraft:speed",256));
    }
    @Test void effectSnapshotsAreImmutableBoundedAndOrderSensitiveForDeltaEquality() {
        var effects=new ArrayList<>(EFFECTS);var value=new PartyVitals(20,20,"ACTIVE",effects,0);effects.clear();
        assertEquals(2,value.effects().size());
        assertThrows(UnsupportedOperationException.class,()->value.effects().clear());
        assertThrows(IllegalArgumentException.class,()->new PartyVitals(20,20,"ACTIVE",List.of(EFFECTS.getFirst(),EFFECTS.getFirst()),0));
        var many=new ArrayList<PartyVitals.Effect>();
        for(int i=0;i<64;i++)many.add(new PartyVitals.Effect("example:effect_"+i,i));
        var max=new PartyVitals(20,20,"ACTIVE",many,8);var buf=Unpooled.buffer();
        try {PartyVitals.CODEC.encode(buf,max);assertEquals(max,PartyVitals.CODEC.decode(buf));}finally{buf.release();}
        many.add(new PartyVitals.Effect("example:extra",0));
        assertThrows(IllegalArgumentException.class,()->new PartyVitals(20,20,"ACTIVE",many,0));
    }
}
