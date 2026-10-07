package net.goui.cosmicdungeon.network;

import java.util.*;
import net.goui.cosmicdungeon.playerclass.resource.ClassResourceKind;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Bounded own-inbox snapshots and explicit consent IDs. Client quantities/participants never authorize a transfer. */
public final class SupplyRequestPayloads {
    public static final int MAX_CARDS=32,MAX_ITEMS=64;
    private SupplyRequestPayloads(){}
    private static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath("cosmicdungeon",path);}
    public enum Decision {REQUEST,ACCEPT,DENY,ACCEPT_ALL,DENY_ALL}
    public record Ingredient(ItemStack stack){
        public Ingredient {stack=stack.copy();if(stack.isEmpty()||stack.getCount()>600)throw new IllegalArgumentException("Invalid ingredient");}
        @Override public ItemStack stack(){return stack.copy();}
        @Override public boolean equals(Object other){return other instanceof Ingredient i&&ItemStack.matches(stack,i.stack);}
        @Override public int hashCode(){return Objects.hash(stack.getItem(),stack.getCount(),stack.getComponents());}
    }
    public record Card(UUID requestId,UUID requesterId,String requesterName,String requesterClass,String resourceId,
                       int yield,List<Ingredient> ingredients,boolean canAccept){
        public Card{
            Objects.requireNonNull(requestId);Objects.requireNonNull(requesterId);Objects.requireNonNull(requesterName);
            Objects.requireNonNull(requesterClass);var kind=ClassResourceKind.byId(resourceId).orElseThrow();
            ingredients=List.copyOf(ingredients);
            if(requesterName.length()>64||!kind.classId().equals(requesterClass)||yield<0||yield>600
                    ||ingredients.size()>MAX_ITEMS||ingredients.stream().mapToInt(i->i.stack.getCount()).sum()!=yield
                    ||canAccept&&yield==0)throw new IllegalArgumentException("Invalid supply card");
        }
    }
    public record View(long runId,long revision,boolean active,boolean alive,boolean canRequest,List<Card> cards)implements CustomPacketPayload{
        public View{
            cards=List.copyOf(cards);
            if(runId<0||revision<0||cards.size()>MAX_CARDS||cards.stream().map(Card::requestId).distinct().count()!=cards.size()
                    ||runId==0&&(active||alive||canRequest||!cards.isEmpty())||runId>0&&!active||canRequest&&!alive)
                throw new IllegalArgumentException("Invalid supply view");
        }
        public static View empty(){return new View(0,0,false,false,false,List.of());}
        public static final Type<View> TYPE=new Type<>(id("supply_requests"));
        public static final StreamCodec<RegistryFriendlyByteBuf,View> STREAM_CODEC=StreamCodec.of((buf,v)->{
            buf.writeVarLong(v.runId);buf.writeVarLong(v.revision);buf.writeBoolean(v.active);buf.writeBoolean(v.alive);buf.writeBoolean(v.canRequest);
            buf.writeVarInt(v.cards.size());
            for(var card:v.cards){
                buf.writeUUID(card.requestId);buf.writeUUID(card.requesterId);buf.writeUtf(card.requesterName,64);
                buf.writeUtf(card.requesterClass,32);buf.writeUtf(card.resourceId,32);buf.writeVarInt(card.yield);
                buf.writeBoolean(card.canAccept);buf.writeVarInt(card.ingredients.size());
                for(var item:card.ingredients)ItemStack.STREAM_CODEC.encode(buf,item.stack);
            }
        },buf->{
            long run=buf.readVarLong(),revision=buf.readVarLong();boolean active=buf.readBoolean(),alive=buf.readBoolean(),request=buf.readBoolean();
            int count=count(buf,MAX_CARDS);var cards=new ArrayList<Card>();
            for(int n=0;n<count;n++){
                var id=buf.readUUID();var owner=buf.readUUID();String name=buf.readUtf(64),classId=buf.readUtf(32),kind=buf.readUtf(32);
                int amount=buf.readVarInt();boolean accept=buf.readBoolean();int items=count(buf,MAX_ITEMS);var ingredients=new ArrayList<Ingredient>();
                for(int i=0;i<items;i++)ingredients.add(new Ingredient(ItemStack.STREAM_CODEC.decode(buf)));
                cards.add(new Card(id,owner,name,classId,kind,amount,ingredients,accept));
            }return new View(run,revision,active,alive,request,cards);
        });
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record Action(long runId,long revision,Decision decision,List<UUID> requestIds)implements CustomPacketPayload{
        public Action{
            Objects.requireNonNull(decision);requestIds=List.copyOf(requestIds);
            boolean single=decision==Decision.ACCEPT||decision==Decision.DENY;
            if(runId<=0||revision<0||requestIds.size()>MAX_CARDS||new HashSet<>(requestIds).size()!=requestIds.size()
                    ||decision==Decision.REQUEST&&!requestIds.isEmpty()||single&&requestIds.size()!=1
                    ||decision!=Decision.REQUEST&&requestIds.isEmpty())throw new IllegalArgumentException("Invalid consent action");
        }
        public static final Type<Action> TYPE=new Type<>(id("supply_request_action"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Action> STREAM_CODEC=StreamCodec.of((buf,a)->{
            buf.writeVarLong(a.runId);buf.writeVarLong(a.revision);buf.writeVarInt(a.decision.ordinal());buf.writeVarInt(a.requestIds.size());
            a.requestIds.forEach(buf::writeUUID);
        },buf->{
            long run=buf.readVarLong(),revision=buf.readVarLong();int choice=count(buf,Decision.values().length-1);
            int length=count(buf,MAX_CARDS);var ids=new ArrayList<UUID>();for(int i=0;i<length;i++)ids.add(buf.readUUID());
            return new Action(run,revision,Decision.values()[choice],ids);
        });
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    private static int count(RegistryFriendlyByteBuf buf,int max){int n=buf.readVarInt();if(n<0||n>max)throw new IllegalArgumentException("Packet list limit exceeded");return n;}
}
