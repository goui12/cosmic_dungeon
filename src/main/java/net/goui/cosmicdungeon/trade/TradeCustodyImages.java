package net.goui.cosmicdungeon.trade;
import net.minecraft.nbt.*;
import net.goui.cosmicdungeon.playerclass.dragoon.repair.RepairCustodyImages;
import java.util.*;
/** Exact owner-local offer and cursor images. Native NBT retains component widths and nesting. */
public final class TradeCustodyImages {
    private TradeCustodyImages(){}
    public static CompoundTag open(UUID session,UUID owner,UUID peer,long run){
        var state=new CompoundTag();state.putInt("schema",1);state.putString("session",session.toString());
        state.putString("owner",owner.toString());state.putString("peer",peer.toString());state.putLong("run",run);
        var offers=new ListTag();for(int i=0;i<9;i++)offers.add(new CompoundTag());
        state.put("offers",offers);state.put("cursor",new CompoundTag());validate(state,owner);return state;
    }
    public static void validate(CompoundTag state,UUID owner){
        if(state.getIntOr("schema",0)!=1||!owner.toString().equals(state.getStringOr("owner",""))
                ||state.getLongOr("run",-1)<0)throw new IllegalArgumentException("Invalid trade custody owner/schema/scope");
        UUID.fromString(state.getStringOr("session",""));
        if(owner.equals(UUID.fromString(state.getStringOr("peer",""))))throw new IllegalArgumentException("Self trade custody");
        if(!(state.get("offers") instanceof ListTag offers)||offers.size()!=9
                ||!(state.get("cursor") instanceof CompoundTag cursor))throw new IllegalArgumentException("Incomplete trade custody");
        for(var entry:offers){if(!(entry instanceof CompoundTag item))throw new IllegalArgumentException("Invalid offer image");RepairCustodyImages.item(item,true);}
        RepairCustodyImages.item(cursor,true);
        if(state.contains("transaction"))UUID.fromString(state.getStringOr("transaction",""));
    }
    public static CompoundTag live(CompoundTag before,ListTag offers,CompoundTag cursor){
        var next=before.copy();next.put("offers",offers.copy());next.put("cursor",cursor.copy());
        validate(next,UUID.fromString(next.getStringOr("owner","")));return next;
    }
    public static List<CompoundTag> held(CompoundTag state){
        validate(state,UUID.fromString(state.getStringOr("owner","")));var result=new ArrayList<CompoundTag>();
        for(var tag:(ListTag)state.get("offers"))if(!((CompoundTag)tag).isEmpty())result.add(((CompoundTag)tag).copy());
        var cursor=state.getCompoundOrEmpty("cursor");if(!cursor.isEmpty())result.add(cursor.copy());return result;
    }
    public static CompoundTag bind(CompoundTag before,UUID id){
        if(before.contains("transaction")&&!id.toString().equals(before.getStringOr("transaction","")))throw new IllegalArgumentException("Trade already reserved");
        var next=before.copy();next.putString("transaction",id.toString());validate(next,UUID.fromString(next.getStringOr("owner","")));return next;
    }
    public static CompoundTag exchanged(CompoundTag own,CompoundTag peer){
        var owner=UUID.fromString(own.getStringOr("owner",""));validate(own,owner);validate(peer,UUID.fromString(peer.getStringOr("owner","")));
        if(!own.getStringOr("session","").equals(peer.getStringOr("session",""))
                ||!own.getStringOr("peer","").equals(peer.getStringOr("owner",""))
                ||!peer.getStringOr("peer","").equals(owner.toString())||own.getLongOr("run",-1)!=peer.getLongOr("run",-1))
            throw new IllegalArgumentException("Unrelated trade owners");
        return live(own,(ListTag)peer.get("offers"),own.getCompoundOrEmpty("cursor"));
    }
}
