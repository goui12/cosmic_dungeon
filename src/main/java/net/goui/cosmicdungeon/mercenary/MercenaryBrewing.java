package net.goui.cosmicdungeon.mercenary;

import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.level.block.entity.*;

/** The stand is a workstation; only the mercenary's own supplies enter the recipe transaction. */
public final class MercenaryBrewing {
    private MercenaryBrewing(){}
    public static boolean enabled(MercenaryContract contract){
        return contract!=null&&"theurgist".equals(contract.classId());
    }
    static boolean brew(MercenaryEntity entity,BaseContainerBlockEntity stand,PotionBrewing recipes){
        if(!enabled(entity.contract())||!(stand instanceof BrewingStandBlockEntity)
                ||!MercenaryCollection.mayOpen(stand,entity.contract()))return false;
        var result=MercenaryInventory.brew(entity.supplies(),recipes);
        if(result==null)return false;
        MercenaryInventory.commit(entity.supplies(),result);
        return true;
    }
}
