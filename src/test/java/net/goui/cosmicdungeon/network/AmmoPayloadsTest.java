package net.goui.cosmicdungeon.network;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class AmmoPayloadsTest {
    private static final ResourceLocation DIM=ResourceLocation.withDefaultNamespace("overworld");
    @Test void requestBoundsRejectInvalidHandSlotAndNonUnitSnapshots(){
        var bow=new ItemStack(Items.BOW);
        for(int hand:new int[]{-1,2})assertThrows(IllegalArgumentException.class,()->new AmmoPayloads.Cycle(hand,0,bow));
        for(int slot:new int[]{-1,9})assertThrows(IllegalArgumentException.class,()->new AmmoPayloads.Cycle(0,slot,bow));
        assertThrows(IllegalArgumentException.class,()->new AmmoPayloads.Cycle(0,0,ItemStack.EMPTY));
        assertThrows(IllegalArgumentException.class,()->new AmmoPayloads.Cycle(0,0,new ItemStack(Items.BOW,2)));
        var request=new AmmoPayloads.Cycle(0,8,bow);bow.setDamageValue(5);
        assertEquals(0,request.weapon().getDamageValue(),"Request is a snapshot, not a mutable inventory alias");
    }
    @Test void emptyViewRoundTripAndWireBounds(){
        var raw=Unpooled.buffer();
        try{
            var b=new RegistryFriendlyByteBuf(raw,RegistryAccess.EMPTY);
            var empty=new AmmoPayloads.View(99,1,DIM,"pyroclast",-1,ItemStack.EMPTY,2);
            AmmoPayloads.View.CODEC.encode(b,empty);
            assertEquals(empty,AmmoPayloads.View.CODEC.decode(b));assertEquals(0,b.readableBytes());
            assertThrows(IllegalArgumentException.class,()->new AmmoPayloads.View(-1,0,DIM,"",0,ItemStack.EMPTY,0));
            assertThrows(IllegalArgumentException.class,()->new AmmoPayloads.View(1,0,DIM,"x".repeat(33),0,ItemStack.EMPTY,0));
            for(int slot:new int[]{-2,41})assertThrows(IllegalArgumentException.class,()->new AmmoPayloads.View(1,0,DIM,"",slot,ItemStack.EMPTY,0));
            assertThrows(IllegalArgumentException.class,()->new AmmoPayloads.View(1,0,DIM,"",0,ItemStack.EMPTY,4));
        }finally{raw.release();}
    }
}
