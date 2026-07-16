package net.goui.cosmicdungeon.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

final class HelpRichTextRenderer {
    private static final int HEADING = 0xFFFFD98A;
    private static final int BODY = 0xFFD8D0C0;
    private static final int BULLET = 0xFFEFE6D0;
    private static final int TIP = 0xFF9FD7FF;
    private static final int COMMAND = 0xFFA5D6A7;
    private static final int BOTTOM_PADDING = 6;

    private HelpRichTextRenderer() {}

    static int measure(Font font, HelpMenuContent.Page page, int width) {
        int y = 0;
        for (HelpMenuContent.HelpBlock block : page.blocks()) y += blockHeight(font, block, width);
        return y + BOTTOM_PADDING;
    }

    static void render(GuiGraphics g, Font font, HelpMenuContent.Page page, int x, int y, int width) {
        int cursor = y;
        for (HelpMenuContent.HelpBlock block : page.blocks()) {
            cursor += renderBlock(g, font, block, x, cursor, width);
        }
    }

    private static int blockHeight(Font font, HelpMenuContent.HelpBlock block, int width) {
        if (block.kind() == HelpMenuContent.Kind.SPACER) return 8;
        int wrapWidth = wrapWidth(block.kind(), width);
        return Math.max(font.lineHeight + 4, font.split(textFor(block), wrapWidth).size() * (font.lineHeight + 2) + 4);
    }

    private static int renderBlock(GuiGraphics g, Font font, HelpMenuContent.HelpBlock block, int x, int y, int width) {
        if (block.kind() == HelpMenuContent.Kind.SPACER) return 8;
        int color = switch (block.kind()) { case HEADING -> HEADING; case BULLET -> BULLET; case TIP -> TIP; case COMMAND -> COMMAND; default -> BODY; };
        int textX = x;
        int wrapWidth = wrapWidth(block.kind(), width);
        if (block.kind() == HelpMenuContent.Kind.BULLET) {
            g.drawString(font, Component.literal("•"), x, y, BULLET, false);
            textX = x + 10;
        } else if (block.kind() == HelpMenuContent.Kind.TIP) {
            g.drawString(font, Component.literal("Note:"), x, y, TIP, false);
            textX = x + 32;
        }
        int cursor = y;
        for (FormattedCharSequence line : font.split(textFor(block), wrapWidth)) {
            g.drawString(font, line, textX, cursor, color, false);
            cursor += font.lineHeight + 2;
        }
        return Math.max(font.lineHeight + 4, cursor - y + 4);
    }

    private static int wrapWidth(HelpMenuContent.Kind kind, int width) {
        return switch (kind) {
            case BULLET -> width - 12;
            case TIP -> width - 34;
            case COMMAND -> width - 6;
            default -> width;
        };
    }

    private static Component textFor(HelpMenuContent.HelpBlock block) {
        if (block.kind() == HelpMenuContent.Kind.COMMAND && !block.label().getString().isBlank()) {
            return Component.literal(block.label().getString() + ": ").append(block.text());
        }
        return block.text();
    }
}
