package net.goui.cosmicdungeon.network;

import java.util.List;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import net.goui.cosmicdungeon.network.BogatyrPayloads.*;

final class BogatyrPayloadsTest {
    @Test void FourFixedQuotesAndEveryActionRoundTripExactGeneration(){
        var view=new View(23,Long.MAX_VALUE,600,List.of(new Quote(1,5,true),new Quote(1,30,true),
                new Quote(600,600,true),new Quote(120,600,true)));
        var buffer=Unpooled.buffer();
        try{View.STREAM_CODEC.encode(buffer,view);assertEquals(view,View.STREAM_CODEC.decode(buffer));assertEquals(0,buffer.readableBytes());}
        finally{buffer.release();}
        for(var kind:Kind.values()){
            var action=new Action(23,Long.MAX_VALUE,kind);var raw=Unpooled.buffer();
            try{Action.STREAM_CODEC.encode(raw,action);assertEquals(action,Action.STREAM_CODEC.decode(raw));assertEquals(0,raw.readableBytes());}
            finally{raw.release();}
        }
    }
    @Test void InvalidCapabilityShapesAndAmountsFailClosed(){
        assertThrows(IllegalArgumentException.class,()->new Quote(-1,5,true));
        assertThrows(IllegalArgumentException.class,()->new Quote(1,601,true));
        assertThrows(IllegalArgumentException.class,()->new Quote(1,5,false));
        assertThrows(IllegalArgumentException.class,()->new Quote(0,0,true));
        assertThrows(IllegalArgumentException.class,()->new View(23,1,1,List.of(Quote.empty())));
        assertThrows(IllegalArgumentException.class,()->new View(0,1,1,List.of(Quote.empty(),Quote.empty(),Quote.empty(),Quote.empty())));
        assertThrows(IllegalArgumentException.class,()->new Action(0,1,Kind.BREED));
        assertThrows(IllegalArgumentException.class,()->new Action(23,-1,Kind.BREED));
        assertThrows(IllegalArgumentException.class,()->new Action(23,1,null));
    }
    @Test void MalformedWireEnumsAndTruncatedViewsAreRejected(){
        for(int kind:new int[]{-1,Kind.values().length,Integer.MAX_VALUE}){
            var raw=Unpooled.buffer();try{var b=new FriendlyByteBuf(raw);b.writeVarLong(23);b.writeVarLong(1);b.writeVarInt(kind);
                assertThrows(IllegalArgumentException.class,()->Action.STREAM_CODEC.decode(raw));}finally{raw.release();}
        }
        var raw=Unpooled.buffer();try{var b=new FriendlyByteBuf(raw);b.writeVarLong(23);b.writeVarLong(1);b.writeVarInt(1);
            assertThrows(IndexOutOfBoundsException.class,()->View.STREAM_CODEC.decode(raw));}finally{raw.release();}
    }
}
