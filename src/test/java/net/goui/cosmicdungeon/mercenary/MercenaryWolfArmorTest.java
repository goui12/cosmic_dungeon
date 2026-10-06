package net.goui.cosmicdungeon.mercenary;

import java.util.*;
import net.goui.cosmicdungeon.dungeon.DungeonRunRegistryData;
import net.goui.cosmicdungeon.playerclass.bogatyr.BogatyrWolfEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class MercenaryWolfArmorTest {
    private final UUID hirer=UUID.randomUUID(),peer=UUID.randomUUID(),merc=UUID.randomUUID();
    private final String dimension="cosmicdungeon:d1_instance_42";
    private MercenaryWolves.Bond bond(){return new MercenaryWolves.Bond(42,merc,hirer);}
    private DungeonRunRegistryData.RunRecord run(){
        return new DungeonRunRegistryData.RunRecord(42,"dungeon_1","minecraft:overworld",0,
                List.of(dimension),1,"ACTIVE","",0,List.of(hirer,peer),List.of(),List.of())
                .withMercenaries(List.of(new MercenaryContract(merc,hirer,"bogatyr",3,500)));
    }
    @Test void activeGroupMemberCanContributeArmorWithoutTakingOwnership(){
        assertTrue(MercenaryWolfArmor.allowed(run(),bond(),hirer,hirer,dimension));
        assertTrue(MercenaryWolfArmor.allowed(run(),bond(),hirer,peer,dimension));
        assertFalse(MercenaryWolfArmor.allowed(run(),bond(),peer,peer,dimension));
    }
    @Test void outsiderExitedMemberWrongInstanceAndBrokenBondAreRejected(){
        assertFalse(MercenaryWolfArmor.allowed(run(),bond(),hirer,UUID.randomUUID(),dimension));
        assertFalse(MercenaryWolfArmor.allowed(run().withCompletionExited(peer),bond(),hirer,peer,dimension));
        assertFalse(MercenaryWolfArmor.allowed(run().withCompletionExited(hirer),bond(),hirer,peer,dimension));
        assertFalse(MercenaryWolfArmor.allowed(run(),bond(),hirer,peer,"minecraft:overworld"));
        assertFalse(MercenaryWolfArmor.allowed(run(),null,hirer,peer,dimension));
        assertFalse(MercenaryWolfArmor.allowed(null,bond(),hirer,peer,dimension));
    }
    @Test void armorEligibilityPreservesExistingEquipmentAndRejectsWrongItems(){
        var wolf=new MercenaryTestWolf();wolf.setTame(true,false);
        var armor=new ItemStack(Items.WOLF_ARMOR);
        armor.set(DataComponents.CUSTOM_NAME,Component.literal("Existing dyed armor"));
        assertTrue(MercenaryWolfArmor.canEquip(wolf,armor));
        wolf.ageForTest=-1;assertFalse(MercenaryWolfArmor.canEquip(wolf,armor));wolf.ageForTest=0;
        assertFalse(MercenaryWolfArmor.canEquip(wolf,new ItemStack(Items.IRON_CHESTPLATE)));
        wolf.setItemSlot(EquipmentSlot.BODY,armor.copy(),true);
        assertFalse(MercenaryWolfArmor.canEquip(wolf,new ItemStack(Items.WOLF_ARMOR)));
        assertTrue(ItemStack.isSameItemSameComponents(armor,wolf.getBodyArmorItem()));
    }
    @Test void mercenaryMarkersCannotEnterPlayerOwnershipEvenWithLegacyPlayerTags(){
        var wolf=new MercenaryTestWolf();wolf.setTame(true,false);
        wolf.getPersistentData().putLong("cosmicdungeon.bogatyr_run",42);
        wolf.getPersistentData().putString("cosmicdungeon.bogatyr_owner",hirer.toString());
        assertTrue(BogatyrWolfEvents.managed(wolf));assertTrue(BogatyrWolfEvents.owned(wolf));
        MercenaryWolves.mark(wolf,bond());
        assertFalse(BogatyrWolfEvents.managed(wolf));assertFalse(BogatyrWolfEvents.owned(wolf));
        assertFalse(BogatyrWolfEvents.breedingAllowed(wolf,new MercenaryTestWolf()));
        wolf.getPersistentData().putString(MercenaryWolves.MARKER,"malformed");
        assertFalse(BogatyrWolfEvents.managed(wolf));assertFalse(BogatyrWolfEvents.owned(wolf));
    }
}
