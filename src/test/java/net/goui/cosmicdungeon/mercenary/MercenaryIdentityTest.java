package net.goui.cosmicdungeon.mercenary;

import java.util.*;
import net.goui.cosmicdungeon.entity.ModEntities;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class MercenaryIdentityTest {
    private static final UUID ID = UUID.fromString("a47e4f40-e5b0-4f89-81b1-fd3e16ee961d");
    private MercenaryContract contract() {
        return new MercenaryContract(ID, new UUID(1, 2), "theurgist", 2, 500);
    }
    @Test void poolContainsOneHundredDistinctShortVisibleNames() {
        assertEquals(100, MercenaryIdentity.NAMES.size());
        assertEquals(100, new HashSet<>(MercenaryIdentity.NAMES).size());
        assertTrue(MercenaryIdentity.NAMES.stream().allMatch(n -> n.matches("[A-Z][a-z]{0,8}")));
        var selected = new HashSet<String>();
        for (int i = 0; i < 100; i++) selected.add(MercenaryIdentity.name(new UUID(0, i)));
        assertEquals(Set.copyOf(MercenaryIdentity.NAMES), selected);
        assertEquals("Aldred", MercenaryIdentity.name(new UUID(0, 0)));
        assertEquals("Susanna", MercenaryIdentity.name(new UUID(0, 99)));
        assertNotNull(MercenaryIdentity.name(new UUID(Long.MIN_VALUE, Long.MAX_VALUE)));
    }
    @Test void legacyContractRoundtripPreservesIdentityWithoutAdditionalFields() {
        var hire = contract();
        var old = new CompoundTag();
        old.putString("id", ID.toString()); old.putString("hirer", hire.hirer().toString());
        old.putString("class", hire.classId()); old.putInt("slot", hire.slot()); old.putLong("fee", hire.fee());
        var loaded = MercenaryContract.CODEC.parse(NbtOps.INSTANCE, old).getOrThrow();
        assertEquals(hire, loaded); assertEquals(hire.name(), loaded.name());
        assertEquals(old, MercenaryContract.CODEC.encodeStart(NbtOps.INSTANCE, loaded).getOrThrow());
        assertEquals(hire.name(), new MercenaryContract(ID, new UUID(2, 3), "bogatyr", 3, 777).name());
    }
    @Test void spawnAndLegacyEntityLoadUseTheSameShortName() {
        var entity = new MercenaryEntity(ModEntities.MERCENARY.get(), null);
        entity.initialize(42, contract());
        assertEquals(ID, entity.getUUID()); assertEquals(contract().name(), entity.getName().getString());
        var lookup = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        var out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, lookup);
        entity.addAdditionalSaveData(out);
        var saved = out.buildResult();
        // Native custom-name state from an earlier revision must not leak into the new identity.
        entity.setCustomName(net.minecraft.network.chat.Component.literal("Mercenary Theurgist"));
        entity.readAdditionalSaveData(TagValueInput.create(ProblemReporter.DISCARDING, lookup, saved));
        assertEquals(contract(), entity.contract());
        assertEquals(contract().name(), entity.getName().getString());
        assertEquals(42, entity.runId());
    }
}
