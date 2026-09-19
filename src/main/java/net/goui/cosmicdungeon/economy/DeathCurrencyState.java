package net.goui.cosmicdungeon.economy;

import net.minecraft.nbt.*;
import java.math.BigInteger;
import java.util.*;

/** Optional account extension. Missing legacy field migrates to empty; malformed present data fails closed. */
final class DeathCurrencyState {
    final Map<UUID,DeathCurrencyRecord> entries=new LinkedHashMap<>();
    final Map<UUID,UUID> latest=new HashMap<>(),pending=new HashMap<>();
    CompoundTag save(){
        var tag=new CompoundTag();tag.putInt("schema",1);var records=new CompoundTag();var last=new CompoundTag();
        entries.forEach((id,record)->records.put(id.toString(),record.save()));
        latest.forEach((owner,id)->last.putString(owner.toString(),id.toString()));
        tag.put("entries",records);tag.put("latest",last);return tag;
    }
    static DeathCurrencyState load(CompoundTag tag){
        var state=new DeathCurrencyState();
        if(tag.getIntOr("schema",0)!=1||!(tag.get("entries") instanceof CompoundTag records)
                ||!(tag.get("latest") instanceof CompoundTag last))throw new IllegalArgumentException("Death currency schema requires review");
        for(String key:last.keySet())state.latest.put(UUID.fromString(key),UUID.fromString(last.getStringOr(key,"")));
        var newestOwners=new HashMap<UUID,UUID>();
        state.latest.forEach((owner,id)->{
            if(newestOwners.putIfAbsent(id,owner)!=null)throw new IllegalArgumentException("Shared death identity");
        });
        for(String key:records.keySet()){
            var record=DeathCurrencyRecord.load(records.getCompound(key).orElseThrow());
            if(!record.id().toString().equals(key))throw new IllegalArgumentException("Death record ID mismatch");
            if(!record.active()){
                if(!record.id().equals(state.latest.get(record.owner()))||state.pending.putIfAbsent(record.owner(),record.id())!=null)
                    throw new IllegalArgumentException("Unbound pending death");
            }
            if(newestOwners.containsKey(record.id())&&!newestOwners.get(record.id()).equals(record.owner()))
                throw new IllegalArgumentException("Death identity assigned to another owner");
            state.entries.put(record.id(),record);
        }
        return state;
    }
    BigInteger activeSupply(){
        var total=BigInteger.ZERO;
        for(var record:entries.values())if(record.active())total=total.add(BigInteger.valueOf(record.amount()));
        return total;
    }
}
