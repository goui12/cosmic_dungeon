package net.goui.cosmicdungeon.client.screen;

import net.goui.cosmicdungeon.client.screen.skills.TheurgistClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ScreenEvent;

/** A compact paged roster above the native death title; all casters remain reachable at small GUI sizes. */
public final class DeathResurrectionRoster {
    private final int x, width;
    private final Button previous, next;
    private int page;
    public DeathResurrectionRoster(ScreenEvent.Init.Post event) {
        int screenWidth = event.getScreen().width;
        width = Math.max(120, Math.min(420, screenWidth - 16)); x = (screenWidth - width) / 2;
        previous = Button.builder(Component.literal("<"), b -> { page--; update(); }).bounds(x + width - 44, 6, 18, 14)
                .tooltip(Tooltip.create(Component.literal("Previous Theurgists"))).build();
        next = Button.builder(Component.literal(">"), b -> { page++; update(); }).bounds(x + width - 22, 6, 18, 14)
                .tooltip(Tooltip.create(Component.literal("Next Theurgists"))).build();
        event.addListener(previous); event.addListener(next); update();
    }
    private ResurrectionPresentation.Page update() {
        var view = TheurgistClient.view();
        var slice = ResurrectionPresentation.page(page, view.casters().size(), 2);
        page = slice.index();
        previous.visible = next.visible = view.run() > 0 && slice.pages() > 1;
        previous.active = page > 0; next.active = page + 1 < slice.pages();
        return slice;
    }
    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        var view = TheurgistClient.view(); var slice = update();
        if (view.run() <= 0) return;
        var font = Minecraft.getInstance().font;
        // Render.Post follows native widgets; leave the paging button area untouched.
        graphics.fill(x, 4, x + width - 48, 21, 0xDC16202C);
        graphics.fill(x, 21, x + width, 49, 0xDC16202C);
        String heading = "Resurrection" + (slice.pages() > 1 ? " " + (page + 1) + "/" + slice.pages() : "");
        graphics.drawString(font, heading, x + 4, 8, 0xFFE4C98A, false);
        if (view.casters().isEmpty()) graphics.drawString(font, "No Theurgists in this dungeon.", x + 4, 25, 0xFFBBBBBB, false);
        for (int i = slice.start(); i < slice.end(); i++) {
            var caster = view.casters().get(i); String line = ResurrectionPresentation.line(caster);
            int y = 24 + (i - slice.start()) * 12;
            graphics.drawString(font, font.plainSubstrByWidth(line, width - 8), x + 4, y,
                    caster.resurrection().state().equals("READY") ? 0xFFAAFFAA : 0xFFDDDDDD, false);
            if (mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + 11)
                graphics.setTooltipForNextFrame(font, Component.literal(line), mouseX, mouseY);
        }
    }
}
