package net.goui.cosmicdungeon.client.screen;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.goui.cosmicdungeon.network.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Hover transport reuses the existing server-authorized inspection payload and optional full detail screen. */
public final class PartyInspectionHover {
    private static final PartyInspectionHoverState STATE = new PartyInspectionHoverState();
    private static Screen screen;
    private PartyInspectionHover() {}
    public static void select(Screen candidate, long run, PartyPayloads.Member member) {
        if (screen != candidate) STATE.clear();
        screen = candidate;
        if (member == null) STATE.clear();
        else STATE.hover(run, UUID.fromString(member.memberId()));
    }
    public static void clear() { STATE.clear(); screen = null; }
    static boolean currentMenu(Screen candidate, net.minecraft.client.player.LocalPlayer player) {
        return net.goui.cosmicdungeon.client.screen.skills.SkillsPanelClient.inventory(candidate)
                && candidate instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> container
                && player.containerMenu == container.getMenu();
    }
    public static void tick() {
        var mc = Minecraft.getInstance();
        if (screen == null || mc.screen != screen || mc.player == null
                || !currentMenu(screen, mc.player)
                || !mc.player.containerMenu.getCarried().isEmpty()) { clear(); return; }
        var request = STATE.tick();
        if (request != null) {
            if (!D1PartyHud.inspectionCurrent(request.run(), request.subject().toString())) { clear(); return; }
            ModNetwork.sendToServer(request);
        }
    }
    public static void receive(PartyInspectionPayloads.View payload) {
        var mc = Minecraft.getInstance();
        if (screen == null || mc.screen != screen
                || !D1PartyHud.inspectionCurrent(payload.request().run(), payload.request().subject().toString())) return;
        STATE.receive(payload);
    }
    public static List<Component> details(String memberId, List<String> fallback) {
        var result = new ArrayList<Component>();
        fallback.forEach(line -> result.add(Component.literal(line)));
        var view = STATE.view(memberId);
        if (view == null) result.add(Component.literal("Refreshing current-run details..."));
        else if (!view.allowed()) result.add(Component.literal("Member details are no longer available."));
        else {
            // Use the same authorized readings as the full inspection screen.
            for (var reading : view.readings())
                result.add(Component.literal(reading.label() + ": " + reading.value()));
        }
        return result;
    }
    public static void tooltip(GuiGraphics graphics, List<Component> lines, int x, int y) {
        var mc = Minecraft.getInstance();
        int width = Math.max(80, Math.min(280, mc.getWindow().getGuiScaledWidth() - 24));
        int limit = Math.max(3, (mc.getWindow().getGuiScaledHeight() - 32) / mc.font.lineHeight);
        var wrapped = new ArrayList<net.minecraft.util.FormattedCharSequence>();
        for (var line : lines) wrapped.addAll(mc.font.split(line, width));
        if (wrapped.size() > limit) {
            wrapped.subList(limit - 1, wrapped.size()).clear();
            wrapped.add(Component.literal("Click for complete details").getVisualOrderText());
        }
        graphics.setTooltipForNextFrame(mc.font, wrapped,
                net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner.INSTANCE, x, y, false);
    }
}
