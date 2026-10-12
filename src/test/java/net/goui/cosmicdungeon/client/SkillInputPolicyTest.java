package net.goui.cosmicdungeon.client;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import net.goui.cosmicdungeon.client.SkillInputPolicy.*;
import net.goui.cosmicdungeon.client.screen.skills.BogatyrClient;
import net.goui.cosmicdungeon.client.screen.skills.TheurgistClient;
import net.goui.cosmicdungeon.network.BogatyrPayloads;
import net.goui.cosmicdungeon.network.TheurgistPayloads;
import net.goui.cosmicdungeon.playerclass.bogatyr.WolfMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class SkillInputPolicyTest {
    private static Input key(int code) { return new Input(Device.KEY, code); }
    private static Binding bind(Action action, int code, Modifier modifier) {
        return new Binding(action, key(code), modifier, false);
    }
    @AfterEach void cleanup() { BogatyrClient.clear(); TheurgistClient.clear(); }
    @Test void defaultsAreClassScopedAndPotionCraftingHasNoBinding() {
        var bindings = java.util.Arrays.stream(Action.values())
                .map(a -> bind(a, a.defaultKey, Modifier.ALT)).toList();
        assertEquals(4, bindings.size());
        for (var b : bindings) assertEquals(b.action(), SkillInputPolicy.resolve(b.action().classId, bindings,
                b.input(), Modifier.ALT, false));
        assertNull(SkillInputPolicy.resolve("venefex", bindings, key(49), Modifier.ALT, false));
        assertNull(SkillInputPolicy.resolve("theurgist", bindings, key(51), Modifier.ALT, false));
        assertNull(Action.forId("theurgist_craft")); assertNull(Action.forId("guide"));
        assertEquals(Action.RESURRECT, Action.forId("theurgist_resurrect:member:death"));
    }
    @Test void rebindUnbindAndModifiersUseTheCurrentSnapshot() {
        var original = bind(Action.FEED_PACK, 51, Modifier.ALT);
        var custom = bind(Action.FEED_PACK, 65, Modifier.CONTROL);
        assertTrue(SkillInputPolicy.matches(original, key(51), Modifier.ALT));
        assertNull(SkillInputPolicy.resolve("bogatyr", List.of(custom), key(51), Modifier.ALT, false));
        assertEquals(Action.FEED_PACK, SkillInputPolicy.resolve("bogatyr", List.of(custom), key(65), Modifier.CONTROL, false));
        for (var wrong : List.of(Modifier.NONE, Modifier.ALT, Modifier.SHIFT))
            assertFalse(SkillInputPolicy.matches(custom, key(65), wrong));
        assertFalse(SkillInputPolicy.matches(custom, key(65), null), "Multi-modifier ambiguity must not trigger");
        assertFalse(SkillInputPolicy.matches(new Binding(Action.FEED_PACK, key(65), Modifier.CONTROL, true), key(65), Modifier.CONTROL));
        assertTrue(SkillInputPolicy.matches(bind(Action.PATROL, 257, Modifier.NONE), key(257), Modifier.NONE));
    }
    @Test void platformControlAndLockFlagsNeverTurnUnsupportedChordsIntoBareKeys() {
        assertEquals(Modifier.CONTROL,SkillInputPolicy.modifier(2,2));
        assertEquals(Modifier.CONTROL,SkillInputPolicy.modifier(8,8));
        assertNull(SkillInputPolicy.modifier(2,8),"Physical macOS Ctrl must not become NONE");
        assertNull(SkillInputPolicy.modifier(6,8),"Ctrl+Alt must not become plain Alt");
        assertNull(SkillInputPolicy.modifier(8,2),"Windows/Super must not become NONE");
        assertNull(SkillInputPolicy.modifier(3,2));
        assertEquals(Modifier.ALT,SkillInputPolicy.modifier(4|16|32,2));
        assertEquals(Modifier.NONE,SkillInputPolicy.modifier(16|32,2));
    }
    @Test void conflictsNeverPickAnArbitraryActionButDifferentClassesCanReuseKeys() {
        var patrol = bind(Action.PATROL, 80, Modifier.NONE);
        var stay = bind(Action.STAY, 80, Modifier.NONE);
        var resurrect = bind(Action.RESURRECT, 80, Modifier.NONE);
        assertNull(SkillInputPolicy.resolve("bogatyr", List.of(patrol, stay), key(80), Modifier.NONE, false));
        assertNull(SkillInputPolicy.resolve("bogatyr", List.of(patrol), key(80), Modifier.NONE, true));
        assertEquals(Action.PATROL, SkillInputPolicy.resolve("bogatyr", List.of(patrol, resurrect), key(80), Modifier.NONE, false));
        assertEquals(Action.RESURRECT, SkillInputPolicy.resolve("theurgist", List.of(patrol, resurrect), key(80), Modifier.NONE, false));
    }
    @Test void keyboardScancodeAndMouseCodesCannotAlias() {
        var input = new Input(Device.MOUSE, 4);
        var binding = new Binding(Action.STAY, input, Modifier.NONE, false);
        assertTrue(SkillInputPolicy.matches(binding, input, Modifier.NONE));
        assertFalse(SkillInputPolicy.matches(binding, key(4), Modifier.NONE));
        assertFalse(SkillInputPolicy.matches(binding, new Input(Device.SCANCODE, 4), Modifier.NONE));
    }
    @Test void everyUnsafeContextBlocksAndOrdinaryInventoryOrWorldIsAllowed() {
        assertTrue(SkillInputPolicy.allowed(new Context(true, true, false, true, false, true, false, false, true)));
        // These represent disconnect, death, spectator, focus loss, overlay, modal, carried/dragged item, typing, stale menu.
        for (var context : List.of(
                new Context(false,true,false,true,false,true,false,false,true),
                new Context(true,false,false,true,false,true,false,false,true),
                new Context(true,true,true,true,false,true,false,false,true),
                new Context(true,true,false,false,false,true,false,false,true),
                new Context(true,true,false,true,true,true,false,false,true),
                new Context(true,true,false,true,false,false,false,false,true),
                new Context(true,true,false,true,false,true,true,false,true),
                new Context(true,true,false,true,false,true,false,true,true),
                new Context(true,true,false,true,false,true,false,false,false)))
            assertFalse(SkillInputPolicy.allowed(context));
    }
    @Test void repeatAndScreenTransitionsDoNotCreateAnotherPressOrQueueAnAction() {
        var presses = new Presses();
        assertTrue(presses.press(key(49)));
        // The same physical press remains held when chat/picker/inventory opens or closes.
        assertFalse(presses.press(key(49))); assertFalse(presses.press(key(49)));
        assertTrue(presses.press(key(50)));
        presses.release(key(49)); assertTrue(presses.press(key(49)));
        assertFalse(presses.press(key(50)));
        presses.clear(); assertTrue(presses.press(key(50)));
    }
    @Test void shortcutDispatchUsesBogatyrCapabilitiesAndNeverOverridesPendingOrSelectedMode() {
        var modes = new ArrayList<BogatyrPayloads.ModeAction>();
        var food = new ArrayList<BogatyrPayloads.Action>();
        BogatyrClient.clear(); BogatyrClient.modeActions(modes::add); BogatyrClient.actions(food::add);
        var closed = new AtomicInteger();
        assertFalse(SkillInputPolicy.dispatch(Action.FEED_PACK,"bogatyr",BogatyrClient::activate,()->false,closed::incrementAndGet));
        BogatyrClient.accept(new BogatyrPayloads.View(23,7,2,java.util.Collections.nCopies(4,BogatyrPayloads.Quote.empty()),WolfMode.PATROL,true,3));
        assertFalse(SkillInputPolicy.dispatch(Action.PATROL,"bogatyr",BogatyrClient::activate,()->false,closed::incrementAndGet));
        assertFalse(SkillInputPolicy.dispatch(Action.STAY,"theurgist",BogatyrClient::activate,()->false,closed::incrementAndGet));
        assertTrue(SkillInputPolicy.dispatch(Action.STAY,"bogatyr",BogatyrClient::activate,()->false,closed::incrementAndGet));
        assertEquals(List.of(new BogatyrPayloads.ModeAction(23,7,WolfMode.STAY)),modes);
        assertFalse(SkillInputPolicy.dispatch(Action.FEED_PACK,"bogatyr",BogatyrClient::activate,()->false,closed::incrementAndGet));
        BogatyrClient.accept(new BogatyrPayloads.View(23,8,2,java.util.Collections.nCopies(4,BogatyrPayloads.Quote.empty()),WolfMode.STAY,true,3));
        assertTrue(SkillInputPolicy.dispatch(Action.FEED_PACK,"bogatyr",BogatyrClient::activate,()->false,closed::incrementAndGet));
        assertEquals(List.of(new BogatyrPayloads.Action(23,8,BogatyrPayloads.Kind.FEED_PACK)),food);
        assertEquals(0,closed.get());
    }
    @Test void resurrectionBindingOnlyOpensEligiblePickerAndCannotCastThroughCooldown() {
        var sent = new ArrayList<TheurgistPayloads.Action>(); var opened = new AtomicInteger();
        TheurgistClient.clear(); TheurgistClient.actions(sent::add);
        var target = new TheurgistPayloads.Target(UUID.randomUUID(),"Teammate",UUID.randomUUID(),false);
        assertFalse(SkillInputPolicy.dispatch(Action.RESURRECT,"theurgist",x->fail("Wrong service"),TheurgistClient::canOpenPicker,opened::incrementAndGet));
        TheurgistClient.receive(new TheurgistPayloads.View(23,10,true,true,false,false,List.of(target),null,
                new TheurgistPayloads.Resurrection("COOLDOWN",180),List.of()));
        assertTrue(SkillInputPolicy.dispatch(Action.RESURRECT,"theurgist",x->fail("Wrong service"),TheurgistClient::canOpenPicker,opened::incrementAndGet));
        assertEquals(1,opened.get()); assertTrue(sent.isEmpty());
        assertFalse(TheurgistClient.activate(TheurgistClient.actionId(target)));
        assertFalse(SkillInputPolicy.dispatch(Action.RESURRECT,"bogatyr",x->fail("Wrong service"),TheurgistClient::canOpenPicker,opened::incrementAndGet));
        TheurgistClient.clear();
        assertFalse(SkillInputPolicy.dispatch(Action.RESURRECT,"theurgist",x->fail("Wrong service"),TheurgistClient::canOpenPicker,opened::incrementAndGet));
        assertEquals(1,opened.get()); assertTrue(sent.isEmpty());
    }
}
