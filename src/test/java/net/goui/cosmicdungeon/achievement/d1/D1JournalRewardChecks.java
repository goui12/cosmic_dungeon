package net.goui.cosmicdungeon.achievement.d1;

import net.minecraft.nbt.*;
import java.util.*;

public final class D1JournalRewardChecks {
    private static int checks;
    private static void check(boolean ok,String name) { checks++; if(!ok)throw new AssertionError(name); }
    private static CompoundTag receipt(UUID owner) {
        var r=new CompoundTag();r.putInt("schema",1);r.putString("owner",owner.toString());
        r.putString("transaction","aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");r.putInt("slot",3);r.putLong("run",42);
        var item=new CompoundTag();item.putString("id","minecraft:elytra");item.putInt("count",1);r.put("delivered",item);
        return r;
    }
    private static CompoundTag snapshot(CompoundTag receipt) {
        var root=new CompoundTag();root.put("stairway_reward_v1",receipt.copy());
        var neo=new CompoundTag();neo.put("cosmicdungeon",root);
        var player=new CompoundTag();player.put("NeoForgeData",neo);
        var items=new ListTag();var item=receipt.getCompoundOrEmpty("delivered").copy();item.putInt("Slot",3);
        items.add(item);player.put("Inventory",items);
        var equipment=new CompoundTag();equipment.putString("unchanged","authored equipment");player.put("equipment",equipment);
        return player;
    }
    public static void main(String[] args) {
        for(int number=1;number<=3;number++) {
            String id="journal_"+number;
            var entry=D1JournalCatalog.get(id);check(entry!=null,"Three approved sources are packaged");
            var pages=D1JournalCatalog.pages(entry);String body=String.join(" ",entry.paragraphs());
            check(!pages.isEmpty()&&pages.size()<100,"Vanilla written-book pagination fits");
            for(String page:pages)check(page.length()<=180,"Page stays within conservative readable size");
            check(D1JournalCatalog.contentMatches(id,String.join(" ",pages),body),"Pagination preserves exact canonical words");
            check(D1JournalCatalog.contentMatches(id,"  "+body+"  ",body),"Harmless edge whitespace is normalized");
            check(!D1JournalCatalog.contentMatches(id,entry.title(),entry.title()),"A title alone is not a journal");
            check(!D1JournalCatalog.contentMatches(id,body+" forged",body),"Changed raw content rejected");
            check(!D1JournalCatalog.contentMatches(id,body,"filtered replacement"),"Altered filtered view rejected");
            check(D1JournalCatalog.validMarker(1,id,entry.digest()),"Current issued edition accepted");
            check(!D1JournalCatalog.validMarker(2,id,entry.digest()),"Unknown schema rejected");
            check(!D1JournalCatalog.validMarker(1,id,"old-edition"),"Stale edition rejected without mutation");
            check(!D1JournalCatalog.validMarker(1,"journal_9",entry.digest()),"Unknown journal rejected");
            check(!D1JournalCatalog.contentMatches("journal_"+(number==3?1:number+1),body,body),"One journal cannot impersonate another");
        }
        check(D1JournalCatalog.get(null)==null,"Missing identity fails closed");
        UUID owner=UUID.fromString("00000000-0000-0000-0000-000000000001");
        var receipt=receipt(owner);
        check(StairwayReward.valid(receipt,owner),"Committed one-Elytra receipt is valid");
        check(!StairwayReward.valid(receipt,UUID.randomUUID()),"Wrong UUID cannot replay reward");
        for(String key:List.of("schema","owner","transaction","slot","run","delivered")) {
            var bad=receipt.copy();bad.remove(key);
            check(!StairwayReward.valid(bad,owner),"Missing receipt prerequisite rejected: "+key);
        }
        for(int slot:new int[]{-1,36,999}) {
            var bad=receipt.copy();bad.putInt("slot",slot);check(!StairwayReward.valid(bad,owner),"Invalid delivery slot rejected");
        }
        var bad=receipt.copy();bad.getCompoundOrEmpty("delivered").putInt("count",2);
        check(!StairwayReward.valid(bad,owner),"Multiple-item receipt cannot impersonate one Elytra");
        bad=receipt.copy();bad.getCompoundOrEmpty("delivered").putString("id","minecraft:paper");
        check(!StairwayReward.valid(bad,owner),"Wrong reward item rejected");
        var expected=snapshot(receipt);
        check(StairwayReward.savedSnapshotMatches(expected,expected.copy()),"Exact player inventory/receipt snapshot matches");
        var saved=expected.copy();saved.put("Inventory",new ListTag());
        check(!StairwayReward.savedSnapshotMatches(expected,saved),"Receipt without delivered inventory is not a commit");
        saved=expected.copy();saved.remove("equipment");
        check(!StairwayReward.savedSnapshotMatches(expected,saved),"Equipment save mismatch is detected");
        saved=expected.copy();saved.getCompoundOrEmpty("NeoForgeData").getCompoundOrEmpty("cosmicdungeon").remove("stairway_reward_v1");
        check(!StairwayReward.savedSnapshotMatches(expected,saved),"Inventory without receipt is not a commit");
        saved=expected.copy();saved.getCompoundOrEmpty("NeoForgeData").getCompoundOrEmpty("cosmicdungeon")
                .getCompoundOrEmpty("stairway_reward_v1").putString("transaction","bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        check(!StairwayReward.savedSnapshotMatches(expected,saved),"Another transaction cannot satisfy readback");
        var malformed=expected.copy();malformed.remove("Inventory");
        check(!StairwayReward.savedSnapshotMatches(malformed,malformed.copy()),"Incomplete expected image fails closed");
        check(!StairwayReward.savedSnapshotMatches(new CompoundTag(),new CompoundTag()),"Two empty saves are not a reward commit");
        System.out.println(checks+" journal/reward checks passed");
    }
}
