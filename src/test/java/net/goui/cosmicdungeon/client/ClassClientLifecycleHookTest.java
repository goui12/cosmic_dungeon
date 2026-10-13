package net.goui.cosmicdungeon.client;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import static org.junit.jupiter.api.Assertions.*;

/** Validates the installed replacement-player event path without starting a licensed client. */
final class ClassClientLifecycleHookTest {
    private static final String EVENTS = "net/goui/cosmicdungeon/playerclass/api/ClassClientEvents";
    private static final String NETWORK_EVENT = "net/neoforged/neoforge/client/event/ClientPlayerNetworkEvent";
    private static ClassNode node(String name) throws Exception {
        try (var input = ClassClientLifecycleHookTest.class.getClassLoader().getResourceAsStream(name + ".class")) {
            assertNotNull(input, name);
            var result = new ClassNode();
            new ClassReader(input).accept(result, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return result;
        }
    }
    private static MethodNode method(ClassNode owner, String name) {
        return owner.methods.stream().filter(m -> m.name.equals(name)).findFirst().orElseThrow();
    }
    private static boolean calls(AbstractInsnNode instruction, String owner, String name) {
        return instruction instanceof MethodInsnNode call && call.owner.equals(owner) && call.name.equals(name);
    }

    @Test void loginAndEveryReplacementRequestOneAuthoritativeClassSnapshot() throws Exception {
        var events = node(EVENTS);
        for (var event : List.of("LoggingIn", "Clone")) {
            var matching = events.methods.stream()
                    .filter(m -> m.desc.equals("(L" + NETWORK_EVENT + "$" + event + ";)V")).toList();
            assertEquals(1, matching.size(), event + " must have exactly one class lifecycle listener");
            var hook = matching.getFirst();
            assertEquals(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                    hook.access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC));
            assertNotNull(hook.visibleAnnotations);
            assertTrue(hook.visibleAnnotations.stream()
                    .anyMatch(a -> a.desc.equals("Lnet/neoforged/bus/api/SubscribeEvent;")));
            int requests = 0;
            for (var instruction : hook.instructions)
                if (calls(instruction, "net/goui/cosmicdungeon/playerclass/api/ClassNet", "requestClassSync")) requests++;
            assertEquals(1, requests, "Use one existing request at the lifecycle boundary, not a frame poll");
        }
    }

    @Test void installedRespawnPathPublishesCloneAfterReplacingTheCurrentPlayer() throws Exception {
        var respawn = method(node("net/minecraft/client/multiplayer/ClientPacketListener"), "handleRespawn");
        int replacement = -1, notification = -1, index = 0;
        for (var instruction : respawn.instructions) {
            if (instruction instanceof FieldInsnNode field && field.getOpcode() == Opcodes.PUTFIELD
                    && field.owner.equals("net/minecraft/client/Minecraft") && field.name.equals("player"))
                replacement = index;
            if (calls(instruction, "net/neoforged/neoforge/client/ClientHooks", "firePlayerRespawn"))
                notification = index;
            index++;
        }
        assertTrue(replacement >= 0, "The installed client must replace LocalPlayer on respawn/dimension travel");
        assertTrue(notification > replacement, "Class refresh must run after Minecraft.player becomes the new instance");
        var callback = method(node("net/neoforged/neoforge/client/ClientHooks"), "firePlayerRespawn");
        boolean createsClone = false, postsClone = false;
        for (var instruction : callback.instructions) {
            if (instruction instanceof TypeInsnNode type && type.getOpcode() == Opcodes.NEW
                    && type.desc.equals(NETWORK_EVENT + "$Clone")) createsClone = true;
            if (instruction instanceof MethodInsnNode call && call.name.equals("post")) postsClone = true;
        }
        assertTrue(createsClone && postsClone, "The installed hook must publish the subscribed Clone event");
    }
}
