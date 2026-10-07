package net.goui.cosmicdungeon.playerclass.bogatyr;

import java.util.*;
import net.minecraft.nbt.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BogatyrLifecycleDataTest {
    private static BogatyrCompanionData data(CompoundTag tag){return BogatyrCompanionData.CODEC.parse(NbtOps.INSTANCE,tag).getOrThrow();}
    @Test void activeUnknownFieldsAndOptionalClearsSurvive(){
        var data=data(new CompoundTag());var wolf=UUID.randomUUID();var owner=UUID.randomUUID();
        data.remember(new BogatyrCompanionData.Companion(wolf,owner,23,"test:run",912,true));
        var image=data.image();image.putString("future_root","keep");
        image.getListOrEmpty("companions").getCompoundOrEmpty(0).putString("future_pet","keep");
        var loaded=data(image);loaded.remember(new BogatyrCompanionData.Companion(wolf,owner,23,"test:run",0,false));
        var round=data(loaded.image());
        assertEquals("keep",round.image().getStringOr("future_root",""));
        assertEquals("keep",round.image().getListOrEmpty("companions").getCompoundOrEmpty(0).getStringOr("future_pet",""));
        assertEquals(0,round.find(wolf).orElseThrow().position());assertFalse(round.find(wolf).orElseThrow().located());
        assertFalse(round.runRetired(23));
    }
    @Test void retirementRetainsAuditAndCannotReenterActiveIndices(){
        var data=data(new CompoundTag());var wolf=UUID.randomUUID();var owner=UUID.randomUUID();
        var entry=new BogatyrCompanionData.Companion(wolf,owner,23,"test:run",12,true);
        data.remember(entry);var entity=new CompoundTag();entity.putString("id","minecraft:wolf");entity.putFloat("Health",20);
        entity.store("UUID",net.minecraft.core.UUIDUtil.CODEC,wolf);entity.store("Owner",net.minecraft.core.UUIDUtil.CODEC,owner);
        var persistent=new CompoundTag();WolfIdentity.set(persistent,wolf);entity.put("NeoForgeData",persistent);
        data.putArchive(wolf,new WolfArchive(WolfArchive.PREPARED,UUID.randomUUID(),23,"test:run",12,"",0,entity));
        var raw=data.image();raw.getCompoundOrEmpty("archives").getCompoundOrEmpty(wolf.toString()).putString("future_archive","keep");
        var observed=data(raw);observed.retireArchive(wolf);observed.retireRecord(entry);
        assertEquals("keep",observed.image().getCompoundOrEmpty("retired_records").getCompoundOrEmpty(wolf.toString())
                .getCompoundOrEmpty("archive").getStringOr("future_archive",""));
        data=data(raw);data.identityHold("test:run",wolf);data.markRunRetired(23);data.retireRecord(entry);
        var round=data(data.image());
        assertTrue(round.runRetired(23));assertEquals(0,round.count(owner));assertTrue(round.archive(wolf).isEmpty());
        assertTrue(round.inDimension("test:run").isEmpty());assertFalse(round.dimensionHeld("test:run"));
        assertEquals("keep",round.image().getCompoundOrEmpty("retired_records").getCompoundOrEmpty(wolf.toString())
                .getCompoundOrEmpty("archive").getStringOr("future_archive",""));
        round.retireRecord(entry);assertEquals(data.image(),round.image());
    }
    @Test void unsupportedTombstonesFailWithoutInventingEmptyData(){
        var tag=new CompoundTag();tag.putString("retired_runs","bad");
        assertTrue(BogatyrCompanionData.CODEC.parse(NbtOps.INSTANCE,tag).error().isPresent());
        var list=new ListTag();list.add(LongTag.valueOf(-1));tag.put("retired_runs",list);
        assertTrue(BogatyrCompanionData.CODEC.parse(NbtOps.INSTANCE,tag).error().isPresent());
    }
}
