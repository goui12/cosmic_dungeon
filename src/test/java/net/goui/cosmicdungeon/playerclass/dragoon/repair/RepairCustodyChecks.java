package net.goui.cosmicdungeon.playerclass.dragoon.repair;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.goui.cosmicdungeon.transaction.PlayerSaveProof;
import net.minecraft.nbt.*;
import java.util.*;
public final class RepairCustodyChecks {
    private static int checks;private static final UUID OWNER=new UUID(0,1),PEER=new UUID(0,2),SESSION=new UUID(0,3),TX=new UUID(0,4);
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static void reject(Runnable action,String why){boolean failed=false;try{action.run();}catch(RuntimeException expected){failed=true;}check(failed,why);}
    private static CompoundTag item(String id,int count){
        var item=new CompoundTag();item.putString("id",id);item.putInt("count",count);
        var components=new CompoundTag();components.putInt("minecraft:damage",81);
        var nested=new CompoundTag();nested.putByteArray("exact",new byte[]{1,-1,127});nested.putString("owner","original");
        components.put("minecraft:custom_data",nested);item.put("components",components);return item;
    }
    private static CompoundTag part(int slot,CompoundTag image){var p=new CompoundTag();p.putInt("slot",slot);p.put("item",image.copy());return p;}
    public static void main(String[] args)throws Exception{
        var target=item("minecraft:iron_sword",1);var cursor=item("minecraft:apple",5);
        var customer=RepairCustodyImages.open(SESSION,OWNER,12,true);
        var provider=RepairCustodyImages.open(SESSION,PEER,12,false);
        RepairCustodyImages.validate(customer,OWNER);RepairCustodyImages.validate(provider,PEER);
        check(RepairCustodyImages.held(customer,false).isEmpty(),"New session owns no items");
        var live=RepairCustodyImages.live(customer,target,cursor);
        check(RepairCustodyImages.held(live,false).equals(List.of(target,cursor)),"Customer target and cursor retained separately");
        check(customer.getCompoundOrEmpty("target").isEmpty(),"Live capture never mutates previous snapshot");
        target.getCompoundOrEmpty("components").putInt("minecraft:damage",10);
        check(live.getCompoundOrEmpty("target").getCompoundOrEmpty("components").getIntOr("minecraft:damage",0)==81,"Image copies cannot mutate the agreed target");
        var returns=RepairCustodyImages.held(live,false);returns.getFirst().putString("id","wrong");
        check(!RepairCustodyImages.held(live,false).getFirst().getStringOr("id","").equals("wrong"),"Detached return copies preserve saved evidence");
        reject(()->RepairCustodyImages.validate(live,PEER),"Peer cannot claim customer custody");
        var invalidProvider=RepairCustodyImages.live(provider,target,new CompoundTag());
        reject(()->RepairCustodyImages.validate(invalidProvider,PEER),"Provider cannot own repair target");
        var providerCursor=RepairCustodyImages.live(provider,new CompoundTag(),cursor);
        check(RepairCustodyImages.held(providerCursor,false).equals(List.of(cursor)),"Provider cursor remains provider-owned");
        var parts=new ListTag();parts.add(part(2,item("minecraft:iron_ingot",2)));parts.add(part(9,item("minecraft:iron_ingot",1)));
        var ready=RepairCustodyImages.reserve(providerCursor,TX,parts);
        check(RepairCustodyImages.sameReservation(ready,TX),"Component images bind to exact transaction");
        check(!RepairCustodyImages.sameReservation(ready,SESSION),"Another transaction cannot consume escrow");
        check(RepairCustodyImages.held(ready,true).size()==2,"Exact component lots retained without combining unrelated metadata");
        check(RepairCustodyImages.held(ready,false).size()==3,"Provider cursor is independent of reserved components");
        parts.clear();check(RepairCustodyImages.held(ready,true).size()==2,"External list mutation cannot erase reservation");
        reject(()->RepairCustodyImages.reserve(ready,SESSION,new ListTag()),"Reserved state cannot be rebound");
        var unready=RepairCustodyImages.unreserve(ready);
        check(!RepairCustodyImages.sameReservation(unready,TX)&&RepairCustodyImages.held(unready,true).isEmpty(),"Unready releases reservation metadata");
        check(RepairCustodyImages.held(unready,false).equals(List.of(cursor)),"Unready preserves native cursor");
        check(RepairCustodyImages.held(ready,true).size()==2,"Cancellation projection preserves original evidence");
        var customerReady=RepairCustodyImages.reserve(live,TX,new ListTag());
        check(RepairCustodyImages.held(customerReady,false).equals(RepairCustodyImages.held(live,false)),"Customer reservation cannot change exact item/cursor");
        var customerParts=new ListTag();customerParts.add(part(0,item("minecraft:iron_ingot",1)));
        reject(()->RepairCustodyImages.reserve(live,TX,customerParts),"Customer cannot receive provider components");
        var duplicate=new ListTag();duplicate.add(part(1,item("minecraft:iron_ingot",1)));duplicate.add(part(1,item("minecraft:iron_ingot",2)));
        reject(()->RepairCustodyImages.reserve(provider,TX,duplicate),"One source slot cannot be reserved twice");
        for(int slot:new int[]{-1,256,Integer.MAX_VALUE}){
            var bad=new ListTag();bad.add(part(slot,item("minecraft:iron_ingot",1)));
            reject(()->RepairCustodyImages.reserve(provider,TX,bad),"Malformed source slot remains quarantined");
        }
        for(int count:new int[]{0,-1,Integer.MIN_VALUE}){
            var bad=item("minecraft:iron_ingot",count);reject(()->RepairCustodyImages.item(bad,false),"Invalid count rejected before movement");
        }
        var empty=new CompoundTag();reject(()->RepairCustodyImages.item(empty,false),"Empty component cannot satisfy repair");
        var future=ready.copy();future.putInt("schema",2);reject(()->RepairCustodyImages.validate(future,PEER),"Future schema must not be silently cleared");
        var missing=ready.copy();missing.remove("cursor");reject(()->RepairCustodyImages.validate(missing,PEER),"Missing cursor image needs review");
        var unbound=ready.copy();unbound.remove("transaction");reject(()->RepairCustodyImages.validate(unbound,PEER),"Unbound components cannot be auto-awarded");
        var wrong=ready.copy();wrong.putString("transaction","broken");reject(()->RepairCustodyImages.validate(wrong,PEER),"Malformed transaction cannot consume items");
        var directory=java.nio.file.Files.createTempDirectory("cosmic-repair-custody-");
        try{
            for(var state:List.of(customer,live,providerCursor,ready,unready,customerReady)){
                var file=directory.resolve("player.dat");var player=new CompoundTag();var root=new CompoundTag();root.put(RepairCustody.KEY,state);
                var neo=new CompoundTag();neo.put(ClassData.ROOT_TAG,root);player.put("NeoForgeData",neo);
                var inventory=new ListTag();inventory.add(part(0,item("minecraft:stick",3)));player.put("Inventory",inventory);
                player.put("equipment",item("minecraft:iron_helmet",1));
                NbtIo.writeCompressed(player,file);var loaded=NbtIo.readCompressed(file,NbtAccounter.unlimitedHeap());
                check(PlayerSaveProof.matches(player,loaded),"Inventory/equipment/custody share exact native disk image");
                check(state.equals(loaded.getCompoundOrEmpty("NeoForgeData").getCompoundOrEmpty(ClassData.ROOT_TAG).getCompoundOrEmpty(RepairCustody.KEY)),"NBT widths/nested components survive compressed save");
                var altered=loaded.copy();altered.put("Inventory",new ListTag());check(!PlayerSaveProof.matches(player,altered),"Old inventory cannot validate new escrow");
                altered=loaded.copy();altered.remove("equipment");check(!PlayerSaveProof.matches(player,altered),"Separate equipped items participate in verification");
                altered=loaded.copy();altered.getCompoundOrEmpty("NeoForgeData").getCompoundOrEmpty(ClassData.ROOT_TAG).remove(RepairCustody.KEY);
                check(!PlayerSaveProof.matches(player,altered),"Lost escrow cannot pass inventory-only readback");
            }
        }finally{java.nio.file.Files.deleteIfExists(directory.resolve("player.dat"));java.nio.file.Files.deleteIfExists(directory);}
        for(long scope:new long[]{0,12,Long.MAX_VALUE}){
            var state=RepairCustodyImages.open(SESSION,OWNER,scope,true);
            check(state.getLongOr("run",-1)==scope,"Recovery preserves exact original inventory scope");
        }
        reject(()->RepairCustodyImages.open(SESSION,OWNER,-1,true),"Unknown inventory context cannot be guessed");
        System.out.println(checks+" repair custody and player-save checks passed");
    }
}
