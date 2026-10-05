package net.goui.cosmicdungeon.mercenary;

import java.util.*;
import net.goui.cosmicdungeon.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;

public final class MercenaryPotionGameTests {
    private MercenaryPotionGameTests(){}
    public static void freeStock(GameTestHelper helper){
        var level=helper.getLevel();var merc=new MercenaryEntity(ModEntities.MERCENARY.get(),level);
        merc.initialize(Long.MAX_VALUE-105,new MercenaryContract(UUID.randomUUID(),UUID.randomUUID(),"theurgist",2,50));
        var stand=new BrewingStandBlockEntity(BlockPos.ZERO,Blocks.BREWING_STAND.defaultBlockState());
        stand.setItem(3,new ItemStack(Items.NETHER_WART,7));
        helper.assertTrue(MercenaryBrewing.brew(merc,stand,level.potionBrewing()),Component.literal("Empty mercenary brews instantly without supplies"));
        helper.assertTrue(stand.getItem(3).getCount()==7&&MercenaryBrewing.stock(merc.supplies(),true)==1
                &&MercenaryBrewing.stock(merc.supplies(),false)==1,Component.literal("Stand materials untouched; positive and negative splash produced"));
        helper.assertTrue(!MercenaryBrewing.brew(merc,stand,level.potionBrewing()),Component.literal("Repeated workstation pass obeys cadence"));
        helper.succeed();
    }
    public static void attribution(GameTestHelper helper){
        var level=helper.getLevel();var cow=EntityType.COW.create(level,EntitySpawnReason.COMMAND);
        helper.assertTrue(cow!=null,Component.literal("Native poison target"));
        var dose=new MercenaryPotionCredit.Dose(UUID.randomUUID(),5,new CompoundTag());
        var weak=new MobEffectInstance(MobEffects.POISON,100,0);
        ((MercenaryPotionCredit)(Object)weak).cosmicdungeon$dose(dose);
        var copy=new MobEffectInstance(weak);
        helper.assertTrue(((MercenaryPotionCredit)(Object)copy).cosmicdungeon$dose()==dose,Component.literal("Native effect copy keeps dose"));
        cow.setHealth(10);
        copy.tickServer(level,cow,()->{});
        helper.assertTrue(cow.getHealth()<10,Component.literal("Attributed native poison still causes actual damage"));
        // Strong player poison must clear active attribution, while the hidden weaker effect retains its origin.
        copy.update(new MobEffectInstance(MobEffects.POISON,1,1));
        helper.assertTrue(((MercenaryPotionCredit)(Object)copy).cosmicdungeon$dose()==null,Component.literal("Player replacement never credits old mercenary"));
        copy.tickServer(level,cow,()->{});
        helper.assertTrue(((MercenaryPotionCredit)(Object)copy).cosmicdungeon$dose()==dose,Component.literal("Hidden native effect restores its own dose"));
        var stronger=new MobEffectInstance(MobEffects.POISON,500,2);copy.update(stronger);
        helper.assertTrue(((MercenaryPotionCredit)(Object)copy).cosmicdungeon$dose()==null,Component.literal("Stronger external effect clears attribution"));
        helper.succeed();
    }
}
