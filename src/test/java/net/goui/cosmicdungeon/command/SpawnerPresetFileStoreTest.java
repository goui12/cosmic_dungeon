package net.goui.cosmicdungeon.command;
import com.google.gson.*;
import java.nio.file.*;
import net.minecraft.nbt.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class SpawnerPresetFileStoreTest {
    @TempDir Path directory;
    private static CompoundTag config(){
        var t=new CompoundTag();t.putString("SpawnerEntityId","minecraft:zombie");t.putInt("MinSpawnDelay",80);t.putInt("MaxSpawnDelay",140);
        t.putInt("SpawnerMobCap",6);t.putInt("SpawnCount",7);t.putInt("MaxNearbyEntities",11);t.putInt("RequiredPlayerRange",22);return t;
    }
    @Test void completeSnbtKeepsNativeWeightedUnknownAndExactNbtTypes(){
        var t=config();t.putByte("FutureByte",(byte)7);t.putLongArray("FutureLongs",new long[]{1L,Long.MAX_VALUE});t.putIntArray("FutureInts",new int[]{-8,9});
        var entity=new CompoundTag();entity.putString("id","minecraft:zombie");entity.putString("FutureEntity","retain");
        var spawn=new CompoundTag();spawn.put("entity",entity);t.put("SpawnData",spawn);
        var choices=new ListTag();var row=new CompoundTag();row.putInt("weight",4);row.put("data",spawn.copy());choices.add(row);t.put("SpawnPotentials",choices);
        var decoded=SpawnerPresetFileStore.decode(SpawnerPresetFileStore.encode(t,new JsonObject()));
        assertTrue(decoded.complete());assertEquals(t,decoded.configuration());
        var target=config();target.putString("TargetUnknown","keep");target.putInt("SpawnCount",1);
        var applied=decoded.applyTo(target);assertEquals(t.get("SpawnPotentials"),applied.get("SpawnPotentials"));
        assertEquals(t.get("FutureByte"),applied.get("FutureByte"));assertEquals(7,applied.getIntOr("SpawnCount",0));assertEquals("keep",applied.getStringOr("TargetUnknown",""));
    }
    @Test void importKeepsIdentityAndResetsOnlyRuntimeProgress(){
        var t=config();t.putString("id","wrong:block");t.putInt("x",999);t.putInt("Delay",1);t.putBoolean("BossHasSpawned",true);
        var target=config();target.putInt("x",12);target.putString("id","cosmicdungeon:cosmic_mob_spawner");
        var result=SpawnerPresetFileStore.decode(SpawnerPresetFileStore.encode(t,new JsonObject())).applyTo(target);
        assertEquals(12,result.getIntOr("x",0));assertEquals("cosmicdungeon:cosmic_mob_spawner",result.getStringOr("id",""));
        assertFalse(result.getBooleanOr("BossHasSpawned",true));assertEquals(80,result.getIntOr("Delay",0));
    }
    @Test void oldPartialPresetsRetainNativeSettingsAndUpdateCurrentEntity(){
        var legacy=JsonParser.parseString("""
{"entityTypeId":"minecraft:skeleton","minSpawnDelay":40,"maxSpawnDelay":90,"futureMetadata":{"number":9}}
""").getAsJsonObject();
        var authored=new CompoundTag();authored.putInt("presetVersion",5);authored.putString("entityType","minecraft:zombie");
        authored.putString("customName","Authored guard");authored.putBoolean("nameOverride",true);
        authored.putInt("equipmentMask",1);authored.putInt("dropMask",1);authored.putFloat("drop_mainhand",0.375F);
        var weapon=new CompoundTag();weapon.putString("id","minecraft:iron_sword");weapon.putInt("count",1);
        var components=new CompoundTag();components.putInt("minecraft:damage",17);weapon.put("components",components);authored.put("eq_mainhand",weapon);
        var flags=new CompoundTag();flags.putBoolean("Silent",true);flags.putLong("futureFlag",42L);authored.put("entityFlags",flags);
        var rules=new CompoundTag();var rule=new CompoundTag();rule.putString("id","old");rule.putString("item","minecraft:bone");
        rule.putFloat("chance",0.25F);rule.putInt("count",3);rule.putString("kind","CUSTOM_ADDITION");rules.put("old",rule);authored.put("intrinsicDropRules",rules);
        authored.putLongArray("futurePreset",new long[]{7L,Long.MAX_VALUE});
        for(int version=0;version<=3;version++){
            var input=legacy.deepCopy();if(version>0)input.addProperty("formatVersion",version);
            var loaded=SpawnerPresetFileStore.decode(input);assertFalse(loaded.complete());
            var target=config();target.put("SpawnerPreset",authored.copy());var before=target.copy();var applied=loaded.applyTo(target);
            var expected=authored.copy();expected.putString("entityType","minecraft:skeleton");
            assertEquals(7,applied.getIntOr("SpawnCount",0));assertEquals(expected,applied.getCompoundOrEmpty("SpawnerPreset"),"Legacy format "+version+" preserves authored settings and synchronizes only imported entity identity");
            assertEquals(before,target,"Import must not mutate its target snapshot");
            assertEquals("minecraft:skeleton",applied.getCompoundOrEmpty("SpawnData").getCompoundOrEmpty("entity").getStringOr("id",""));
            var next=SpawnerPresetFileStore.encode(applied,new JsonObject());assertEquals(9,next.getAsJsonObject("futureMetadata").get("number").getAsInt());
        }
    }
    @Test void completePresetWithoutOptionalConfigurationStillReplacesThoseFields(){
        var complete=SpawnerPresetFileStore.decode(SpawnerPresetFileStore.encode(config(),new JsonObject()));
        var target=config();var authored=new CompoundTag();authored.putString("futurePreset","keep only in partial overlays");
        target.put("SpawnerPreset",authored);target.put("SpawnData",new CompoundTag());target.put("SpawnPotentials",new ListTag());
        var applied=complete.applyTo(target);
        assertFalse(applied.contains("SpawnerPreset"));assertFalse(applied.contains("SpawnData"));assertFalse(applied.contains("SpawnPotentials"));
        assertTrue(target.contains("SpawnerPreset"));
    }
    @Test void futureAndMalformedConfigurationsFailWithoutNormalizing(){
        assertThrows(IllegalArgumentException.class,()->SpawnerPresetFileStore.decode(JsonParser.parseString("""
{"formatVersion":99}
""").getAsJsonObject()));
        assertThrows(IllegalArgumentException.class,()->SpawnerPresetFileStore.decode(JsonParser.parseString("""
{"formatVersion":4}
""").getAsJsonObject()));
        var t=config();t.putInt("MinSpawnDelay",0);assertThrows(IllegalArgumentException.class,()->SpawnerPresetFileStore.validate(t));
        t.putInt("MinSpawnDelay",80);t.putInt("CosmicSpawnerDataVersion",999);assertThrows(IllegalArgumentException.class,()->SpawnerPresetFileStore.validate(t));
    }
    @Test void safeNamesExcludeTraversalAndWindowsDevices(){
        for(String name:new String[]{"../world","a/b","a\\b","hello.json","CON","lpt1","","x".repeat(65)})assertFalse(SpawnerPresetFileStore.validName(name),name);
        assertTrue(SpawnerPresetFileStore.validName("Skeleton_Master-2"));
    }
    @Test void fileWritesAreExplicitAndPreserveExistingUnknownMetadata()throws Exception{
        var path=directory.resolve("Test.json");var first=SpawnerPresetFileStore.encode(config(),new JsonObject());first.addProperty("futureFileNote","keep");Files.writeString(path,first.toString());
        var original=Files.readString(path);assertThrows(FileAlreadyExistsException.class,()->SpawnerPresetFileStore.writeFile(path,config(),false));assertEquals(original,Files.readString(path));
        var changed=config();changed.putInt("SpawnCount",12);SpawnerPresetFileStore.writeFile(path,changed,true);
        var saved=JsonParser.parseString(Files.readString(path)).getAsJsonObject();assertEquals("keep",saved.get("futureFileNote").getAsString());
        assertEquals(12,SpawnerPresetFileStore.decode(saved).configuration().getIntOr("SpawnCount",0));
        try(var files=Files.list(directory)){assertEquals(1,files.count());}
    }
    @Test void rejectedOverwriteKeepsOriginalFile()throws Exception{
        var path=directory.resolve("Future.json");Files.writeString(path,"""
{"formatVersion":999,"unknown":true}
""");var before=Files.readAllBytes(path);
        assertThrows(IllegalArgumentException.class,()->SpawnerPresetFileStore.writeFile(path,config(),true));assertArrayEquals(before,Files.readAllBytes(path));
    }
}
