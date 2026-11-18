package net.goui.cosmicdungeon.client.screen.skills;

import net.goui.cosmicdungeon.client.SkillKeybindClient;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.goui.cosmicdungeon.playerclass.skill.ClassSkillSlot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = "cosmicdungeon", value = Dist.CLIENT)
public final class PrimarySkillHud {
    private PrimarySkillHud() {}

    @SubscribeEvent
    public static void afterHearts(RenderGuiLayerEvent.Post event) {
        if (!VanillaGuiLayers.PLAYER_HEALTH.equals(event.getName())) return;
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.gameMode == null || !mc.gameMode.canHurtPlayer()
                || mc.options.hideGui || mc.getCameraEntity() != mc.player || !mc.player.isAlive()) return;
        var primary = ClassAbilityClient.primary(ClassData.getClassId(mc.player));
        if (primary == null) return;
        var g = event.getGuiGraphics();
        int x = g.guiWidth() / 2 - 91;
        int bottom = g.guiHeight() - mc.gui.leftHeight + 8;
        boolean ready = ClassAbilityClient.ready(primary);
        drawSlot(g, Math.max(0, x - 20), bottom - 18, ready, mc.player.tickCount);
        int fill = primary.maxCharge() > 0 ? 79 * primary.charge() / primary.maxCharge() : ready ? 79 : 0;
        g.fill(x, bottom - 8, x + 81, bottom - 2, 0xFF19212C);
        g.fill(x + 1, bottom - 7, x + 1 + fill, bottom - 3, ready ? 0xFFFFDA78 : 0xFF69B9D1);
        g.pose().pushMatrix();
        g.pose().translate(x, bottom - 17);
        g.pose().scale(.75f, .75f);
        g.drawString(mc.font, mc.font.plainSubstrByWidth(primary.title(), 108), 0, 0, 0xFFF6F0DF, true);
        g.pose().popMatrix();
        mc.gui.leftHeight += 20;
    }

    private static void drawSlot(GuiGraphics g, int x, int y, boolean ready, int ticks) {
        g.fill(x, y, x+18, y+18, ready ? 0xFFE2B859 : 0xFF76818D);
        g.fill(x+1, y+1, x+17, y+17, ready ? 0xFF604C29 : 0xFF283442);
        if (ready) {
            int phase = Math.floorMod(ticks, 44);
            for (int row = 1; row < 17; row++) {
                int left = Math.max(1, phase-row), right = Math.min(17, phase-row+4);
                if (right > left) g.fill(x+left, y+row, x+right, y+row+1, 0x88FFECAC);
            }
        }
        var font = Minecraft.getInstance().font;
        var key = SkillKeybindClient.shortBinding(ClassAbilityClient.actionId(ClassSkillSlot.PRIMARY));
        float scale = Math.min(1f, 15f / Math.max(1, font.width(key)));
        g.pose().pushMatrix();
        g.pose().translate(x+9f, y+(18f-font.lineHeight*scale)/2);
        g.pose().scale(scale, scale);
        g.drawCenteredString(font, key, 0, 0, 0xFFFFFFFF);
        g.pose().popMatrix();
    }
}
