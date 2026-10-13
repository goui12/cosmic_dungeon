package net.goui.cosmicdungeon.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Requests carry only an expected held-weapon snapshot; server stock is never supplied by the client. */
public final class AmmoPayloads {
    private AmmoPayloads(){}
    private static ResourceLocation id(String value){return ResourceLocation.fromNamespaceAndPath("cosmicdungeon",value);}
    public record Cycle(int hand,int hotbar,ItemStack weapon) implements CustomPacketPayload {
        public Cycle{if(hand<0||hand>1||hotbar<0||hotbar>8||weapon.isEmpty()||weapon.getCount()!=1)
            throw new IllegalArgumentException("Invalid ammunition request");weapon=weapon.copy();}
        public static final Type<Cycle> TYPE=new Type<>(id("ammo_cycle"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Cycle> CODEC=new StreamCodec<>(){
            public void encode(RegistryFriendlyByteBuf b,Cycle v){b.writeByte(v.hand);b.writeByte(v.hotbar);ItemStack.STREAM_CODEC.encode(b,v.weapon);}
            public Cycle decode(RegistryFriendlyByteBuf b){return new Cycle(b.readUnsignedByte(),b.readUnsignedByte(),ItemStack.STREAM_CODEC.decode(b));}
        };
        public Type<Cycle> type(){return TYPE;}
    }
    public record View(long revision,int hand,ResourceLocation dimension,String classId,int slot,ItemStack selected,int notice)
            implements CustomPacketPayload {
        public View{if(revision<0||hand<0||hand>1||classId.length()>32||slot< -1||slot>40||notice<0||notice>3)
            throw new IllegalArgumentException("Invalid ammunition view");selected=selected.copyWithCount(selected.isEmpty()?0:1);}
        public static final Type<View> TYPE=new Type<>(id("ammo_view"));
        public static final StreamCodec<RegistryFriendlyByteBuf,View> CODEC=new StreamCodec<>(){
            public void encode(RegistryFriendlyByteBuf b,View v){
                b.writeVarLong(v.revision);b.writeByte(v.hand);b.writeResourceLocation(v.dimension);
                b.writeUtf(v.classId,32);b.writeVarInt(v.slot);ItemStack.OPTIONAL_STREAM_CODEC.encode(b,v.selected);b.writeByte(v.notice);
            }
            public View decode(RegistryFriendlyByteBuf b){
                return new View(b.readVarLong(),b.readUnsignedByte(),b.readResourceLocation(),b.readUtf(32),
                        b.readVarInt(),ItemStack.OPTIONAL_STREAM_CODEC.decode(b),b.readUnsignedByte());
            }
        };
        public Type<View> type(){return TYPE;}
    }
}
