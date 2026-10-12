package net.goui.cosmicdungeon.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import static org.junit.jupiter.api.Assertions.*;

/** Checks installed native input paths and mixin signatures without creating a client or world. */
final class InventoryCraftingHookTest {
    private static final String SCREEN = "net/minecraft/client/gui/screens/inventory/AbstractContainerScreen";
    private static ClassNode node(String name) throws Exception {
        try (var input = InventoryCraftingHookTest.class.getClassLoader().getResourceAsStream(name + ".class")) {
            assertNotNull(input, name);
            var result = new ClassNode();
            new ClassReader(input).accept(result, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return result;
        }
    }
    private static Object value(AnnotationNode annotation, String key) {
        if (annotation.values != null)
            for (int i = 0; i < annotation.values.size(); i += 2)
                if (key.equals(annotation.values.get(i))) return annotation.values.get(i + 1);
        return null;
    }
    private static MethodNode method(ClassNode owner, String name) {
        return owner.methods.stream().filter(m -> m.name.equals(name)).findFirst().orElseThrow();
    }
    private static boolean invokes(MethodNode method, String name) {
        for (var instruction : method.instructions)
            if (instruction instanceof MethodInsnNode call && call.owner.equals(SCREEN) && call.name.equals(name))
                return true;
        return false;
    }

    @Test void installedNativeMethodsMatchEveryCancellableInjection() throws Exception {
        var nativeScreen = node(SCREEN);
        var mixin = node("net/goui/cosmicdungeon/mixin/client/InventoryCraftingSlotsMixin");
        int hooks = 0;
        for (var callback : mixin.methods) {
            var annotations = new ArrayList<AnnotationNode>();
            if (callback.visibleAnnotations != null) annotations.addAll(callback.visibleAnnotations);
            if (callback.invisibleAnnotations != null) annotations.addAll(callback.invisibleAnnotations);
            for (var annotation : annotations) {
                if (!annotation.desc.equals("Lorg/spongepowered/asm/mixin/injection/Inject;")) continue;
                hooks++;
                assertEquals(Boolean.TRUE, value(annotation, "cancellable"));
                @SuppressWarnings("unchecked") var targets = (List<String>)value(annotation, "method");
                assertEquals(1, targets.size());
                String target = targets.getFirst();
                var matches = nativeScreen.methods.stream()
                        .filter(m -> target.equals(m.name) || target.equals(m.name + m.desc)).toList();
                assertEquals(1, matches.size(), "Installed native target must be unambiguous: " + target);
                var nativeMethod = matches.getFirst();
                assertEquals(nativeMethod.access & Opcodes.ACC_STATIC, callback.access & Opcodes.ACC_STATIC);
                var expected = Type.getArgumentTypes(nativeMethod.desc);
                var actual = Type.getArgumentTypes(callback.desc);
                assertEquals(expected.length + 1, actual.length, callback.name);
                for (int i = 0; i < expected.length; i++) assertEquals(expected[i], actual[i], callback.name);
                String info = Type.getReturnType(nativeMethod.desc).equals(Type.BOOLEAN_TYPE)
                        ? "org/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable"
                        : "org/spongepowered/asm/mixin/injection/callback/CallbackInfo";
                assertEquals(info, actual[actual.length - 1].getInternalName(), callback.name);
            }
        }
        assertEquals(3, hooks);
    }

    @Test void nativePointerKeyboardAndRenderingPathsReachTheGuardedMethods() throws Exception {
        var screen = node(SCREEN);
        assertTrue(invokes(method(screen, "getHoveredSlot"), "isHovering"));
        for (String input : List.of("mouseClicked", "mouseDragged", "mouseReleased"))
            assertTrue(invokes(method(screen, input), "getHoveredSlot"), input + " must use guarded hit testing");
        for (String input : List.of("keyPressed", "checkHotbarKeyPressed", "checkHotbarMouseClicked"))
            assertTrue(invokes(method(screen, input), "slotClicked"), input + " must use guarded dispatch");
        assertTrue(invokes(method(screen, "render"), "renderContents"));
        assertTrue(invokes(method(screen, "renderContents"), "getHoveredSlot"));
        assertTrue(invokes(method(screen, "renderSlots"), "renderSlot"));
    }

    @Test void scopeExcludesCreativePickersAndOtherContainerScreens() throws Exception {
        var helper = node("net/goui/cosmicdungeon/client/InventoryCraftingPresentation");
        var acceptedTypes = new java.util.HashSet<String>();
        for (var instruction : method(helper, "hidden").instructions)
            if (instruction instanceof TypeInsnNode type && type.getOpcode() == Opcodes.INSTANCEOF)
                acceptedTypes.add(type.desc);
        assertEquals(Set.of("net/minecraft/client/gui/screens/inventory/InventoryScreen",
                "net/goui/cosmicdungeon/playerclass/api/ExtraInventoryScreen",
                "net/minecraft/world/inventory/AbstractCraftingMenu"), acceptedTypes);
    }
}
