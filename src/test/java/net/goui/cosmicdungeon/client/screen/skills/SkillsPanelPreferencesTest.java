package net.goui.cosmicdungeon.client.screen.skills;

import com.google.gson.JsonParser;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

final class SkillsPanelPreferencesTest {
    @TempDir Path directory;
    @Test void placementsRoundTripPerClassAndUnchangedSaveDoesNotWrite()throws Exception{
        var path=directory.resolve("skills.json");var prefs=new SkillsPanelPreferences(path);
        assertTrue(prefs.get("bogatyr").isEmpty());assertFalse(Files.exists(path));
        var first=new SkillsPanelState.Placement(.2,.6,true);var second=new SkillsPanelState.Placement(.8,.1,false);
        assertTrue(prefs.put("bogatyr",first));assertTrue(prefs.put("theurgist",second));
        var modified=Files.getLastModifiedTime(path);var text=Files.readString(path);
        assertFalse(prefs.put("bogatyr",first));
        assertEquals(modified,Files.getLastModifiedTime(path));assertEquals(text,Files.readString(path));
        var reloaded=new SkillsPanelPreferences(path);
        assertEquals(first,reloaded.get("bogatyr").orElseThrow());
        assertEquals(second,reloaded.get("theurgist").orElseThrow());
        assertTrue(reloaded.get("dragoon").isEmpty());
        try(var files=Files.list(directory)){assertEquals(1,files.count(),"Atomic save leaves no temporary files");}
    }
    @Test void resetCoversNoneAndAllKnownClassesWhilePreservingUnknownData()throws Exception{
        var path=directory.resolve("skills.json");
        Files.writeString(path,"""
                {"version":1,"futureRoot":{"keep":7},"layouts":{
                  "none":{"x":0.1,"y":0.3,"minimized":true,"futureField":"preserved"},
                  "bogatyr":{"x":0.2,"y":0.4,"minimized":true},
                  "theurgist":{"x":0.8,"y":0.6,"minimized":false},
                  "future_class":{"x":0.9,"y":0.7,"minimized":true,"extra":9}
                }}
                """);
        var prefs=new SkillsPanelPreferences(path);
        assertTrue(prefs.put("none",new SkillsPanelState.Placement(.4,.5,false)));
        assertTrue(prefs.reset());assertFalse(prefs.reset());
        var reloaded=new SkillsPanelPreferences(path);
        for(var id:new String[]{"none","bogatyr","theurgist"})assertTrue(reloaded.get(id).isEmpty());
        var json=JsonParser.parseString(Files.readString(path)).getAsJsonObject();
        assertEquals(7,json.getAsJsonObject("futureRoot").get("keep").getAsInt());
        assertEquals("preserved",json.getAsJsonObject("layouts").getAsJsonObject("none").get("futureField").getAsString());
        assertEquals(9,json.getAsJsonObject("layouts").getAsJsonObject("future_class").get("extra").getAsInt());
    }
    @Test void malformedAndFutureFilesAreNeverOverwrittenAndBadCoordinatesDefault()throws Exception{
        var path=directory.resolve("skills.json");
        for(var original:new String[]{"broken {","[]","{\"version\":2,\"layouts\":{}}","{\"version\":1,\"layouts\":[]}" }){
            Files.writeString(path,original);var prefs=new SkillsPanelPreferences(path);
            assertFalse(prefs.writable());
            assertFalse(prefs.put("bogatyr",new SkillsPanelState.Placement(.3,.4,false)));assertFalse(prefs.reset());
            assertEquals(original,Files.readString(path));
        }
        Files.writeString(path,"{\"version\":1,\"layouts\":{\"bogatyr\":{\"x\":\"NaN\",\"y\":1,\"minimized\":true}}}");
        assertTrue(new SkillsPanelPreferences(path).get("bogatyr").isEmpty());
    }
}
