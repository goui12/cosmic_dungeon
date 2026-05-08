package net.minecraft.client.gui.components;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EditBox extends AbstractWidget {
    private static final WidgetSprites SPRITES = new WidgetSprites(
        ResourceLocation.withDefaultNamespace("widget/text_field"), ResourceLocation.withDefaultNamespace("widget/text_field_highlighted")
    );
    public static final int BACKWARDS = -1;
    public static final int FORWARDS = 1;
    private static final int CURSOR_INSERT_WIDTH = 1;
    private static final String CURSOR_APPEND_CHARACTER = "_";
    public static final int DEFAULT_TEXT_COLOR = -2039584;
    public static final Style DEFAULT_HINT_STYLE = Style.EMPTY.withColor(ChatFormatting.DARK_GRAY);
    public static final Style SEARCH_HINT_STYLE = Style.EMPTY.applyFormats(ChatFormatting.GRAY, ChatFormatting.ITALIC);
    private static final int CURSOR_BLINK_INTERVAL_MS = 300;
    private final Font font;
    /**
     * Has the current text being edited on the textbox.
     */
    private String value = "";
    private int maxLength = 32;
    private boolean bordered = true;
    /**
     * if true the textbox can lose focus by clicking elsewhere on the screen
     */
    private boolean canLoseFocus = true;
    /**
     * If this value is true along with isFocused, keyTyped will process the keys.
     */
    private boolean isEditable = true;
    private boolean centered = false;
    private boolean textShadow = true;
    /**
     * The current character index that should be used as start of the rendered text.
     */
    private int displayPos;
    private int cursorPos;
    /**
     * other selection position, maybe the same as the cursor
     */
    private int highlightPos;
    private int textColor = -2039584;
    private int textColorUneditable = -9408400;
    @Nullable
    private String suggestion;
    @Nullable
    private Consumer<String> responder;
    /**
     * Called to check if the text is valid
     */
    private Predicate<String> filter = Objects::nonNull;
    private final List<EditBox.TextFormatter> formatters = new ArrayList<>();
    @Nullable
    private Component hint;
    private long focusedTime = Util.getMillis();
    private int textX;
    private int textY;

    public EditBox(Font font, int width, int height, Component message) {
        this(font, 0, 0, width, height, message);
    }

    public EditBox(Font font, int x, int y, int width, int height, Component message) {
        this(font, x, y, width, height, null, message);
    }

    public EditBox(Font font, int x, int y, int width, int height, @Nullable EditBox editBox, Component message) {
        super(x, y, width, height, message);
        this.font = font;
        if (editBox != null) {
            this.setValue(editBox.getValue());
        }

        this.updateTextPosition();
    }

    public void setResponder(Consumer<String> responder) {
        this.responder = responder;
    }

    public void addFormatter(EditBox.TextFormatter formatter) {
        this.formatters.add(formatter);
    }

    @Override
    protected MutableComponent createNarrationMessage() {
        Component component = this.getMessage();
        return Component.translatable("gui.narrate.editBox", component, this.value);
    }

    /**
     * Sets the text of the textbox, and moves the cursor to the end.
     */
    public void setValue(String text) {
        if (this.filter.test(text)) {
            if (text.length() > this.maxLength) {
                this.value = text.substring(0, this.maxLength);
            } else {
                this.value = text;
            }

            this.moveCursorToEnd(false);
            this.setHighlightPos(this.cursorPos);
            this.onValueChange(text);
        }
    }

    public String getValue() {
        return this.value;
    }

    public String getHighlighted() {
        int i = Math.min(this.cursorPos, this.highlightPos);
        int j = Math.max(this.cursorPos, this.highlightPos);
        return this.value.substring(i, j);
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        this.updateTextPosition();
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        this.updateTextPosition();
    }

    public void setFilter(Predicate<String> validator) {
        this.filter = validator;
    }

    /**
     * Adds the given text after the cursor, or replaces the currently selected text if there is a selection.
     */
    public void insertText(String textToWrite) {
        int i = Math.min(this.cursorPos, this.highlightPos);
        int j = Math.max(this.cursorPos, this.highlightPos);
        int k = this.maxLength - this.value.length() - (i - j);
        if (k > 0) {
            String s = StringUtil.filterText(textToWrite);
            int l = s.length();
            if (k < l) {
                if (Character.isHighSurrogate(s.charAt(k - 1))) {
                    k--;
                }

                s = s.substring(0, k);
                l = k;
            }

            String s1 = new StringBuilder(this.value).replace(i, j, s).toString();
            if (this.filter.test(s1)) {
                this.value = s1;
                this.setCursorPosition(i + l);
                this.setHighlightPos(this.cursorPos);
                this.onValueChange(this.value);
            }
        }
    }

    private void onValueChange(String newText) {
        if (this.responder != null) {
            this.responder.accept(newText);
        }

        this.updateTextPosition();
    }

    private void deleteText(int num, boolean words) {
        if (words) {
            this.deleteWords(num);
        } else {
            this.deleteChars(num);
        }
    }

    /**
     * Deletes the given number of words from the current cursor's position, unless there is currently a selection, in which case the selection is deleted instead.
     */
    public void deleteWords(int num) {
        if (!this.value.isEmpty()) {
            if (this.highlightPos != this.cursorPos) {
                this.insertText("");
            } else {
                this.deleteCharsToPos(this.getWordPosition(num));
            }
        }
    }

    /**
     * Deletes the given number of characters from the current cursor's position, unless there is currently a selection, in which case the selection is deleted instead.
     */
    public void deleteChars(int num) {
        this.deleteCharsToPos(this.getCursorPos(num));
    }

    public void deleteCharsToPos(int num) {
        if (!this.value.isEmpty()) {
            if (this.highlightPos != this.cursorPos) {
                this.insertText("");
            } else {
                int i = Math.min(num, this.cursorPos);
                int j = Math.max(num, this.cursorPos);
                if (i != j) {
                    String s = new StringBuilder(this.value).delete(i, j).toString();
                    if (this.filter.test(s)) {
                        this.value = s;
                        this.moveCursorTo(i, false);
                    }
                }
            }
        }
    }

    /**
     * Gets the starting index of the word at the specified number of words away from the cursor position.
     */
    public int getWordPosition(int numWords) {
        return this.getWordPosition(numWords, this.getCursorPosition());
    }

    /**
     * Gets the starting index of the word at a distance of the specified number of words away from the given position.
     */
    private int getWordPosition(int numWords, int pos) {
        return this.getWordPosition(numWords, pos, true);
    }

    /**
     * Like getNthWordFromPos (which wraps this), but adds option for skipping consecutive spaces
     */
    private int getWordPosition(int numWords, int pos, boolean skipConsecutiveSpaces) {
        int i = pos;
        boolean flag = numWords < 0;
        int j = Math.abs(numWords);

        for (int k = 0; k < j; k++) {
            if (!flag) {
                int l = this.value.length();
                i = this.value.indexOf(32, i);
                if (i == -1) {
                    i = l;
                } else {
                    while (skipConsecutiveSpaces && i < l && this.value.charAt(i) == ' ') {
                        i++;
                    }
                }
            } else {
                while (skipConsecutiveSpaces && i > 0 && this.value.charAt(i - 1) == ' ') {
                    i--;
                }

                while (i > 0 && this.value.charAt(i - 1) != ' ') {
                    i--;
                }
            }
        }

        return i;
    }

    public void moveCursor(int delta, boolean select) {
        this.moveCursorTo(this.getCursorPos(delta), select);
    }

    private int getCursorPos(int delta) {
        return Util.offsetByCodepoints(this.value, this.cursorPos, delta);
    }

    public void moveCursorTo(int delta, boolean select) {
        this.setCursorPosition(delta);
        if (!select) {
            this.setHighlightPos(this.cursorPos);
        }

        this.onValueChange(this.value);
    }

    public void setCursorPosition(int pos) {
        this.cursorPos = Mth.clamp(pos, 0, this.value.length());
        this.scrollTo(this.cursorPos);
    }

    public void moveCursorToStart(boolean select) {
        this.moveCursorTo(0, select);
    }

    public void moveCursorToEnd(boolean select) {
        this.moveCursorTo(this.value.length(), select);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.isActive() && this.isFocused()) {
            switch (event.key()) {
                case 259:
                    if (this.isEditable) {
                        this.deleteText(-1, event.hasControlDown());
                    }

                    return true;
                case 260:
                case 264:
                case 265:
                case 266:
                case 267:
                default:
                    if (event.isSelectAll()) {
                        this.moveCursorToEnd(false);
                        this.setHighlightPos(0);
                        return true;
                    } else if (event.isCopy()) {
                        Minecraft.getInstance().keyboardHandler.setClipboard(this.getHighlighted());
                        return true;
                    } else if (event.isPaste()) {
                        if (this.isEditable()) {
                            this.insertText(Minecraft.getInstance().keyboardHandler.getClipboard());
                        }

                        return true;
                    } else {
                        if (event.isCut()) {
                            Minecraft.getInstance().keyboardHandler.setClipboard(this.getHighlighted());
                            if (this.isEditable()) {
                                this.insertText("");
                            }

                            return true;
                        }

                        return false;
                    }
                case 261:
                    if (this.isEditable) {
                        this.deleteText(1, event.hasControlDown());
                    }

                    return true;
                case 262:
                    if (event.hasControlDown()) {
                        this.moveCursorTo(this.getWordPosition(1), event.hasShiftDown());
                    } else {
                        this.moveCursor(1, event.hasShiftDown());
                    }

                    return true;
                case 263:
                    if (event.hasControlDown()) {
                        this.moveCursorTo(this.getWordPosition(-1), event.hasShiftDown());
                    } else {
                        this.moveCursor(-1, event.hasShiftDown());
                    }

                    return true;
                case 268:
                    this.moveCursorToStart(event.hasShiftDown());
                    return true;
                case 269:
                    this.moveCursorToEnd(event.hasShiftDown());
                    return true;
            }
        } else {
            return false;
        }
    }

    public boolean canConsumeInput() {
        return this.isActive() && this.isFocused() && this.isEditable();
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (!this.canConsumeInput()) {
            return false;
        } else if (event.isAllowedChatCharacter()) {
            if (this.isEditable) {
                this.insertText(event.codepointAsString());
            }

            return true;
        } else {
            return false;
        }
    }

    private int findClickedPositionInText(MouseButtonEvent event) {
        int i = Math.min(Mth.floor(event.x()) - this.textX, this.getInnerWidth());
        String s = this.value.substring(this.displayPos);
        return this.displayPos + this.font.plainSubstrByWidth(s, i).length();
    }

    private void selectWord(MouseButtonEvent event) {
        int i = this.findClickedPositionInText(event);
        int j = this.getWordPosition(-1, i);
        int k = this.getWordPosition(1, i);
        this.moveCursorTo(j, false);
        this.moveCursorTo(k, true);
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean isDoubleClick) {
        if (isDoubleClick) {
            this.selectWord(event);
        } else {
            this.moveCursorTo(this.findClickedPositionInText(event), event.hasShiftDown());
        }
    }

    @Override
    protected void onDrag(MouseButtonEvent event, double mouseX, double mouseY) {
        this.moveCursorTo(this.findClickedPositionInText(event), true);
    }

    @Override
    public void playDownSound(SoundManager handler) {
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (this.isVisible()) {
            if (this.isBordered()) {
                ResourceLocation resourcelocation = SPRITES.get(this.isActive(), this.isFocused());
                guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, resourcelocation, this.getX(), this.getY(), this.getWidth(), this.getHeight());
            }

            int i1 = this.isEditable ? this.textColor : this.textColorUneditable;
            int i = this.cursorPos - this.displayPos;
            String s = this.font.plainSubstrByWidth(this.value.substring(this.displayPos), this.getInnerWidth());
            boolean flag = i >= 0 && i <= s.length();
            boolean flag1 = this.isFocused() && (Util.getMillis() - this.focusedTime) / 300L % 2L == 0L && flag;
            int j = this.textX;
            int k = Mth.clamp(this.highlightPos - this.displayPos, 0, s.length());
            if (!s.isEmpty()) {
                String s1 = flag ? s.substring(0, i) : s;
                FormattedCharSequence formattedcharsequence = this.applyFormat(s1, this.displayPos);
                guiGraphics.drawString(this.font, formattedcharsequence, j, this.textY, i1, this.textShadow);
                j += this.font.width(formattedcharsequence) + 1;
            }

            boolean flag2 = this.cursorPos < this.value.length() || this.value.length() >= this.getMaxLength();
            int j1 = j;
            if (!flag) {
                j1 = i > 0 ? this.textX + this.width : this.textX;
            } else if (flag2) {
                j1 = j - 1;
                j--;
            }

            if (!s.isEmpty() && flag && i < s.length()) {
                guiGraphics.drawString(this.font, this.applyFormat(s.substring(i), this.cursorPos), j, this.textY, i1, this.textShadow);
            }

            if (this.hint != null && s.isEmpty() && !this.isFocused()) {
                guiGraphics.drawString(this.font, this.hint, j, this.textY, i1);
            }

            if (!flag2 && this.suggestion != null) {
                guiGraphics.drawString(this.font, this.suggestion, j1 - 1, this.textY, -8355712, this.textShadow);
            }

            if (k != i) {
                int l = this.textX + this.font.width(s.substring(0, k));
                guiGraphics.textHighlight(Math.min(j1, this.getX() + this.width), this.textY - 1, Math.min(l - 1, this.getX() + this.width), this.textY + 1 + 9);
            }

            if (flag1) {
                if (flag2) {
                    guiGraphics.fill(j1, this.textY - 1, j1 + 1, this.textY + 1 + 9, i1);
                } else {
                    guiGraphics.drawString(this.font, "_", j1, this.textY, i1, this.textShadow);
                }
            }

            if (this.isHovered()) {
                guiGraphics.requestCursor(this.isEditable() ? CursorTypes.IBEAM : CursorTypes.NOT_ALLOWED);
            }
        }
    }

    private FormattedCharSequence applyFormat(String text, int displayPos) {
        for (EditBox.TextFormatter editbox$textformatter : this.formatters) {
            FormattedCharSequence formattedcharsequence = editbox$textformatter.format(text, displayPos);
            if (formattedcharsequence != null) {
                return formattedcharsequence;
            }
        }

        return FormattedCharSequence.forward(text, Style.EMPTY);
    }

    private void updateTextPosition() {
        if (this.font != null) {
            String s = this.font.plainSubstrByWidth(this.value.substring(this.displayPos), this.getInnerWidth());
            this.textX = this.getX() + (this.isCentered() ? (this.getWidth() - this.font.width(s)) / 2 : (this.bordered ? 4 : 0));
            this.textY = this.bordered ? this.getY() + (this.height - 8) / 2 : this.getY();
        }
    }

    /**
     * Sets the maximum length for the text in this text box. If the current text is longer than this length, the current text will be trimmed.
     */
    public void setMaxLength(int length) {
        this.maxLength = length;
        if (this.value.length() > length) {
            this.value = this.value.substring(0, length);
            this.onValueChange(this.value);
        }
    }

    private int getMaxLength() {
        return this.maxLength;
    }

    public int getCursorPosition() {
        return this.cursorPos;
    }

    public boolean isBordered() {
        return this.bordered;
    }

    /**
     * Sets whether the background and outline of this text box should be drawn.
     */
    public void setBordered(boolean enableBackgroundDrawing) {
        this.bordered = enableBackgroundDrawing;
        this.updateTextPosition();
    }

    /**
     * Sets the color to use when drawing this text box's text. A different color is used if this text box is disabled.
     */
    public void setTextColor(int color) {
        this.textColor = color;
    }

    /**
     * Sets the color to use for text in this text box when this text box is disabled.
     */
    public void setTextColorUneditable(int color) {
        this.textColorUneditable = color;
    }

    /**
     * Sets the focus state of the GUI element.
     *
     * @param focused {@code true} to apply focus, {@code false} to remove focus
     */
    @Override
    public void setFocused(boolean focused) {
        if (this.canLoseFocus || focused) {
            super.setFocused(focused);
            if (focused) {
                this.focusedTime = Util.getMillis();
            }
        }
    }

    private boolean isEditable() {
        return this.isEditable;
    }

    /**
     * Sets whether this text box is enabled. Disabled text boxes cannot be typed in.
     */
    public void setEditable(boolean enabled) {
        this.isEditable = enabled;
    }

    private boolean isCentered() {
        return this.centered;
    }

    public void setCentered(boolean centered) {
        this.centered = centered;
        this.updateTextPosition();
    }

    public void setTextShadow(boolean textShadow) {
        this.textShadow = textShadow;
    }

    public int getInnerWidth() {
        return this.isBordered() ? this.width - 8 : this.width;
    }

    /**
     * Sets the position of the selection anchor (the selection anchor and the cursor position mark the edges of the selection). If the anchor is set beyond the bounds of the current text, it will be put back inside.
     */
    public void setHighlightPos(int position) {
        this.highlightPos = Mth.clamp(position, 0, this.value.length());
        this.scrollTo(this.highlightPos);
    }

    private void scrollTo(int position) {
        if (this.font != null) {
            this.displayPos = Math.min(this.displayPos, this.value.length());
            int i = this.getInnerWidth();
            String s = this.font.plainSubstrByWidth(this.value.substring(this.displayPos), i);
            int j = s.length() + this.displayPos;
            if (position == this.displayPos) {
                this.displayPos = this.displayPos - this.font.plainSubstrByWidth(this.value, i, true).length();
            }

            if (position > j) {
                this.displayPos += position - j;
            } else if (position <= this.displayPos) {
                this.displayPos = this.displayPos - (this.displayPos - position);
            }

            this.displayPos = Mth.clamp(this.displayPos, 0, this.value.length());
        }
    }

    /**
     * Sets whether this text box loses focus when something other than it is clicked.
     */
    public void setCanLoseFocus(boolean canLoseFocus) {
        this.canLoseFocus = canLoseFocus;
    }

    public boolean isVisible() {
        return this.visible;
    }

    /**
     * Sets whether this textbox is visible.
     */
    public void setVisible(boolean isVisible) {
        this.visible = isVisible;
    }

    public void setSuggestion(@Nullable String suggestion) {
        this.suggestion = suggestion;
    }

    public int getScreenX(int charNum) {
        return charNum > this.value.length() ? this.getX() : this.getX() + this.font.width(this.value.substring(0, charNum));
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        narrationElementOutput.add(NarratedElementType.TITLE, this.createNarrationMessage());
    }

    public void setHint(Component hint) {
        boolean flag = hint.getStyle().equals(Style.EMPTY);
        this.hint = (Component)(flag ? hint.copy().withStyle(DEFAULT_HINT_STYLE) : hint);
    }

    public boolean getTextShadow() {
        return this.textShadow;
    }

    @FunctionalInterface
    @OnlyIn(Dist.CLIENT)
    public interface TextFormatter {
        @Nullable
        FormattedCharSequence format(String text, int displayPos);
    }
}
