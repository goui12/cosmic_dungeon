package net.goui.cosmicdungeon.playerclass.resource;

import com.mojang.serialization.Codec;
import java.util.*;
import net.minecraft.nbt.*;

/** Immutable two-owner decision. Full images exist only while either native owner save is unsettled. */
public final class SupplyTransferPlan {
    public static final Codec<SupplyTransferPlan> CODEC=CompoundTag.CODEC.xmap(SupplyTransferPlan::new,SupplyTransferPlan::image);
    private final CompoundTag image;
    public SupplyTransferPlan(CompoundTag source){
        image=source.copy();
        if(!(image.get("schema") instanceof IntTag)||image.getIntOr("schema",0)!=1)
            throw new IllegalArgumentException("Unsupported supply transfer schema");
        id();donor();recipient();
        if(donor().equals(recipient())||!(image.get("run") instanceof LongTag)||run()<=0
                ||!(image.get("yield") instanceof IntTag)||amount()<1||amount()>600
                ||ClassResourceKind.byId(image.getStringOr("kind","")).isEmpty())
            throw new IllegalArgumentException("Invalid supply transfer identity");
        for(String flag:List.of("committed","donor_ack","recipient_ack"))
            if(image.getBoolean(flag).isEmpty())throw new IllegalArgumentException("Missing transfer decision");
        for(String key:List.of("inventory_before","inventory_after","resource_before","resource_after"))
            if(!(image.get(key) instanceof CompoundTag))throw new IllegalArgumentException("Missing transfer image");
        var root=new CompoundTag();root.put(ClassResourceLedger.KEY,tag("resource_before"));
        var before=ClassResourceLedger.forRun(root,run());
        if(!before.image().equals(tag("resource_before"))||before.amount(kind())+amount()>600
                ||!before.credit(kind(),amount()).nextRevision().image().equals(tag("resource_after")))
            throw new IllegalArgumentException("Transfer changes unrelated resources or exceeds headroom");
    }
    public UUID id(){return UUID.fromString(image.getStringOr("id",""));}
    public UUID donor(){return UUID.fromString(image.getStringOr("donor",""));}
    public UUID recipient(){return UUID.fromString(image.getStringOr("recipient",""));}
    public long run(){return image.getLongOr("run",0);}
    public int amount(){return image.getIntOr("yield",0);}
    public ClassResourceKind kind(){return ClassResourceKind.byId(image.getStringOr("kind","")).orElseThrow();}
    public boolean committed(){return image.getBooleanOr("committed",false);}
    public boolean owns(UUID owner){return donor().equals(owner)||recipient().equals(owner);}
    private String ackKey(UUID owner){
        if(!owns(owner))throw new IllegalArgumentException("Foreign transfer owner");
        return donor().equals(owner)?"donor_ack":"recipient_ack";
    }
    public boolean acknowledged(UUID owner){return image.getBooleanOr(ackKey(owner),false);}
    public boolean complete(){return acknowledged(donor())&&acknowledged(recipient());}
    public CompoundTag image(){return image.copy();}
    public CompoundTag tag(String key){return image.getCompoundOrEmpty(key).copy();}
    public SupplyTransferPlan commit(){
        if(acknowledged(donor())||acknowledged(recipient()))throw new IllegalStateException("Cancelled transfer cannot commit");
        var next=image();next.putBoolean("committed",true);return new SupplyTransferPlan(next);
    }
    public SupplyTransferPlan acknowledge(UUID owner){
        var next=image();next.putBoolean(ackKey(owner),true);return new SupplyTransferPlan(next);
    }
    public CompoundTag reservation(UUID owner){
        ackKey(owner);var value=image();value.putBoolean("committed",false);
        value.putBoolean("donor_ack",false);value.putBoolean("recipient_ack",false);
        value.putInt("schema",1);value.putString("id",id().toString());
        value.putString("owner",owner.toString());value.putString("donor",donor().toString());value.putString("recipient",recipient().toString());
        value.putLong("run",run());value.putString("kind",kind().id());value.putInt("yield",amount());return value;
    }
    public CompoundTag receipt(UUID owner){
        ackKey(owner);var next=new CompoundTag();next.putInt("schema",1);next.putString("id",id().toString());
        next.putString("owner",owner.toString());next.putLong("run",run());next.putString("kind",kind().id());
        next.putInt("yield",amount());next.putBoolean("committed",committed());return next;
    }
    public boolean recoverable(UUID owner,CompoundTag custody,CompoundTag receipt){
        if(receipt(owner).equals(receipt))return custody.isEmpty();
        if(acknowledged(owner))return false;
        return custody.equals(reservation(owner))||(!committed()&&custody.isEmpty());
    }
    public static SupplyTransferPlan create(UUID donor,UUID recipient,long run,ClassResourceKind kind,int amount,
            CompoundTag inventoryBefore,CompoundTag inventoryAfter,ClassResourceLedger resourceBefore){
        var image=new CompoundTag();image.putInt("schema",1);image.putString("id",UUID.randomUUID().toString());
        image.putString("donor",donor.toString());image.putString("recipient",recipient.toString());image.putLong("run",run);
        image.putString("kind",kind.id());image.putInt("yield",amount);image.putBoolean("committed",false);
        image.putBoolean("donor_ack",false);image.putBoolean("recipient_ack",false);
        image.put("inventory_before",inventoryBefore.copy());image.put("inventory_after",inventoryAfter.copy());
        image.put("resource_before",resourceBefore.image());image.put("resource_after",resourceBefore.credit(kind,amount).nextRevision().image());
        return new SupplyTransferPlan(image);
    }
}
