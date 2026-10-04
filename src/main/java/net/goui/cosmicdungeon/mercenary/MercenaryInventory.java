package net.goui.cosmicdungeon.mercenary;

import java.util.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.goui.cosmicdungeon.item.identity.ClassItemOwnership;

/** Copy/plan/commit; no source consumption until the complete result fits. */
public final class MercenaryInventory {
    private MercenaryInventory(){}
    public static List<ItemStack> copy(List<ItemStack> inventory){
        return new ArrayList<>(inventory.stream().map(ItemStack::copy).toList());
    }
    public static void commit(List<ItemStack> inventory,List<ItemStack> result){
        for(int i=0;i<inventory.size();i++)inventory.set(i,result.get(i));
    }
    public static boolean insert(List<ItemStack> inventory,ItemStack incoming){
        var plan=copy(inventory);var left=incoming.copy();
        for(var stack:plan)if(!stack.isEmpty()&&ItemStack.isSameItemSameComponents(stack,left)){
            int amount=Math.min(left.getCount(),Math.max(0,stack.getMaxStackSize()-stack.getCount()));
            stack.grow(amount);left.shrink(amount);if(left.isEmpty())break;
        }
        for(int i=0;i<plan.size()&&!left.isEmpty();i++)if(plan.get(i).isEmpty()){
            int count=Math.min(left.getCount(),left.getMaxStackSize());
            plan.set(i,left.copyWithCount(count));left.shrink(count);
        }
        if(!left.isEmpty())return false;
        commit(inventory,plan);return true;
    }
    public static boolean permitted(ItemStack stack,MercenaryContract contract){
        return contract!=null&&ClassItemOwnership.mayAcquire(stack,contract.id(),contract.classId());
    }
    /** Native recipes, including registered mod recipes; one reagent, one bottle per cycle. */
    public static List<ItemStack> brew(List<ItemStack> inventory,PotionBrewing recipes){
        for(int bottle=0;bottle<inventory.size();bottle++){
            var input=inventory.get(bottle);
            if(input.isEmpty()||!recipes.isInput(input))continue;
            for(int reagent=0;reagent<inventory.size();reagent++){
                var ingredient=inventory.get(reagent);
                if(bottle==reagent||ingredient.isEmpty()||!recipes.hasMix(input.copyWithCount(1),ingredient.copyWithCount(1)))continue;
                var result=recipes.mix(ingredient.copyWithCount(1),input.copyWithCount(1));
                if(result.isEmpty()||ItemStack.matches(result,input.copyWithCount(1)))continue;
                var plan=copy(inventory);plan.get(bottle).shrink(1);plan.get(reagent).shrink(1);
                var remainder=ingredient.copyWithCount(1).getCraftingRemainder();
                if(insert(plan,result)&&insert(plan,remainder))return plan;
            }
        }
        return null;
    }
}
