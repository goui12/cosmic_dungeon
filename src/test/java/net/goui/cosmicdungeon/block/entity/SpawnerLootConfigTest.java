package net.goui.cosmicdungeon.block.entity;
import java.util.*;
import io.netty.buffer.Unpooled;
import net.goui.cosmicdungeon.spawner.editor.*;
import net.goui.cosmicdungeon.spawner.editor.SpawnerLootDraft.*;
import net.goui.cosmicdungeon.network.SpawnerEditorPayloads.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SpawnerLootConfigTest {
    private static Rule row(String id,float chance,int count){return new Rule(id,"minecraft:bone",chance,count,CosmicSpawnerPreset.IntrinsicDropRule.Kind.CUSTOM_ADDITION);}
    @Test void stableSameItemRowsStayIndependentAndGearOnlyClaimsEditedSlot(){
        var gear=new ArrayList<>(Collections.nCopies(6,-1F));gear.set(0,0F);gear.set(5,1F);
        var d=new SpawnerLootDraft(gear,true,List.of(row("first",0,2),row("second",1,3)));
        var out=SpawnerLootConfig.patch(new CompoundTag(),"minecraft:zombie",d).getCompoundOrEmpty("SpawnerPreset");
        assertEquals(33,out.getIntOr("dropMask",0));assertEquals(0,out.getIntOr("equipmentMask",-1));
        var rows=out.getCompoundOrEmpty("intrinsicDropRules");assertEquals(2,rows.size());
        assertEquals(2,rows.getCompoundOrEmpty("first").getIntOr("count",0));assertEquals(3,rows.getCompoundOrEmpty("second").getIntOr("count",0));
    }
    @Test void invalidRowsAndPercentagesCannotPatchOriginal(){
        var original=new CompoundTag();original.putString("unknown","keep");
        for(var d:List.of(new SpawnerLootDraft(Collections.nCopies(6,Float.NaN),false,List.of()),
                new SpawnerLootDraft(Collections.nCopies(6,-1F),true,List.of(row("a",Float.POSITIVE_INFINITY,1))),
                new SpawnerLootDraft(Collections.nCopies(6,-1F),true,List.of(row("a",1,65))),
                new SpawnerLootDraft(Collections.nCopies(6,-1F),true,List.of(row("a",1,1),row("a",0,1)))))
            assertThrows(IllegalArgumentException.class,()->SpawnerLootConfig.patch(original,"minecraft:zombie",d));
        assertEquals(1,original.size());assertEquals("keep",original.getStringOr("unknown",""));
    }
    @Test void unchangedFutureKindAndUnknownRuleFieldsSurviveOtherRowEditsAndSave(){
        var original=new CompoundTag();var preset=new CompoundTag();preset.putInt("presetVersion",5);
        var rows=new CompoundTag();var old=new CompoundTag();old.putString("id","a");old.putString("item","minecraft:bone");
        old.putFloat("chance",0.33333334F);old.putInt("count",2);old.putString("kind","FUTURE_CONDITIONAL");old.putLong("future",123456789L);
        rows.put("a",old);preset.put("intrinsicDropRules",rows);original.put("SpawnerPreset",preset);
        var unchanged=new Rule("a","minecraft:bone",0.33333334F,2,CosmicSpawnerPreset.IntrinsicDropRule.Kind.UNKNOWN_CONFIGURED);
        var out=SpawnerLootConfig.patch(original,"minecraft:zombie",new SpawnerLootDraft(Collections.nCopies(6,-1F),true,List.of(unchanged,row("b",1,3))));
        var lookup=RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        var loaded=CosmicSpawnerPreset.load(TagValueInput.create(ProblemReporter.DISCARDING,lookup,out.getCompoundOrEmpty("SpawnerPreset")));
        var save=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,lookup);loaded.save(save);
        assertEquals(old,save.buildResult().getCompoundOrEmpty("intrinsicDropRules").getCompoundOrEmpty("a"));
    }
    @Test void replacingRulesWithEmptyExplicitlyRestoresNativeLoot(){
        var d=new SpawnerLootDraft(Collections.nCopies(6,-1F),true,List.of());
        var p=SpawnerLootConfig.patch(new CompoundTag(),"minecraft:zombie",d).getCompoundOrEmpty("SpawnerPreset");
        assertTrue(p.getBooleanOr("intrinsicDropsOverride",false));assertTrue(p.getCompoundOrEmpty("intrinsicDropRules").isEmpty());
        assertTrue(p.getCompoundOrEmpty("intrinsicDrops").isEmpty());
    }
    @Test void libraryAndLootCodecRoundTripKeepsKindsAndFractions(){
        var loot=new SpawnerLootDraft(Collections.nCopies(6,-1F),true,List.of(row("a",0.125F,64),row("b",1,1)));
        var packet=new LibraryRequest(UUID.randomUUID(),UUID.randomUUID(),LibraryKind.SAVE,"Named_1",0,SpawnerEditorDraft.empty(),SpawnerEquipmentDraft.unchanged(),loot);
        var raw=Unpooled.buffer();try{var buf=new RegistryFriendlyByteBuf(raw,RegistryAccess.EMPTY);
            LibraryRequest.STREAM_CODEC.encode(buf,packet);assertEquals(packet,LibraryRequest.STREAM_CODEC.decode(buf));assertEquals(0,buf.readableBytes());
        }finally{raw.release();}
    }
}
