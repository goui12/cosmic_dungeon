package net.goui.cosmicdungeon.leaderboard;
import com.google.gson.*;
import com.mojang.serialization.Codec;
import io.netty.buffer.Unpooled;
import net.goui.cosmicdungeon.dungeon.d1.*;
import net.goui.cosmicdungeon.network.LeaderboardPayloads.*;
import net.goui.cosmicdungeon.client.screen.LeaderboardScreen;
import net.minecraft.nbt.*;
import net.minecraft.SharedConstants;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.stats.*;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
final class LeaderboardTest{
    @TempDir Path folder;
    private static final UUID A=new UUID(0,1),B=new UUID(0,2);
    @SuppressWarnings("unchecked") private static Codec<D1LifetimeData> codec()throws Exception{
        var field=D1LifetimeData.class.getDeclaredField("CODEC");field.setAccessible(true);return (Codec<D1LifetimeData>)field.get(null);
    }
    private static D1LifetimeData empty()throws Exception{return codec().parse(NbtOps.INSTANCE,new CompoundTag()).getOrThrow();}
    private static D1LifetimeData reload(D1LifetimeData d)throws Exception{return codec().parse(NbtOps.INSTANCE,codec().encodeStart(NbtOps.INSTANCE,d).getOrThrow()).getOrThrow();}
    private static JsonElement stats(String body){return JsonParser.parseString("{\"stats\":{"+body+"}}");}
    @Test void oldSaveLoadsWithoutInventingCustomHistory()throws Exception{
        var old=empty();old.complete(A,12,7);old.recordLesserBlooms(A,3);
        var tag=(CompoundTag)codec().encodeStart(NbtOps.INSTANCE,old).getOrThrow();tag.remove("activity");
        var restored=codec().parse(NbtOps.INSTANCE,tag).getOrThrow();
        assertEquals(old.totals(A),restored.totals(A));assertEquals(LifetimeActivity.EMPTY,restored.activity(A));
        assertEquals(7,restored.leaderboard(A,"legacy|successful_kills"));
    }
    @Test void activitySurvivesFailedOutcomeAndRepeatedSave()throws Exception{
        var d=empty();d.name(A,"Before");d.activity(A,"hostile_kills",13);d.activity(A,"doors_unlocked",2);
        var failure=new WatsonReceipt(A,15,UUID.randomUUID(),false);d.applyWatson(failure,999,999);
        d=reload(d);d.applyWatson(failure,999,999);d.name(A,"Renamed");d=reload(d);
        assertEquals(13,d.leaderboard(A,"cosmic|hostile_kills"));assertEquals(2,d.leaderboard(A,"cosmic|doors_unlocked"));
        assertEquals(0,d.totals(A).successfulKills());assertEquals("Renamed",d.activity(A).name());
        assertEquals(Set.of(A),d.owners());
    }
    @Test void successProjectionAndObservedActivityStayIndependent()throws Exception{
        var d=empty();d.activity(A,"hostile_kills",9);d.activity(B,"hostile_kills",3);
        var receipt=new WatsonReceipt(A,20,UUID.randomUUID(),true);
        d.applyWatson(receipt,4,6);d=reload(d);d.applyWatson(receipt,4,6);
        assertEquals(9,d.leaderboard(A,"cosmic|hostile_kills"));assertEquals(3,d.leaderboard(B,"cosmic|hostile_kills"));
        assertEquals(4,d.totals(A).successfulKills());assertEquals(1,d.totals(A).completions());
    }
    @Test void customCountersSaturateAndRejectInvalidChanges(){
        var d=new LifetimeActivity("Player",Map.of("hostile_kills",Long.MAX_VALUE-2,"future_counter",7L)).add("hostile_kills",10);
        assertEquals(Long.MAX_VALUE,d.counts().get("hostile_kills"));assertEquals(7,d.counts().get("future_counter"));
        assertThrows(IllegalArgumentException.class,()->d.add("hostile_kills",-1));
        assertThrows(IllegalArgumentException.class,()->d.add("unknown",1));
        assertThrows(UnsupportedOperationException.class,()->d.counts().put("logins",3L));
        assertThrows(IllegalArgumentException.class,()->new LifetimeActivity("Name",Map.of("logins",-1L)));
    }
    @Test void unknownSavedActivityKeysRoundTrip(){
        var d=new LifetimeActivity("Player",Map.of("future_counter",17L));
        assertEquals(d,LifetimeActivity.CODEC.parse(NbtOps.INSTANCE,LifetimeActivity.CODEC.encodeStart(NbtOps.INSTANCE,d).getOrThrow()).getOrThrow());
    }
    @Test void nativeArchiveCoversMiningCraftingTravelAndKills(){
        var json=stats("\"minecraft:mined\":{\"minecraft:stone\":5,\"cosmicdungeon:cosmic_mob_spawner\":3},\"minecraft:crafted\":{\"minecraft:stick\":12},\"minecraft:custom\":{\"minecraft:walk_one_cm\":300,\"minecraft:mob_kills\":4}");
        assertEquals(8,StatisticsArchive.value(json,"total|minecraft:mined"));
        assertEquals(3,StatisticsArchive.value(json,"minecraft:mined|cosmicdungeon:cosmic_mob_spawner"));
        assertEquals(12,StatisticsArchive.value(json,"minecraft:crafted|minecraft:stick"));
        assertEquals(300,StatisticsArchive.value(json,"minecraft:custom|minecraft:walk_one_cm"));
        assertEquals(4,StatisticsArchive.value(json,"minecraft:custom|minecraft:mob_kills"));
        assertEquals(0,StatisticsArchive.value(json,"minecraft:used|minecraft:stone"));
    }
    @Test void malformedNativeCountsCannotProducePlausibleRankings(){
        for(String value:List.of("-1","1.5","9223372036854775808","\"12\"","null","{}")){
            var json=stats("\"minecraft:custom\":{\"minecraft:mob_kills\":"+value+"}");
            assertThrows(RuntimeException.class,()->StatisticsArchive.value(json,"minecraft:custom|minecraft:mob_kills"),value);
        }
        assertThrows(RuntimeException.class,()->StatisticsArchive.value(JsonParser.parseString("{}"),"minecraft:custom|minecraft:mob_kills"));
    }
    @Test void nativeTotalArithmeticCannotWrapNegative(){
        var json=stats("\"minecraft:mined\":{\"a\":9223372036854775807,\"b\":3}");
        assertEquals(Long.MAX_VALUE,StatisticsArchive.value(json,"total|minecraft:mined"));
    }
    @Test void archiveReadUsesNativeDataVersionWithoutWriting()throws Exception{
        var path=folder.resolve(A+".json");
        var json=stats("\"minecraft:custom\":{\"minecraft:mob_kills\":27}").getAsJsonObject();
        json.addProperty("DataVersion",SharedConstants.getCurrentVersion().dataVersion().version());
        Files.writeString(path,json.toString());byte[] before=Files.readAllBytes(path);
        assertEquals(27,StatisticsArchive.read(path,"minecraft:custom|minecraft:mob_kills",DataFixers.getDataFixer()));
        assertArrayEquals(before,Files.readAllBytes(path));
    }
    @Test void archiveRejectsOversizedAndMalformedFiles()throws Exception{
        var path=folder.resolve(A+".json");Files.write(path,new byte[StatisticsArchive.MAX_FILE_BYTES+1]);
        assertThrows(java.io.IOException.class,()->StatisticsArchive.read(path,"minecraft:custom|minecraft:mob_kills",DataFixers.getDataFixer()));
        Files.writeString(path,"{broken");
        assertThrows(RuntimeException.class,()->StatisticsArchive.read(path,"minecraft:custom|minecraft:mob_kills",DataFixers.getDataFixer()));
    }
    @Test void allTiedPagesAreStableWithoutDuplicates(){
        var input=new ArrayList<Row>();for(int i=1;i<=41;i++)input.add(new Row(new UUID(0,i).toString(),"P"+i,42));
        Collections.shuffle(input,new Random(31));var result=new ArrayList<Row>();long cursor=-1;String id="";
        while(true){
            var page=new LeaderboardRanking(cursor,id,12);input.forEach(page::accept);
            assertEquals(result.size()+1,page.firstRank());result.addAll(page.rows());
            if(!page.more())break;var last=page.rows().getLast();cursor=last.value();id=last.id();
        }
        assertEquals(input.stream().sorted(LeaderboardRanking.ORDER).toList(),result);
        assertEquals(41,result.stream().map(Row::id).distinct().count());
    }
    @Test void rankingsSortExtremeValuesAndEmptyTail(){
        var input=List.of(new Row(A.toString(),"A",0),new Row(B.toString(),"B",Long.MAX_VALUE));
        var page=new LeaderboardRanking(-1,"",1);input.forEach(page::accept);
        assertEquals(B.toString(),page.rows().getFirst().id());assertTrue(page.more());
        var tail=new LeaderboardRanking(0,A.toString(),12);input.forEach(tail::accept);
        assertTrue(tail.rows().isEmpty());assertEquals(3,tail.firstRank());assertFalse(tail.more());
    }
    @Test void realNativeCountersAndAggregatesMatch(){
        var counter=new StatsCounter();counter.setValue(null,Stats.BLOCK_MINED.get(Blocks.STONE),5);counter.setValue(null,Stats.BLOCK_MINED.get(Blocks.DIRT),7);
        counter.setValue(null,Stats.CUSTOM.get(Stats.MOB_KILLS),3);
        assertEquals(5,LeaderboardMetrics.nativeValue(counter,"minecraft:mined|minecraft:stone"));
        assertEquals(12,LeaderboardMetrics.nativeValue(counter,"total|minecraft:mined"));
        assertEquals(3,LeaderboardMetrics.nativeValue(counter,"minecraft:custom|minecraft:mob_kills"));
    }
    @Test void catalogExposesOnlyTheApprovedCuratedStatistics(){
        var list=LeaderboardMetrics.catalog();
        assertEquals(List.of(
            new Metric("legacy|completions","Dungeons completed"),
            new Metric("minecraft:mined|cosmicdungeon:cosmic_mob_spawner","Cosmic mob spawners broken"),
            new Metric("minecraft:custom|minecraft:mob_kills","Mobs killed"),
            new Metric("minecraft:custom|minecraft:deaths","Death count"),
            new Metric("summary|blocks_traveled","Blocks traveled"),
            new Metric("cosmic|doors_unlocked","Doors unlocked"),
            new Metric("cosmic|lesser_harvests","Lesser Blooms harvested"),
            new Metric("minecraft:custom|minecraft:play_time","Time played")),list);
        assertEquals(LeaderboardMetrics.DEFAULT,list.getFirst().key());
        assertEquals(list.size(),list.stream().map(Metric::key).distinct().count());
    }
    @Test void completionDefaultPreservesHistoryAndRejectsFailedOrRepeatedAwards()throws Exception{
        var d=empty();assertTrue(d.complete(A,10));assertFalse(d.complete(A,10));
        d=reload(d);assertEquals(1,d.leaderboard(A,LeaderboardMetrics.DEFAULT));
        d.applyWatson(new WatsonReceipt(A,11,UUID.randomUUID(),false),50,20);
        assertEquals(1,d.leaderboard(A,LeaderboardMetrics.DEFAULT));
        var success=new WatsonReceipt(A,12,UUID.randomUUID(),true);
        d.applyWatson(success,3,1);d=reload(d);d.applyWatson(success,3,1);
        assertEquals(2,d.leaderboard(A,LeaderboardMetrics.DEFAULT));
    }
    @Test void traveledBlocksMatchesLiveAndOfflineAcrossNativeMovementModes(){
        var counter=new StatsCounter();var group=new JsonObject();var document=new JsonObject();var body=new JsonObject();
        int centimeters=25;
        for(var id:List.of(Stats.WALK_ONE_CM,Stats.CROUCH_ONE_CM,Stats.SPRINT_ONE_CM,Stats.WALK_ON_WATER_ONE_CM,
                Stats.FALL_ONE_CM,Stats.CLIMB_ONE_CM,Stats.FLY_ONE_CM,Stats.WALK_UNDER_WATER_ONE_CM,
                Stats.MINECART_ONE_CM,Stats.BOAT_ONE_CM,Stats.PIG_ONE_CM,Stats.HAPPY_GHAST_ONE_CM,
                Stats.HORSE_ONE_CM,Stats.AVIATE_ONE_CM,Stats.SWIM_ONE_CM,Stats.STRIDER_ONE_CM)){
            counter.setValue(null,Stats.CUSTOM.get(id),centimeters);group.addProperty(id.toString(),centimeters);
        }
        counter.setValue(null,Stats.CUSTOM.get(Stats.JUMP),999);group.addProperty("minecraft:jump",999);
        body.add("minecraft:custom",group);document.add("stats",body);
        assertEquals(4,LeaderboardMetrics.nativeValue(counter,LeaderboardMetrics.TRAVEL));
        assertEquals(4,StatisticsArchive.value(document,LeaderboardMetrics.TRAVEL));
        assertEquals("1,234",LeaderboardMetrics.format(LeaderboardMetrics.TRAVEL,1234));
    }
    @Test void traveledBlocksRoundsAfterSummingAndRejectsMalformedDistance(){
        assertEquals(1,StatisticsArchive.value(stats("\"minecraft:custom\":{\"minecraft:walk_one_cm\":50,\"minecraft:sprint_one_cm\":75}"),LeaderboardMetrics.TRAVEL));
        assertEquals(0,StatisticsArchive.value(stats(""),LeaderboardMetrics.TRAVEL));
        assertThrows(RuntimeException.class,()->StatisticsArchive.value(stats("\"minecraft:custom\":{\"minecraft:walk_one_cm\":-1}"),LeaderboardMetrics.TRAVEL));
        assertEquals(1475739525896764129L,LeaderboardMetrics.traveledBlocks(id->Long.MAX_VALUE));
    }
    @Test void timeAndDistanceUseNativeFormatting(){
        assertEquals(Stats.CUSTOM.get(Stats.PLAY_TIME).format(72000),LeaderboardMetrics.format("minecraft:custom|minecraft:play_time",72000));
        assertEquals(Stats.CUSTOM.get(Stats.WALK_ONE_CM).format(10000),LeaderboardMetrics.format("minecraft:custom|minecraft:walk_one_cm",10000));
        assertEquals("9,223,372,036,854,775,807",LeaderboardMetrics.format("cosmic|hostile_kills",Long.MAX_VALUE));
    }
    @Test void requestRoundTripAndInvalidCursors(){
        var expected=new Request(3,"minecraft:custom|minecraft:mob_kills","kills",4,Long.MAX_VALUE,A.toString());
        var buffer=Unpooled.buffer();try{Request.CODEC.encode(buffer,expected);assertEquals(expected,Request.CODEC.decode(buffer));}finally{buffer.release();}
        assertThrows(IllegalArgumentException.class,()->new Request(0,"","",0,-1,A.toString()));
        assertThrows(IllegalArgumentException.class,()->new Request(0,"","",0,0,""));
        assertThrows(IllegalArgumentException.class,()->new Request(0,"","",0,1,"invalid"));
        assertThrows(IllegalArgumentException.class,()->new Request(0,"","x".repeat(49),0,-1,""));
        assertThrows(IllegalArgumentException.class,()->new Request(0,"","",-1,-1,""));
    }
    @Test void fullPageWireRoundTripAndLimits(){
        var rows=new ArrayList<Row>();var metrics=new ArrayList<Metric>();
        for(int i=0;i<12;i++){rows.add(new Row(new UUID(0,i).toString(),"Player"+i,Long.MAX_VALUE-i));metrics.add(new Metric("cosmic|logins","Logins"));}
        var page=new Page("cosmic|logins",0,3,1,true,"Recorded activity");
        var expected=new View(9,metrics,rows,page);var buffer=Unpooled.buffer();
        try{View.CODEC.encode(buffer,expected);assertEquals(expected,View.CODEC.decode(buffer));}finally{buffer.release();}
        rows.add(new Row(A.toString(),"Extra",0));assertThrows(IllegalArgumentException.class,()->new View(1,List.of(),rows,page));
        var oversized=Unpooled.buffer();try{net.minecraft.network.codec.ByteBufCodecs.VAR_INT.encode(oversized,1);net.minecraft.network.codec.ByteBufCodecs.VAR_INT.encode(oversized,13);
            assertThrows(RuntimeException.class,()->View.CODEC.decode(oversized));}finally{oversized.release();}
    }
    @Test void nativePauseInjectionAndSmallestGuiAreCompatible()throws Exception{
        // Force the real native pause class through Mixin transformation without opening a world.
        Class.forName("net.minecraft.client.gui.screens.PauseScreen");
        for(int height:List.of(240,270,360,480,720,1080)){
            int rows=LeaderboardScreen.visibleRows(height);assertTrue(rows>=3&&rows<=12);assertTrue(64+rows*18<=height-72);
        }
    }
}
