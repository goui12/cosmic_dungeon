package net.goui.cosmicdungeon.mercenary;

import java.util.UUID;
import net.goui.cosmicdungeon.entity.ModEntities;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;

/** Exercises native health and retained-entity death/resume without spawning a replacement. */
public final class MercenaryRecoveryGameTests {
    private MercenaryRecoveryGameTests(){}
    public static void recovery(GameTestHelper helper){
        var entity=new MercenaryEntity(ModEntities.MERCENARY.get(),helper.getLevel());
        var contract=new MercenaryContract(UUID.randomUUID(),UUID.randomUUID(),"theurgist",2,50);
        entity.initialize(1,contract);entity.setHealth(10);
        entity.supplies().set(0,new ItemStack(Items.DIAMOND,3));
        for(int i=0;i<30;i++)MercenaryRegeneration.tick(entity,10,false);
        helper.assertTrue(entity.getHealth()==10,Component.literal("No regeneration during the first 15 quiet seconds"));
        MercenaryRegeneration.tick(entity,20,false);
        helper.assertTrue(entity.getHealth()==10.5F,Component.literal("Heal exactly half a health point per second"));
        entity.hurtServer(helper.getLevel(),entity.damageSources().generic(),1);
        float health=entity.getHealth();
        for(int i=0;i<30;i++)MercenaryRegeneration.tick(entity,10,false);
        helper.assertTrue(entity.getHealth()==health,Component.literal("Native damage resets the recovery delay"));
        entity.die(entity.damageSources().generic());
        MercenaryRegeneration.tick(entity,20,false);
        helper.assertTrue(entity.dormant()&&entity.getHealth()==1,Component.literal("Resting entities cannot regenerate"));
        entity.resumeAfterRest();
        helper.assertTrue(!entity.dormant()&&entity.getHealth()==entity.getMaxHealth()
                &&entity.getUUID().equals(contract.id())&&entity.supplies().getFirst().getCount()==3,
                Component.literal("Revival restores the original entity and inventory"));
        entity.setHealth(entity.getMaxHealth()-.25F);
        for(int i=0;i<16;i++)MercenaryRegeneration.tick(entity,20,false);
        helper.assertTrue(entity.getHealth()==entity.getMaxHealth(),Component.literal("Regeneration cannot exceed maximum health"));
        helper.succeed();
    }
}
