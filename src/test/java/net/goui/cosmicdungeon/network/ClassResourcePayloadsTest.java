package net.goui.cosmicdungeon.network;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ClassResourcePayloadsTest {
    @Test void snapshotsAndTokensRoundTrip(){
        var buf=Unpooled.buffer();
        try{
            var value=new ClassResourcePayloads.View(55,"brewing_supplies",599,600,true,true,true,12);
            ClassResourcePayloads.View.STREAM_CODEC.encode(buf,value);assertEquals(value,ClassResourcePayloads.View.STREAM_CODEC.decode(buf));
            var request=new ClassResourcePayloads.Recycle(55,"kibble",13);
            ClassResourcePayloads.Recycle.STREAM_CODEC.encode(buf,request);assertEquals(request,ClassResourcePayloads.Recycle.STREAM_CODEC.decode(buf));
            ClassResourcePayloads.View.STREAM_CODEC.encode(buf,ClassResourcePayloads.View.empty());
            assertEquals(ClassResourcePayloads.View.empty(),ClassResourcePayloads.View.STREAM_CODEC.decode(buf));assertFalse(buf.isReadable());
        }finally{buf.release();}
    }
    @Test void untrustedBoundsAndUnknownIdentitiesFail(){
        assertThrows(IllegalArgumentException.class,()->new ClassResourcePayloads.Recycle(0,"kibble",0));
        assertThrows(IllegalArgumentException.class,()->new ClassResourcePayloads.Recycle(1,"fish",0));
        assertThrows(IllegalArgumentException.class,()->new ClassResourcePayloads.Recycle(1,"kibble",-1));
        assertThrows(IllegalArgumentException.class,()->new ClassResourcePayloads.View(1,"kibble",601,600,true,true,false,0));
        assertThrows(IllegalArgumentException.class,()->new ClassResourcePayloads.View(1,"kibble",10,600,true,false,true,0));
        assertThrows(IllegalArgumentException.class,()->new ClassResourcePayloads.View(0,"",0,600,false,false,false,1));
    }
}
