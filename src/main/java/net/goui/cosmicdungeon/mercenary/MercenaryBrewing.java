package net.goui.cosmicdungeon.mercenary;

import java.util.List;
import net.goui.cosmicdungeon.playerclass.theurgist.VitalityPotions;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.level.block.entity.*;

/** Free splash stock. Workstations never spend or rewrite either inventory's materials. */
public final class MercenaryBrewing {
    private MercenaryBrewing(){}
    public static boolean enabled(MercenaryContract contract){
        return supports(contract,true)||supports(contract,false);
    }
    static boolean supports(MercenaryContract contract,boolean positive){
        return (positive?MercenarySkill.POSITIVE_POTIONS:MercenarySkill.NEGATIVE_POTIONS).supports(contract);
    }
    /** Unsupported legacy stock stays intact but is not collected or cast. */
    static boolean roleSplash(ItemStack stack,MercenaryContract contract){
        if(!effectSplash(stack))return false;
        for(var effect:stack.getOrDefault(DataComponents.POTION_CONTENTS,PotionContents.EMPTY).getAllEffects())
            if(!MercenaryPotions.roleAllows(contract,effect.getEffect().value(),false))return false;
        return true;
    }
    static boolean brew(MercenaryEntity entity,BaseContainerBlockEntity stand,PotionBrewing recipes){
        if(!enabled(entity.contract())||!(stand instanceof BrewingStandBlockEntity)
                ||!MercenaryCollection.mayOpen(stand,entity.contract()))return false;
        boolean positive=restock(entity,true,true),negative=restock(entity,false,true);
        return positive||negative;
    }
    static boolean effectSplash(ItemStack stack){
        return stack.is(Items.SPLASH_POTION)&&stack.getOrDefault(DataComponents.POTION_CONTENTS,PotionContents.EMPTY)
                .getAllEffects().iterator().hasNext();
    }
    static boolean positive(ItemStack stack){
        for(var effect:stack.getOrDefault(DataComponents.POTION_CONTENTS,PotionContents.EMPTY).getAllEffects())
            if(!MercenaryPotions.helpful(effect.getEffect().value(),false))return false;
        return effectSplash(stack);
    }
    static boolean positiveStock(ItemStack stack,int level){
        if(!effectSplash(stack))return false;
        for(var effect:stack.getOrDefault(DataComponents.POTION_CONTENTS,PotionContents.EMPTY).getAllEffects()){
            if(!MercenaryPotions.positiveEffect(effect.getEffect().value()))return false;
            if(effect.getEffect().equals(net.minecraft.world.effect.MobEffects.INSTANT_HEALTH)
                    &&level<25&&effect.getAmplifier()>0)return false;
        }
        return true;
    }
    static boolean usable(ItemStack stack,MercenaryContract contract,int level){
        return roleSplash(stack,contract)&&(!supports(contract,true)||positiveStock(stack,level));
    }
    static int stock(List<ItemStack> supplies,boolean positive){
        return stock(supplies,positive,Integer.MAX_VALUE);
    }
    static int stock(List<ItemStack> supplies,boolean positive,int level){
        int count=0;
        for(var stack:supplies)if(positive?positiveStock(stack,level):effectSplash(stack)&&!positive(stack))
            count=Math.min(MercenaryPotionBalance.STOCK,count+Math.min(stack.getCount(),MercenaryPotionBalance.STOCK));
        return count;
    }
    static ItemStack create(boolean positive,int kind,boolean strong){
        return create(positive,kind,strong,0);
    }
    static ItemStack create(boolean positive,int kind,boolean strong,int level){
        if(positive&&kind!=0)return VitalityPotions.splash(level);
        Holder<Potion> potion=positive?(strong?Potions.STRONG_HEALING:Potions.HEALING)
                :kind==0?(strong?Potions.STRONG_HARMING:Potions.HARMING)
                :(strong?Potions.STRONG_POISON:Potions.POISON);
        return PotionContents.createItemStack(Items.SPLASH_POTION,potion);
    }
    static int kind(ItemStack stack,boolean positive){
        var contents=stack.getOrDefault(DataComponents.POTION_CONTENTS,PotionContents.EMPTY);
        if(positive){
            for(var effect:contents.getAllEffects())if(VitalityPotions.isVitality(effect))return 1;
            if(contents.is(Potions.HEALING)||contents.is(Potions.STRONG_HEALING))return 0;
        }else{
            if(contents.is(Potions.HARMING)||contents.is(Potions.STRONG_HARMING))return 0;
            if(contents.is(Potions.POISON)||contents.is(Potions.STRONG_POISON))return 1;
        }
        return -1;
    }
    /** Rebuild only the thrown max-health dose; saved/acquired inventory stacks retain their components. */
    static ItemStack castStack(MercenaryEntity entity,ItemStack stock){
        return castStack(stock,supports(entity.contract(),true),MercenarySkills.level(entity,MercenarySkill.POSITIVE_POTIONS));
    }
    static ItemStack castStack(ItemStack stock,boolean positive,int level){
        return positive&&kind(stock,true)==1?VitalityPotions.splash(level):stock.copyWithCount(1);
    }
    static boolean restock(MercenaryEntity entity,boolean positive,boolean stand){
        if(!supports(entity.contract(),positive))return false;
        String key=MercenaryPotionBalance.stockKey(positive,stand);
        if(!entity.timers().ready(key))return false;
        var skill=positive?MercenarySkill.POSITIVE_POTIONS:MercenarySkill.NEGATIVE_POTIONS;
        int level=MercenarySkills.level(entity,skill);
        int base=MercenaryConfig.BREW_TICKS.get()/(stand?2:1);
        int cadence=positive?MercenaryPotionBalance.positiveCadence(base,level,40)
                :MercenaryPotionBalance.cadence(base,level,40);
        // Failed/full attempts also wait: no tight retries or backlog on unload.
        entity.timers(entity.timers().used(key,cadence));
        if(stock(entity.supplies(),positive,level)>=MercenaryPotionBalance.STOCK)return false;
        int[] counts=new int[2];
        for(var stack:entity.supplies())if(positive?positiveStock(stack,level):effectSplash(stack)){
            int kind=kind(stack,positive);if(kind>=0)counts[kind]+=Math.min(stack.getCount(),6);
        }
        int chosen=counts[1]<counts[0]?1:0;
        double chance=positive?MercenaryPotionBalance.positiveQualityChance(level):MercenaryPotionBalance.qualityChance(level);
        boolean strong=entity.getRandom().nextDouble()<chance;
        return MercenaryInventory.insert(entity.supplies(),create(positive,chosen,strong,level));
    }
}
