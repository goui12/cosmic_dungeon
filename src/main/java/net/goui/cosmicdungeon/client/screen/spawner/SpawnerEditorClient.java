package net.goui.cosmicdungeon.client.screen.spawner;

import java.util.function.Consumer;
import net.goui.cosmicdungeon.network.SpawnerEditorPayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Client session ownership. Only explicit Apply/Cancel emits a server request. */
public final class SpawnerEditorClient {
    private static Consumer<SpawnerEditorPayloads.Request> sender;
    private static SpawnerEditorScreen screen;
    private static Consumer<SpawnerEditorPayloads.LibraryRequest> librarySender;
    private SpawnerEditorClient() {}
    public static void actions(Consumer<SpawnerEditorPayloads.Request> value) { sender = value; }
    public static void libraryActions(Consumer<SpawnerEditorPayloads.LibraryRequest> value) { librarySender = value; }
    public static void receiveLibrary(SpawnerEditorPayloads.LibraryView view) {
        if (screen != null && screen.session().equals(view.session())) screen.receiveLibrary(view);
    }
    static boolean sendLibrary(SpawnerEditorPayloads.LibraryRequest request) {
        if (librarySender == null) return false;
        librarySender.accept(request); return true;
    }
    public static void receive(SpawnerEditorPayloads.View view) {
        Minecraft mc = Minecraft.getInstance();
        if (view.status() == SpawnerEditorPayloads.Status.OPEN) {
            if (mc.player == null || mc.level == null) return;
            if (screen != null && screen.session().equals(view.session())) return;
            if (screen != null) screen.finished();
            var state = new SpawnerEditorState(view.session(), view.draft());
            state.presetLoaded(view.presetLoaded()); state.notice(view.message());
            screen = new SpawnerEditorScreen(state, view.pos(), view.equipment(), view.held(), view.loot(), view.presetNames(), view.shortcuts());
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
