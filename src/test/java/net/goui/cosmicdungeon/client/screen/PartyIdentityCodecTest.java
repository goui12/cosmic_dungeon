package net.goui.cosmicdungeon.client.screen;
import java.util.List;
import io.netty.buffer.Unpooled;
import net.goui.cosmicdungeon.network.PartyPayloads;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
final class PartyIdentityCodecTest {
    @Test void humanAndMercenaryIdentitySurviveWireRoundTripWithoutNameHeuristics() {
        var buf=Unpooled.buffer();
        try {
            for(var member:List.of(new PartyPayloads.Member("Edward","bogatyr",true,true),
                    new PartyPayloads.Member("Edward","theurgist",false,false,true))) {
                PartyPayloads.Member.CODEC.encode(buf,member);assertEquals(member,PartyPayloads.Member.CODEC.decode(buf));
            }
            assertEquals(0,buf.readableBytes());
        } finally {buf.release();}
    }
    @Test void everyMercenaryStatusKeepsItsClassWithOrWithoutSkills() {
        var buf=Unpooled.buffer();
        try {
            for(String status:List.of("ACTIVE","UNLOADED","RESPAWNING")) {
                var row=new PartyPayloads.Mercenary("Edmund","PrivateOwner",0,0,status.equals("RESPAWNING")?20:-1,
                        status,PartyPayloads.Recovery.NONE,List.of(),PartyPayloads.Resurrection.LOCKED,"theurgist");
                PartyPayloads.Mercenary.CODEC.encode(buf,row);var decoded=PartyPayloads.Mercenary.CODEC.decode(buf);
                assertEquals(row,decoded);assertTrue(MercenaryHudLayout.label(decoded).startsWith("Edmund / "));
                assertFalse(MercenaryHudLayout.tooltip(decoded).toString().contains("PrivateOwner"));
                assertEquals(MercenaryHudLayout.label(decoded),MercenaryHudLayout.tooltip(decoded).getFirst());
            }
            assertEquals(0,buf.readableBytes());
        } finally {buf.release();}
    }
}
