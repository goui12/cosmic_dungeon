package net.goui.cosmicdungeon.block.entity;

import net.goui.cosmicdungeon.block.ModBlocks;
import net.goui.cosmicdungeon.spawner.editor.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SpawnerEditorPreservationTest {
    private static RegistryAccess lookup(){return RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);}
    private static CosmicSpawnerBlockEntity be(){return new CosmicSpawnerBlockEntity(new BlockPos(12,60,-9),ModBlocks.COSMIC_MOB_SPAWNER.get().defaultBlockState());}
    private static void load(CosmicSpawnerBlockEntity be,CompoundTag tag){
        be.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,lookup(),tag));
    }
    private static SpawnerEditorDraft cap(SpawnerEditorDraft d,int cap){
        return new SpawnerEditorDraft(d.entityId(),d.name(),d.captain(),d.boss(),cap,d.minDelay(),d.maxDelay(),
                d.silent(),d.noAi(),d.invulnerable(),d.persistent(),d.nameVisible());
    }
    @Test void unknownRootPresetAndRuleFieldsSurviveRealLoadSave(){
        var b=be();var p=new CosmicSpawnerPreset();p.setEntityTypeId(ResourceLocation.withDefaultNamespace("zombie"));
        p.addConfiguredIntrinsicDropRule(ResourceLocation.withDefaultNamespace("diamond"),0.35F,2,CosmicSpawnerPreset.IntrinsicDropRule.Kind.CUSTOM_ADDITION);
        b.setSpawnerPreset(p);var tag=b.saveWithoutMetadata(lookup());tag.putString("FutureAuthor","untouched");
        var preset=tag.getCompoundOrEmpty("SpawnerPreset");preset.putString("FuturePreset","retained");
        var rules=preset.getCompoundOrEmpty("intrinsicDropRules");
        for(String key:rules.keySet())rules.getCompoundOrEmpty(key).putLong("FutureRule",912L);
        load(b,tag);var round=b.saveWithoutMetadata(lookup());
        assertEquals("untouched",round.getStringOr("FutureAuthor",""));
        assertEquals("retained",round.getCompoundOrEmpty("SpawnerPreset").getStringOr("FuturePreset",""));
        for(String key:rules.keySet())assertEquals(912L,round.getCompoundOrEmpty("SpawnerPreset")
                .getCompoundOrEmpty("intrinsicDropRules").getCompoundOrEmpty(key).getLongOr("FutureRule",0));
    }
    @Test void capEditPreservesStyledNameWeightedDataNativeLimitsAndBossProgress(){
        var b=be();var p=new CosmicSpawnerPreset();p.setEntityTypeId(ResourceLocation.withDefaultNamespace("zombie"));
        p.setCustomName(Component.literal("Authored").withStyle(net.minecraft.ChatFormatting.GOLD));b.setSpawnerPreset(p);
        var tag=b.saveWithoutMetadata(lookup());tag.putBoolean("BossOneShot",true);tag.putBoolean("BossHasSpawned",true);
        tag.putInt("MaxNearbyEntities",11);tag.putInt("SpawnCount",7);tag.putInt("RequiredPlayerRange",19);
        var spawn=new CompoundTag();var entity=new CompoundTag();entity.putString("id","minecraft:zombie");
        entity.putString("CustomMarker","authored");spawn.put("entity",entity);
        var potentials=new ListTag();var choice=new CompoundTag();choice.putInt("weight",4);choice.put("data",spawn);
        potentials.add(choice);tag.put("SpawnData",spawn.copy());tag.put("SpawnPotentials",potentials);
        load(b,tag);tag=b.saveWithoutMetadata(lookup());var before=SpawnerEditorConfig.read(b,tag);
        var patched=SpawnerEditorConfig.patch(tag,before,cap(before,8));load(b,patched);var round=b.saveWithoutMetadata(lookup());
        assertEquals(tag.get("SpawnerPreset"),round.get("SpawnerPreset"));
        assertEquals(tag.get("SpawnPotentials"),round.get("SpawnPotentials"));
        assertEquals(tag.get("SpawnData"),round.get("SpawnData"));
        assertTrue(round.getBooleanOr("BossHasSpawned",false));assertEquals(11,b.getSpawnerMaxNearbyEntities());
        assertEquals(7,b.getSpawnerSpawnCount());assertEquals(19,b.getSpawnerRequiredPlayerRange());assertEquals(8,b.getSpawnerMobCap());
    }
    @Test void noOpPatchIsDetachedAndByteEquivalent(){
        var d=SpawnerEditorDraft.empty();var source=new CompoundTag();source.putString("unknown","keep");
        var result=SpawnerEditorConfig.patch(source,d,d);assertEquals(source,result);assertNotSame(source,result);
    }
    @Test void conflictFingerprintIgnoresOnlyRuntimeDelay(){
        var a=new CompoundTag();a.putInt("Delay",1);a.putInt("MinSpawnDelay",40);
        var b=a.copy();b.putInt("Delay",99);assertEquals(SpawnerEditorConfig.comparable(a),SpawnerEditorConfig.comparable(b));
        b.putInt("MinSpawnDelay",50);assertNotEquals(SpawnerEditorConfig.comparable(a),SpawnerEditorConfig.comparable(b));
    }
    @Test void publicUpdateDoesNotDisclosePrivateConfiguration(){
        var b=be();b.setSpawnerPreset(new CosmicSpawnerPreset());b.setSpawnerMobCap(9);b.setBossOneShot(true);
        var publicTag=b.getUpdateTag(lookup());
        assertEquals(java.util.Set.of("CosmicSpawnerDataVersion","SpawnerEntityId","CosmicSpawnBlocked"),publicTag.keySet());
        assertFalse(publicTag.contains("SpawnerPreset"));assertFalse(publicTag.contains("SpawnPotentials"));
    }
    @Test void futureStorageVersionsAreNeverDowngraded(){
        var b=be();var tag=b.saveWithoutMetadata(lookup());tag.putInt(CosmicSpawnerBlockEntity.DATA_VERSION_KEY,999);
        var p=new CompoundTag();p.putInt("presetVersion",999);p.putString("entityType","minecraft:zombie");tag.put("SpawnerPreset",p);
        load(b,tag);var round=b.saveWithoutMetadata(lookup());
        assertEquals(999,round.getIntOr(CosmicSpawnerBlockEntity.DATA_VERSION_KEY,0));
        assertEquals(999,round.getCompoundOrEmpty("SpawnerPreset").getIntOr("presetVersion",0));
    }
    @Test void editorOnlyPresetDoesNotClaimAuthoredEquipmentSlots(){
        var before=SpawnerEditorDraft.empty();
        var after=new SpawnerEditorDraft(before.entityId(),"name",false,false,0,200,800,true,false,false,false,false);
        var patched=SpawnerEditorConfig.patch(new CompoundTag(),before,after);
        assertEquals(0,patched.getCompoundOrEmpty("SpawnerPreset").getIntOr("equipmentMask",-1));
        assertFalse(patched.getCompoundOrEmpty("SpawnerPreset").getBooleanOr("intrinsicDropsOverride",true));
        var p=CosmicSpawnerPreset.load(TagValueInput.create(ProblemReporter.DISCARDING,lookup(),patched.getCompoundOrEmpty("SpawnerPreset")));
        p.setEquipment(CosmicSpawnerPreset.Slot.MAINHAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STICK));
        var out=net.minecraft.world.level.storage.TagValueOutput.createWithContext(ProblemReporter.DISCARDING,lookup());p.save(out);
        assertEquals(1<<CosmicSpawnerPreset.Slot.MAINHAND.ordinal(),out.buildResult().getIntOr("equipmentMask",0));
    }
    @Test void explicitClearWinsOverUnchangedWeightedAuthoredName(){
        var b=be();var tag=b.saveWithoutMetadata(lookup());
        var spawn=new CompoundTag();var entity=new CompoundTag();entity.putString("id","minecraft:zombie");
        entity.store("CustomName",ComponentSerialization.CODEC,Component.literal("Weighted name"));spawn.put("entity",entity);tag.put("SpawnData",spawn);
        tag.putString("SpawnerEntityId","minecraft:zombie");var preset=new CompoundTag();preset.putString("entityType","minecraft:zombie");
        preset.putBoolean("nameOverride",true);tag.put("SpawnerPreset",preset);load(b,tag);
        assertEquals("",SpawnerEditorConfig.read(b,b.saveWithoutMetadata(lookup())).name());
    }
    @Test void failedLootCommandDoesNotClaimAuthoredDrops(){
        var tag=new CompoundTag();tag.putBoolean("intrinsicDropsOverride",false);
        var p=CosmicSpawnerPreset.load(TagValueInput.create(ProblemReporter.DISCARDING,lookup(),tag));
        assertNull(p.addConfiguredIntrinsicDropRule(null,1,1,CosmicSpawnerPreset.IntrinsicDropRule.Kind.CUSTOM_ADDITION));
        assertFalse(p.removeConfiguredIntrinsicDropRule("missing"));assertFalse(p.adjustIntrinsicDropRuleChance("missing",1));
        assertFalse(p.adjustIntrinsicDropRuleCount("missing",1));
        var out=net.minecraft.world.level.storage.TagValueOutput.createWithContext(ProblemReporter.DISCARDING,lookup());p.save(out);
        assertFalse(out.buildResult().getBooleanOr("intrinsicDropsOverride",true));
    }
    @Test void invalidDraftsCannotPatch(){
        var d=SpawnerEditorDraft.empty();assertThrows(IllegalArgumentException.class,()->SpawnerEditorConfig.patch(new CompoundTag(),d,cap(d,-1)));
        assertFalse(new SpawnerEditorDraft("minecraft:zombie","",true,false,0,200,800,false,false,false,false,false).error().isEmpty());
        assertFalse(new SpawnerEditorDraft("minecraft:zombie","",false,false,0,801,800,false,false,false,false,false).error().isEmpty());
    }
}
