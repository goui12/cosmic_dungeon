package net.goui.cosmicdungeon.network;
import java.util.*;
import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.core.RegistryAccess;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
final class PotionCraftPayloadsTest {
    @Test void exactActionAndOpenCloseWireRoundTrip() {
        for (boolean shift : new boolean[] {false, true}) {
            var action = new PotionCraftPayloads.Action(32, 71, "minecraft:strong_healing_splash", shift);
            var b = Unpooled.buffer();
            try { PotionCraftPayloads.Action.STREAM_CODEC.encode(b, action); assertEquals(action, PotionCraftPayloads.Action.STREAM_CODEC.decode(b)); assertEquals(0, b.readableBytes()); }
            finally { b.release(); }
            b = Unpooled.buffer();
            try { var request = new PotionCraftPayloads.Request(shift); PotionCraftPayloads.Request.STREAM_CODEC.encode(b, request);
                assertEquals(request, PotionCraftPayloads.Request.STREAM_CODEC.decode(b)); }
            finally { b.release(); }
        }
    }
    @Test void invalidGenerationsTargetBoundsAndQuoteCountsFailBeforeAllocation() {
        assertThrows(IllegalArgumentException.class, () -> new PotionCraftPayloads.Action(0, 0, "minecraft:healing_splash", false));
        assertThrows(IllegalArgumentException.class, () -> new PotionCraftPayloads.Action(1, -1, "minecraft:healing_splash", false));
        for (var id : List.of("no namespace", "minecraft:" + "x".repeat(129), "minecraft:../bad?"))
            assertThrows(IllegalArgumentException.class, () -> new PotionCraftPayloads.Action(1, 0, id, false));
        assertThrows(IllegalArgumentException.class, () -> new PotionCraftPayloads.View(1, 1, "judicator", 0, 20, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new PotionCraftPayloads.View(0, 1, "theurgist", 0, 20, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new PotionCraftPayloads.View(1, 1, "theurgist", 20, 20, List.of()));
        var raw = Unpooled.buffer();
        try {
            var b = new RegistryFriendlyByteBuf(raw, RegistryAccess.EMPTY);
            b.writeVarLong(1); b.writeVarLong(1); b.writeUtf("theurgist"); b.writeVarInt(0); b.writeVarInt(20); b.writeVarInt(Integer.MAX_VALUE);
            assertThrows(IllegalArgumentException.class, () -> PotionCraftPayloads.View.STREAM_CODEC.decode(b));
        } finally { raw.release(); }
    }
    @Test void inactiveViewWireRequiresNoRegistryItems() {
        var raw = Unpooled.buffer();
        try { var b = new RegistryFriendlyByteBuf(raw, RegistryAccess.EMPTY); var empty = PotionCraftPayloads.View.empty(91);
            PotionCraftPayloads.View.STREAM_CODEC.encode(b, empty); assertEquals(empty, PotionCraftPayloads.View.STREAM_CODEC.decode(b)); }
        finally { raw.release(); }
    }
}
