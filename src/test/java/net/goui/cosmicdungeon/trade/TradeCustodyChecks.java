package net.goui.cosmicdungeon.trade;
import net.minecraft.nbt.*;
import java.util.*;
public final class TradeCustodyChecks {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static void reject(Runnable action,String why){boolean rejected=false;try{action.run();}catch(RuntimeException expected){rejected=true;}check(rejected,why);}
    private static CompoundTag item(String id,int count){var t=new CompoundTag();t.putString("id",id);t.putInt("count",count);
        var c=new CompoundTag();var custom=new CompoundTag();custom.putByteArray("origin",new byte[]{1,-1,127});
        custom.putLong("run",9223372036854775806L);c.put("minecraft:custom_data",custom);t.put("components",c);return t;}
    public static void main(String[] args)throws Exception{
        var owner=new UUID(0,1);var peer=new UUID(0,2);var session=new UUID(0,3);
        for(long run:new long[]{0,12,Long.MAX_VALUE}){
            var a=TradeCustodyImages.open(session,owner,peer,run);var b=TradeCustodyImages.open(session,peer,owner,run);
            var offer=(ListTag)a.get("offers");offer.set(0,item("minecraft:iron_sword",1));offer.set(8,item("minecraft:apple",9));
            a=TradeCustodyImages.live(a,offer,item("minecraft:stick",3));
            var other=(ListTag)b.get("offers");other.set(4,item("minecraft:diamond_sword",1));
            b=TradeCustodyImages.live(b,other,item("minecraft:carrot",6));
            var exchanged=TradeCustodyImages.exchanged(a,b);
            check(exchanged.get("offers").equals(b.get("offers")),"Only partner offer transfers");
            check(exchanged.get("cursor").equals(a.get("cursor")),"Own cursor is never sent to partner");
            check(exchanged.getStringOr("owner","").equals(owner.toString()),"Receiving custody belongs to original owner");
            check(TradeCustodyImages.held(a).size()==3,"Cancellation includes both offer slots and own cursor");
            var detached=TradeCustodyImages.held(a);detached.getFirst().putInt("count",500);
            check(TradeCustodyImages.held(a).getFirst().getIntOr("count",0)==1,"Returns cannot mutate immutable custody evidence");
            var immutable=a.copy();var finalA=a;var finalB=b;
            reject(()->TradeCustodyImages.validate(finalA,peer),"Peer cannot claim another owner's offers");
            var bound=TradeCustodyImages.bind(a,session);check(bound.get("offers").equals(a.get("offers")),"Binding preserves exact offers");
            reject(()->TradeCustodyImages.bind(bound,owner),"One custody cannot join two transactions");
            check(TradeCustodyImages.bind(bound,session).equals(bound),"Repeated same binding is idempotent");
            for(String key:List.of("owner","peer","session","run","offers","cursor","schema")){
                var bad=a.copy();bad.remove(key);reject(()->TradeCustodyImages.validate(bad,owner),"Incomplete custody quarantined: "+key);
            }
            for(int count:new int[]{0,-1,Integer.MIN_VALUE}){
                var bad=a.copy();((CompoundTag)((ListTag)bad.get("offers")).get(0)).putInt("count",count);
                reject(()->TradeCustodyImages.validate(bad,owner),"Invalid offered count rejected");
            }
            var wrong=b.copy();wrong.putString("session",peer.toString());reject(()->TradeCustodyImages.exchanged(finalA,wrong),"Offers from different sessions cannot mix");
            var scope=b.copy();scope.putLong("run",run==0?12:0);reject(()->TradeCustodyImages.exchanged(finalA,scope),"Outside/dungeon belongings cannot mix");
            var future=a.copy();future.putInt("schema",2);reject(()->TradeCustodyImages.validate(future,owner),"Future version preserved for review");
            var shortOffer=a.copy();shortOffer.put("offers",new ListTag());reject(()->TradeCustodyImages.validate(shortOffer,owner),"Truncated offer rejected");
            var file=java.nio.file.Files.createTempFile("trade-custody-",".dat");
            try{NbtIo.writeCompressed(a,file);var loaded=NbtIo.readCompressed(file,NbtAccounter.unlimitedHeap());
                check(loaded.equals(immutable),"Native compressed file preserves exact nested item data");
                check(TradeCustodyImages.held(loaded).equals(TradeCustodyImages.held(a)),"Reload returns originals without losing cursor/slot8");
            }finally{java.nio.file.Files.deleteIfExists(file);}
            check(TradeCustodyImages.exchanged(b,a).get("cursor").equals(b.get("cursor")),"Other owner keeps their own cursor too");
        }
        reject(()->TradeCustodyImages.open(session,owner,owner,0),"Self exchange rejected");
        reject(()->TradeCustodyImages.open(session,owner,peer,-1),"Unknown inventory context rejected");
        System.out.println(checks+" trade custody checks passed");
    }
}
