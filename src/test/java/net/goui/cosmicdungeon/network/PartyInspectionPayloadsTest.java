package net.goui.cosmicdungeon.network;
import io.netty.buffer.Unpooled;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
final class PartyInspectionPayloadsTest {
    @Test void identityAndReadingsRoundTrip() {
        var request=new PartyInspectionPayloads.Request(12,7,UUID.randomUUID());
        var view=new PartyInspectionPayloads.View(request,true,"Member","metalmancer",
                new PartyVitals(8,20,"ACTIVE",List.of(new PartyVitals.Effect("minecraft:strength",1)),0),
                List.of(new PartyInspectionPayloads.Reading("Magnet attack","2s")));
        var buf=Unpooled.buffer();
        try {PartyInspectionPayloads.View.STREAM_CODEC.encode(buf,view);
            assertEquals(view,PartyInspectionPayloads.View.STREAM_CODEC.decode(buf));assertEquals(0,buf.readableBytes());}
        finally{buf.release();}
        var member=new PartyPayloads.Member("Member","metalmancer",false,false,false,view.vitals(),request.subject().toString());
        buf=Unpooled.buffer();
        try{PartyPayloads.Member.CODEC.encode(buf,member);assertEquals(member,PartyPayloads.Member.CODEC.decode(buf));}
        finally{buf.release();}
    }
    @Test void deniedViewsCannotCarryPrivateDetailsAndBoundsAreEnforced() {
        var request=new PartyInspectionPayloads.Request(1,1,UUID.randomUUID());
        assertThrows(IllegalArgumentException.class,()->new PartyInspectionPayloads.Request(0,1,request.subject()));
        assertThrows(IllegalArgumentException.class,()->new PartyInspectionPayloads.Request(1,0,request.subject()));
        assertThrows(IllegalArgumentException.class,()->new PartyInspectionPayloads.View(request,false,"Member","",PartyVitals.UNLOADED,List.of()));
        assertThrows(IllegalArgumentException.class,()->new PartyInspectionPayloads.Reading("a".repeat(49),""));
        assertThrows(IllegalArgumentException.class,()->new PartyInspectionPayloads.View(request,true,"Member","none",PartyVitals.OFFLINE,
                Collections.nCopies(25,new PartyInspectionPayloads.Reading("x","y"))));
        var denied=PartyInspectionPayloads.View.denied(request);var buf=Unpooled.buffer();
        try{PartyInspectionPayloads.View.STREAM_CODEC.encode(buf,denied);assertEquals(denied,PartyInspectionPayloads.View.STREAM_CODEC.decode(buf));}
        finally{buf.release();}
    }
}
