package net.minecraft.world.entity.ai.behavior;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryAccessor;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;

public class ResetProfession {
    public static BehaviorControl<Villager> create() {
        return BehaviorBuilder.create(
            p_259684_ -> p_259684_.group(p_259684_.absent(MemoryModuleType.JOB_SITE)).apply(p_259684_, p_260035_ -> (p_396747_, p_396748_, p_396749_) -> {
                VillagerData villagerdata = p_396748_.getVillagerData();
                boolean flag = !villagerdata.profession().is(VillagerProfession.NONE) && !villagerdata.profession().is(VillagerProfession.NITWIT);
                if (flag && p_396748_.getVillagerXp() == 0 && villagerdata.level() <= 1) {
                    p_396748_.setVillagerData(p_396748_.getVillagerData().withProfession(p_396747_.registryAccess(), VillagerProfession.NONE));
                    p_396748_.refreshBrain(p_396747_);
                    return true;
                } else {
                    return false;
                }
            })
        );
    }
}
