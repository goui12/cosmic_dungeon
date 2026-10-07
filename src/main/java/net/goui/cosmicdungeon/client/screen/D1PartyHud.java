package net.goui.cosmicdungeon.client.screen;

import java.util.List;
import net.goui.cosmicdungeon.client.screen.skills.SharedInventoryLayout;
import net.goui.cosmicdungeon.client.screen.skills.PanelScroll;
import net.goui.cosmicdungeon.client.screen.skills.SkillsPanelClient;
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
    private static final PanelScroll lobbyScroll = new PanelScroll();
    private D1PartyHud() {}
    public static void receive(PartyPayloads.View snapshot) {
        if (snapshot.containerId() != -1) return;
        view = snapshot;
        updateControls();
    }
    public static PartyPayloads.Mercenary resurrectionOffer(){
        return view==null?null:view.mercenaries().stream().filter(row->row.resurrection().offered()).findFirst().orElse(null);
    }
    private static void clear() { lobbyScroll.reset(); PartyHealthHud.reset(); view = null; inventory = null; ready = leave = join = revive = null; }
    @SubscribeEvent public static void login(ClientPlayerNetworkEvent.LoggingIn event) { clear(); }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { clear(); }
    private static boolean inventory(Screen screen) {
        return screen instanceof InventoryScreen || screen instanceof CreativeModeInventoryScreen;
    }
    public static boolean visible() {
        return Minecraft.getInstance().player != null && view != null
                && (!view.members().isEmpty() || !view.invitation().token().isEmpty());
    }
    public static SharedInventoryLayout inventoryLayout(AbstractContainerScreen<?> screen) {
        return SharedInventoryLayout.of(screen.width, screen.height, screen.getGuiLeft(), screen.getGuiTop(),
                screen.getXSize(), screen.getYSize());
    }
    private static D1PartyHudLayout layout() {
        var screen = Minecraft.getInstance().screen;
        if (inventory(screen)) {
            var group = inventoryLayout((AbstractContainerScreen<?>)screen).group();
            return new D1PartyHudLayout(group.x(), group.y(), group.width(), Math.max(24, group.height() - 48));
        }
        return D1PartyHudLayout.forView(-1, view == null ? 0 : view.members().size());
    }
    private static boolean activeHealth() { return visible() && view.state().phase().equals("ACTIVE"); }
    private static PartyHealthLayout healthLayout() {
        var screen = Minecraft.getInstance().screen;
        if (inventory(screen)) {
            var group = inventoryLayout((AbstractContainerScreen<?>)screen).group();
            return new PartyHealthLayout(group.x(), group.y(), group.width(), group.height(), 1, 0);
        }
        return PartyHealthHud.worldLayout(view);
    }
    static boolean inspectionCurrent(long run,String subject) {
        return activeHealth()&&view.state().revision()==run&&view.members().stream().anyMatch(m->m.memberId().equals(subject));
    }
    public static int worldHeight() { if(activeHealth())return PartyHealthHud.worldLayout(view).height()+8; return visible() ? layout().height() + MercenaryHudLayout.stackHeight(view.mercenaries().size()) + 8 : 0; }
    public static int inventoryAccountX() {
        var screen = Minecraft.getInstance().screen;
        return inventory(screen) ? inventoryLayout((AbstractContainerScreen<?>)screen).account().x() : 8;
    }
    private static void send(String action) {
        if (view == null || !inventory(Minecraft.getInstance().screen)) return;
        ModNetwork.sendToServer(new PartyPayloads.Action(-1, view.state().revision(), action,
                action.equals("accept") ? view.invitation().token() : ""));
    }
    @SubscribeEvent public static void init(ScreenEvent.Init.Post event) {
        if (!inventory(event.getScreen())) return;
        inventory = event.getScreen();
        PartyHealthHud.reset();
        lobbyScroll.reset();
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
        if (inventory != Minecraft.getInstance().screen) { lobbyScroll.release(); PartyHealthHud.release(); inventory = null; ready = leave = join = revive = null; }
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
        // Footer remains outside inventory slots and the reserved group lane.
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
    }
    @SubscribeEvent public static void hud(RenderGuiEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (!visible() || mc.screen != null || mc.options.hideGui || mc.getDebugOverlay().showDebugScreen()) return;
        draw(event.getGuiGraphics(), -1, -1);
    }
    @SubscribeEvent public static void beforeScreen(ScreenEvent.Render.Pre event) {
        if (!inventory(event.getScreen()) || ready==null) return;
        updateControls();
        leave.setTooltip(Tooltip.create(Component.literal("Leave Group (leaders disband their group)")));
        join.setTooltip(Tooltip.create(Component.literal("Join the inviting group for Dungeon 1")));
        if (SkillsPanelClient.ownsInput(event.getScreen(),event.getMouseX(),event.getMouseY()))
            for (Button button:List.of(ready,leave,join,revive)) button.setTooltip(null);
    }
    @SubscribeEvent public static void screen(ScreenEvent.Render.Post event) {
        if (!visible() || !inventory(event.getScreen())) return;
        updateControls();
        boolean covered=SkillsPanelClient.ownsInput(event.getScreen(),event.getMouseX(),event.getMouseY());
        draw(event.getGuiGraphics(), covered?-1:event.getMouseX(), covered?-1:event.getMouseY());
    }
    private static boolean input(Screen screen, double x, double y) {
        var mc = Minecraft.getInstance();
        return visible() && inventory(screen) && mc.player != null && mc.player.containerMenu.getCarried().isEmpty()
                && !SkillsPanelClient.ownsInput(screen, x, y);
    }
    private static int lobbyContentHeight() {
        return 8 + (view.members().isEmpty() ? 12 : view.members().size() * 12) + view.mercenaries().size() * 28;
    }
    private static void configureLobbyScroll() {
        lobbyScroll.configure(lobbyContentHeight(), Math.max(1, layout().height() - 24));
    }
    @SubscribeEvent public static void scroll(ScreenEvent.MouseScrolled.Pre event) {
        if (!input(event.getScreen(), event.getMouseX(), event.getMouseY())) return;
        if (activeHealth()) {
            if (PartyHealthHud.wheel(view, healthLayout(), event.getMouseX(), event.getMouseY(), event.getScrollDeltaY()))
                event.setCanceled(true);
        } else {
            var box = layout();
            configureLobbyScroll();
            if (event.getMouseX() >= box.x() && event.getMouseX() < box.x()+box.width()
                    && event.getMouseY() >= box.y()+24 && event.getMouseY() < box.y()+box.height()
                    && lobbyScroll.wheel(event.getScrollDeltaY())) event.setCanceled(true);
        }
    }
    @SubscribeEvent public static void drag(ScreenEvent.MouseDragged.Pre event) {
        if (event.getMouseButton() != 0 || !inventory(event.getScreen())) return;
        if (activeHealth() && PartyHealthHud.drag(view,healthLayout(),event.getMouseY())) event.setCanceled(true);
        else if (lobbyScroll.drag(layout().y()+24,event.getMouseY())) event.setCanceled(true);
    }
    @SubscribeEvent public static void release(ScreenEvent.MouseButtonReleased.Pre event) {
        if (event.getButton() == 0) {
            boolean captured = PartyHealthHud.release() | lobbyScroll.release();
            if (captured) event.setCanceled(true);
        }
    }
    @SubscribeEvent public static void click(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getMouseButtonEvent().button() != 0 || !input(event.getScreen(),event.getMouseX(),event.getMouseY())) return;
        if (activeHealth()) {
            if (PartyHealthHud.press(view,healthLayout(),event.getMouseX(),event.getMouseY())) { event.setCanceled(true); return; }
            var member=PartyHealthHud.memberAt(view,healthLayout(),event.getMouseX(),event.getMouseY());
            if (member != null) {
                Minecraft.getInstance().setScreen(new PartyInspectionScreen(event.getScreen(),view.state().revision(),member));
                event.setCanceled(true);
            }
        } else {
            var box=layout();
            configureLobbyScroll();
            if (event.getMouseX() >= box.x()+box.width()-6 && event.getMouseX() < box.x()+box.width()
                    && event.getMouseY() >= box.y()+24 && event.getMouseY() < box.y()+box.height()
                    && lobbyScroll.press(box.y()+24,event.getMouseY())) event.setCanceled(true);
        }
    }
    private static void line(GuiGraphics graphics, String text, int y, int color, int mouseX, int mouseY) {
        var box = layout(); var font = Minecraft.getInstance().font;
        graphics.drawString(font, font.plainSubstrByWidth(text, Math.max(1, box.width() - 8)), box.x() + 4, y, color, false);
        if (mouseX >= box.x() && mouseX < box.x() + box.width() && mouseY >= y && mouseY < y + 11)
            graphics.setComponentTooltipForNextFrame(font, List.of(Component.literal(text)), mouseX, mouseY);
    }
    private static void draw(GuiGraphics graphics, int mouseX, int mouseY) {
        if(activeHealth()) {
            PartyHealthHud.draw(graphics,view,healthLayout(),inventory(Minecraft.getInstance().screen),mouseX,mouseY);return;
        }
        var box = layout();
        graphics.fill(box.x(), box.y(), box.x() + box.width(), box.y() + box.height(), 0xD0181820);
        if (inventory(Minecraft.getInstance().screen)) {
            drawInventoryLobby(graphics, mouseX, mouseY);
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
    private static String lobbySummary() {
        String status=switch(view.state().phase()) {
            case "READY_CHECK" -> D1PartyPresentation.readySummary(view);
            case "QUEUED" -> view.state().countdownSeconds()<0?"Queue "+view.state().queuePosition():"Starting in "+view.state().countdownSeconds()+"s";
            case "PREPARING" -> "Preparing";
            default -> view.members().size()+"/"+view.state().capacity()+" members";
        };
        return net.goui.cosmicdungeon.dungeon.DungeonDifficulty.parse(view.difficulty())
                .orElse(net.goui.cosmicdungeon.dungeon.DungeonDifficulty.HARD).title()+" / "+status;
    }
    private static void drawInventoryLobby(GuiGraphics g, int mouseX, int mouseY) {
        var box=layout();
        configureLobbyScroll();
        boolean grouped=!view.members().isEmpty();
        line(g,grouped?view.recruitment().groupName():"Dungeon 1 invitation",box.y()+4,0xFFE4C98A,mouseX,mouseY);
        line(g,grouped?lobbySummary():"From: "+view.invitation().inviter(),
                box.y()+16,0xFF90CAF9,mouseX,mouseY);
        int top=box.y()+24,bottom=box.y()+box.height();
        int pointerY=mouseY>=top&&mouseY<bottom?mouseY:-1;
        g.enableScissor(box.x(),top,box.x()+Math.max(0,box.width()-6),bottom);
        int y=top+6-lobbyScroll.offset();
        if (!grouped) {
            line(g,view.invitation().accepted()?"Finish setup at Tamsin":"Open inventory to join",y,0xFFBBBBBB,mouseX,pointerY);
        } else for (var member:view.members()) {
            line(g,(member.leader()?"* ":"")+member.name()+" / "+ClassSelectorScreen.className(member.classId()).getString()
                    +D1PartyPresentation.readySuffix(view,member),y,
                    D1PartyPresentation.showsReadiness(view,member)&&member.ready()?0xFFAAFFAA:0xFFDDDDDD,mouseX,pointerY);
            y+=12;
        }
        for (var hire:view.mercenaries()) {
            line(g,MercenaryHudLayout.label(hire),y,0xFFE4C98A,-1,-1);
            line(g,MercenaryHudLayout.status(hire),y+11,0xFFBBBBBB,-1,-1);
            int barWidth=Math.max(0,box.width()-14);
            g.fill(box.x()+4,y+22,box.x()+4+barWidth,y+24,0xFF553333);
            g.fill(box.x()+4,y+22,box.x()+4+MercenaryHudLayout.healthWidth(hire,barWidth),y+24,0xFF66CC88);
            if(mouseX>=box.x()&&mouseX<box.x()+box.width()-6&&pointerY>=y&&pointerY<y+26)
                g.setComponentTooltipForNextFrame(Minecraft.getInstance().font,
                        MercenaryHudLayout.tooltip(hire).stream().map(Component::literal).map(c->(Component)c).toList(),mouseX,mouseY);
            y+=28;
        }
        g.disableScissor();
        if (lobbyScroll.max()>0) {
            int x=box.x()+box.width()-5,thumb=lobbyScroll.thumbTop(top);
            g.fill(x,top,x+4,bottom,0xFF30303A);
            g.fill(x,thumb,x+4,thumb+lobbyScroll.thumbHeight(),0xFFB5B5CA);
        }
    }

}
