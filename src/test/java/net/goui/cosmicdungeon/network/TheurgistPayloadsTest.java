package net.goui.cosmicdungeon.network;

import java.util.*;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class TheurgistPayloadsTest {
    @Test void boundedNativeWireRoundTripsExactTokens(){
        var target=new TheurgistPayloads.Target(UUID.randomUUID(),"Teammate",UUID.randomUUID(),true);
        var offer=new TheurgistPayloads.Offer(UUID.randomUUID(),UUID.randomUUID(),"Theurgist",target.death());
        var view=new TheurgistPayloads.View(22,7,true,true,true,true,List.of(target),offer);
        var buffer=Unpooled.buffer();try{TheurgistPayloads.View.STREAM_CODEC.encode(buffer,view);assertEquals(view,TheurgistPayloads.View.STREAM_CODEC.decode(buffer));assertEquals(0,buffer.readableBytes());}finally{buffer.release();}
        for(var kind:TheurgistPayloads.Kind.values()){
            boolean craft=kind==TheurgistPayloads.Kind.CRAFT||kind==TheurgistPayloads.Kind.EPIC;
            var action=new TheurgistPayloads.Action(22,7,kind,craft?TheurgistPayloads.NONE:target.player(),craft?TheurgistPayloads.NONE:target.death());
            var wire=Unpooled.buffer();try{TheurgistPayloads.Action.STREAM_CODEC.encode(wire,action);assertEquals(action,TheurgistPayloads.Action.STREAM_CODEC.decode(wire));}finally{wire.release();}
        }
    }
    @Test void malformedCapabilitiesAndUnboundedListsAreRejectedBeforeAllocation(){
        assertThrows(IllegalArgumentException.class,()->new TheurgistPayloads.View(0,0,true,true,true,false,List.of(),null));
        assertThrows(IllegalArgumentException.class,()->new TheurgistPayloads.View(22,0,false,true,true,false,List.of(),null));
        var target=new TheurgistPayloads.Target(UUID.randomUUID(),"x",UUID.randomUUID(),true);
        assertThrows(IllegalArgumentException.class,()->new TheurgistPayloads.View(22,0,true,true,false,false,List.of(target,target),null));
        assertThrows(IllegalArgumentException.class,()->new TheurgistPayloads.Action(22,0,TheurgistPayloads.Kind.CRAFT,target.player(),target.death()));
        var raw=Unpooled.buffer();try{var b=new FriendlyByteBuf(raw);b.writeVarLong(22);b.writeVarLong(0);for(int i=0;i<4;i++)b.writeBoolean(false);b.writeVarInt(Integer.MAX_VALUE);
            assertThrows(IllegalArgumentException.class,()->TheurgistPayloads.View.STREAM_CODEC.decode(raw));}finally{raw.release();}
    }
}
