package net.goui.cosmicdungeon.npc.tamsin;

import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import java.util.UUID;
import static net.goui.cosmicdungeon.npc.tamsin.TamsinFlow.Stage.*;

public final class TamsinChecks {
    private static int checks;
    private static void check(boolean value, String label) { checks++; if (!value) throw new AssertionError(label); }
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        check(TamsinFlow.initial(false, false) == AGREEMENT, "First visit requires agreement");
        check(TamsinFlow.initial(false, true) == AGREEMENT, "Legacy class does not imply agreement");
        check(TamsinFlow.initial(true, false) == SELECTOR, "Closing map retains agreement");
        check(TamsinFlow.initial(true, true) == READY, "Returning player skips class selection");
        check(TamsinFlow.advance(AGREEMENT, "no", false) == AGREEMENT, "No never advances acceptance");
        check(TamsinFlow.advance(AGREEMENT, "continue", false) == AGREEMENT, "Cannot skip agreement");
        check(TamsinFlow.advance(AGREEMENT, "yes", false) == MAP, "Yes shows map first");
        check(TamsinFlow.advance(MAP, "yes", false) == MAP, "Duplicate Yes cannot skip map");
        check(TamsinFlow.advance(MAP, "continue", false) == SELECTOR, "Continue opens six-class selector");
        check(TamsinFlow.advance(MAP, "continue", true) == READY, "Legacy selected class is preserved");
        check(TamsinFlow.advance(SELECTOR, "yes", false) == SELECTOR, "Stale Yes cannot change class stage");
        check(TamsinFlow.advance(READY, "continue", true) == READY, "Stale Continue is harmless");
        for (var stage : TamsinFlow.Stage.values()) {
            check(!TamsinFlow.canSelect(false, stage) && !TamsinFlow.canReady(false, stage), "Unaccepted gates " + stage);
        }
        check(!TamsinFlow.canSelect(true, MAP) && !TamsinFlow.canReady(true, MAP), "Map gates class and entry");
        check(TamsinFlow.canSelect(true, SELECTOR) && TamsinFlow.canReady(true, SELECTOR), "Accepted selector enabled");
        check(!TamsinFlow.canSelect(true, READY) && TamsinFlow.canReady(true, READY), "Returning NPC flow cannot switch class");
        var field = TamsinData.class.getDeclaredField("CODEC"); field.setAccessible(true);
        var codec = (Codec<TamsinData>) field.get(null);
        var empty = codec.parse(JsonOps.INSTANCE, JsonParser.parseString("{}")).getOrThrow();
        UUID player = new UUID(0, 1), other = new UUID(0, 2), npc = new UUID(0, 3);
        check(!empty.accepted(player) && empty.bindingCount() == 0, "Absent optional data defaults safely");
        check(empty.accept(player), "First Yes is recorded");
        check(!empty.accept(player), "Repeated acceptance is idempotent");
        var binding = new TamsinData.Binding("minecraft:overworld", 0L);
        empty.bind(npc, binding);
        var encoded = codec.encodeStart(JsonOps.INSTANCE, empty).getOrThrow();
        var restored = codec.parse(JsonOps.INSTANCE, encoded).getOrThrow();
        check(restored.accepted(player) && !restored.accepted(other), "Acceptance survives restart per UUID");
        check(binding.equals(restored.binding(npc)), "NPC identity and origin selector survive restart");
        check(restored.unbind(npc) && restored.binding(npc) == null, "Unbinding invalidates existing binding");
        check(restored.accepted(player), "Unbinding never erases agreements");
        check(!restored.unbind(npc), "Repeated unbind is harmless");
        var action = new net.goui.cosmicdungeon.network.ClassPayloads.C2S_TamsinAction(17, "yes");
        var buffer = io.netty.buffer.Unpooled.buffer();
        try {
            net.goui.cosmicdungeon.network.ClassPayloads.C2S_TamsinAction.STREAM_CODEC.encode(buffer, action);
            check(action.equals(net.goui.cosmicdungeon.network.ClassPayloads.C2S_TamsinAction.STREAM_CODEC.decode(buffer)),
                    "Action codec preserves container and decision");
        } finally { buffer.release(); }
        var selector = new net.goui.cosmicdungeon.network.ClassPayloads.S2C_SelectorData(17, "MAP", "none", java.util.List.of("bogatyr"));
        buffer = io.netty.buffer.Unpooled.buffer();
        try {
            net.goui.cosmicdungeon.network.ClassPayloads.S2C_SelectorData.STREAM_CODEC.encode(buffer, selector);
            check(selector.equals(net.goui.cosmicdungeon.network.ClassPayloads.S2C_SelectorData.STREAM_CODEC.decode(buffer)),
                    "Selector codec preserves server stage and container");
        } finally { buffer.release(); }
        var select = new net.goui.cosmicdungeon.network.ClassPayloads.C2S_SelectClass(17, "bogatyr");
        buffer = io.netty.buffer.Unpooled.buffer();
        try {
            net.goui.cosmicdungeon.network.ClassPayloads.C2S_SelectClass.STREAM_CODEC.encode(buffer, select);
            check(select.equals(net.goui.cosmicdungeon.network.ClassPayloads.C2S_SelectClass.STREAM_CODEC.decode(buffer)),
                    "Class choice binds the requesting container");
        } finally { buffer.release(); }
        var ready = new net.goui.cosmicdungeon.network.PartyPayloads.Action(17, 9, "ready", "");
        buffer = io.netty.buffer.Unpooled.buffer();
        try {
            net.goui.cosmicdungeon.network.PartyPayloads.Action.STREAM_CODEC.encode(buffer, ready);
            check(ready.equals(net.goui.cosmicdungeon.network.PartyPayloads.Action.STREAM_CODEC.decode(buffer)),
                    "Ready binds the requesting container and ready-check revision");
        } finally { buffer.release(); }
        System.out.println(checks + " Tamsin flow/save/packet checks passed");
    }
}
