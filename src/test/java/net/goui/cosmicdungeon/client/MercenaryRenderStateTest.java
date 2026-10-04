package net.goui.cosmicdungeon.client;

import java.util.*;
import net.goui.cosmicdungeon.client.render.MercenaryRenderState;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class MercenaryRenderStateTest {
    @Test void identityUsesAllNineBundledWideSkinsAndIsStable() {
        var state = new MercenaryRenderState();
        var found = new HashSet<ResourceLocation>();
        for (int i = 0; i < 9; i++) {
            UUID id = new UUID(0, i);
            state.captureIdentity(id); var first = state.texture();
            state.captureIdentity(new UUID(Long.MIN_VALUE, Long.MAX_VALUE));
            state.captureIdentity(UUID.fromString(id.toString()));
            assertEquals(first, state.texture());
            assertEquals("minecraft", first.getNamespace());
            assertTrue(first.getPath().startsWith("textures/entity/player/wide/"));
            found.add(first);
        }
        assertEquals(9, found.size());
    }
    @Test void reusedStateClearsPreviousHireWithoutChangingOtherSubmittedStates() {
        var first = new MercenaryRenderState(); var second = new MercenaryRenderState();
        first.captureIdentity(new UUID(0, 0)); var captured = first.texture();
        second.captureIdentity(new UUID(0, 1)); assertNotEquals(captured, second.texture());
        second.captureIdentity(new UUID(0, 2));
        assertEquals(captured, first.texture());
        first.captureIdentity(new UUID(0, 2)); assertEquals(second.texture(), first.texture());
    }
}
