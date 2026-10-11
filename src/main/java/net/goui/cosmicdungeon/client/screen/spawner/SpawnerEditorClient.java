package net.goui.cosmicdungeon.client.screen.spawner;

import java.util.function.Consumer;
import net.goui.cosmicdungeon.network.SpawnerEditorPayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Client session ownership. Only explicit Apply/Cancel emits a server request. */
public final class SpawnerEditorClient {
    private static Consumer<SpawnerEditorPayloads.Request> sender;
    private static SpawnerEditorScreen screen;
    private SpawnerEditorClient() {}
    public static void actions(Consumer<SpawnerEditorPayloads.Request> value) { sender = value; }
    public static void receive(SpawnerEditorPayloads.View view) {
        Minecraft mc = Minecraft.getInstance();
        if (view.status() == SpawnerEditorPayloads.Status.OPEN) {
            if (mc.player == null || mc.level == null) return;
            if (screen != null && screen.session().equals(view.session())) return;
            screen = new SpawnerEditorScreen(new SpawnerEditorState(view.session(), view.draft()), view.pos());
            mc.setScreen(screen);
        } else if (screen != null && screen.session().equals(view.session())) {
            if (view.status() == SpawnerEditorPayloads.Status.INVALID) {
                screen.rejected(view.message());
            } else {
                screen.finished();
                screen = null;
                if (mc.player != null && !view.message().isEmpty())
                    mc.player.displayClientMessage(Component.literal(view.message()), false);
            }
        }
    }
    static boolean send(SpawnerEditorPayloads.Request request) {
        if (sender == null) return false;
        sender.accept(request); return true;
    }
    public static void clear() {
        if (screen != null) screen.finished();
        screen = null;
    }
}
