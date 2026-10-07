package net.goui.cosmicdungeon.playerclass.theurgist;

import java.util.*;
import net.minecraft.nbt.*;
import net.goui.cosmicdungeon.playerclass.resource.*;

/** Immutable pending accepted-resurrection decision; zero/uncommitted never debits resources. */
public final class RevivalPlan {
    public static final int COST=120;
    private final CompoundTag image;
    public RevivalPlan(CompoundTag source){
        image=source.copy();
        if(!(image.get("schema") instanceof IntTag)||image.getIntOr("schema",0)!=1
                ||!(image.get("run") instanceof LongTag)||run()<=0||caster().equals(target())
                ||!(image.get("decision") instanceof IntTag)||decision()<0||decision()>2
                ||image.getBoolean("caster_ack").isEmpty()||image.getBoolean("target_ack").isEmpty())
            throw new IllegalArgumentException("Invalid revival plan");
        id();death();
        if(decision()==0&&(acknowledged(caster())||acknowledged(target())))throw new IllegalArgumentException("Undecided revival cannot be acknowledged");
        for(String key:List.of("before","after"))if(!(image.get(key) instanceof CompoundTag))throw new IllegalArgumentException("Missing revival resource image");
        var root=new CompoundTag();root.put(ClassResourceLedger.KEY,tag("before"));var before=ClassResourceLedger.forRun(root,run());
        int amount=before.amount(ClassResourceKind.BREWING_SUPPLIES);
        if(!before.image().equals(tag("before"))||amount<COST||!before.withAmount(ClassResourceKind.BREWING_SUPPLIES,amount-COST).nextRevision().image().equals(tag("after")))
            throw new IllegalArgumentException("Revival must debit exactly120 and preserve all other resources");
    }
    public UUID id(){return UUID.fromString(image.getStringOr("id",""));}
    public UUID caster(){return UUID.fromString(image.getStringOr("caster",""));}
    public UUID target(){return UUID.fromString(image.getStringOr("target",""));}
    public UUID death(){return UUID.fromString(image.getStringOr("death",""));}
    public long run(){return image.getLongOr("run",0);}
    public int decision(){return image.getIntOr("decision",-1);}
    public CompoundTag image(){return image.copy();}
    public CompoundTag tag(String key){return image.getCompoundOrEmpty(key).copy();}
    private String ackKey(UUID owner){if(owner.equals(caster()))return "caster_ack";if(owner.equals(target()))return "target_ack";throw new IllegalArgumentException("Foreign owner");}
    public boolean acknowledged(UUID owner){return image.getBooleanOr(ackKey(owner),false);}
    public boolean complete(){return acknowledged(caster())&&acknowledged(target());}
    public RevivalPlan decide(boolean success){if(decision()!=0)throw new IllegalStateException("Revival already decided");var n=image();n.putInt("decision",success?1:2);return new RevivalPlan(n);}
    public RevivalPlan acknowledge(UUID owner){if(decision()==0)throw new IllegalStateException("Undecided revival");var n=image();n.putBoolean(ackKey(owner),true);return new RevivalPlan(n);}
    public CompoundTag reservation(UUID owner){ackKey(owner);var n=image();n.putInt("decision",0);n.putBoolean("caster_ack",false);n.putBoolean("target_ack",false);n.putString("owner",owner.toString());return n;}
    public CompoundTag receipt(UUID owner,boolean success){ackKey(owner);var n=new CompoundTag();n.putInt("schema",1);n.putString("id",id().toString());n.putString("owner",owner.toString());n.putString("caster",caster().toString());n.putString("target",target().toString());n.putString("death",death().toString());n.putLong("run",run());n.putBoolean("success",success);return n;}
    public static RevivalPlan create(UUID caster,UUID target,UUID death,long run,ClassResourceLedger ledger){
        var n=new CompoundTag();n.putInt("schema",1);n.putString("id",UUID.randomUUID().toString());n.putString("caster",caster.toString());n.putString("target",target.toString());n.putString("death",death.toString());n.putLong("run",run);
        n.putInt("decision",0);n.putBoolean("caster_ack",false);n.putBoolean("target_ack",false);n.put("before",ledger.image());
        n.put("after",ledger.withAmount(ClassResourceKind.BREWING_SUPPLIES,ledger.amount(ClassResourceKind.BREWING_SUPPLIES)-COST).nextRevision().image());
        return new RevivalPlan(n);
    }
}
