package net.goui.cosmicdungeon.client.screen;

import net.goui.cosmicdungeon.network.ModNetwork;
import net.goui.cosmicdungeon.network.PartyPayloads;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Tamsin's explicit choice for a join accepted outside her menu. It never changes server menus. */
final class GroupResourceConsentScreen extends Screen {
    private final Screen parent;
    private final String context;
    private String requested;
    private int waitingTicks;
    private Button yes, no;
    GroupResourceConsentScreen(Screen parent, String context) {
        super(Component.literal("Tamsin: Group resources"));
        this.parent = parent;
        this.context = context;
    }
    private D1PartyPanelLayout box() { return D1PartyPanelLayout.forViewport(width, height); }
    @Override protected void init() {
        var box = box(); int x = (width - box.width()) / 2, y = (height - box.height()) / 2;
        yes = addRenderableWidget(Button.builder(Component.literal("YES - contribute"), b -> choose("YES"))
                .bounds(x + 10, y + box.managementY(), box.halfWidth(), 20).build());
        no = addRenderableWidget(Button.builder(Component.literal("NO - keep materials"), b -> choose("NO"))
                .bounds(x + box.rightColumn(), y + box.managementY(), box.halfWidth(), 20).build());
        addRenderableWidget(Button.builder(Component.literal("Close"), b -> onClose())
                .bounds(x + 10, y + box.progressionY(), box.innerWidth(), 20).build());
    }
    private void choose(String choice) {
        var view = D1PartyHud.currentView();
        if (!D1PartyPresentation.sharingMutable(view) || !context.equals(view.sharing().context()) || requested != null) return;
        requested = choice; waitingTicks = 0; yes.active = no.active = false;
        ModNetwork.sendToServer(new PartyPayloads.Action(-1, view.state().revision(),
                choice.equals("YES") ? "resources_yes" : "resources_no", context));
    }
    @Override public void tick() {
        var view = D1PartyHud.currentView();
        if (!InventoryModalContext.current(parent, minecraft.player)
                || !D1PartyPresentation.sharingMutable(view) || !context.equals(view.sharing().context())) {
            InventoryModalContext.dismiss(parent); return;
        }
        if (requested != null && requested.equals(view.sharing().choice())) { onClose(); return; }
        if (requested != null && ++waitingTicks >= 60) {
            requested = null; yes.active = no.active = true;
        }
    }
    @Override public void onClose() { InventoryModalContext.restore(parent); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        yes.visible = no.visible = D1PartyPresentation.sharingMutable(D1PartyHud.currentView());
        renderBackground(g, mouseX, mouseY, partialTick);
        var box = box(); int x = (width - box.width()) / 2, y = (height - box.height()) / 2;
        g.fill(x, y, x + box.width(), y + box.height(), 0xEF181820);
        g.drawString(font, title, x + 10, y + 10, 0xFFE4C98A, false);
        var view = D1PartyHud.currentView();
        String choice = view != null && context.equals(view.sharing().context()) ? view.sharing().choice() : "UNKNOWN";
        int textY = y + 36;
        for (String paragraph : D1PartyPresentation.consentExplanation(choice, D1PartyPresentation.sharingMutable(view))) {
            for (var line : font.split(Component.literal(paragraph), box.innerWidth())) {
                g.drawString(font, line, x + 10, textY, 0xFFEEEEEE, false); textY += 11;
            }
            textY += 6;
        }
        if (requested != null) g.drawString(font, "Waiting for server...", x + 10, y + box.navigationY(), 0xFF90CAF9, false);
        super.render(g, mouseX, mouseY, partialTick);
    }
}
