package net.goui.cosmicdungeon.playerclass.resource;

import com.mojang.serialization.Codec;
import java.util.*;
import net.goui.cosmicdungeon.transaction.SavedDataProof;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.*;

/** Pending decisions only, not another resource balance store; acknowledgements remove completed images. */
public final class SupplyTransferData extends SavedData {
    public static final String ID="cosmicdungeon_supply_transfers_v1";
    public static final Codec<SupplyTransferData> CODEC=CompoundTag.CODEC.xmap(SupplyTransferData::new,SupplyTransferData::image);
    private static final SavedDataType<SupplyTransferData> TYPE=new SavedDataType<>(ID,SupplyTransferData::new,CODEC);
    private final CompoundTag extra;
    private final Map<UUID,SupplyTransferPlan> plans=new LinkedHashMap<>();
    private final Map<UUID,UUID> owners=new HashMap<>();
    private MinecraftServer server;
    private SupplyTransferData(){extra=new CompoundTag();extra.putInt("schema",1);}
    public SupplyTransferData(CompoundTag image){
        extra=image.copy();
        if(!(image.get("schema") instanceof IntTag)||image.getIntOr("schema",0)!=1
                ||image.contains("pending")&&!(image.get("pending") instanceof CompoundTag))
            throw new IllegalArgumentException("Unsupported supply journal; original data preserved");
        var pending=image.getCompoundOrEmpty("pending");
        if(pending.size()>128)throw new IllegalArgumentException("Supply journal limit exceeded");
        for(String key:pending.keySet()){
            var plan=new SupplyTransferPlan(pending.getCompound(key).orElseThrow());
            if(!key.equals(plan.id().toString())||plan.complete())throw new IllegalArgumentException("Invalid pending transfer");
            add(plan);
        }
    }
    public static SupplyTransferData get(MinecraftServer server){
        SavedDataProof.validate(server,ID,CODEC);
        var data=server.overworld().getDataStorage().computeIfAbsent(TYPE);data.server=server;return data;
    }
    private void add(SupplyTransferPlan plan){
        if(plans.containsKey(plan.id())||owners.containsKey(plan.donor())||owners.containsKey(plan.recipient()))
            throw new IllegalStateException("Owner already has a pending supply transfer");
        plans.put(plan.id(),plan);owners.put(plan.donor(),plan.id());owners.put(plan.recipient(),plan.id());
    }
    public SupplyTransferPlan pending(UUID owner){return plans.get(owners.get(owner));}
    public CompoundTag image(){
        var image=extra.copy();var pending=new CompoundTag();plans.forEach((id,plan)->pending.put(id.toString(),plan.image()));
        image.put("pending",pending);return image;
    }
    public void reserve(SupplyTransferPlan plan){
        if(plans.size()>=128||plan.committed()||plan.complete())throw new IllegalStateException("Invalid supply reservation");
        add(plan);setDirty();
    }
    public void commit(UUID id){var plan=Objects.requireNonNull(plans.get(id));plans.put(id,plan.commit());setDirty();}
    public void acknowledge(UUID id,UUID owner){
        var plan=Objects.requireNonNull(plans.get(id)).acknowledge(owner);
        if(plan.complete()){plans.remove(id);owners.remove(plan.donor());owners.remove(plan.recipient());}
        else plans.put(id,plan);
        setDirty();
    }
    public boolean flushVerified(){return SavedDataProof.save(server,ID,CODEC,this);}
}
