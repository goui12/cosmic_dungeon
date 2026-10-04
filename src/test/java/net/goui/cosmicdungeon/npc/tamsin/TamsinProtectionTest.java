package net.goui.cosmicdungeon.npc.tamsin;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.neoforged.neoforge.event.entity.EntityInvulnerabilityCheckEvent;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public final class TamsinProtectionTest {
    @Test
    public void immunityOverridesVanillaBypassAndRetainsOriginalEventEvidence() {
        var protection = new TamsinProtection(entity -> true);
        // A bypassing source reaches this event with vanilla immunity=false. The listener must
        // protect without consulting source tags, damage values, attacker type, or the NBT flag.
        var source = new DamageSource(Holder.direct(new DamageType("unlisted_damage", 0)));
        var event = new EntityInvulnerabilityCheckEvent(null, source, false);
        protection.protect(event);
        assertTrue(event.isInvulnerable());
        assertFalse(event.getOriginalInvulnerability());
        assertSame(source, event.getSource());
    }

    @Test
    public void unrelatedEntitiesAndExistingImmunityAreUnchanged() {
        var unrelated = new TamsinProtection(entity -> false);
        var source = new DamageSource(Holder.direct(new DamageType("test_damage", 0)));
        var vulnerable = new EntityInvulnerabilityCheckEvent(null, source, false);
        unrelated.protect(vulnerable);
        assertFalse(vulnerable.isInvulnerable());

        AtomicInteger lookups = new AtomicInteger();
        var protection = new TamsinProtection(entity -> { lookups.incrementAndGet(); return false; });
        var immune = new EntityInvulnerabilityCheckEvent(null, source, true);
        protection.protect(immune);
        assertTrue(immune.isInvulnerable());
        assertEquals(0, lookups.get(), "Already immune entities need no binding lookup");
    }

    @Test
    public void onlyCurrentOrLegacyBoundIdentityIsProtectedWithoutRewritingSaves() throws Exception {
        var constructor = TamsinData.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        var data = constructor.newInstance();
        var protection = new TamsinProtection(entity -> false);
        UUID current = UUID.randomUUID(), retired = UUID.randomUUID(), stranger = UUID.randomUUID();
        var binding = new TamsinData.Binding("minecraft:overworld", 0);
        data.bind(current, binding);
        data.bind(retired, binding);
        data.accept(stranger);
        data.setDirty(false);

        assertTrue(protection.matchesBinding(data, current, current.toString()));
        assertTrue(protection.matchesBinding(data, current, null), "Existing legacy NPC requires no replacement");
        assertFalse(protection.matchesBinding(data, retired, current.toString()));
        assertFalse(protection.matchesBinding(data, current, ""), "Explicit retirement stays effective");
        assertFalse(protection.matchesBinding(data, stranger, stranger.toString()));
        assertFalse(protection.matchesBinding(data, stranger, null));
        data.unbind(current);
        data.setDirty(false);
        assertFalse(protection.matchesBinding(data, current, current.toString()));
        assertTrue(data.accepted(stranger));
        assertFalse(data.isDirty(), "Protection reads never dirty persistent data");
    }
}
