package net.goui.cosmicdungeon.dungeon;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

/** Immutable exact images; no registry bootstrap is needed to inspect/recover a journal decision. */
public final class ChopTravelPlan {
    public static final Codec<ChopTravelPlan> CODEC=CompoundTag.CODEC.xmap(ChopTravelPlan::new,ChopTravelPlan::image);
    private final CompoundTag image;
    public ChopTravelPlan(CompoundTag image){
        this.image=image.copy();
        if(image.getIntOr("version",0)!=1)throw new IllegalArgumentException("Unsupported Chop travel journal");
        UUID.fromString(text("id"));UUID.fromString(text("owner"));
        long run=image.getLong("run").orElseThrow();
        String kind=text("kind");
        if(!Set.of("leave","return","refresh","adopt").contains(kind)||run<0
                ||((kind.equals("leave")||kind.equals("return"))&&run==0))
            throw new IllegalArgumentException("Invalid Chop travel scope");
        if(image.getBoolean("committed").isEmpty())throw new IllegalArgumentException("Missing Chop decision");
        for(String key:List.of("before","after","ownership_before","ownership_after","escrow_before","escrow_after","source","destination"))
            image.getCompound(key).orElseThrow(()->new IllegalArgumentException("Missing travel image: "+key));
        validatePose(tag("source"));validatePose(tag("destination"));
        for(String key:List.of("escrow_before","escrow_after")){
            var e=tag(key);if(e.isEmpty())continue;
            var entry=DungeonInventoryEscrowData.Entry.CODEC.parse(NbtOps.INSTANCE,e).getOrThrow();
            if(entry.runId()!=run||!entry.playerId().equals(owner()))throw new IllegalArgumentException("Foreign travel escrow");
        }
        for(String key:List.of("ownership_before","ownership_after")){
            var e=tag(key);if(!e.isEmpty())ChopOwnershipData.Entry.CODEC.parse(NbtOps.INSTANCE,e).getOrThrow();
        }
        if((kind.equals("adopt")||kind.equals("refresh"))&&(!tag("source").equals(tag("destination"))
                ||!tag("escrow_before").isEmpty()||!tag("escrow_after").isEmpty()))
            throw new IllegalArgumentException("Local Chop recovery cannot teleport or replace escrow");
        if(kind.equals("return")){
            var fire=image.getCompound("campfire").orElseThrow();
            net.minecraft.core.BlockPos.CODEC.parse(NbtOps.INSTANCE,fire.get("pos")).getOrThrow();
        }
    }
    private String text(String key){return image.getString(key).orElseThrow();}
    public UUID id(){return UUID.fromString(text("id"));}
    public UUID owner(){return UUID.fromString(text("owner"));}
    public long run(){return image.getLongOr("run",-1);}
    public String kind(){return text("kind");}
    public boolean committed(){return image.getBooleanOr("committed",false);}
    public CompoundTag tag(String key){return image.getCompoundOrEmpty(key).copy();}
    public CompoundTag image(){return image.copy();}
    public ChopTravelPlan commit(){var next=image();next.putBoolean("committed",true);return new ChopTravelPlan(next);}
    public CompoundTag reservation(){var next=image();next.putBoolean("committed",false);return next;}
    public CompoundTag receipt(){
        var receipt=new CompoundTag();receipt.putString("id",id().toString());receipt.putString("owner",owner().toString());
        receipt.putBoolean("committed",committed());return receipt;
    }
    public boolean receipted(CompoundTag receipt){return receipt().equals(receipt);}
    public boolean recoverable(CompoundTag custody,CompoundTag receipt){
        if(receipted(receipt))return custody.isEmpty();
        return custody.equals(reservation())||(!committed()&&custody.isEmpty());
    }
    public static CompoundTag pose(String dimension,double x,double y,double z,float yaw,float pitch){
        var pose=new CompoundTag();pose.putString("dimension",dimension);
        pose.putDouble("x",x);pose.putDouble("y",y);pose.putDouble("z",z);pose.putFloat("yaw",yaw);pose.putFloat("pitch",pitch);
        validatePose(pose);return pose;
    }
    private static void validatePose(CompoundTag pose){
        if(ResourceLocation.tryParse(pose.getStringOr("dimension",""))==null)throw new IllegalArgumentException("Invalid travel dimension");
        for(String key:List.of("x","y","z","yaw","pitch"))
            if(!Double.isFinite(pose.getDouble(key).orElseThrow()))throw new IllegalArgumentException("Non-finite travel pose");
        if(Math.abs(pose.getDoubleOr("x",0))>30_000_000||Math.abs(pose.getDoubleOr("z",0))>30_000_000)
            throw new IllegalArgumentException("Travel position outside world bounds");
    }
    public static ChopTravelPlan create(UUID owner,long run,String kind,CompoundTag before,CompoundTag after,
            CompoundTag source,CompoundTag destination,CompoundTag escrowBefore,CompoundTag escrowAfter,
            CompoundTag ownershipBefore,CompoundTag ownershipAfter,CompoundTag campfire){
        var t=new CompoundTag();t.putInt("version",1);t.putString("id",UUID.randomUUID().toString());
        t.putString("owner",owner.toString());t.putLong("run",run);t.putString("kind",kind);t.putBoolean("committed",false);
        t.put("before",before.copy());t.put("after",after.copy());t.put("source",source.copy());t.put("destination",destination.copy());
        t.put("escrow_before",escrowBefore.copy());t.put("escrow_after",escrowAfter.copy());
        t.put("ownership_before",ownershipBefore.copy());t.put("ownership_after",ownershipAfter.copy());
        if(campfire!=null)t.put("campfire",campfire.copy());return new ChopTravelPlan(t);
    }
}
