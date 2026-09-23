package net.goui.cosmicdungeon.dungeon;

import net.minecraft.nbt.*;
import java.util.*;

/** Developer-reviewed orphan recovery, never an inference from an item's name or location. */
public final class ChopRecoveryReview {
    private static final String RAW="cosmicdungeon:raw_farrows_chop", COOKED="cosmicdungeon:farrows_chop";
    private static final String OWNER="cosmicdungeon:chop_owner", TOKEN="cosmicdungeon:chop_token";
    private static final List<String> TRAVEL=List.of("cosmicdungeon:dungeon_return_target","cosmicdungeon:coordinates");
    private ChopRecoveryReview() {}
    public record Evidence(UUID owner,int count,int inventoryChops,int enderChops,UUID itemOwner,UUID token,
                           ChopOwnershipData.Entry ownership,UUID returnOwner,long returnRun,
                           boolean retainedRun,boolean otherInventory,boolean foreignToken) {}
    public static String rejection(Evidence e) {
        if(e.count()!=1||e.inventoryChops()!=1||e.enderChops()!=0)
            return "Keep duplicates, overstacks and Ender Chest Chops intact for complete-save review.";
        if(e.retainedRun()||e.otherInventory())
            return "Run, escrow or stored belongings still exist; recover those before reviewing this Chop.";
        if(e.foreignToken())return "Another owner already holds this token.";
        if((e.itemOwner()==null)!=(e.token()==null))return "Incomplete owner/token markers need complete-save review.";
        if(e.itemOwner()!=null&&!e.owner().equals(e.itemOwner()))return "The Chop belongs to another owner.";
        if(e.returnOwner()!=null&&(!e.owner().equals(e.returnOwner())||e.returnRun()<=0))
            return "The return target has a foreign owner or invalid run.";
        if(e.returnOwner()==null&&e.returnRun()!=0)return "Incomplete return target.";
        var entry=e.ownership();
        if(entry!=null) {
            if(entry.deliver())return "A Raw delivery is already pending; recover its saved entitlement.";
            if(e.token()==null||!entry.token().equals(e.token().toString()))return "The item and ownership record disagree.";
            if(entry.runId()>0&&e.returnRun()>0&&entry.runId()!=e.returnRun())return "The ownership and return runs disagree.";
        }
        return "";
    }
    /** Audit only the selected exact item; other inventory slots may never be rewritten by review. */
    public static CompoundTag audit(ChopTravelPlan plan,UUID developer,int slot) {
        var audit=new CompoundTag();audit.putInt("version",1);audit.putString("developer",developer.toString());
        audit.putInt("slot",slot);audit.put("before_item",selected(plan.tag("before"),slot));
        audit.put("after_item",selected(plan.tag("after"),slot));
        audit.put("ownership_before",plan.tag("ownership_before"));audit.put("ownership_after",plan.tag("ownership_after"));
        return audit;
    }
    private static CompoundTag selected(CompoundTag inventory,int slot) {
        if(slot<0||slot>=36)throw new IllegalArgumentException("Review requires an ordinary inventory slot.");
        var slots=new HashSet<Integer>();CompoundTag selected=null;
        for(var value:inventory.getListOrEmpty("Items")) {
            if(!(value instanceof CompoundTag item))throw new IllegalArgumentException("Invalid item image.");
            int index=Byte.toUnsignedInt(item.getByte("Slot").orElseThrow());
            if(!slots.add(index))throw new IllegalArgumentException("Duplicate inventory slot.");
            if(index==slot)selected=item;
        }
        if(selected==null)throw new IllegalArgumentException("Reviewed slot is empty.");
        return selected.copy();
    }
    private static UUID componentUuid(CompoundTag components,String key) {
        return components.contains(key)?UUID.fromString(components.getString(key).orElseThrow()):null;
    }
    private static CompoundTag withoutTravel(CompoundTag item) {
        var result=item.copy();result.remove("id");
        var components=result.getCompoundOrEmpty("components").copy();
        for(String key:TRAVEL) {components.remove(key);components.remove("!"+key);}
        for(String key:List.of(OWNER,TOKEN)) {components.remove(key);components.remove("!"+key);}
        if(components.isEmpty())result.remove("components");else result.put("components",components);
        return result;
    }
    public static void validate(ChopTravelPlan plan) {
        var audit=plan.tag("review");
        if(audit.getIntOr("version",0)!=1)throw new IllegalArgumentException("Unknown Chop review.");
        UUID.fromString(audit.getString("developer").orElseThrow());
        if(plan.run()!=0||!Set.of("adopt","refresh").contains(plan.kind()))
            throw new IllegalArgumentException("Review cannot authorize a journey.");
        int slot=audit.getInt("slot").orElseThrow();
        var before=selected(plan.tag("before"),slot);var after=selected(plan.tag("after"),slot);
        if(!audit.equals(audit(plan,UUID.fromString(audit.getString("developer").orElseThrow()),slot)))
            throw new IllegalArgumentException("Review evidence differs from its decision.");
        if(!Set.of(RAW,COOKED).contains(before.getStringOr("id",""))||!RAW.equals(after.getStringOr("id",""))
                ||before.getIntOr("count",1)!=1||after.getIntOr("count",1)!=1||!withoutTravel(before).equals(withoutTravel(after)))
            throw new IllegalArgumentException("Review may only rebind one Chop and clear its old return target.");
        var original=before.getCompoundOrEmpty("components");
        var oldTarget=original.contains(TRAVEL.get(0))?original.getCompound(TRAVEL.get(0)).orElseThrow():new CompoundTag();
        var oldOwnership=plan.tag("ownership_before").isEmpty()?null:ChopOwnershipData.Entry.CODEC.parse(NbtOps.INSTANCE,plan.tag("ownership_before")).getOrThrow();
        int copies=0;
        for(var value:plan.tag("before").getListOrEmpty("Items"))
            if(value instanceof CompoundTag item&&Set.of(RAW,COOKED).contains(item.getStringOr("id","")))copies++;
        String reason=rejection(new Evidence(plan.owner(),before.getIntOr("count",1),copies,0,
                componentUuid(original,OWNER),componentUuid(original,TOKEN),oldOwnership,
                oldTarget.isEmpty()?null:UUID.fromString(oldTarget.getString("owner").orElseThrow()),
                oldTarget.isEmpty()?0:oldTarget.getLong("run_id").orElseThrow(),false,false,false));
        if(!reason.isEmpty())throw new IllegalArgumentException(reason);
        var components=after.getCompoundOrEmpty("components");
        if(TRAVEL.stream().anyMatch(components::contains)||!plan.owner().toString().equals(components.getStringOr(OWNER,"")))
            throw new IllegalArgumentException("Reviewed Chop must be Raw and owner-bound.");
        var token=UUID.fromString(components.getString(TOKEN).orElseThrow());
        var originalToken=componentUuid(original,TOKEN);
        if(originalToken!=null&&!originalToken.equals(token))
            throw new IllegalArgumentException("Review must preserve an existing self-owned token.");
        if(!plan.tag("ownership_after").equals(ChopOwnershipData.issuedImage(token)))
            throw new IllegalArgumentException("Reviewed item token differs from ownership.");
        var expected=plan.tag("before");var items=expected.getListOrEmpty("Items");
        for(int i=0;i<items.size();i++)
            if(Byte.toUnsignedInt(((CompoundTag)items.get(i)).getByte("Slot").orElseThrow())==slot)items.set(i,after.copy());
        if(!expected.equals(plan.tag("after")))throw new IllegalArgumentException("Review changed another inventory slot.");
    }
}
