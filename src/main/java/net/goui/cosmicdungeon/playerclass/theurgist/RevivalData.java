package net.goui.cosmicdungeon.playerclass.theurgist;

import com.mojang.serialization.Codec;
import java.util.*;
import net.goui.cosmicdungeon.transaction.SavedDataProof;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.*;

/** Pending decisions only, not another resource balance store; acknowledgements remove completed images. */
public final class RevivalData extends SavedData {
    public static final String ID="cosmicdungeon_player_revivals_v1";
    public static final Codec<RevivalData> CODEC=CompoundTag.CODEC.xmap(RevivalData::new,RevivalData::image);
    private static final SavedDataType<RevivalData> TYPE=new SavedDataType<>(ID,RevivalData::new,CODEC);
    private final CompoundTag extra;
    private final Map<UUID,RevivalPlan> plans=new LinkedHashMap<>();
    private final Map<UUID,UUID> owners=new HashMap<>();
    private MinecraftServer server;
    private RevivalData(){extra=new CompoundTag();extra.putInt("schema",1);}
    public RevivalData(CompoundTag image){
        extra=image.copy();
        if(!(image.get("schema") instanceof IntTag)||image.getIntOr("schema",0)!=1
                ||image.contains("pending")&&!(image.get("pending") instanceof CompoundTag))
            throw new IllegalArgumentException("Unsupported revival journal; original data preserved");
        var pending=image.getCompoundOrEmpty("pending");
        if(pending.size()>128)throw new IllegalArgumentException("Revival journal limit exceeded");
        for(String key:pending.keySet()){
            var plan=new RevivalPlan(pending.getCompound(key).orElseThrow());
            if(!key.equals(plan.id().toString())||plan.complete())throw new IllegalArgumentException("Invalid pending transfer");
            add(plan);
        }
    }
    public static RevivalData get(MinecraftServer server){
        SavedDataProof.validate(server,ID,CODEC);
        var data=server.overworld().getDataStorage().computeIfAbsent(TYPE);data.server=server;return data;
    }
    private void add(RevivalPlan plan){
        if(plans.containsKey(plan.id())||owners.containsKey(plan.caster())||owners.containsKey(plan.target()))
            throw new IllegalStateException("Owner already has a pending revival");
        plans.put(plan.id(),plan);owners.put(plan.caster(),plan.id());owners.put(plan.target(),plan.id());
    }
    public RevivalPlan pending(UUID owner){return plans.get(owners.get(owner));}
    public CompoundTag image(){
        var image=extra.copy();var pending=new CompoundTag();plans.forEach((id,plan)->pending.put(id.toString(),plan.image()));
        image.put("pending",pending);return image;
    }
    public void reserve(RevivalPlan plan){
        if(plans.size()>=128||plan.decision()!=0||plan.complete())throw new IllegalStateException("Invalid revival reservation");
        add(plan);setDirty();
    }
    public void decide(UUID id,boolean success){var plan=Objects.requireNonNull(plans.get(id));plans.put(id,plan.decide(success));setDirty();}
    public void acknowledge(UUID id,UUID owner){
        var plan=Objects.requireNonNull(plans.get(id)).acknowledge(owner);
        if(plan.complete()){plans.remove(id);owners.remove(plan.caster());owners.remove(plan.target());}
        else plans.put(id,plan);
        setDirty();
    }
    public boolean flushVerified(){return SavedDataProof.save(server,ID,CODEC,this);}
}
