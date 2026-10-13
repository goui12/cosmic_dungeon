package net.goui.cosmicdungeon.client;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import static org.junit.jupiter.api.Assertions.*;

/** Protects real native release/drop routing for both survival and extra inventory screens. */
final class PotionInventoryBoundsHookTest {
    private static final String CONTAINER = "net/minecraft/client/gui/screens/inventory/AbstractContainerScreen";
    private static final String RECIPE_BOOK = "net/minecraft/client/gui/screens/inventory/AbstractRecipeBookScreen";
    private static ClassNode node(String name) throws Exception {
        try (var input = PotionInventoryBoundsHookTest.class.getClassLoader().getResourceAsStream(name + ".class")) {
            assertNotNull(input, name);
            var result = new ClassNode();
            new ClassReader(input).accept(result, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return result;
        }
    }
    private static Object value(AnnotationNode annotation, String key) {
        for (int i = 0; annotation.values != null && i < annotation.values.size(); i += 2)
            if (key.equals(annotation.values.get(i))) return annotation.values.get(i + 1);
        return null;
    }
    private static List<AnnotationNode> annotations(List<AnnotationNode> visible, List<AnnotationNode> invisible) {
        var result = new java.util.ArrayList<AnnotationNode>();
        if (visible != null) result.addAll(visible);
        if (invisible != null) result.addAll(invisible);
        return result;
    }
    private static MethodNode method(ClassNode owner, String name) {
        return owner.methods.stream().filter(m -> m.name.equals(name)).findFirst().orElseThrow();
    }

    @Test void installedNativeReleaseUsesBothGuardedOutsideBoundsImplementations() throws Exception {
        var targets = Set.of(CONTAINER, RECIPE_BOOK);
        var mixin = node("net/goui/cosmicdungeon/mixin/client/PotionInventoryBoundsMixin");
        var annotation = annotations(mixin.visibleAnnotations, mixin.invisibleAnnotations).stream()
                .filter(a -> a.desc.equals("Lorg/spongepowered/asm/mixin/Mixin;")).findFirst().orElseThrow();
        @SuppressWarnings("unchecked") var owners = (List<Type>)value(annotation, "value");
        assertEquals(targets, owners.stream().map(Type::getInternalName).collect(java.util.stream.Collectors.toSet()));
        for (String owner : targets) {
            var outside = method(node(owner), "hasClickedOutside");
            assertEquals("(DDII)Z", outside.desc);
            assertEquals(0, outside.access & Opcodes.ACC_STATIC);
        }
        var release = method(node(CONTAINER), "mouseReleased");
        boolean bounds = false, outsideDrop = false, cleanup = false;
        for (var instruction : release.instructions) {
            if (instruction instanceof MethodInsnNode call && call.name.equals("hasClickedOutside")) bounds = true;
            if (instruction instanceof IntInsnNode number && number.operand == -999) outsideDrop = true;
            if (instruction instanceof FieldInsnNode field && field.getOpcode() == Opcodes.PUTFIELD
                    && field.name.equals("isQuickCrafting")) cleanup = true;
        }
        assertTrue(bounds && outsideDrop && cleanup,
                "Native release must retain drag cleanup while the bounds hook prevents -999 outside drops");
    }

    @Test void hookOnlyOverridesCoveredPanelBoundsAndKeepsNativeInputSequence() throws Exception {
        var hook = method(node("net/goui/cosmicdungeon/mixin/client/PotionInventoryBoundsMixin"), "cosmicdungeon$includePotionList");
        var injection = annotations(hook.visibleAnnotations, hook.invisibleAnnotations).stream()
                .filter(a -> a.desc.equals("Lorg/spongepowered/asm/mixin/injection/Inject;")).findFirst().orElseThrow();
        assertEquals(Boolean.TRUE, value(injection, "cancellable"));
        assertEquals(List.of("hasClickedOutside"), value(injection, "method"));
        boolean panel = false, returns = false;
        for (var instruction : hook.instructions) {
            if (instruction instanceof MethodInsnNode call) {
                if (call.owner.equals("net/goui/cosmicdungeon/client/screen/crafting/PotionCraftClient")
                        && call.name.equals("ownsInput")) panel = true;
                if (call.name.equals("setReturnValue")) returns = true;
                assertNotEquals("slotClicked", call.name, "The bounds hook must not synthesize inventory transactions");
            }
        }
        assertTrue(panel && returns);
    }
}
