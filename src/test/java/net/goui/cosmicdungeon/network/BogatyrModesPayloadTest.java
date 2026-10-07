package net.goui.cosmicdungeon.network;

import java.util.Collections;
import java.util.List;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.goui.cosmicdungeon.network.BogatyrPayloads.*;
import net.goui.cosmicdungeon.playerclass.bogatyr.WolfMode;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class BogatyrModesPayloadTest {
    private static final List<Quote> EMPTY=Collections.nCopies(4,Quote.empty());
    @Test void eachModeAndZeroKibbleCapabilityRoundTripWithoutLosingGeneration(){
        for(var mode:WolfMode.values()){
            var view=new View(24,Long.MAX_VALUE,0,EMPTY,mode,true);
            var raw=Unpooled.buffer();
            try{
                View.STREAM_CODEC.encode(raw,view);
                var decoded=View.STREAM_CODEC.decode(raw);
                assertEquals(view,decoded);assertTrue(decoded.modesEnabled());
                assertTrue(decoded.quotes().stream().noneMatch(Quote::enabled));
                assertEquals(0,raw.readableBytes());
            }finally{raw.release();}
            var action=new ModeAction(24,Long.MAX_VALUE,mode);raw=Unpooled.buffer();
            try{
                ModeAction.STREAM_CODEC.encode(raw,action);
                assertEquals(action,ModeAction.STREAM_CODEC.decode(raw));
                assertEquals(0,raw.readableBytes());
            }finally{raw.release();}
        }
        var legacy=new View(24,0,0,EMPTY);
        assertEquals(WolfMode.DEFENSIVE,legacy.mode());assertFalse(legacy.modesEnabled());
        assertEquals(legacy,new View(24,0,0,EMPTY,WolfMode.DEFENSIVE,false));
    }
    @Test void invalidCapabilitiesCannotBecomeModeCommands(){
        for(long run:new long[]{0,-1,Long.MIN_VALUE})
            assertThrows(IllegalArgumentException.class,()->new ModeAction(run,1,WolfMode.DEFENSIVE));
        assertThrows(IllegalArgumentException.class,()->new ModeAction(24,-1,WolfMode.DEFENSIVE));
        assertThrows(IllegalArgumentException.class,()->new ModeAction(24,1,null));
        assertThrows(IllegalArgumentException.class,()->new View(24,1,0,EMPTY,null,true));
        assertThrows(IllegalArgumentException.class,()->new View(0,1,0,EMPTY,WolfMode.DEFENSIVE,true));
        assertThrows(IllegalArgumentException.class,()->new View(-1,1,0,EMPTY,WolfMode.DEFENSIVE,false));
        assertThrows(IllegalArgumentException.class,()->new View(24,-1,0,EMPTY,WolfMode.DEFENSIVE,true));
        assertFalse(View.empty(1).modesEnabled());
    }
    @Test void unknownWireOrdinalsAreRejectedForBothDirections(){
        for(int mode:new int[]{-1,WolfMode.values().length,Integer.MAX_VALUE}){
            var action=actionWire(24,1,mode);
            try{assertThrows(IllegalArgumentException.class,()->ModeAction.STREAM_CODEC.decode(action));}
            finally{action.release();}
            var view=viewWire(24,1,mode,true);
            try{assertThrows(IllegalArgumentException.class,()->View.STREAM_CODEC.decode(view));}
            finally{view.release();}
        }
    }
    @Test void decoderValidatesRunAndGenerationAndRequiresModeCapabilityTail(){
        for(long[] values:new long[][]{{0,1},{-1,1},{24,-1}}){
            var action=actionWire(values[0],values[1],0);
            try{assertThrows(IllegalArgumentException.class,()->ModeAction.STREAM_CODEC.decode(action));}
            finally{action.release();}
            var view=viewWire(values[0],values[1],0,true);
            try{assertThrows(IllegalArgumentException.class,()->View.STREAM_CODEC.decode(view));}
            finally{view.release();}
        }
        var raw=viewWire(24,1,0,true);
        try{
            raw.writerIndex(raw.writerIndex()-1);
            assertThrows(IndexOutOfBoundsException.class,()->View.STREAM_CODEC.decode(raw));
        }finally{raw.release();}
    }
    private static ByteBuf actionWire(long run,long revision,int mode){
        var raw=Unpooled.buffer();var b=new FriendlyByteBuf(raw);
        b.writeVarLong(run);b.writeVarLong(revision);b.writeVarInt(mode);return raw;
    }
    private static ByteBuf viewWire(long run,long revision,int mode,boolean enabled){
        var raw=Unpooled.buffer();var b=new FriendlyByteBuf(raw);
        b.writeVarLong(run);b.writeVarLong(revision);b.writeVarInt(0);
        for(int i=0;i<4;i++){b.writeVarInt(0);b.writeVarInt(0);b.writeBoolean(false);}
        b.writeVarInt(mode);b.writeBoolean(enabled);return raw;
    }
}
