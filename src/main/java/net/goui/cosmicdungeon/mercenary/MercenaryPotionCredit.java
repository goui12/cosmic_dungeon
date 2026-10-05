package net.goui.cosmicdungeon.mercenary;

import java.util.UUID;
import java.util.function.BooleanSupplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

/** Transient attribution on native effect instances; no global cache or saved-data schema. */
public interface MercenaryPotionCredit {
    Dose cosmicdungeon$dose();
    void cosmicdungeon$dose(Dose dose);

    record Dose(UUID owner,long run,CompoundTag source) {
        MercenaryEntity owner(ServerLevel level){
            return level.getEntity(owner) instanceof MercenaryEntity merc&&merc.runId()==run?merc:null;
        }
        boolean once(BooleanSupplier award){
            return MercenarySkillEffects.credit(source,MercenarySkill.NEGATIVE_POTIONS,award);
        }
        public boolean tick(ServerLevel level,LivingEntity target,BooleanSupplier apply){
            var merc=owner(level);
            // A former enemy can become a friendly companion before the next poison tick.
            if(net.goui.cosmicdungeon.playerclass.bogatyr.CompanionAllies.friendly(target)
                    ||merc!=null&&!MercenaryBrain.enemy(merc,target))return true;
            float health=target.getHealth(),absorption=target.getAbsorptionAmount();
            boolean result=apply.getAsBoolean();
            if(merc!=null&&target.getHealth()+target.getAbsorptionAmount()<health+absorption)
                once(()->MercenarySkills.success(merc,MercenarySkill.NEGATIVE_POTIONS));
            return result;
        }
    }
}
