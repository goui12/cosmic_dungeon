package net.goui.cosmicdungeon.client.screen;

import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import static org.junit.jupiter.api.Assertions.*;

final class InventoryThemeTest {
    @Test void everyActiveClassHasBoundedArtOriginalEmblemAndGuide() throws Exception {
        int bytes = 0;
        for (String id : List.of("bogatyr", "dragoon", "judicator", "pyroclast", "theurgist", "venefex")) {
            var theme = InventoryTheme.forClass(id);
            assertTrue(theme.illustrated());
            assertEquals("class." + id, HelpMenuContent.classGuide(id).id());
            String base = "/assets/cosmicdungeon/";
            try (var input = getClass().getResourceAsStream(base + theme.artPath())) {
                assertNotNull(input, id);
                byte[] png = input.readAllBytes(); bytes += png.length;
                var image = ImageIO.read(new java.io.ByteArrayInputStream(png));
                assertEquals(256, image.getWidth()); assertEquals(256, image.getHeight());
            }
            try (var input = getClass().getResourceAsStream(base + theme.emblemPath())) {
                assertNotNull(input, id);
                var image = ImageIO.read(input);
                assertEquals(32, image.getWidth()); assertEquals(32, image.getHeight());
            }
        }
        assertTrue(bytes <= 512 * 1024, "Six packaged illustrations stay within512KiB");
    }

    @Test void unknownAndLegacyClassesUseSafePresentationFallbacks() {
        for (String id : new String[]{null, "", "unknown", "none"}) {
            var theme = InventoryTheme.forClass(id);
            assertFalse(theme.hasClass()); assertFalse(theme.illustrated());
        }
        for (String id : List.of("deadeye", "metalmancer")) {
            var theme = InventoryTheme.forClass(id);
            assertTrue(theme.hasClass()); assertFalse(theme.illustrated());
            assertEquals(id, theme.classId());
        }
    }

    @Test void installedInventoryBackgroundRedirectMatchesOneBlitAndKeepsAvatar() throws Exception {
        String nativeName = "net/minecraft/client/gui/screens/inventory/InventoryScreen";
        ClassNode nativeScreen = node(nativeName);
        var render = nativeScreen.methods.stream().filter(m -> m.name.equals("renderBg")).findFirst().orElseThrow();
        int matchingBlits = 0, avatars = 0;
        for (var instruction : render.instructions) {
            if (!(instruction instanceof MethodInsnNode call)) continue;
            if (call.owner.equals("net/minecraft/client/gui/GuiGraphics") && call.name.equals("blit")
                    && call.desc.equals("(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/ResourceLocation;IIFFIIII)V"))
                matchingBlits++;
            if (call.name.equals("renderEntityInInventoryFollowsMouse")) avatars++;
        }
        assertEquals(1, matchingBlits, "Background redirect must match exactly one native blit");
        assertEquals(1, avatars, "Avatar stays in the unmodified native render path");
        var mixin = node("net/goui/cosmicdungeon/mixin/client/ClassInventoryScreenMixin");
        var hook = mixin.methods.stream().filter(m -> m.name.equals("cosmicdungeon$theme")).findFirst().orElseThrow();
        assertEquals("(Lnet/minecraft/client/gui/GuiGraphics;Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/ResourceLocation;IIFFIIII)V", hook.desc);
    }
    @Test void nativeTouchscreenDragAccessorMatchesInstalledPrivateStack() throws Exception {
        var screen = node("net/minecraft/client/gui/screens/inventory/AbstractContainerScreen");
        var field = screen.fields.stream().filter(f -> f.name.equals("draggingItem")).findFirst().orElseThrow();
        assertEquals("Lnet/minecraft/world/item/ItemStack;", field.desc);
        assertTrue((field.access & Opcodes.ACC_PRIVATE) != 0);
        var accessor = node("net/goui/cosmicdungeon/mixin/client/InventoryDragAccess");
        var method = accessor.methods.stream().filter(m -> m.name.equals("cosmicdungeon$draggingItem")).findFirst().orElseThrow();
        assertEquals("()" + field.desc, method.desc);
    }
    private static ClassNode node(String name) throws Exception {
        try (var input = InventoryThemeTest.class.getClassLoader().getResourceAsStream(name + ".class")) {
            assertNotNull(input);
            var node = new ClassNode();
            new ClassReader(input).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return node;
        }
    }
}
