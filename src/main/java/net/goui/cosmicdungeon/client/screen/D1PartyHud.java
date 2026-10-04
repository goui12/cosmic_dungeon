package net.goui.cosmicdungeon.client.screen;

import java.util.List;
import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.network.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;

/** Read-only HUD in the world; native widgets exist only on the player's inventory screen. */
@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID, value = Dist.CLIENT)
public final class D1PartyHud {
    private static PartyPayloads.View view;
    private static Screen inventory;
    private static Button ready, leave, join;
    private D1PartyHud() {}
    public static void receive(PartyPayloads.View snapshot) {
        if (snapshot.containerId() != -1) return;
        view = snapshot;
        updateControls();
    }
    private static void clear() { view = null; inventory = null; ready = leave = join = null; }
    @SubscribeEvent public static void login(ClientPlayerNetworkEvent.LoggingIn event) { clear(); }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { clear(); }
    private static boolean inventory(Screen screen) {
        return screen instanceof InventoryScreen || screen instanceof CreativeModeInventoryScreen;
    }
    public static boolean visible() {
        return Minecraft.getInstance().player != null && view != null
                && (!view.members().isEmpty() || !view.invitation().token().isEmpty());
    }
    private static boolean bookVisible() {
        var screen = Minecraft.getInstance().screen;
        if (!(screen instanceof InventoryScreen)) return false;
        for (var child : screen.children())
            if (child instanceof net.minecraft.client.gui.screens.recipebook.RecipeBookComponent<?> book && book.isVisible()) return true;
        return false;
    }
    private static D1PartyHudLayout layout() {
        var screen = Minecraft.getInstance().screen;
        if (bookVisible()) return D1PartyHudLayout.withRecipeBook(screen.width);
        int left = inventory(screen) ? ((AbstractContainerScreen<?>)screen).getGuiLeft() : -1;
        return D1PartyHudLayout.forView(left, view == null ? 0 : view.members().size());
    }
    public static int worldHeight() { return visible() ? layout().height() + MercenaryHudLayout.stackHeight(view.mercenaries().size()) + 8 : 0; }
    public static int inventoryAccountX() {
        return visible() && inventory(Minecraft.getInstance().screen) ? layout().x() + layout().width() + 8 : 8;
    }
    private static void send(String action) {
        if (view == null || !inventory(Minecraft.getInstance().screen)) return;
        ModNetwork.sendToServer(new PartyPayloads.Action(-1, view.state().revision(), action,
                action.equals("accept") ? view.invitation().token() : ""));
    }
    @SubscribeEvent public static void init(ScreenEvent.Init.Post event) {
        if (!inventory(event.getScreen())) return;
        inventory = event.getScreen();
        ready = Button.builder(Component.literal("Ready"), button -> {
            var player = Minecraft.getInstance().player;
            if (player != null) send(D1PartyPresentation.readiness(view, player.getGameProfile().name()).action());
        }).bounds(8, 8, 100, 20).build();
        leave = Button.builder(Component.literal("Leave Group"), button -> send("leave")).bounds(8, 32, 100, 20).build();
        join = Button.builder(Component.literal("Join Group"), button -> send("accept")).bounds(8, 8, 100, 20).build();
        leave.setTooltip(Tooltip.create(Component.literal("Leave Group (leaders disband their group)")));
        join.setTooltip(Tooltip.create(Component.literal("Join the inviting group for Dungeon 1")));
        event.addListener(ready); event.addListener(leave); event.addListener(join);
        updateControls();
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if (inventory != Minecraft.getInstance().screen) { inventory = null; ready = leave = join = null; }
        else updateControls();
    }
    private static void updateControls() {
        var mc = Minecraft.getInstance();
        if (ready == null || inventory != mc.screen) return;
        var box = layout();
        boolean shown = visible();
        boolean grouped = shown && !view.members().isEmpty();
        boolean lobby = grouped && !view.state().phase().equals("ACTIVE");
        var control = D1PartyPresentation.readiness(view, mc.player == null ? null : mc.player.getGameProfile().name());
        ready.setMessage(Component.literal(control.label()));
        ready.setTooltip(Tooltip.create(Component.literal(control.label())));
        ready.visible = lobby; ready.active = lobby && control.enabled();
        leave.visible = lobby; leave.active = lobby && !view.state().phase().equals("PREPARING");
        join.visible = shown && !grouped && !view.invitation().token().isEmpty();
        join.active = join.visible && !view.invitation().accepted();
        for (Button button : List.of(ready, leave, join)) { button.setX(box.x()); button.setWidth(box.width()); }
        ready.setY(box.controlsY()); leave.setY(box.controlsY() + 24); join.setY(box.controlsY());
        if (bookVisible()) {
            int y = inventory.height - 26;
            ready.setY(y); leave.setY(y); join.setY(y);
            leave.setX(box.x() + box.width() + 8);
        }
    }
    @SubscribeEvent public static void hud(RenderGuiEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (!visible() || mc.screen != null || mc.options.hideGui || mc.getDebugOverlay().showDebugScreen()) return;
        draw(event.getGuiGraphics(), -1, -1);
    }
    @SubscribeEvent public static void screen(ScreenEvent.Render.Post event) {
        if (!visible() || !inventory(event.getScreen())) return;
        updateControls();
        draw(event.getGuiGraphics(), event.getMouseX(), event.getMouseY());
        // Vanilla skips container widgets behind its narrow recipe-book overlay.
        if (bookVisible() && event.getScreen().width < 379 && ready != null)
            for (Button button : List.of(ready, leave, join))
                button.render(event.getGuiGraphics(), event.getMouseX(), event.getMouseY(), event.getPartialTick());
    }
    @SubscribeEvent public static void click(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!visible() || !bookVisible() || event.getScreen().width >= 379 || ready == null) return;
        updateControls();
        for (Button button : List.of(ready, leave, join)) {
            if (button.visible && button.active && button.mouseClicked(event.getMouseButtonEvent(), false)) {
                event.getScreen().setFocused(button); event.setCanceled(true); return;
            }
        }
    }
    private static void line(GuiGraphics graphics, String text, int y, int color, int mouseX, int mouseY) {
        var box = layout(); var font = Minecraft.getInstance().font;
        graphics.drawString(font, font.plainSubstrByWidth(text, box.width() - 8), box.x() + 4, y, color, false);
        if (mouseX >= box.x() && mouseX < box.x() + box.width() && mouseY >= y && mouseY < y + 11)
            graphics.setComponentTooltipForNextFrame(font, List.of(Component.literal(text)), mouseX, mouseY);
    }
    private static void draw(GuiGraphics graphics, int mouseX, int mouseY) {
        var box = layout();
        graphics.fill(box.x(), box.y(), box.x() + box.width(), box.y() + box.height(), 0xD0181820);
        if (bookVisible()) {
            boolean grouped = !view.members().isEmpty();
            line(graphics, grouped ? view.recruitment().groupName() : "Dungeon 1 invitation", box.y() + 2, 0xFFE4C98A, -1, -1);
            line(graphics, !view.mercenaries().isEmpty() ? String.join(", ", view.mercenaries().stream().map(PartyPayloads.Mercenary::name).toList()) : grouped ? "Ready " + view.members().stream().filter(PartyPayloads.Member::ready).count() + "/" + view.members().size()
                    : "From: " + view.invitation().inviter(), box.y() + 13, 0xFFFFFFFF, -1, -1);
            if (mouseX >= box.x() && mouseX < box.x() + box.width() && mouseY >= box.y() && mouseY < box.y() + box.height()) {
                var details = new java.util.ArrayList<Component>();
                details.add(Component.literal(grouped ? view.recruitment().groupName() : "Dungeon 1 invitation from " + view.invitation().inviter()));
                if (grouped) details.add(Component.literal("Difficulty: " + view.difficulty()));
                for (var member : view.members()) details.add(Component.literal(member.name() + " / "
                        + ClassSelectorScreen.className(member.classId()).getString() + (member.ready() ? " / Ready" : " / Not Ready")));
                for(var hire:view.mercenaries())details.add(Component.literal(hire.name()+" / "+MercenaryHudLayout.status(hire)));
                if (!grouped && view.invitation().accepted()) details.add(Component.literal("Finish agreement/class selection at Tamsin"));
                graphics.setComponentTooltipForNextFrame(Minecraft.getInstance().font, details, mouseX, mouseY);
            }
            return;
        }
        if (view.members().isEmpty()) {
            line(graphics, "Dungeon 1 invitation", box.y() + 4, 0xFFE4C98A, mouseX, mouseY);
            line(graphics, "From: " + view.invitation().inviter(), box.y() + 16, 0xFFFFFFFF, mouseX, mouseY);
            line(graphics, view.invitation().accepted() ? "Finish setup at Tamsin" : "Open inventory to join", box.y() + 28, 0xFFBBBBBB, mouseX, mouseY);
            return;
        }
        line(graphics, view.recruitment().groupName(), box.y() + 4, 0xFFE4C98A, mouseX, mouseY);
        String status = switch (view.state().phase()) {
            case "READY_CHECK" -> "Ready " + view.members().stream().filter(PartyPayloads.Member::ready).count() + "/" + view.members().size();
            case "QUEUED" -> view.state().countdownSeconds() < 0 ? "Queue " + view.state().queuePosition()
                    : "Starting in " + view.state().countdownSeconds() + "s";
            case "PREPARING" -> "Preparing";
            case "ACTIVE" -> "Dungeon 1";
            default -> view.members().size() + "/" + view.state().capacity() + " members";
        };
        String difficulty = net.goui.cosmicdungeon.dungeon.DungeonDifficulty.parse(view.difficulty())
                .orElse(net.goui.cosmicdungeon.dungeon.DungeonDifficulty.HARD).title();
        line(graphics, difficulty + " / " + status, box.y() + 16, 0xFF90CAF9, mouseX, mouseY);
        int y = box.y() + 30;
        for (var member : view.members()) {
            line(graphics, (member.leader() ? "* " : "") + member.name() + " / "
                    + ClassSelectorScreen.className(member.classId()).getString()
                    + (member.ready() ? " / Ready" : " / Not Ready"), y,
                    member.ready() ? 0xFFAAFFAA : 0xFFDDDDDD, mouseX, mouseY);
            y += 12;
        }
        y=box.y()+box.height()+6;
        for(var hire:view.mercenaries()){
            graphics.fill(box.x(),y,box.x()+box.width(),y+26,0xD0181820);
            line(graphics,hire.name(),y+2,0xFFE4C98A,mouseX,mouseY);
            line(graphics,MercenaryHudLayout.status(hire),y+12,hire.status().equals("ACTIVE")?0xFFAAFFAA:0xFFDDDDDD,mouseX,mouseY);
            int width=box.width()-8;
            graphics.fill(box.x()+4,y+23,box.x()+4+width,y+25,0xFF553333);
            graphics.fill(box.x()+4,y+23,box.x()+4+MercenaryHudLayout.healthWidth(hire,width),y+25,0xFF66CC88);
            y+=28;
        }
    }
}
