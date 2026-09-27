package net.goui.cosmicdungeon.block.entity;

import java.util.UUID;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ClassChestOwnershipTest {
    private static ClassChestOwnership read(CompoundTag tag) {
        return ClassChestOwnership.load(TagValueInput.create(ProblemReporter.DISCARDING, RegistryAccess.EMPTY, tag));
    }

    @Test void uuidControlsAccessRegardlessOfDisplayName() {
        UUID first = UUID.randomUUID(), other = UUID.randomUUID();
        var owner = new ClassChestOwnership(first, "PlayerOne");
        assertTrue(owner.permits(first));
        assertFalse(owner.permits(other));
        assertFalse(owner.permits(null));
        assertTrue(new ClassChestOwnership(first, "RenamedPlayer").permits(first));
        assertFalse(new ClassChestOwnership(other, "PlayerOne").permits(first));
    }

    @Test void oldUnboundChestsRemainUnbound() {
        var old = new CompoundTag();
        old.putInt("UnrelatedField", 42);
        assertNull(read(old));
        assertEquals(42, old.getIntOr("UnrelatedField", 0));
    }

    @Test void assignmentRoundTripsWithoutInventoryFields() {
        var owner = new ClassChestOwnership(UUID.randomUUID(), "PlayerOne");
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, RegistryAccess.EMPTY);
        owner.save(output);
        var tag = output.buildResult();
        assertEquals(java.util.Set.of(ClassChestOwnership.TAG), tag.keySet());
        assertEquals(owner, read(tag));
        assertTrue(read(tag).permits(owner.playerId()));
    }

    @Test void malformedOrEmptyOwnershipStaysLockedOnResave() {
        for (boolean wrongType : new boolean[]{false, true}) {
            var tag = new CompoundTag();
            if (wrongType) tag.putString(ClassChestOwnership.TAG, "broken");
            else {
                var owner = new CompoundTag();
                owner.putString("uuid", "not-a-uuid");
                owner.putString("name", "PlayerOne");
                tag.put(ClassChestOwnership.TAG, owner);
            }
            var locked = read(tag);
            assertNotNull(locked);
            assertFalse(locked.permits(UUID.randomUUID()));
            var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, RegistryAccess.EMPTY);
            locked.save(output);
            assertNotNull(read(output.buildResult()));
            assertFalse(read(output.buildResult()).permits(UUID.randomUUID()));
        }
    }
}
