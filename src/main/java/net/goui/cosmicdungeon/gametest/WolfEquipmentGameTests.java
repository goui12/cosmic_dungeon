package net.goui.cosmicdungeon.gametest;

import net.goui.cosmicdungeon.Config;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.*;
import net.minecraft.world.level.levelgen.LegacyRandomSource;

/** Verifies the actual transformed vanilla equipment-generation path in CI's server. */
public final class WolfEquipmentGameTests {
    private WolfEquipmentGameTests() {}
    private static final class Probe extends Zombie {
        Probe(ServerLevel level){super(EntityType.ZOMBIE,level);}
        void generate(RandomSource random){
            super.populateDefaultEquipmentSlots(random,new DifficultyInstance(Difficulty.HARD,72000,3600000,1));
        }
    }
    public static void naturalArmor(GameTestHelper helper){
        var mob=new Probe(helper.getLevel());
        var authored=new ItemStack(Items.DIAMOND_CHESTPLATE);
        mob.setItemSlot(EquipmentSlot.CHEST,authored.copy());
        mob.setDropChance(EquipmentSlot.CHEST,0.35F);
        var random=new LegacyRandomSource(0){
            @Override public float nextFloat(){return 0;}
            @Override public int nextInt(int bound){return 0;}
        };
        mob.generate(random);
        for(var slot:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.LEGS,EquipmentSlot.FEET}){
            helper.assertTrue(!mob.getItemBySlot(slot).isEmpty(),Component.literal("Generated armor must remain equipped"));
            helper.assertTrue(mob.getDropChances().byEquipment(slot)==Config.NATURAL_ARMOR_DROP_CHANCE.get().floatValue(),
                    Component.literal("Generated armor must use the independent drop setting"));
        }
        helper.assertTrue(ItemStack.isSameItemSameComponents(authored,mob.getItemBySlot(EquipmentSlot.CHEST))
                &&mob.getDropChances().byEquipment(EquipmentSlot.CHEST)==0.35F,
                Component.literal("Existing authored armor and drop chance must survive generation"));
        mob.setGuaranteedDrop(EquipmentSlot.FEET);
        helper.assertTrue(mob.getDropChances().isPreserved(EquipmentSlot.FEET),
                Component.literal("Later picked-up/player-given armor must retain its guaranteed drop"));
        helper.succeed();
    }
}
