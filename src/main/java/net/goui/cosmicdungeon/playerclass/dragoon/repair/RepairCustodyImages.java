package net.goui.cosmicdungeon.playerclass.dragoon.repair;
import net.minecraft.nbt.*;
import java.util.*;
/** Lossless owner-local images. Inventory, cursor and escrow are serialized in one player snapshot. */
public final class RepairCustodyImages {
    private RepairCustodyImages(){}
    public static CompoundTag open(UUID session,UUID owner,long scope,boolean customer){
        if(scope<0)throw new IllegalArgumentException("Negative repair scope");
        var state=new CompoundTag();state.putInt("schema",1);state.putString("session",session.toString());
        state.putString("owner",owner.toString());state.putLong("run",scope);state.putBoolean("customer",customer);
        state.put("target",new CompoundTag());state.put("cursor",new CompoundTag());state.put("components",new ListTag());
        return state;
    }
    public static void validate(CompoundTag state,UUID owner){
        if(state.getIntOr("schema",0)!=1||!state.getStringOr("owner","").equals(owner.toString())
                ||state.getLongOr("run",-1)<0||!state.contains("customer"))throw new IllegalArgumentException("Invalid repair custody owner/schema");
        UUID.fromString(state.getStringOr("session",""));
        for(String key:List.of("target","cursor")){
            if(!(state.get(key) instanceof CompoundTag item))throw new IllegalArgumentException("Missing repair image");
            item(item,true);
        }
        if(!state.getBooleanOr("customer",false)&&!state.getCompoundOrEmpty("target").isEmpty())
            throw new IllegalArgumentException("Provider cannot own customer's target");
        if(!(state.get("components") instanceof ListTag components)||components.size()>64)
            throw new IllegalArgumentException("Invalid component escrow");
        var slots=new HashSet<Integer>();
        for(var entry:components){
            if(!(entry instanceof CompoundTag part)||!slots.add(part.getIntOr("slot",-1))
                    ||part.getIntOr("slot",-1)<0||part.getIntOr("slot",-1)>255
                    ||!(part.get("item") instanceof CompoundTag image))throw new IllegalArgumentException("Invalid component source");
            item(image,false);
        }
        if(state.getBooleanOr("customer",false)&&!components.isEmpty())throw new IllegalArgumentException("Customer cannot hold provider components");
        String tx=state.getStringOr("transaction","");
        if(!tx.isEmpty())UUID.fromString(tx);
        if(!components.isEmpty()&&tx.isEmpty())throw new IllegalArgumentException("Unbound component reservation");
    }
    public static void item(CompoundTag item,boolean emptyAllowed){
        if(emptyAllowed&&item.isEmpty())return;
        if(item.getStringOr("id","").isBlank()||item.getIntOr("count",1)<1)
            throw new IllegalArgumentException("Invalid serialized item");
    }
    public static CompoundTag live(CompoundTag state,CompoundTag target,CompoundTag cursor){
        item(target,true);item(cursor,true);var next=state.copy();
        next.put("target",target.copy());next.put("cursor",cursor.copy());return next;
    }
    public static CompoundTag reserve(CompoundTag state,UUID transaction,ListTag components){
        if(!state.getStringOr("transaction","").isEmpty())throw new IllegalStateException("Custody already reserved");
        var next=state.copy();next.putString("transaction",transaction.toString());next.put("components",components.copy());
        validate(next,UUID.fromString(next.getStringOr("owner","")));return next;
    }
    public static CompoundTag unreserve(CompoundTag state){
        var next=state.copy();next.remove("transaction");next.put("components",new ListTag());return next;
    }
    public static List<CompoundTag> held(CompoundTag state,boolean componentsOnly){
        validate(state,UUID.fromString(state.getStringOr("owner","")));
        var result=new ArrayList<CompoundTag>();
        if(!componentsOnly)for(String key:List.of("target","cursor")){
            var image=state.getCompoundOrEmpty(key);if(!image.isEmpty())result.add(image.copy());
        }
        for(var entry:(ListTag)state.get("components"))result.add(((CompoundTag)entry).getCompoundOrEmpty("item").copy());
        return List.copyOf(result);
    }
    public static boolean sameReservation(CompoundTag state,UUID transaction){
        return transaction!=null&&transaction.toString().equals(state.getStringOr("transaction",""));
    }
}
