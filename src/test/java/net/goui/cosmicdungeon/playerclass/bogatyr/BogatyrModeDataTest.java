package net.goui.cosmicdungeon.playerclass.bogatyr;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class BogatyrModeDataTest {
    private static final UUID OWNER=UUID.fromString("00000000-0000-0000-0000-000000002401");
    private static final UUID OTHER=UUID.fromString("00000000-0000-0000-0000-000000002402");
    private static BogatyrCompanionData data(CompoundTag tag){
        return BogatyrCompanionData.CODEC.parse(NbtOps.INSTANCE,tag).getOrThrow();
    }
    @Test void oldSavesDefaultToDefensiveWithoutBeingRewritten(){
        var root=new CompoundTag();root.putString("future_root","keep");
        var before=root.copy();
        assertEquals(new WolfModeData.State(WolfMode.DEFENSIVE,true),WolfModeData.read(root,OWNER,24));
        assertEquals(before,root);
        var loaded=data(root);var image=loaded.image();
        assertEquals(new WolfModeData.State(WolfMode.DEFENSIVE,true),loaded.mode(OWNER,24));
        assertEquals(image,loaded.image());assertFalse(loaded.image().contains(WolfModeData.KEY));
        assertTrue(loaded.setMode(OWNER,24,WolfMode.AGGRESSIVE));
        assertEquals(WolfMode.AGGRESSIVE,data(loaded.image()).mode(OWNER,24).mode());
    }
    @Test void selectionsAreIndependentPerOwnerAndRunAcrossSaveReload(){
        var directory=data(new CompoundTag());
        assertTrue(directory.setMode(OWNER,24,WolfMode.AGGRESSIVE));
        assertTrue(directory.setMode(OTHER,24,WolfMode.STAND_GROUND));
        assertTrue(directory.setMode(OWNER,25,WolfMode.STAND_GROUND));
        var loaded=data(directory.image());
        assertEquals(WolfMode.AGGRESSIVE,loaded.mode(OWNER,24).mode());
        assertEquals(WolfMode.STAND_GROUND,loaded.mode(OTHER,24).mode());
        assertEquals(WolfMode.STAND_GROUND,loaded.mode(OWNER,25).mode());
        assertEquals(WolfMode.DEFENSIVE,loaded.mode(OTHER,25).mode());
        assertTrue(loaded.setMode(OWNER,24,WolfMode.DEFENSIVE));
        var finalSave=data(loaded.image());
        assertEquals(WolfMode.DEFENSIVE,finalSave.mode(OWNER,24).mode());
        assertEquals(WolfMode.STAND_GROUND,finalSave.mode(OTHER,24).mode());
        assertEquals(WolfMode.STAND_GROUND,finalSave.mode(OWNER,25).mode());
    }
    @Test void changingAKnownModePreservesUnknownRootContainerAndEntryFields(){
        var root=validRoot();root.putString("future_root","keep");
        var modes=root.getCompoundOrEmpty(WolfModeData.KEY);
        modes.putString("future_container","keep");
        var entry=modes.getCompoundOrEmpty(OWNER+"/24");entry.putLong("future_entry",Long.MAX_VALUE);
        var foreign=new CompoundTag();foreign.putString("mode","FUTURE_MODE");foreign.putString("future","keep");
        modes.put(OTHER+"/24",foreign);
        var directory=data(root);
        assertTrue(directory.setMode(OWNER,24,WolfMode.AGGRESSIVE));
        var image=data(directory.image()).image();var savedModes=image.getCompoundOrEmpty(WolfModeData.KEY);
        assertEquals("keep",image.getStringOr("future_root",""));
        assertEquals("keep",savedModes.getStringOr("future_container",""));
        assertEquals(Long.MAX_VALUE,savedModes.getCompoundOrEmpty(OWNER+"/24").getLongOr("future_entry",0));
        assertEquals(foreign,savedModes.getCompoundOrEmpty(OTHER+"/24"));
        assertEquals(WolfMode.AGGRESSIVE,WolfModeData.read(image,OWNER,24).mode());
        assertEquals(new WolfModeData.State(WolfMode.STAND_GROUND,false),WolfModeData.read(image,OTHER,24));
    }
    @Test void unknownOrMalformedModeDataIsHeldAndBytePreserved() throws IOException {
        var wrongRoot=new CompoundTag();wrongRoot.putString(WolfModeData.KEY,"not a compound");
        var futureSchema=validRoot();futureSchema.getCompoundOrEmpty(WolfModeData.KEY).putInt("schema",2);
        var absentSchema=validRoot();absentSchema.getCompoundOrEmpty(WolfModeData.KEY).remove("schema");
        var badSchema=validRoot();badSchema.getCompoundOrEmpty(WolfModeData.KEY).putString("schema","1");
        var wrongEntry=validRoot();wrongEntry.getCompoundOrEmpty(WolfModeData.KEY).putInt(OWNER+"/24",7);
        var missingMode=validRoot();missingMode.getCompoundOrEmpty(WolfModeData.KEY).getCompoundOrEmpty(OWNER+"/24").remove("mode");
        var wrongMode=validRoot();wrongMode.getCompoundOrEmpty(WolfModeData.KEY).getCompoundOrEmpty(OWNER+"/24").putInt("mode",0);
        var futureMode=validRoot();futureMode.getCompoundOrEmpty(WolfModeData.KEY).getCompoundOrEmpty(OWNER+"/24").putString("mode","STRATEGIC");
        for(var root:List.of(wrongRoot,futureSchema,absentSchema,badSchema,wrongEntry,missingMode,wrongMode,futureMode)){
            var bytes=bytes(root);
            assertEquals(new WolfModeData.State(WolfMode.STAND_GROUND,false),WolfModeData.read(root,OWNER,24));
            for(var mode:WolfMode.values())assertFalse(WolfModeData.write(root,OWNER,24,mode));
            assertArrayEquals(bytes,bytes(root),"An unsupported save must not be silently repaired or downgraded");
            var directory=data(root);var before=bytes(directory.image());
            assertFalse(directory.setMode(OWNER,24,WolfMode.DEFENSIVE));
            assertEquals(new WolfModeData.State(WolfMode.STAND_GROUND,false),directory.mode(OWNER,24));
            assertArrayEquals(before,bytes(directory.image()));
        }
    }
    @Test void invalidIdentityRunOrNullModeCannotCreatePersistentSelections() throws IOException {
        var root=new CompoundTag();var before=bytes(root);
        for(long run:new long[]{0,-1,Long.MIN_VALUE}){
            assertEquals(new WolfModeData.State(WolfMode.STAND_GROUND,false),WolfModeData.read(root,OWNER,run));
            assertFalse(WolfModeData.write(root,OWNER,run,WolfMode.AGGRESSIVE));
        }
        assertEquals(new WolfModeData.State(WolfMode.STAND_GROUND,false),WolfModeData.read(root,null,24));
        assertFalse(WolfModeData.write(root,null,24,WolfMode.AGGRESSIVE));
        assertFalse(WolfModeData.write(root,OWNER,24,null));
        assertArrayEquals(before,bytes(root));
    }
    private static CompoundTag validRoot(){
        var root=new CompoundTag();var modes=new CompoundTag();var entry=new CompoundTag();
        entry.putString("mode","DEFENSIVE");modes.putInt("schema",1);modes.put(OWNER+"/24",entry);
        root.put(WolfModeData.KEY,modes);return root;
    }
    private static byte[] bytes(CompoundTag tag) throws IOException {
        var out=new ByteArrayOutputStream();try(var data=new DataOutputStream(out)){NbtIo.write(tag,data);}
        return out.toByteArray();
    }
}
