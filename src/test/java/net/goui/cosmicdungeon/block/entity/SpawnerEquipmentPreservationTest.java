package net.goui.cosmicdungeon.block.entity;

import java.util.*;
import io.netty.buffer.Unpooled;
import net.goui.cosmicdungeon.block.entity.CosmicSpawnerPreset.Slot;
import net.goui.cosmicdungeon.network.SpawnerEditorPayloads.*;
import net.goui.cosmicdungeon.spawner.editor.*;
import net.goui.cosmicdungeon.spawner.editor.SpawnerEquipmentDraft.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemLore;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SpawnerEquipmentPreservationTest {
    private static RegistryAccess lookup(){return RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);}
    @Test void recipeCodecKeepsAllSlotsAndZeroRemovalWithoutClientComponents(){
        var draft=SpawnerEquipmentDraft.unchanged().with(Slot.HEAD,new Edit(Origin.ITEM,"minecraft:diamond_helmet",true,
                List.of(new Enchant("minecraft:protection",255),new Enchant("minecraft:thorns",0))))
                .with(Slot.MAINHAND,new Edit(Origin.HELD,"",false,List.of()));
        var value=new Request(UUID.randomUUID(),Kind.APPLY,SpawnerEditorDraft.empty(),draft);var raw=Unpooled.buffer();
        try{var buf=new RegistryFriendlyByteBuf(raw,RegistryAccess.EMPTY);Request.STREAM_CODEC.encode(buf,value);
            assertEquals(value,Request.STREAM_CODEC.decode(buf));assertEquals(0,buf.readableBytes());}finally{raw.release();}
    }
    @Test void fixedShapeAndNestedBoundsRejectUnboundedRequests(){
        assertThrows(IllegalArgumentException.class,()->new SpawnerEquipmentDraft(List.of()));
        assertThrows(IllegalArgumentException.class,()->new Edit(Origin.ITEM,"x".repeat(257),false,List.of()));
        assertThrows(IllegalArgumentException.class,()->new Edit(Origin.KEEP,"",false,Collections.nCopies(65,new Enchant("x",1))));
        var mutable=new ArrayList<>(Collections.nCopies(6,Edit.keep()));var draft=new SpawnerEquipmentDraft(mutable);
        mutable.clear();assertEquals(6,draft.slots().size());assertFalse(draft.dirty());
    }
    @Test void equipmentOverlayPreservesWeightedNativeDataDropsUnknownAndComponents(){
        var source=new CompoundTag();source.putString("FutureRoot","retained");
        var nativeSpawn=new CompoundTag();nativeSpawn.putString("unknown","native");source.put("SpawnData",nativeSpawn);
        var weights=new ListTag();var weighted=new CompoundTag();weighted.putInt("weight",8);weights.add(weighted);source.put("SpawnPotentials",weights);
        var preset=new CompoundTag();preset.putInt("presetVersion",5);preset.putInt("equipmentMask",0);preset.putInt("dropMask",32);
        preset.putFloat("drop_feet",0.37F);preset.putString("UnknownPreset","retain");source.put("SpawnerPreset",preset);
        var stack=new ItemStack(Items.DIAMOND_SWORD,2);stack.setDamageValue(17);
        stack.set(DataComponents.CUSTOM_NAME,Component.literal("Renamed blade"));stack.set(DataComponents.LORE,new ItemLore(List.of(Component.literal("Lore"))));
        var draft=SpawnerEquipmentDraft.unchanged().with(Slot.MAINHAND,new Edit(Origin.HELD,"",false,List.of()));
        var slots=new ArrayList<>(Collections.nCopies(6,ItemStack.EMPTY));slots.set(0,stack);
        var before=source.copy();var patched=SpawnerEquipmentConfig.patch(source,"minecraft:zombie",draft,slots,lookup());
        assertEquals(before,source);assertEquals(source.get("SpawnData"),patched.get("SpawnData"));
        assertEquals(source.get("SpawnPotentials"),patched.get("SpawnPotentials"));assertEquals("retained",patched.getStringOr("FutureRoot",""));
        var out=patched.getCompoundOrEmpty("SpawnerPreset");
        assertEquals(1,out.getIntOr("equipmentMask",0));assertEquals(32,out.getIntOr("dropMask",0));assertEquals(0.37F,out.getFloatOr("drop_feet",0));
        assertEquals("retain",out.getStringOr("UnknownPreset",""));
        assertTrue(ItemStack.matches(stack,SpawnerEquipmentConfig.snapshot(patched,lookup()).get(0)));
    }
    @Test void legacyPresetOwnsAllSlotsWhileNativeSnapshotUsesFullComponents(){
        var stack=new ItemStack(Items.GOLDEN_BOOTS);stack.setDamageValue(7);
        var equipment=new net.minecraft.world.entity.EntityEquipment();equipment.set(Slot.FEET.equipmentSlot,stack.copy());
        var entity=new CompoundTag();entity.put("equipment",net.minecraft.world.entity.EntityEquipment.CODEC
                .encodeStart(lookup().createSerializationContext(NbtOps.INSTANCE),equipment).getOrThrow());
        var spawn=new CompoundTag();spawn.put("entity",entity);var source=new CompoundTag();source.put("SpawnData",spawn);
        assertTrue(ItemStack.matches(stack,SpawnerEquipmentConfig.snapshot(source,lookup()).get(Slot.FEET.ordinal())));
        source.put("SpawnerPreset",new CompoundTag());assertTrue(SpawnerEquipmentConfig.snapshot(source,lookup()).get(Slot.FEET.ordinal()).isEmpty());
        source.getCompoundOrEmpty("SpawnerPreset").putInt("equipmentMask",0);
        assertTrue(ItemStack.matches(stack,SpawnerEquipmentConfig.snapshot(source,lookup()).get(Slot.FEET.ordinal())));
    }
    @Test void newOverlayClaimsOnlyEditedEquipmentNeverLootOrDropChances(){
        var source=new CompoundTag();var slots=Collections.nCopies(6,ItemStack.EMPTY);
        var draft=SpawnerEquipmentDraft.unchanged().with(Slot.HEAD,new Edit(Origin.CLEAR,"",false,List.of()));
        var out=SpawnerEquipmentConfig.patch(source,"minecraft:zombie",draft,slots,lookup()).getCompoundOrEmpty("SpawnerPreset");
        assertEquals(4,out.getIntOr("equipmentMask",0));assertEquals(0,out.getIntOr("dropMask",-1));
        assertFalse(out.getBooleanOr("intrinsicDropsOverride",true));assertFalse(out.contains("eq_head"));
        assertTrue(source.isEmpty());
    }
    @Test void viewSnapshotsCannotBeMutatedByCallers(){
        var original=new ItemStack(Items.DIAMOND_SWORD);var list=new ArrayList<>(Collections.nCopies(6,ItemStack.EMPTY));list.set(0,original);
        var view=new View(UUID.randomUUID(),BlockPos.ZERO,SpawnerEditorDraft.empty(),Status.OPEN,"",list,original);
        original.setDamageValue(44);view.held().setDamageValue(3);view.equipment().get(0).setDamageValue(9);
        assertEquals(0,view.held().getDamageValue());assertEquals(0,view.equipment().get(0).getDamageValue());
    }
}
