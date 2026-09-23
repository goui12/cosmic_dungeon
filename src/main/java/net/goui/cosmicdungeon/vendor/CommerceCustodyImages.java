package net.goui.cosmicdungeon.vendor;
import net.minecraft.nbt.*;
import net.goui.cosmicdungeon.playerclass.dragoon.repair.RepairCustodyImages;
import java.util.*;
/** Detached exact input lots and approved outputs; both remain evidence until the decision is archived. */
public final class CommerceCustodyImages {
    private CommerceCustodyImages(){}
    public static CompoundTag create(UUID transaction,UUID owner,long run,ListTag inputs,ListTag outputs,CompoundTag details){
        var p=new CompoundTag();p.putInt("schema",1);p.putString("transaction",transaction.toString());p.putString("owner",owner.toString());
        p.putLong("run",run);p.put("inputs",inputs.copy());p.put("outputs",outputs.copy());p.put("details",details.copy());validate(p,owner);return p;
    }
    public static void validate(CompoundTag p,UUID owner){
        if(p.getIntOr("schema",0)!=1||!owner.toString().equals(p.getStringOr("owner",""))||p.getLongOr("run",-1)<0||!(p.get("details") instanceof CompoundTag))throw new IllegalArgumentException("Invalid commerce owner/schema/scope");
        UUID.fromString(p.getStringOr("transaction",""));
        for(String key:List.of("inputs","outputs")){
            if(!(p.get(key) instanceof ListTag lots)||lots.size()>41)throw new IllegalArgumentException("Invalid commerce item list");
            var slots=new HashSet<Integer>();for(var tag:lots){
                if(!(tag instanceof CompoundTag lot)||!(lot.get("item") instanceof CompoundTag item))throw new IllegalArgumentException("Invalid commerce item");
                int slot=lot.getIntOr("slot",-2);if(slot< -1||slot>=41||(key.equals("inputs")&&(slot<0||!slots.add(slot))))throw new IllegalArgumentException("Invalid/duplicate input slot");
                RepairCustodyImages.item(item,false);
            }
        }
    }
    /** Pure recovery gate used by runtime and native interrupted-save fixtures. */
    public static ListTag recoverable(net.goui.cosmicdungeon.economy.AccountOperation op,UUID id,UUID owner,CompoundTag current,CompoundTag receipt){
        if(!op.owner().equals(owner)||op.reserved())throw new IllegalArgumentException("Undecided or foreign commerce recovery");
        boolean committed=op.status().equals(net.goui.cosmicdungeon.economy.AccountTransfer.COMMITTED);
        String outcome=committed?"committed":"cancelled";
        if(net.goui.cosmicdungeon.playerclass.dragoon.repair.RepairTransactions.receiptMatches(receipt,owner,id,outcome)){
            if(!current.isEmpty())throw new IllegalStateException("Applied commerce receipt still has custody");
            return new ListTag();
        }
        var plan=op.plan();validate(plan,owner);
        if(!id.toString().equals(plan.getStringOr("transaction",""))||op.run()!=plan.getLongOr("run",-1))throw new IllegalArgumentException("Unbound commerce recovery");
        if(current.isEmpty()){
            if(op.prepared()||committed)throw new IllegalStateException("Verified commerce custody missing; full-save review required");
            return new ListTag();
        }
        if(!current.equals(plan))throw new IllegalStateException("Owner commerce custody differs from prepared plan");
        return returns(plan,owner,committed);
    }
    public static CompoundTag lot(int slot,CompoundTag item){var lot=new CompoundTag();lot.putInt("slot",slot);lot.put("item",item.copy());return lot;}
    public static ListTag returns(CompoundTag plan,UUID owner,boolean committed){validate(plan,owner);return ((ListTag)plan.get(committed?"outputs":"inputs")).copy();}
}
