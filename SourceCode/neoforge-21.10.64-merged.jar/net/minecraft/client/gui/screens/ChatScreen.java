package net.minecraft.client.gui.screens;

import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.lang3.StringUtils;

@OnlyIn(Dist.CLIENT)
public class ChatScreen extends Screen {
    public static final double MOUSE_SCROLL_SPEED = 7.0;
    private static final Component USAGE_TEXT = Component.translatable("chat_screen.usage");
    private static final int TOOLTIP_MAX_WIDTH = 210;
    private String historyBuffer = "";
    /**
     * keeps position of which chat message you will select when you press up, (does not increase for duplicated messages sent immediately after each other)
     */
    private int historyPos = -1;
    /**
     * Chat entry field
     */
    protected EditBox input;
    /**
     * is the text that appears when you press the chat key and the input box appears pre-filled
     */
    protected String initial;
    protected boolean isDraft;
    protected ChatScreen.ExitReason exitReason = ChatScreen.ExitReason.INTERRUPTED;
    CommandSuggestions commandSuggestions;

    public ChatScreen(String initial, boolean isDraft) {
        super(Component.translatable("chat_screen.title"));
        this.initial = initial;
        this.isDraft = isDraft;
    }

    @Override
    protected void init() {
        this.historyPos = this.minecraft.gui.getChat().getRecentChat().size();
        this.input = new EditBox(this.minecraft.fontFilterFishy, 4, this.height - 12, this.width - 4, 12, Component.translatable("chat.editBox")) {
            @Override
            protected MutableComponent createNarrationMessage() {
                return super.createNarrationMessage().append(ChatScreen.this.commandSuggestions.getNarrationMessage());
            }
        };
        this.input.setMaxLength(256);
        this.input.setBordered(false);
        this.input.setValue(this.initial);
        this.input.setResponder(this::onEdited);
        this.input.addFormatter(this::formatChat);
        this.input.setCanLoseFocus(false);
        this.addRenderableWidget(this.input);
        this.commandSuggestions = new CommandSuggestions(this.minecraft, this, this.input, this.font, false, false, 1, 10, true, -805306368);
        this.commandSuggestions.setAllowHiding(false);
        this.commandSuggestions.setAllowSuggestions(false);
        this.commandSuggestions.updateCommandInfo();
    }

    @Override
    protected void setInitialFocus() {
        this.setInitialFocus(this.input);
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        this.initial = this.input.getValue();
        this.init(minecraft, width, height);
    }

    @Override
    public void onClose() {
        this.exitReason = ChatScreen.ExitReason.INTENTIONAL;
        super.onClose();
    }

    @Override
    public void removed() {
        this.minecraft.gui.getChat().resetChatScroll();
        this.initial = this.input.getValue();
        if (this.shouldDiscardDraft() || StringUtils.isBlank(this.initial)) {
            this.minecraft.gui.getChat().discardDraft();
        } else if (!this.isDraft) {
            this.minecraft.gui.getChat().saveAsDraft(this.initial);
        }
    }

    protected boolean shouldDiscardDraft() {
        return this.exitReason != ChatScreen.ExitReason.INTERRUPTED
            && (this.exitReason != ChatScreen.ExitReason.INTENTIONAL || !this.minecraft.options.saveChatDrafts().get());
    }

    private void onEdited(String value) {
        this.commandSuggestions.setAllowSuggestions(true);
        this.commandSuggestions.updateCommandInfo();
        this.isDraft = false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.commandSuggestions.keyPressed(event)) {
            return true;
        } else if (this.isDraft && event.key() == 259) {
            this.input.setValue("");
            this.isDraft = false;
            return true;
        } else if (super.keyPressed(event)) {
            return true;
        } else if (event.isConfirmation()) {
            this.handleChatInput(this.input.getValue(), true);
            this.exitReason = ChatScreen.ExitReason.DONE;
            // FORGE: Prevent closing the screen if another screen has been opened.
            if (minecraft.screen == this) {
                this.minecraft.setScreen(null);
            }
            return true;
        } else {
            switch (event.key()) {
                case 264:
                    this.moveInHistory(1);
                    break;
                case 265:
                    this.moveInHistory(-1);
                    break;
                case 266:
                    this.minecraft.gui.getChat().scrollChat(this.minecraft.gui.getChat().getLinesPerPage() - 1);
                    break;
                case 267:
                    this.minecraft.gui.getChat().scrollChat(-this.minecraft.gui.getChat().getLinesPerPage() + 1);
                    break;
                default:
                    return false;
            }

            return true;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scrollY = Mth.clamp(scrollY, -1.0, 1.0);
        if (this.commandSuggestions.mouseScrolled(scrollY)) {
            return true;
        } else {
            if (!this.minecraft.hasShiftDown()) {
                scrollY *= 7.0;
            }

            this.minecraft.gui.getChat().scrollChat((int)scrollY);
            return true;
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        if (this.commandSuggestions.mouseClicked(event)) {
            return true;
        } else {
            if (event.button() == 0) {
                ChatComponent chatcomponent = this.minecraft.gui.getChat();
                if (chatcomponent.handleChatQueueClicked(event.x(), event.y())) {
                    return true;
                }

                Style style = this.getComponentStyleAt(event.x(), event.y());
                if (style != null && this.handleComponentClicked(style)) {
                    this.initial = this.input.getValue();
                    return true;
                }
            }

            return super.mouseClicked(event, isDoubleClick);
        }
    }

    @Override
    public void insertText(String text, boolean overwrite) {
        if (overwrite) {
            this.input.setValue(text);
        } else {
            this.input.insertText(text);
        }
    }

    /**
     * Input is relative and is applied directly to the sentHistoryCursor so -1 is the previous message, 1 is the next message from the current cursor position.
     */
    public void moveInHistory(int msgPos) {
        int i = this.historyPos + msgPos;
        int j = this.minecraft.gui.getChat().getRecentChat().size();
        i = Mth.clamp(i, 0, j);
        if (i != this.historyPos) {
            if (i == j) {
                this.historyPos = j;
                this.input.setValue(this.historyBuffer);
            } else {
                if (this.historyPos == j) {
                    this.historyBuffer = this.input.getValue();
                }

                this.input.setValue(this.minecraft.gui.getChat().getRecentChat().get(i));
                this.commandSuggestions.setAllowSuggestions(false);
                this.historyPos = i;
            }
        }
    }

    @Nullable
    private FormattedCharSequence formatChat(String text, int displayPos) {
        return this.isDraft ? FormattedCharSequence.forward(text, Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(true)) : null;
    }

    /**
     * Renders the graphical user interface (GUI) element.
     *
     * @param guiGraphics the GuiGraphics object used for rendering.
     * @param mouseX      the x-coordinate of the mouse cursor.
     * @param mouseY      the y-coordinate of the mouse cursor.
     * @param partialTick the partial tick time.
     */
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(2, this.height - 14, this.width - 2, this.height - 2, this.minecraft.options.getBackgroundColor(Integer.MIN_VALUE));
        this.minecraft.gui.getChat().render(guiGraphics, this.minecraft.gui.getGuiTicks(), mouseX, mouseY, true);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.commandSuggestions.render(guiGraphics, mouseX, mouseY);
        GuiMessageTag guimessagetag = this.minecraft.gui.getChat().getMessageTagAt(mouseX, mouseY);
        if (guimessagetag != null && guimessagetag.text() != null) {
            guiGraphics.setTooltipForNextFrame(this.font, this.font.split(guimessagetag.text(), 210), mouseX, mouseY);
        } else {
            Style style = this.getComponentStyleAt(mouseX, mouseY);
            guiGraphics.renderComponentHoverEffect(this.font, style, mouseX, mouseY);
        }
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean isAllowedInPortal() {
        return true;
    }

    @Override
    protected void updateNarrationState(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, this.getTitle());
        output.add(NarratedElementType.USAGE, USAGE_TEXT);
        String s = this.input.getValue();
        if (!s.isEmpty()) {
            output.nest().add(NarratedElementType.TITLE, Component.translatable("chat_screen.message", s));
        }
    }

    @Nullable
    private Style getComponentStyleAt(double mouseX, double mouseY) {
        return this.minecraft.gui.getChat().getClickedComponentStyleAt(mouseX, mouseY);
    }

    public void handleChatInput(String message, boolean addToRecentChat) {
        message = this.normalizeChatMessage(message);
        if (!message.isEmpty()) {
            if (addToRecentChat) {
                this.minecraft.gui.getChat().addRecentChat(message);
            }

            if (message.startsWith("/")) {
                this.minecraft.player.connection.sendCommand(message.substring(1));
            } else {
                this.minecraft.player.connection.sendChat(message);
            }
        }
    }

    public String normalizeChatMessage(String message) {
        return StringUtil.trimChatMessage(StringUtils.normalizeSpace(message.trim()));
    }

    @FunctionalInterface
    @OnlyIn(Dist.CLIENT)
    public interface ChatConstructor<T extends ChatScreen> {
        T create(String initial, boolean isDraft);
    }

    @OnlyIn(Dist.CLIENT)
    protected static enum ExitReason {
        INTENTIONAL,
        INTERRUPTED,
        DONE;
    }
}
