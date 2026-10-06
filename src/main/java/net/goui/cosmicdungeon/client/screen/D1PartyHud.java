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
    private static Button ready, leave, join, revive;
    private D1PartyHud() {}
    public static void receive(PartyPayloads.View snapshot) {
        if (snapshot.containerId() != -1) return;
        view = snapshot;
        updateControls();
    }
    public static PartyPayloads.Mercenary resurrectionOffer(){
        return view==null?null:view.mercenaries().stream().filter(row->row.resurrection().offered()).findFirst().orElse(null);
    }
    private static void clear() { view = null; inventory = null; ready = leave = join = revive = null; }
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
        revive=Button.builder(Component.literal("Revive"),button->{
            var hire=ownRevival();if(hire==null)return;
            ModNetwork.sendToServer(new PartyPayloads.Action(-1,hire.recovery().deadline(),"revive",hire.recovery().id()));
        }).bounds(8,inventory.height-26,160,20).build();
        event.addListener(ready); event.addListener(leave); event.addListener(join);event.addListener(revive);
        updateControls();
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if (inventory != Minecraft.getInstance().screen) { inventory = null; ready = leave = join = revive = null; }
        else updateControls();
    }
    private static PartyPayloads.Mercenary ownRevival(){
        return view==null?null:view.mercenaries().stream().filter(h->h.recovery().own()&&h.status().equals("RESPAWNING")).findFirst().orElse(null);
    }
    public static void revivePrompt(PartyPayloads.RevivePrompt prompt){
        var mc=Minecraft.getInstance();var hire=ownRevival();
        if(hire==null||!inventory(mc.screen)||!hire.recovery().id().equals(prompt.id())
                ||hire.recovery().deadline()!=prompt.deadline())return;
        var previous=mc.screen;
        mc.setScreen(new net.minecraft.client.gui.screens.ConfirmScreen(yes->{
            mc.setScreen(previous);
            if(yes)ModNetwork.sendToServer(new PartyPayloads.Action(-1,prompt.deadline(),"revive_help",prompt.id()));
        },Component.literal("Revive "+prompt.name()),Component.literal(
                "You do not have the funds to revive the mercenary. Would you like to ask the group for help?")));
    }
    private static void updateControls() {
        var mc = Minecraft.getInstance();
        if (ready == null || inventory != mc.screen) return;
        var box = layout();
        boolean shown = visible();
        var hire=ownRevival();
        revive.visible=shown&&hire!=null;revive.active=revive.visible;
        if(hire!=null){
            revive.setMessage(Component.literal("Revive ("+hire.recovery().price()+" Trace)"));
            revive.setTooltip(Tooltip.create(Component.literal("Revive "+hire.name()+" now")));
        }
        // Footer remains outside inventory slots at minimum GUI scale and with the recipe book open.
        revive.setX(8);revive.setY(inventory.height-26);revive.setWidth(Math.min(224,inventory.width-16));
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
            for (Button button : List.of(ready, leave, join, revive))
                button.render(event.getGuiGraphics(), event.getMouseX(), event.getMouseY(), event.getPartialTick());
    }
    @SubscribeEvent public static void click(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!visible() || !bookVisible() || event.getScreen().width >= 379 || ready == null) return;
        updateControls();
        for (Button button : List.of(ready, leave, join, revive)) {
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
            line(graphics, !view.mercenaries().isEmpty() ? String.join(", ", view.mercenaries().stream().map(MercenaryHudLayout::label).toList()) : grouped ? D1PartyPresentation.readySummary(view)
                    : "From: " + view.invitation().inviter(), box.y() + 13, 0xFFFFFFFF, -1, -1);
            if (mouseX >= box.x() && mouseX < box.x() + box.width() && mouseY >= box.y() && mouseY < box.y() + box.height()) {
                var details = new java.util.ArrayList<Component>();
                details.add(Component.literal(grouped ? view.recruitment().groupName() : "Dungeon 1 invitation from " + view.invitation().inviter()));
                if (grouped) details.add(Component.literal("Difficulty: " + view.difficulty()));
                for (var member : view.members()) details.add(Component.literal(member.name() + " / "
                        + ClassSelectorScreen.className(member.classId()).getString() + D1PartyPresentation.readySuffix(view, member)));
                for(var hire:view.mercenaries())for(String detail:MercenaryHudLayout.tooltip(hire))details.add(Component.literal(detail));
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
            case "READY_CHECK" -> D1PartyPresentation.readySummary(view);
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
                    + D1PartyPresentation.readySuffix(view, member), y,
                    D1PartyPresentation.showsReadiness(view, member) && member.ready() ? 0xFFAAFFAA : 0xFFDDDDDD, mouseX, mouseY);
            y += 12;
        }
        y=box.y()+box.height()+6;
        for(var hire:view.mercenaries()){
            graphics.fill(box.x(),y,box.x()+box.width(),y+26,0xD0181820);
            // Schedule only the complete row tooltip: GuiGraphics keeps the first tooltip submitted.
            line(graphics,MercenaryHudLayout.label(hire),y+2,0xFFE4C98A,-1,-1);
            line(graphics,MercenaryHudLayout.status(hire),y+12,hire.status().equals("ACTIVE")?0xFFAAFFAA:0xFFDDDDDD,-1,-1);
            int width=box.width()-8;
            graphics.fill(box.x()+4,y+23,box.x()+4+width,y+25,0xFF553333);
            graphics.fill(box.x()+4,y+23,box.x()+4+MercenaryHudLayout.healthWidth(hire,width),y+25,0xFF66CC88);
            if(mouseX>=box.x()&&mouseX<box.x()+box.width()&&mouseY>=y&&mouseY<y+26)
                graphics.setComponentTooltipForNextFrame(Minecraft.getInstance().font,
                        MercenaryHudLayout.tooltip(hire).stream().map(Component::literal).map(c->(Component)c).toList(),mouseX,mouseY);
            y+=28;
        }
    }
}
