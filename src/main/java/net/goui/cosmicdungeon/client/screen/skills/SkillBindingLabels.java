package net.goui.cosmicdungeon.client.screen.skills;

import java.util.function.BiFunction;
import java.util.function.ToIntFunction;
import net.goui.cosmicdungeon.client.SkillKeybindClient;
import net.goui.cosmicdungeon.client.screen.skills.SharedInventoryLayout.Rect;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Live controls presentation; model/tooltip tests can supply hints without creating native key mappings. */
public final class SkillBindingLabels {
    public static final int HINT_COLOR = 0xFFAAAAAA;
    static final float TEXT_SCALE = .80F;
    public static final float HINT_SCALE = .65F;
    static int textWidth(Font font, String text) {
        return (int)Math.ceil(font.width(text) * TEXT_SCALE);
    }
    static void drawText(GuiGraphics g, Font font, String text, int x, int y, int color) {
        var pose = g.pose();
        pose.pushMatrix();
        try {
            pose.translate(x, y);
            pose.scale(TEXT_SCALE, TEXT_SCALE);
            g.drawString(font, text, 0, 0, color, false);
        } finally { pose.popMatrix(); }
    }
    public record Layout(String title, String hint, int titleX, int hintX, boolean stacked) {}
    private SkillBindingLabels() {}

    public static Component tooltip(String actionId, String title, Component description) {
        return tooltip(title, description, SkillKeybindClient.hint(actionId));
    }
    public static Component tooltip(String title, Component description, Component hint) {
        var result = description.getString().startsWith(title) ? description.copy()
                : Component.literal(title + "\n").append(description.copy());
        if (!hint.getString().isEmpty())
            result.append(Component.literal("\nHotkey: ").withStyle(ChatFormatting.GRAY))
                    .append(hint.copy().withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        return result;
    }
    /** Coordinates are relative to the padded row. Small rows retain a separate title and binding line. */
    public static Layout layout(String title, String hint, int available, ToIntFunction<String> measure,
                                BiFunction<String, Integer, String> clip) {
        return layout(title, hint, available, measure, measure, clip, clip);
    }
    public static Layout layout(String title, String hint, int available, ToIntFunction<String> titleMeasure,
                                ToIntFunction<String> hintMeasure, BiFunction<String, Integer, String> titleClip,
                                BiFunction<String, Integer, String> hintClip) {
        int width = Math.max(0, available), gap = 4;
        if (hint.isEmpty()) {
            String name = titleClip.apply(title, width);
            return new Layout(name, "", Math.max(0, (width - titleMeasure.applyAsInt(name)) / 2), 0, false);
        }
        int nameWidth = titleMeasure.applyAsInt(title), hintWidth = hintMeasure.applyAsInt(hint);
        if (nameWidth + gap + hintWidth <= width) {
            int x = (width - nameWidth - gap - hintWidth) / 2;
            return new Layout(title, hint, x, x + nameWidth + gap, false);
        }
        String name = titleClip.apply(title, width), key = hintClip.apply(hint, width);
        return new Layout(name, key, Math.max(0, (width - titleMeasure.applyAsInt(name)) / 2),
                Math.max(0, (width - hintMeasure.applyAsInt(key)) / 2), true);
    }
    private static String clip(Font font, String text, int width) {
        if (font.width(text) <= width) return text;
        int ellipsis = font.width("…");
        return width < ellipsis ? "" : font.plainSubstrByWidth(text, width - ellipsis) + "…";
    }
    public static void draw(GuiGraphics g, Font font, String actionId, String title, Rect row, int color) {
        // Query every draw so changes in Controls, including unbinding, appear immediately.
        String hint = SkillKeybindClient.hint(actionId).getString();
        var line = layout(title, hint, row.width() - 4, text -> textWidth(font, text),
                text -> (int)Math.ceil(font.width(Component.literal(text).withStyle(ChatFormatting.ITALIC)) * HINT_SCALE),
                (text, width) -> clip(font, text, (int)(width / TEXT_SCALE)),
                (text, width) -> clip(font, text, Math.max(0,(int)(width / HINT_SCALE)-2)));
        int titleHeight = (int)Math.ceil(font.lineHeight * TEXT_SCALE);
        int y = row.y() + (line.stacked() ? 0 : Math.max(0, (row.height() - titleHeight) / 2));
        drawText(g, font, line.title(), row.x() + 2 + line.titleX(), y, color);
        if (!line.hint().isEmpty()) {
            var pose=g.pose();pose.pushMatrix();
            try {
                pose.translate(row.x()+2+line.hintX(),line.stacked()?row.y()+titleHeight:y+1);
                pose.scale(HINT_SCALE,HINT_SCALE);
                g.drawString(font,Component.literal(line.hint()).withStyle(ChatFormatting.ITALIC),0,0,HINT_COLOR,false);
            } finally { pose.popMatrix(); }
        }
    }
}
