package net.goui.cosmicdungeon.dungeon;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DungeonKillCreditTest {
    @Test void untouchedEnvironmentalDeathHasNoCredit() {
        assertNull(DungeonKillCredit.resolve(new CompoundTag(), null, false));
    }
    @Test void environmentalDeathUsesLastPlayerAndDirectKillSupersedesIt() {
        UUID earlier = UUID.randomUUID(), later = UUID.randomUUID();
        var tag = new CompoundTag();
        tag.putString(DungeonKillCredit.LAST_PLAYER, earlier.toString());
        assertEquals(earlier, DungeonKillCredit.resolve(tag.copy(), null, false));
        assertEquals(later, DungeonKillCredit.resolve(tag, later, false));
        tag.putString(DungeonKillCredit.LAST_PLAYER, later.toString());
        assertEquals(later, DungeonKillCredit.resolve(tag.copy(), null, false));
    }
    @Test void administrativeOrCorruptAttributionCannotGenerateRewards() {
        var tag = new CompoundTag();
        tag.putString(DungeonKillCredit.LAST_PLAYER, UUID.randomUUID().toString());
        assertNull(DungeonKillCredit.resolve(tag, UUID.randomUUID(), true));
        tag.putString(DungeonKillCredit.LAST_PLAYER, "invalid");
        assertNull(DungeonKillCredit.resolve(tag, null, false));
    }
}
