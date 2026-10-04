package net.goui.cosmicdungeon.npc.tamsin;

import java.util.UUID;
import net.minecraft.core.RegistryAccess;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.neoforge.attachment.AttachmentHolder;
import net.neoforged.neoforge.attachment.AttachmentType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public final class TamsinAppearanceTest {
    @Test
    public void attachmentReadsDoNotAllocateAndOnlyChangesSync() {
        var appearance = TamsinAppearance.INSTANCE;
        var holder = new CountingHolder();
        assertFalse(appearance.isApplied(holder));
        appearance.apply(holder, false);
        assertFalse(holder.hasAttachments());
        assertEquals(0, holder.syncs);

        appearance.apply(holder, true);
        assertTrue(appearance.isApplied(holder));
        assertEquals(1, holder.syncs);
        appearance.apply(holder, true);
        assertEquals(1, holder.syncs, "Unchanged visuals must not emit another sync");

        appearance.apply(holder, false);
        assertFalse(appearance.isApplied(holder));
        assertFalse(holder.hasAttachments());
        assertEquals(2, holder.syncs, "Unbinding must sync removal");
        appearance.apply(holder, false);
        assertEquals(2, holder.syncs);
    }

    @Test
    public void appearanceIsDerivedAndNeverWrittenToEntitySaves() {
        var holder = new CountingHolder();
        TamsinAppearance.INSTANCE.apply(holder, true);
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, RegistryAccess.EMPTY);
        holder.serializeAttachments(output);
        assertTrue(output.buildResult().isEmpty(), "The existing UUID binding owns persistence");
    }

    @Test
    public void onlyCurrentOrLegacyBindingSelectsAppearance() throws Exception {
        var constructor = TamsinData.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        var data = constructor.newInstance();
        UUID current = UUID.randomUUID(), retired = UUID.randomUUID(), stranger = UUID.randomUUID();
        var binding = new TamsinData.Binding("minecraft:overworld", 0);
        data.bind(current, binding);
        data.bind(retired, binding);
        data.accept(stranger);
        data.setDirty(false);
        var appearance = TamsinAppearance.INSTANCE;

        assertTrue(appearance.matchesBinding(data, current, current.toString()));
        assertTrue(appearance.matchesBinding(data, current, null));
        assertFalse(appearance.matchesBinding(data, retired, current.toString()));
        assertFalse(appearance.matchesBinding(data, current, ""));
        assertFalse(appearance.matchesBinding(data, stranger, stranger.toString()));
        assertFalse(appearance.matchesBinding(data, stranger, null));
        assertFalse(data.isDirty());
        data.unbind(current);
        data.setDirty(false);
        assertFalse(appearance.matchesBinding(data, current, current.toString()));
        assertTrue(data.accepted(stranger));
        assertFalse(data.isDirty());
    }

    private static final class CountingHolder extends AttachmentHolder {
        private int syncs;

        @Override
        public void syncData(AttachmentType<?> type) {
            syncs++;
        }
    }
}
