package net.goui.cosmicdungeon.combat;

import com.electronwill.nightconfig.toml.TomlFormat;
import net.goui.cosmicdungeon.Config;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Zombie;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

final class NaturalArmorDropsTest {
    @BeforeAll static void config()throws Exception{
        var defaults=TomlFormat.newConfig();Config.SPEC.correct(defaults);
        var ctor=Class.forName("net.neoforged.fml.config.LoadedConfig").getDeclaredConstructor(
                com.electronwill.nightconfig.core.CommentedConfig.class,java.nio.file.Path.class,net.neoforged.fml.config.ModConfig.class);
        ctor.setAccessible(true);
        Config.SPEC.acceptConfig((net.neoforged.fml.config.IConfigSpec.ILoadedConfig)ctor.newInstance(defaults,null,null));
    }
    @AfterAll static void unload(){Config.SPEC.acceptConfig(null);}
    @Test void generatedArmorDropSettingSurvivesNativeDropChanceCodec(){
        var mob=new Zombie(EntityType.ZOMBIE,null);
        for(var slot:EquipmentSlot.VALUES)NaturalArmorDrops.generated(mob,slot);
        var saved=DropChances.CODEC.encodeStart(NbtOps.INSTANCE,mob.getDropChances()).getOrThrow();
        var restored=DropChances.CODEC.parse(NbtOps.INSTANCE,saved).getOrThrow();
        for(var slot:EquipmentSlot.VALUES)
            assertEquals(slot.getType()==EquipmentSlot.Type.HUMANOID_ARMOR?0F:0.085F,restored.byEquipment(slot));
    }
    @Test void explicitDropRatesAndLaterGuaranteedEquipmentRemainIndependent(){
        var mob=new Zombie(EntityType.ZOMBIE,null);
        mob.setDropChance(EquipmentSlot.HEAD,0.35F);
        mob.setGuaranteedDrop(EquipmentSlot.CHEST);
        NaturalArmorDrops.generated(mob,EquipmentSlot.HEAD);
        NaturalArmorDrops.generated(mob,EquipmentSlot.CHEST);
        assertEquals(0.35F,mob.getDropChances().byEquipment(EquipmentSlot.HEAD));
        assertTrue(mob.getDropChances().isPreserved(EquipmentSlot.CHEST));
        NaturalArmorDrops.generated(mob,EquipmentSlot.FEET);
        mob.setGuaranteedDrop(EquipmentSlot.FEET);
        assertTrue(mob.getDropChances().isPreserved(EquipmentSlot.FEET));
    }
}
