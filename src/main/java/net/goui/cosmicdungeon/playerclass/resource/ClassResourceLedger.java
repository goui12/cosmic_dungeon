package net.goui.cosmicdungeon.playerclass.resource;

import net.minecraft.nbt.*;

/** Additive owner-local schema. Inventory and resource credits share the same native player save. */
public final class ClassResourceLedger {
    public static final String KEY="class_resources";
    private final CompoundTag image;
    private ClassResourceLedger(CompoundTag image){this.image=image;}
    public static ClassResourceLedger forRun(CompoundTag classRoot,long run){
        if(run<=0)throw new IllegalArgumentException("Positive run required");
        if(classRoot.contains(KEY)&&!(classRoot.get(KEY) instanceof CompoundTag))
            throw new IllegalArgumentException("Malformed resource ledger");
        var next=classRoot.getCompoundOrEmpty(KEY).copy();
        if(!next.isEmpty()){
            if(!(next.get("schema") instanceof IntTag)||next.getIntOr("schema",0)!=1)
                throw new IllegalArgumentException("Unsupported resource schema");
            if(!(next.get("run_id") instanceof LongTag)||next.getLongOr("run_id",0)<=0)
                throw new IllegalArgumentException("Invalid saved resource run");
            if(next.contains("balances")&&!(next.get("balances") instanceof CompoundTag))
                throw new IllegalArgumentException("Invalid resource balances");
            if(next.contains("revision")&&(!(next.get("revision") instanceof LongTag)||next.getLongOr("revision",-1)<0))
                throw new IllegalArgumentException("Invalid action revision");
            var balances=next.getCompoundOrEmpty("balances");
            for(var kind:ClassResourceKind.values())if(balances.contains(kind.id())
                    &&(!(balances.get(kind.id()) instanceof IntTag)||balances.getIntOr(kind.id(),-1)<0
                    ||balances.getIntOr(kind.id(),-1)>ClassResourceKind.CAP))
                throw new IllegalArgumentException("Invalid resource balance");
        }
        if(next.getLongOr("run_id",0)!=run){
            var balances=next.getCompoundOrEmpty("balances").copy();
            for(var kind:ClassResourceKind.values())balances.putInt(kind.id(),0);
            next.put("balances",balances);next.putLong("run_id",run);next.putLong("revision",0);
        }
        next.putInt("schema",1);return new ClassResourceLedger(next);
    }
    public long runId(){return image.getLongOr("run_id",0);}
    public long revision(){return image.getLongOr("revision",0);}
    public int amount(ClassResourceKind kind){return image.getCompoundOrEmpty("balances").getIntOr(kind.id(),0);}
    public CompoundTag image(){return image.copy();}
    public CompoundTag applyTo(CompoundTag root){var next=root.copy();next.put(KEY,image.copy());return next;}
    public ClassResourceLedger withAmount(ClassResourceKind kind,int value){
        if(value<0||value>ClassResourceKind.CAP)throw new IllegalArgumentException("Resource outside cap");
        var next=image.copy();var balances=next.getCompoundOrEmpty("balances").copy();
        balances.putInt(kind.id(),value);next.put("balances",balances);return new ClassResourceLedger(next);
    }
    public ClassResourceLedger credit(ClassResourceKind kind,int amount){
        if(amount<0)throw new IllegalArgumentException("Negative credit");
        return withAmount(kind,(int)Math.min(ClassResourceKind.CAP,(long)amount(kind)+amount));
    }
    public ClassResourceLedger nextRevision(){
        if(revision()==Long.MAX_VALUE)throw new IllegalStateException("Resource revision exhausted");
        var next=image.copy();next.putLong("revision",revision()+1);return new ClassResourceLedger(next);
    }
}
