package net.goui.cosmicdungeon.mercenary;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class MercenaryNamePoolTest {
    @Test void approvedRegionsEachContainFiveMaleAndFiveFemaleNames() {
        assertEquals(Set.of("English", "French", "German-speaking", "Italian",
            "Asia", "Africa", "Russia", "Mediterranean"),
            MercenaryNamePool.REGIONS.stream().map(MercenaryNamePool.RegionPool::region)
                .collect(java.util.stream.Collectors.toSet()));
        assertEquals(8, MercenaryNamePool.REGIONS.size());
        for (var region : MercenaryNamePool.REGIONS) {
            assertFalse(region.culture().isBlank(), region.region());
            assertEquals(5, region.male().size(), region.region());
            assertEquals(5, region.female().size(), region.region());
        }
    }

    @Test void poolHasEightyDistinctNamesWithinTheVisibleNineCharacterLimit() {
        assertEquals(80, MercenaryNamePool.NAMES.size());
        assertEquals(80, new HashSet<>(MercenaryNamePool.NAMES).size());
        for (String name : MercenaryNamePool.NAMES) {
            assertTrue(name.matches("[A-Z][a-z]{0,8}"), name);
        }
        assertThrows(UnsupportedOperationException.class,
            () -> MercenaryNamePool.NAMES.add("Extra"));
        assertThrows(UnsupportedOperationException.class,
            () -> MercenaryNamePool.REGIONS.getFirst().male().add("Extra"));
    }

    @Test void newHireSelectionReachesEveryEntryAndHandlesNegativeUuidHashes() {
        var selected = new HashSet<String>();
        for (int i = 0; i < 80; i++) {
            UUID hireId = new UUID(0, i);
            String name = MercenaryIdentity.newName(hireId);
            assertEquals(name, MercenaryIdentity.newName(UUID.fromString(hireId.toString())));
            selected.add(name);
        }
        assertEquals(Set.copyOf(MercenaryNamePool.NAMES), selected);
        for (long lowBits : new long[] {0x80000000L, 0xffffffffL}) {
            UUID hireId = new UUID(0, lowBits);
            assertTrue(hireId.hashCode() < 0);
            assertTrue(MercenaryNamePool.NAMES.contains(MercenaryIdentity.newName(hireId)));
        }
    }

    @Test void newHirePoolCannotRemapLegacyIdentities() {
        assertEquals("Aldred", MercenaryIdentity.name(new UUID(0, 0)));
        assertEquals("Susanna", MercenaryIdentity.name(new UUID(0, 99)));
        assertEquals("John", MercenaryIdentity.newName(new UUID(0, 0)));
        assertEquals("Isabel", MercenaryIdentity.newName(new UUID(0, 79)));
        assertEquals(100, MercenaryIdentity.NAMES.size());
    }
}
