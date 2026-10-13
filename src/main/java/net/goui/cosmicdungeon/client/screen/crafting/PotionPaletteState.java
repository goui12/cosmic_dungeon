package net.goui.cosmicdungeon.client.screen.crafting;

import java.util.*;
import java.util.function.Consumer;
import net.goui.cosmicdungeon.network.PotionCraftPayloads.*;

/** Exact server generations; preview entries cannot authorize a craft. */
public final class PotionPaletteState {
    private View view = empty();
    private List<Entry> visible = List.of(), preview = List.of();
    private Consumer<Action> sender;
    private Consumer<Request> requests;
    private boolean open, pending;
    private int first;
    private static View empty() { return View.empty(0); }
    public void actions(Consumer<Action> sender, Consumer<Request> requests) {
        this.sender = Objects.requireNonNull(sender); this.requests = Objects.requireNonNull(requests);
    }
    public View view() { return view; }
    public boolean open() { return open; }
    public boolean pending() { return pending; }
    public int first() { return first; }
    public void first(int value) { first = Math.max(0, value); }
    public void preview(List<Entry> entries) {
        preview = PotionPaletteModel.visible(entries);
        if (view.run() == 0) visible = preview;
    }
    public List<Entry> entries() { return visible; }
    public void receive(View value) {
        Objects.requireNonNull(value);
        if (!open || value.run() == view.run() && value.revision() < view.revision()) return;
        view = value; visible = value.run() == 0 ? preview : PotionPaletteModel.visible(value.entries()); pending = false;
    }
    public boolean ensureOpen() {
        if (open) return true;
        if (requests == null) return false;
        open = true; view = empty(); visible = preview; pending = false; first = 0;
        requests.accept(new Request(true)); return true;
    }
    public void refreshUnavailable() { if (open && view.run() == 0 && requests != null) requests.accept(new Request(true)); }
    public boolean toggle() { if (open) { close(); return true; } return ensureOpen(); }
    public void close() {
        if (open && requests != null) requests.accept(new Request(false));
        clear();
    }
    public void clear() { view = empty(); visible = List.of(); preview = List.of(); open = false; pending = false; first = 0; }
    public boolean craft(String target, boolean shift) {
        if (!open || pending || sender == null || view.run() <= 0) return false;
        var entry = entries().stream().filter(e -> e.targetId().equals(target)).findFirst().orElse(null);
        if (entry == null || PotionPaletteModel.count(entry) == 0) return false;
        pending = true;
        try { sender.accept(new Action(view.run(), view.revision(), target, shift)); return true; }
        catch (RuntimeException error) { pending = false; throw error; }
    }
}
