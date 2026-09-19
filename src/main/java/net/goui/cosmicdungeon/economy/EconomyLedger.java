package net.goui.cosmicdungeon.economy;
import net.minecraft.nbt.*;
import java.nio.file.*;
import java.util.*;
/** Fixed 128-row native-NBT pages: bounded rewrite, stable sequence keys, exact item components.
 * Account outbox is authoritative until a page is written and read back successfully. */
public final class EconomyLedger {
    public static final int PAGE_SIZE=128;
    private EconomyLedger(){}
    public static void archive(Path root,Map<Long,CompoundTag> rows)throws java.io.IOException{
        var pages=new TreeMap<Long,Map<Long,CompoundTag>>();
        for(var entry:rows.entrySet()){
            if(entry.getKey()<1)throw new IllegalArgumentException("Invalid ledger sequence");validateRow(entry.getValue());
            pages.computeIfAbsent(entry.getKey()/PAGE_SIZE,k->new TreeMap<>()).put(entry.getKey(),entry.getValue());
        }
        Files.createDirectories(root);
        for(var page:pages.entrySet()){
            Path file=root.resolve(String.format(java.util.Locale.ROOT,"%016x.nbt",page.getKey()));
            var body=Files.exists(file)?NbtIo.readCompressed(file,NbtAccounter.create(32L*1024*1024)):new CompoundTag();
            for(String key:body.keySet()){
                long id=Long.parseLong(key);if(id<1||id/PAGE_SIZE!=page.getKey()||!key.equals(Long.toString(id))||!(body.get(key) instanceof CompoundTag existing))throw new java.io.IOException("Invalid existing ledger page");
                validateRow(existing);
            }
            for(var row:page.getValue().entrySet()){
                String key=Long.toString(row.getKey());var existing=body.get(key);
                if(existing!=null&&!existing.equals(row.getValue()))throw new java.io.IOException("Ledger sequence conflicts with existing evidence: "+key);
                body.put(key,row.getValue().copy());
            }
            Path staged=Files.createTempFile(root,"ledger-",".tmp");
            try{
                NbtIo.writeCompressed(body,staged);
                if(!NbtIo.readCompressed(staged,NbtAccounter.create(32L*1024*1024)).equals(body))throw new java.io.IOException("Ledger staging readback failed");
                Files.move(staged,file,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);
                if(!NbtIo.readCompressed(file,NbtAccounter.create(32L*1024*1024)).equals(body))throw new java.io.IOException("Ledger page readback failed");
            }finally{Files.deleteIfExists(staged);}
        }
    }
    public static void validateRow(CompoundTag row){
        UUID.fromString(row.getStringOr("player",""));
        for(String key:List.of("transaction","type","category","status"))if(row.getStringOr(key,"").isBlank())throw new IllegalArgumentException("Missing ledger "+key);
        for(String key:List.of("requested_trace","before","after","run","timestamp"))if(!(row.get(key) instanceof NumericTag))throw new IllegalArgumentException("Missing ledger number "+key);
        if(row.getLongOr("before",-1)<0||row.getLongOr("after",-1)<0||row.getLongOr("run",-1)<0||!(row.get("details") instanceof CompoundTag))throw new IllegalArgumentException("Invalid ledger values");
        if(row.getStringOr("type","").equals("death_despawn")
                &&(!(row.getCompoundOrEmpty("details").get("drop_destroyed_trace") instanceof NumericTag)
                ||row.getCompoundOrEmpty("details").getLongOr("drop_destroyed_trace",0)<=0
                ||row.getLongOr("before",-1)!=row.getLongOr("after",-1)))
            throw new IllegalArgumentException("Invalid logical drop destruction evidence");
        if(!row.getStringOr("category","").equals(category(row.getStringOr("type",""),row.getLongOr("after",0)-row.getLongOr("before",0))))throw new IllegalArgumentException("Ledger category mismatch");
    }
    public static String category(String type,long delta){
        return switch(type){
            case "player_trade","dragoon_repair","death_debit","death_pickup" -> "transfer";
            case "vendor_retail","direct_repair","inn","inn_fee","death_despawn" -> "sink";
            case "vendor_sale","mob_reward","mob","first_trace","quest_reward","system_reward" -> "generation";
            case "credit","debit","legacy_credit","legacy_debit","admin_or_rollback","admin_adjustment","wealth_review","wealth_observation" -> "administrative";
            default -> delta>=0?"generation":"sink";
        };
    }
    public static CompoundTag row(String transaction,UUID owner,String name,String type,long requested,long before,long after,
            String related,long run,String status,long timestamp,CompoundTag details){
        var row=new CompoundTag();row.putString("transaction",transaction);row.putString("player",owner.toString());row.putString("name",name);
        row.putString("type",type);row.putString("category",category(type,after-before));row.putLong("requested_trace",requested);
        row.putLong("before",before);row.putLong("after",after);row.putString("related",related);row.putLong("run",run);
        row.putString("status",status);row.putLong("timestamp",timestamp);row.put("details",details.copy());return row;
    }
}
