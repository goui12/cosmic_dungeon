package net.goui.cosmicdungeon.client.screen.crafting;

import java.util.*;
import java.util.function.Consumer;
import net.goui.cosmicdungeon.network.PotionCraftPayloads;
import net.goui.cosmicdungeon.network.PotionCraftPayloads.*;

/** Exact server generations; repeated clicks are admitted only after an authoritative acknowledgement. */
public final class PotionPaletteState {
    private View view = empty();
    private List<Entry> visible = List.of();
    private Consumer<Action> sender;
    private Consumer<Request> requests;
    private boolean open, pending;
    private PotionPaletteModel.Variant variant = PotionPaletteModel.Variant.NORMAL;
    private static View empty() { return new View(0, 0, "", 0, 20, List.of()); }
    public void actions(Consumer<Action> sender, Consumer<Request> requests) {
        this.sender = Objects.requireNonNull(sender); this.requests = Objects.requireNonNull(requests);
    }
    public View view() { return view; }
    public boolean open() { return open; }
    public boolean pending() { return pending; }
    public PotionPaletteModel.Variant variant() { return variant; }
    public void variant(PotionPaletteModel.Variant value) {
        Objects.requireNonNull(value);
        if (variant != value) { variant = value; visible = PotionPaletteModel.visible(view.entries(), variant); }
    }
    public List<Entry> entries() { return visible; }
    public void receive(View value) {
        Objects.requireNonNull(value);
        if (!open || value.run() == view.run() && value.revision() < view.revision()) return;
        view = value; visible = PotionPaletteModel.visible(view.entries(), variant); pending = false;
    }
    public boolean toggle() {
        if (requests == null) return false;
        open = !open; view = empty(); visible = List.of(); pending = false;
        requests.accept(new Request(open)); return true;
    }
    public void close() {
        if (open && requests != null) requests.accept(new Request(false));
        clear();
    }
    public void clear() { view = empty(); visible = List.of(); open = false; pending = false; variant = PotionPaletteModel.Variant.NORMAL; }
    public boolean craft(String target, boolean shift) {
        if (!open || pending || sender == null || view.run() <= 0) return false;
        var entry = entries().stream().filter(e -> e.targetId().equals(target)).findFirst().orElse(null);
        if (entry == null || PotionPaletteModel.count(entry) == 0) return false;
        pending = true;
        try { sender.accept(new Action(view.run(), view.revision(), target, shift)); return true; }
        catch (RuntimeException error) { pending = false; throw error; }
    }
}
