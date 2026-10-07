package net.goui.cosmicdungeon.mercenary;

import java.util.List;
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
    /** Opposite/mixed legacy stock stays intact but is not collected or cast. */
    static boolean roleSplash(ItemStack stack,MercenaryContract contract){
        if(!effectSplash(stack))return false;
        for(var effect:stack.getOrDefault(DataComponents.POTION_CONTENTS,PotionContents.EMPTY).getAllEffects())
            if(!supports(contract,MercenaryPotions.helpful(effect.getEffect().value(),false)))return false;
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
    static int stock(List<ItemStack> supplies,boolean positive){
        int count=0;
        for(var stack:supplies)if(effectSplash(stack)&&positive(stack)==positive)
            count=Math.min(MercenaryPotionBalance.STOCK,count+Math.min(stack.getCount(),MercenaryPotionBalance.STOCK));
        return count;
    }
    static ItemStack create(boolean positive,int kind,boolean strong){
        Holder<Potion> potion=positive ? switch(kind) {
            case 0 -> strong?Potions.STRONG_HEALING:Potions.HEALING;
            case 1 -> strong?Potions.STRONG_STRENGTH:Potions.STRENGTH;
            default -> strong?Potions.STRONG_SWIFTNESS:Potions.SWIFTNESS;
        } : kind==0?(strong?Potions.STRONG_HARMING:Potions.HARMING)
                :(strong?Potions.STRONG_POISON:Potions.POISON);
        return PotionContents.createItemStack(Items.SPLASH_POTION,potion);
    }
    static int kind(ItemStack stack,boolean positive){
        var contents=stack.getOrDefault(DataComponents.POTION_CONTENTS,PotionContents.EMPTY);
        if(positive){
            if(contents.is(Potions.HEALING)||contents.is(Potions.STRONG_HEALING))return 0;
            if(contents.is(Potions.STRENGTH)||contents.is(Potions.STRONG_STRENGTH))return 1;
            if(contents.is(Potions.SWIFTNESS)||contents.is(Potions.STRONG_SWIFTNESS))return 2;
        }else{
            if(contents.is(Potions.HARMING)||contents.is(Potions.STRONG_HARMING))return 0;
            if(contents.is(Potions.POISON)||contents.is(Potions.STRONG_POISON))return 1;
        }
        return -1;
    }
    static boolean restock(MercenaryEntity entity,boolean positive,boolean stand){
        if(!supports(entity.contract(),positive))return false;
        String key=MercenaryPotionBalance.stockKey(positive,stand);
        if(!entity.timers().ready(key))return false;
        var skill=positive?MercenarySkill.POSITIVE_POTIONS:MercenarySkill.NEGATIVE_POTIONS;
        int level=MercenarySkills.level(entity,skill);
        int cadence=MercenaryPotionBalance.cadence(MercenaryConfig.BREW_TICKS.get()/(stand?2:1),level,40);
        // Failed/full attempts also wait: no tight retries or backlog on unload.
        entity.timers(entity.timers().used(key,cadence));
        if(stock(entity.supplies(),positive)>=MercenaryPotionBalance.STOCK)return false;
        int[] counts=new int[positive?3:2];
        for(var stack:entity.supplies())if(effectSplash(stack)){
            int kind=kind(stack,positive);if(kind>=0)counts[kind]+=Math.min(stack.getCount(),6);
        }
        int chosen=0;for(int i=1;i<counts.length;i++)if(counts[i]<counts[chosen])chosen=i;
        boolean strong=entity.getRandom().nextDouble()<MercenaryPotionBalance.qualityChance(level);
        return MercenaryInventory.insert(entity.supplies(),create(positive,chosen,strong));
    }
}
