package net.goui.cosmicdungeon.playerclass.resource;

import java.util.*;
import java.util.function.Predicate;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/** Deterministic, no-waste item plan; validates every original stack before any mutation. */
public final class ResourceRecycling {
    private ResourceRecycling(){}
    public record Change(int slot,ItemStack before,ItemStack after){}
    public record Plan(List<Change> changes,int credit){
        public Plan{changes=List.copyOf(changes);if(credit<0||credit>ClassResourceKind.CAP)throw new IllegalArgumentException("Invalid yield");}
    }
    public static Plan plan(Container inventory,int current,Predicate<ItemStack> eligible){
        if(current<0||current>ClassResourceKind.CAP)throw new IllegalArgumentException("Invalid resource amount");
        int headroom=ClassResourceKind.CAP-current,total=0;var changes=new ArrayList<Change>();
        for(int slot=0;slot<inventory.getContainerSize()&&headroom>0;slot++){
            var stack=inventory.getItem(slot);
            if(stack.isEmpty()||!eligible.test(stack))continue;
            int take=Math.min(headroom,stack.getCount());var after=stack.copy();after.shrink(take);
            changes.add(new Change(slot,stack.copy(),after));headroom-=take;total+=take;
        }
        return new Plan(changes,total);
    }
    public static boolean any(Container inventory,ClassResourceKind kind){
        for(int slot=0;slot<inventory.getContainerSize();slot++){
            var stack=inventory.getItem(slot);if(!stack.isEmpty()&&stack.is(kind.tag()))return true;
        }return false;
    }
    public static boolean apply(Container inventory,Plan plan){
        for(var change:plan.changes())if(change.slot()<0||change.slot()>=inventory.getContainerSize()
                ||!ItemStack.matches(inventory.getItem(change.slot()),change.before()))return false;
        for(var change:plan.changes())inventory.setItem(change.slot(),change.after().copy());
        if(plan.credit()>0)inventory.setChanged();return true;
    }
    public static void rollbackBeforeSave(Container inventory,Plan plan){
        for(var change:plan.changes())inventory.setItem(change.slot(),change.before().copy());
        inventory.setChanged();
    }
}
