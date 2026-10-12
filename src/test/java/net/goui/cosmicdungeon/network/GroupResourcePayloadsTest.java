package net.goui.cosmicdungeon.network;
import io.netty.buffer.Unpooled;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
final class GroupResourcePayloadsTest {
    @Test void lobbyAndRunChoicesRoundTripWithIdentityAndContext() {
        String id=UUID.randomUUID().toString();
        for(String context:List.of(UUID.randomUUID().toString(),"123"))
            for(String choice:List.of("UNKNOWN","YES","NO")) {
                var row=new PartyPayloads.Member("Member","theurgist",true,false,false,PartyVitals.UNLOADED,id,choice);
                var view=new PartyPayloads.View(-1,new PartyPayloads.State(123,"ACTIVE",false,6,0,-1),
                    List.of(row),new PartyPayloads.Invite("","",false,false),PartyPayloads.Recruitment.EMPTY,
                    "HARD",PartyPayloads.Hire.NONE,List.of(),new PartyPayloads.Sharing(context,choice));
                var buf=Unpooled.buffer();
                try {PartyPayloads.View.STREAM_CODEC.encode(buf,view);
                    assertEquals(view,PartyPayloads.View.STREAM_CODEC.decode(buf));assertEquals(0,buf.readableBytes());}
                finally {buf.release();}
            }
    }
    @Test void invalidChoicesAndOversizedContextAreRejected() {
        assertThrows(IllegalArgumentException.class,()->new PartyPayloads.Sharing("id","OPTED_IN"));
        assertThrows(IllegalArgumentException.class,()->new PartyPayloads.Sharing("","YES"));
        assertThrows(IllegalArgumentException.class,()->new PartyPayloads.Sharing("x".repeat(37),"NO"));
        assertThrows(IllegalArgumentException.class,()->new PartyPayloads.Member("Name","none",false,false,false,PartyVitals.UNLOADED,"","BAD"));
        assertEquals("UNKNOWN",new PartyPayloads.Member("Name","none",false,false).resourceSharing());
    }
}
