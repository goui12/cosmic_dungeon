package net.goui.cosmicdungeon.playerclass.bogatyr;
import java.util.*;
import net.minecraft.nbt.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
final class BogatyrModeDataTest {
    private static final UUID OWNER=UUID.fromString("00000000-0000-0000-0000-000000002401");
    private static final UUID OTHER=UUID.fromString("00000000-0000-0000-0000-000000002402");
    private static BogatyrCompanionData data(CompoundTag tag){return BogatyrCompanionData.CODEC.parse(NbtOps.INSTANCE,tag).getOrThrow();}
    @Test void legacyModesBecomePatrolWithoutRewritingOldRecords(){
        for(var mode:WolfMode.values()){
            if(mode.active())continue;
            var root=legacy(mode.name());var before=root.copy();
            assertEquals(new WolfModeData.State(WolfMode.PATROL,true),WolfModeData.read(root,OWNER,24));
            assertEquals(before,root);
            assertTrue(WolfModeData.write(root,OWNER,24,WolfMode.STAY));
            assertEquals(before.get(WolfModeData.LEGACY),root.get(WolfModeData.LEGACY));
            assertEquals(WolfMode.STAY,data(root).mode(OWNER,24).mode());
            assertEquals(1,WolfModeData.epoch(root,OWNER,24));
        }
    }
    @Test void radioSelectionsAndEpochsSurviveReloadIndependently(){
        var directory=data(new CompoundTag());
        assertEquals(WolfMode.PATROL,directory.mode(OWNER,24).mode());
        assertTrue(directory.setMode(OWNER,24,WolfMode.STAY));
        assertEquals(1,directory.modeEpoch(OWNER,24));
        var image=directory.image();assertTrue(directory.setMode(OWNER,24,WolfMode.STAY));
        assertEquals(image,directory.image());
        assertTrue(directory.setMode(OTHER,24,WolfMode.STAY));
        assertTrue(directory.setMode(OWNER,25,WolfMode.STAY));
        directory=data(directory.image());
        assertTrue(directory.setMode(OWNER,24,WolfMode.PATROL));
        assertTrue(directory.setMode(OWNER,24,WolfMode.STAY));
        assertEquals(3,directory.modeEpoch(OWNER,24));
        assertEquals(1,directory.modeEpoch(OTHER,24));assertEquals(1,directory.modeEpoch(OWNER,25));
        assertEquals(WolfMode.PATROL,directory.mode(OTHER,25).mode());
    }
    @Test void unknownFieldsSurviveOverlay(){
        var root=legacy("STAND_GROUND");root.putString("future_root","keep");
        assertTrue(WolfModeData.write(root,OWNER,24,WolfMode.STAY));
        var modes=root.getCompoundOrEmpty(WolfModeData.KEY);modes.putString("future_container","keep");
        modes.getCompoundOrEmpty(OWNER+"/24").putLong("future_entry",Long.MAX_VALUE);
        var unknown=new CompoundTag();unknown.putString("mode","FUTURE");unknown.putLong("epoch",1);
        modes.put(OTHER+"/24",unknown);
        var legacy=root.get(WolfModeData.LEGACY).copy();
        assertTrue(WolfModeData.write(root,OWNER,24,WolfMode.PATROL));
        var image=data(data(root).image()).image();var saved=image.getCompoundOrEmpty(WolfModeData.KEY);
        assertEquals("keep",image.getStringOr("future_root",""));assertEquals(legacy,image.get(WolfModeData.LEGACY));
        assertEquals("keep",saved.getStringOr("future_container",""));
        assertEquals(Long.MAX_VALUE,saved.getCompoundOrEmpty(OWNER+"/24").getLongOr("future_entry",0));
        assertEquals(unknown,saved.get(OTHER+"/24"));
        assertFalse(WolfModeData.read(image,OTHER,24).writable());
    }
    @Test void unknownMalformedAndOverflowStatesRemainUnchanged(){
        var invalid=new ArrayList<CompoundTag>();
        var wrong=new CompoundTag();wrong.putString(WolfModeData.KEY,"invalid");invalid.add(wrong);
        var future=valid();future.getCompoundOrEmpty(WolfModeData.KEY).putInt("schema",3);invalid.add(future);
        var missing=valid();missing.getCompoundOrEmpty(WolfModeData.KEY).remove("schema");invalid.add(missing);
        var entry=valid();entry.getCompoundOrEmpty(WolfModeData.KEY).putInt(OWNER+"/24",7);invalid.add(entry);
        var mode=valid();mode.getCompoundOrEmpty(WolfModeData.KEY).getCompoundOrEmpty(OWNER+"/24").putString("mode","FUTURE");invalid.add(mode);
        var epoch=valid();epoch.getCompoundOrEmpty(WolfModeData.KEY).getCompoundOrEmpty(OWNER+"/24").putString("epoch","1");invalid.add(epoch);
        invalid.add(legacy("FUTURE"));
        var badLegacy=legacy("DEFENSIVE");badLegacy.getCompoundOrEmpty(WolfModeData.LEGACY).putInt("schema",2);invalid.add(badLegacy);
        for(var root:invalid){
            var before=root.copy();assertEquals(new WolfModeData.State(WolfMode.STAY,false),WolfModeData.read(root,OWNER,24));
            for(var value:WolfMode.values())assertFalse(WolfModeData.write(root,OWNER,24,value));
            assertEquals(before,root);assertEquals(before.get(WolfModeData.KEY),data(root).image().get(WolfModeData.KEY));
        }
        var max=valid();max.getCompoundOrEmpty(WolfModeData.KEY).getCompoundOrEmpty(OWNER+"/24").putLong("epoch",Long.MAX_VALUE);
        var before=max.copy();assertFalse(WolfModeData.write(max,OWNER,24,WolfMode.PATROL));assertEquals(before,max);
    }
    @Test void invalidIdentityAndRetiredSelectionsCannotWrite(){
        var root=new CompoundTag();var before=root.copy();
        for(long run:new long[]{0,-1,Long.MIN_VALUE})assertFalse(WolfModeData.write(root,OWNER,run,WolfMode.STAY));
        assertFalse(WolfModeData.write(root,null,24,WolfMode.STAY));assertFalse(WolfModeData.write(root,OWNER,24,null));
        for(var mode:WolfMode.values())if(!mode.active())assertFalse(WolfModeData.write(root,OWNER,24,mode));
        assertEquals(before,root);
    }
    private static CompoundTag valid(){var root=new CompoundTag();assertTrue(WolfModeData.write(root,OWNER,24,WolfMode.STAY));return root;}
    private static CompoundTag legacy(String mode){
        var root=new CompoundTag();var modes=new CompoundTag();var entry=new CompoundTag();
        entry.putString("mode",mode);entry.putString("unknown","preserve");modes.putInt("schema",1);modes.put(OWNER+"/24",entry);
        root.put(WolfModeData.LEGACY,modes);return root;
    }
}
