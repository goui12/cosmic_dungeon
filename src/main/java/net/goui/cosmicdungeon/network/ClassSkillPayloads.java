package net.goui.cosmicdungeon.network;

import io.netty.buffer.ByteBuf;
import java.util.*;
import net.goui.cosmicdungeon.playerclass.skill.ClassSkillSlot;
import net.goui.cosmicdungeon.playerclass.skill.SignatureSkill;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Four bounded generic slots; client requests never contain an ability ID, level, cost or effect. */
public final class ClassSkillPayloads {
    private ClassSkillPayloads(){}
    private static ResourceLocation id(String id){return ResourceLocation.fromNamespaceAndPath("cosmicdungeon",id);}
    private static void text(String value,int max){if(value==null||value.length()>max)throw new IllegalArgumentException("Invalid class skill text");}
    public record SlotView(ClassSkillSlot slot,String abilityId,String title,String description,
                           boolean enabled,int remainingTicks,int charge,int maxCharge){
        public SlotView{
            Objects.requireNonNull(slot);text(abilityId,128);text(title,128);text(description,512);
            if(remainingTicks<0||charge<0||maxCharge<0||charge>maxCharge||enabled&&(abilityId.isBlank()||remainingTicks>0))
                throw new IllegalArgumentException("Invalid class skill slot");
        }
        public static SlotView unassigned(ClassSkillSlot slot){
            return new SlotView(slot,"","Unassigned","No ability is assigned to this slot.",false,0,0,0);
        }
    }
    public record Track(String id,String title,String abbreviation,int level,int progress,int next){
        public Track{
            text(id,32);text(title,64);text(abbreviation,8);
            if(SignatureSkill.byId(id).isEmpty()||level<0||level>65537||progress<0||next<0||next>0&&progress>=next)
                throw new IllegalArgumentException("Invalid signature track");
        }
    }
    public record View(long run,long revision,String classId,List<SlotView> slots,List<Track> tracks) implements CustomPacketPayload{
        public View{
            text(classId,32);slots=List.copyOf(slots);tracks=List.copyOf(tracks);
            if(run<0||revision<0||slots.size()!=4||tracks.size()>1
                    ||slots.stream().map(SlotView::slot).distinct().count()!=4
                    ||run==0&&slots.stream().anyMatch(SlotView::enabled)
                    ||tracks.stream().anyMatch(t->!SignatureSkill.byId(t.id()).orElseThrow().classId().equals(classId)))
                throw new IllegalArgumentException("Invalid class skill view");
        }
        public static View empty(long revision,String classId){
            return new View(0,revision,classId,Arrays.stream(ClassSkillSlot.values()).map(SlotView::unassigned).toList(),List.of());
        }
        public static final Type<View> TYPE=new Type<>(id("class_skill_view"));
        public static final StreamCodec<ByteBuf,View> STREAM_CODEC=new StreamCodec<>(){
            public void encode(ByteBuf raw,View v){
                var b=new FriendlyByteBuf(raw);b.writeVarLong(v.run);b.writeVarLong(v.revision);b.writeUtf(v.classId,32);
                for(var s:v.slots){b.writeEnum(s.slot);b.writeUtf(s.abilityId,128);b.writeUtf(s.title,128);b.writeUtf(s.description,512);
                    b.writeBoolean(s.enabled);b.writeVarInt(s.remainingTicks);b.writeVarInt(s.charge);b.writeVarInt(s.maxCharge);}
                b.writeVarInt(v.tracks.size());
                for(var t:v.tracks){b.writeUtf(t.id,32);b.writeUtf(t.title,64);b.writeUtf(t.abbreviation,8);
                    b.writeVarInt(t.level);b.writeVarInt(t.progress);b.writeVarInt(t.next);}
            }
            public View decode(ByteBuf raw){
                var b=new FriendlyByteBuf(raw);long run=b.readVarLong(),revision=b.readVarLong();String cls=b.readUtf(32);
                var slots=new ArrayList<SlotView>(4);
                for(int i=0;i<4;i++)slots.add(new SlotView(b.readEnum(ClassSkillSlot.class),b.readUtf(128),b.readUtf(128),b.readUtf(512),
                        b.readBoolean(),b.readVarInt(),b.readVarInt(),b.readVarInt()));
                int count=b.readVarInt();if(count<0||count>1)throw new IllegalArgumentException("Invalid signature count");
                var tracks=new ArrayList<Track>(count);
                for(int i=0;i<count;i++)tracks.add(new Track(b.readUtf(32),b.readUtf(64),b.readUtf(8),b.readVarInt(),b.readVarInt(),b.readVarInt()));
                return new View(run,revision,cls,slots,tracks);
            }
        };
        public Type<View> type(){return TYPE;}
    }
    public record Action(long run,long revision,String classId,ClassSkillSlot slot) implements CustomPacketPayload{
        public Action{
            text(classId,32);Objects.requireNonNull(slot);
            if(run<=0||revision<0||classId.isBlank())throw new IllegalArgumentException("Invalid class skill action");
        }
        public static final Type<Action> TYPE=new Type<>(id("class_skill_action"));
        public static final StreamCodec<ByteBuf,Action> STREAM_CODEC=new StreamCodec<>(){
            public void encode(ByteBuf raw,Action a){var b=new FriendlyByteBuf(raw);b.writeVarLong(a.run);b.writeVarLong(a.revision);b.writeUtf(a.classId,32);b.writeEnum(a.slot);}
            public Action decode(ByteBuf raw){var b=new FriendlyByteBuf(raw);return new Action(b.readVarLong(),b.readVarLong(),b.readUtf(32),b.readEnum(ClassSkillSlot.class));}
        };
        public Type<Action> type(){return TYPE;}
    }
}
