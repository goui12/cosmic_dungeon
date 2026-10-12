package net.goui.cosmicdungeon.client.screen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

/** One measured document per active page/width/tag snapshot; rendering never re-wraps its paragraphs. */
final class HelpRichTextRenderer {
    static final int BODY = 0xFFD8D0C0, HEADING = 0xFFFFD98A, LINK = 0xFF73BBFF;
    static final String LINK_PREFIX = "cosmic_help:", KEY_PREFIX = "cosmic_key:";
    interface Typesetter {
        List<FormattedCharSequence> wrap(Component text, int width);
        int lineHeight();
        float glyph(int codepoint, Style style);
    }
    record Line(int x, int y, FormattedCharSequence text, int color, float scale, int height) {}
    record Target(HelpMenuGeometry.Rect bounds, String pageId, String keyId, ItemStack item) {
        boolean isItem() { return item != null; }
    }
    record Layout(List<Line> lines, List<Target> targets, int height) {
        Target hit(int x, int y) {
            for (var target : targets) if (target.bounds().contains(x, y)) return target;
            return null;
        }
        Target hitInViewport(HelpMenuGeometry.Rect viewport, int offset, double x, double y) {
            return !viewport.contains(x, y) ? null : hit((int) x - viewport.x(), (int) y - viewport.y() + offset);
        }
    }
    private HelpRichTextRenderer() {}
    static Layout layout(Font font, List<HelpMenuContent.HelpBlock> blocks, int width) {
        return layout(new Typesetter() {
            public List<FormattedCharSequence> wrap(Component text, int available) { return font.split(text, available); }
            public int lineHeight() { return font.lineHeight; }
            public float glyph(int codepoint, Style style) { return font.getSplitter().stringWidth(FormattedCharSequence.codepoint(codepoint, style)); }
        }, blocks, width, HelpDemoItems::create);
    }
    static Layout layout(Typesetter type, List<HelpMenuContent.HelpBlock> blocks, int width,
                         Function<String, List<ItemStack>> examples) {
        var lines = new ArrayList<Line>();
        var targets = new ArrayList<Target>();
        int y = 0, available = Math.max(1, width);
        for (var block : blocks) {
            if (block.kind() == HelpMenuContent.Kind.SPACER) { y += 8; continue; }
            if (block.kind() == HelpMenuContent.Kind.ITEMS) {
                int x = 0;
                for (var item : examples.apply(block.text().getString())) {
                    if (x > 0 && x + 22 > available) { x = 0; y += 26; }
                    targets.add(new Target(new HelpMenuGeometry.Rect(x, y, 22, 22), null, null, item.copy()));
                    x += 26;
                }
                if (x > 0) y += 28;
                continue;
            }
            float scale = block.kind() == HelpMenuContent.Kind.HEADING ? 1.08F : 1F;
            int inset = block.kind() == HelpMenuContent.Kind.BULLET ? 8 : 0;
            int color = switch (block.kind()) {
                case HEADING -> HEADING;
                case TIP -> 0xFF9FD7FF;
                case COMMAND -> 0xFFA5D6A7;
                default -> BODY;
            };
            Component text = switch (block.kind()) {
                case HEADING -> block.text().copy().withStyle(ChatFormatting.BOLD);
                case COMMAND -> block.label().copy().withStyle(ChatFormatting.BOLD).append(Component.literal(": ").withStyle(ChatFormatting.RESET)).append(block.text());
                case TIP -> Component.literal("Note: ").withStyle(ChatFormatting.ITALIC).append(block.text());
                default -> block.text();
            };
            int advance = (int) Math.ceil(type.lineHeight() * scale) + 3;
            boolean first = true;
            for (var sequence : type.wrap(text, Math.max(1, (int) ((available - inset) / scale)))) {
                if (first && inset > 0) lines.add(new Line(0, y, Component.literal("•").getVisualOrderText(), color, 1F, advance));
                lines.add(new Line(inset, y, sequence, color, scale, advance));
                collectTargets(sequence, type, targets, inset, y, scale, advance);
                y += advance;
                first = false;
            }
            y += block.kind() == HelpMenuContent.Kind.HEADING ? 5 : 7;
        }
        return new Layout(List.copyOf(lines), List.copyOf(targets), y);
    }
    private static void collectTargets(FormattedCharSequence sequence, Typesetter type, List<Target> targets,
                                       int x, int y, float scale, int height) {
        float[] pen = {x};
        sequence.accept((index, style, codepoint) -> {
            float next = pen[0] + type.glyph(codepoint, style) * scale;
            String insertion = style.getInsertion();
            String page = insertion != null && insertion.startsWith(LINK_PREFIX) ? insertion.substring(LINK_PREFIX.length()) : null;
            String key = insertion != null && insertion.startsWith(KEY_PREFIX) ? insertion.substring(KEY_PREFIX.length()) : null;
            if (page != null || key != null) {
                int left = (int) Math.floor(pen[0]), right = (int) Math.ceil(next);
                var bounds = new HelpMenuGeometry.Rect(left, y, Math.max(1, right - left), height);
                int last = targets.size() - 1;
                if (last >= 0) {
                    var previous = targets.get(last);
                    if (!previous.isItem() && java.util.Objects.equals(page, previous.pageId()) && java.util.Objects.equals(key, previous.keyId())
                            && previous.bounds().y() == y && previous.bounds().right() >= left) {
                        var old = previous.bounds();
                        targets.set(last, new Target(new HelpMenuGeometry.Rect(old.x(), y, right - old.x(), height), page, key, null));
                        pen[0] = next;
                        return true;
                    }
                }
                targets.add(new Target(bounds, page, key, null));
            }
            pen[0] = next;
            return true;
        });
    }
    static void render(GuiGraphics g, Font font, Layout layout, HelpMenuGeometry.Rect viewport, int offset,
                       Target hovered, Target focused) {
        g.enableScissor(viewport.x(), viewport.y(), viewport.right(), viewport.bottom());
        try {
            for (var target : layout.targets()) {
                var r = target.bounds().move(viewport.x(), viewport.y() - offset);
                if (!r.intersects(viewport)) continue;
                if (target.isItem()) {
                    fill(g, r, target == hovered || target == focused ? 0xFF325573 : 0xD0202730);
                    g.renderFakeItem(target.item(), r.x() + 3, r.y() + 3);
                } else if (target == hovered || target == focused) fill(g, r, 0xAA16374B);
                if (target == focused) border(g, r, LINK);
            }
            for (var line : layout.lines()) {
                int y = viewport.y() + line.y() - offset;
                if (y + line.height() <= viewport.y() || y >= viewport.bottom()) continue;
                g.pose().pushMatrix();
                g.pose().translate(viewport.x() + line.x(), y);
                g.pose().scale(line.scale(), line.scale());
                g.drawString(font, line.text(), 0, 0, line.color(), false);
                g.pose().popMatrix();
            }
        } finally { g.disableScissor(); }
    }
    static void fill(GuiGraphics g, HelpMenuGeometry.Rect r, int color) { if (r.w() > 0 && r.h() > 0) g.fill(r.x(), r.y(), r.right(), r.bottom(), color); }
    static void border(GuiGraphics g, HelpMenuGeometry.Rect r, int color) {
        if (r.w() < 1 || r.h() < 1) return;
        g.fill(r.x(), r.y(), r.right(), r.y() + 1, color);
        g.fill(r.x(), r.bottom() - 1, r.right(), r.bottom(), color);
        g.fill(r.x(), r.y(), r.x() + 1, r.bottom(), color);
        g.fill(r.right() - 1, r.y(), r.right(), r.bottom(), color);
    }
}
