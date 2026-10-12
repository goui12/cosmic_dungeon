package net.goui.cosmicdungeon.block.entity;
import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SpawnerLootInspectionTest {
    private static CosmicSpawnerLootTableSummary inspect(JsonObject root)throws Exception{
        var result=new CosmicSpawnerLootTableSummary(ResourceLocation.parse("minecraft:zombie"),null,null);
        var method=CosmicSpawnerLootTableInspector.class.getDeclaredMethod("readPools",JsonObject.class,CosmicSpawnerLootTableSummary.class);
        method.setAccessible(true);method.invoke(null,root,result);return result;
    }
    private static JsonObject table(){
        var entry=new JsonObject();entry.addProperty("type","minecraft:item");entry.addProperty("name","minecraft:bone");
        var entries=new JsonArray();entries.add(entry);var pool=new JsonObject();pool.addProperty("rolls",1);pool.add("entries",entries);
        var pools=new JsonArray();pools.add(pool);var root=new JsonObject();root.add("pools",pools);return root;
    }
    private static JsonObject pool(JsonObject root){return root.getAsJsonArray("pools").get(0).getAsJsonObject();}
    private static JsonObject entry(JsonObject root){return pool(root).getAsJsonArray("entries").get(0).getAsJsonObject();}
    @Test void simpleUnconditionalSingleRollCanBeNumeric()throws Exception{
        var result=inspect(table()).defaultDrops().getFirst().chance();assertTrue(result.numeric());assertEquals(1F,result.chance());
    }
    @Test void combinedConditionsProvidersAndRollsNeverInventPercentages()throws Exception{
        var root=table();var conditions=new JsonArray();
        var random=new JsonObject();random.addProperty("condition","minecraft:random_chance");random.addProperty("chance",0.5);
        var killed=new JsonObject();killed.addProperty("condition","minecraft:killed_by_player");conditions.add(random);conditions.add(killed);
        pool(root).add("conditions",conditions);assertFalse(inspect(root).defaultDrops().getFirst().chance().numeric());
        conditions.remove(1);random.add("chance",new JsonObject());assertFalse(inspect(root).defaultDrops().getFirst().chance().numeric());
        pool(root).remove("conditions");pool(root).addProperty("rolls",2);assertFalse(inspect(root).defaultDrops().getFirst().chance().numeric());
    }
    @Test void uniformCountProvidersNeverBecomeFallbackPercentages()throws Exception{
        var root=table();var functions=new JsonArray();var function=new JsonObject();function.addProperty("function","minecraft:set_count");
        var count=new JsonObject();count.addProperty("type","minecraft:uniform");count.addProperty("min",0);count.addProperty("max",3);
        function.add("count",count);functions.add(function);entry(root).add("functions",functions);
        assertEquals(0.75F,inspect(root).defaultDrops().getFirst().chance().chance());
        var provider=new JsonObject();provider.addProperty("type","minecraft:constant");provider.addProperty("value",2);
        count.add("min",provider);assertFalse(inspect(root).defaultDrops().getFirst().chance().numeric());
        count.addProperty("min",0);count.add("max",provider);assertFalse(inspect(root).defaultDrops().getFirst().chance().numeric());
        count.addProperty("max",3);count.addProperty("min",0.5);assertFalse(inspect(root).defaultDrops().getFirst().chance().numeric());
    }
    @Test void tableFunctionsAdditiveCountsAndZeroWeightsAreComplex()throws Exception{
        var root=table();var functions=new JsonArray();var function=new JsonObject();function.addProperty("function","minecraft:set_count");function.addProperty("count",0);function.addProperty("add",true);functions.add(function);
        entry(root).add("functions",functions);assertFalse(inspect(root).defaultDrops().getFirst().chance().numeric());
        entry(root).remove("functions");root.add("functions",functions);assertFalse(inspect(root).defaultDrops().getFirst().chance().numeric());
        root.remove("functions");entry(root).addProperty("weight",0);assertFalse(inspect(root).defaultDrops().getFirst().chance().numeric());
    }
    @Test void moddedConditionsAndFunctionsNeverClaimFixedProbabilities()throws Exception{
        var root=table();var conditions=JsonParser.parseString("[{\"condition\":\"example:season\",\"season\":\"winter\"}]").getAsJsonArray();
        pool(root).add("conditions",conditions);
        assertFalse(inspect(root).defaultDrops().getFirst().chance().numeric());
        pool(root).remove("conditions");
        entry(root).add("functions",JsonParser.parseString("[{\"function\":\"example:scale_by_difficulty\"}]").getAsJsonArray());
        assertFalse(inspect(root).defaultDrops().getFirst().chance().numeric());
    }

}
