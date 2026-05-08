package net.minecraft.client.gui.screens.packs;

import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TransferableSelectionList extends ObjectSelectionList<TransferableSelectionList.Entry> {
    static final ResourceLocation SELECT_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace("transferable_list/select_highlighted");
    static final ResourceLocation SELECT_SPRITE = ResourceLocation.withDefaultNamespace("transferable_list/select");
    static final ResourceLocation UNSELECT_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace("transferable_list/unselect_highlighted");
    static final ResourceLocation UNSELECT_SPRITE = ResourceLocation.withDefaultNamespace("transferable_list/unselect");
    static final ResourceLocation MOVE_UP_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace("transferable_list/move_up_highlighted");
    static final ResourceLocation MOVE_UP_SPRITE = ResourceLocation.withDefaultNamespace("transferable_list/move_up");
    static final ResourceLocation MOVE_DOWN_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace("transferable_list/move_down_highlighted");
    static final ResourceLocation MOVE_DOWN_SPRITE = ResourceLocation.withDefaultNamespace("transferable_list/move_down");
    static final Component INCOMPATIBLE_TITLE = Component.translatable("pack.incompatible");
    static final Component INCOMPATIBLE_CONFIRM_TITLE = Component.translatable("pack.incompatible.confirm.title");
    private static final int ENTRY_PADDING = 2;
    private final Component title;
    final PackSelectionScreen screen;

    public TransferableSelectionList(Minecraft minecraft, PackSelectionScreen screen, int width, int height, Component title) {
        super(minecraft, width, height, 33, 36);
        this.screen = screen;
        this.title = title;
        this.centerListVertically = false;
    }

    @Override
    public int getRowWidth() {
        return this.width - 4;
    }

    @Override
    protected int scrollBarX() {
        return this.getRight() - 6;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return this.getSelected() != null ? this.getSelected().keyPressed(event) : super.keyPressed(event);
    }

    public void updateList(Stream<PackSelectionModel.Entry> entries, @Nullable PackSelectionModel.EntryBase focused) {
        this.clearEntries();
        Component component = Component.empty().append(this.title).withStyle(ChatFormatting.UNDERLINE, ChatFormatting.BOLD);
        this.addEntry(new TransferableSelectionList.HeaderEntry(this.minecraft.font, component), (int)(9.0F * 1.5F));
        this.setSelected(null);
        entries.forEach(p_439515_ -> {
            TransferableSelectionList.PackEntry transferableselectionlist$packentry = new TransferableSelectionList.PackEntry(this.minecraft, this, p_439515_);
            this.addEntry(transferableselectionlist$packentry);
            if (focused != null && focused.getId().equals(p_439515_.getId())) {
                this.screen.setFocused(this);
                this.setFocused(transferableselectionlist$packentry);
            }
        });
    }

    @OnlyIn(Dist.CLIENT)
    public abstract class Entry extends ObjectSelectionList.Entry<TransferableSelectionList.Entry> {
        @Override
        public int getWidth() {
            return super.getWidth() - (TransferableSelectionList.this.scrollbarVisible() ? 6 : 0);
        }

        public abstract String getPackId();
    }

    @OnlyIn(Dist.CLIENT)
    public class HeaderEntry extends TransferableSelectionList.Entry {
        private final Font font;
        private final Component text;

        public HeaderEntry(Font font, Component text) {
            this.font = font;
            this.text = text;
        }

        @Override
        public void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean isHovering, float partialTick) {
            guiGraphics.drawCenteredString(this.font, this.text, this.getX() + this.getWidth() / 2, this.getContentYMiddle() - 9 / 2, -1);
        }

        @Override
        public Component getNarration() {
            return this.text;
        }

        @Override
        public String getPackId() {
            return "";
        }
    }

    @OnlyIn(Dist.CLIENT)
    public class PackEntry extends TransferableSelectionList.Entry {
        private static final int MAX_DESCRIPTION_WIDTH_PIXELS = 157;
        private static final int MAX_NAME_WIDTH_PIXELS = 157;
        private static final String TOO_LONG_NAME_SUFFIX = "...";
        private final TransferableSelectionList parent;
        protected final Minecraft minecraft;
        private final PackSelectionModel.Entry pack;
        private final FormattedCharSequence nameDisplayCache;
        private final MultiLineLabel descriptionDisplayCache;
        private final FormattedCharSequence incompatibleNameDisplayCache;
        private final MultiLineLabel incompatibleDescriptionDisplayCache;

        public PackEntry(Minecraft minecraft, TransferableSelectionList parent, PackSelectionModel.Entry entry) {
            this.minecraft = minecraft;
            this.pack = entry;
            this.parent = parent;
            this.nameDisplayCache = cacheName(minecraft, entry.getTitle());
            this.descriptionDisplayCache = cacheDescription(minecraft, entry.getExtendedDescription());
            this.incompatibleNameDisplayCache = cacheName(minecraft, TransferableSelectionList.INCOMPATIBLE_TITLE);
            this.incompatibleDescriptionDisplayCache = cacheDescription(minecraft, entry.getCompatibility().getDescription());
        }

        private static FormattedCharSequence cacheName(Minecraft minecraft, Component name) {
            int i = minecraft.font.width(name);
            if (i > 157) {
                FormattedText formattedtext = FormattedText.composite(
                    minecraft.font.substrByWidth(name, 157 - minecraft.font.width("...")), FormattedText.of("...")
                );
                return Language.getInstance().getVisualOrder(formattedtext);
            } else {
                return name.getVisualOrderText();
            }
        }

        private static MultiLineLabel cacheDescription(Minecraft minecraft, Component text) {
            return MultiLineLabel.create(minecraft.font, 157, 2, text);
        }

        @Override
        public Component getNarration() {
            return Component.translatable("narrator.select", this.pack.getTitle());
        }

        @Override
        public void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean isHovering, float partialTick) {
            PackCompatibility packcompatibility = this.pack.getCompatibility();
            if (!packcompatibility.isCompatible()) {
                int i = this.getContentX() - 1;
                int j = this.getContentY() - 1;
                int k = this.getContentRight() + 1;
                int l = this.getContentBottom() + 1;
                guiGraphics.fill(i, j, k, l, -8978432);
            }

            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, this.pack.getIconTexture(), this.getContentX(), this.getContentY(), 0.0F, 0.0F, 32, 32, 32, 32);
            FormattedCharSequence formattedcharsequence = this.nameDisplayCache;
            MultiLineLabel multilinelabel = this.descriptionDisplayCache;
            if (this.showHoverOverlay()
                && (this.minecraft.options.touchscreen().get() || isHovering || this.parent.getSelected() == this && this.parent.isFocused())) {
                guiGraphics.fill(this.getContentX(), this.getContentY(), this.getContentX() + 32, this.getContentY() + 32, -1601138544);
                int i1 = mouseX - this.getContentX();
                int j1 = mouseY - this.getContentY();
                if (!this.pack.getCompatibility().isCompatible()) {
                    formattedcharsequence = this.incompatibleNameDisplayCache;
                    multilinelabel = this.incompatibleDescriptionDisplayCache;
                }

                if (this.pack.canSelect()) {
                    if (i1 < 32) {
                        guiGraphics.blitSprite(
                            RenderPipelines.GUI_TEXTURED, TransferableSelectionList.SELECT_HIGHLIGHTED_SPRITE, this.getContentX(), this.getContentY(), 32, 32
                        );
                    } else {
                        guiGraphics.blitSprite(
                            RenderPipelines.GUI_TEXTURED, TransferableSelectionList.SELECT_SPRITE, this.getContentX(), this.getContentY(), 32, 32
                        );
                    }
                } else {
                    if (this.pack.canUnselect()) {
                        if (i1 < 16) {
                            guiGraphics.blitSprite(
                                RenderPipelines.GUI_TEXTURED,
                                TransferableSelectionList.UNSELECT_HIGHLIGHTED_SPRITE,
                                this.getContentX(),
                                this.getContentY(),
                                32,
                                32
                            );
                        } else {
                            guiGraphics.blitSprite(
                                RenderPipelines.GUI_TEXTURED, TransferableSelectionList.UNSELECT_SPRITE, this.getContentX(), this.getContentY(), 32, 32
                            );
                        }
                    }

                    if (this.pack.canMoveUp()) {
                        if (i1 < 32 && i1 > 16 && j1 < 16) {
                            guiGraphics.blitSprite(
                                RenderPipelines.GUI_TEXTURED,
                                TransferableSelectionList.MOVE_UP_HIGHLIGHTED_SPRITE,
                                this.getContentX(),
                                this.getContentY(),
                                32,
                                32
                            );
                        } else {
                            guiGraphics.blitSprite(
                                RenderPipelines.GUI_TEXTURED, TransferableSelectionList.MOVE_UP_SPRITE, this.getContentX(), this.getContentY(), 32, 32
                            );
                        }
                    }

                    if (this.pack.canMoveDown()) {
                        if (i1 < 32 && i1 > 16 && j1 > 16) {
                            guiGraphics.blitSprite(
                                RenderPipelines.GUI_TEXTURED,
                                TransferableSelectionList.MOVE_DOWN_HIGHLIGHTED_SPRITE,
                                this.getContentX(),
                                this.getContentY(),
                                32,
                                32
                            );
                        } else {
                            guiGraphics.blitSprite(
                                RenderPipelines.GUI_TEXTURED, TransferableSelectionList.MOVE_DOWN_SPRITE, this.getContentX(), this.getContentY(), 32, 32
                            );
                        }
                    }
                }
            }

            guiGraphics.drawString(this.minecraft.font, formattedcharsequence, this.getContentX() + 32 + 2, this.getContentY() + 1, -1);
            multilinelabel.render(guiGraphics, MultiLineLabel.Align.LEFT, this.getContentX() + 32 + 2, this.getContentY() + 12, 10, true, -8355712);
        }

        @Override
        public String getPackId() {
            return this.pack.getId();
        }

        private boolean showHoverOverlay() {
            return !this.pack.isFixedPosition() || !this.pack.isRequired();
        }

        @Override
        public boolean keyPressed(KeyEvent event) {
            if (event.isConfirmation()) {
                this.keyboardSelection();
                return true;
            } else {
                if (event.hasShiftDown()) {
                    if (event.isUp()) {
                        this.keyboardMoveUp();
                        return true;
                    }

                    if (event.isDown()) {
                        this.keyboardMoveDown();
                        return true;
                    }
                }

                return super.keyPressed(event);
            }
        }

        public void keyboardSelection() {
            if (this.pack.canSelect()) {
                this.handlePackSelection();
            } else if (this.pack.canUnselect()) {
                this.pack.unselect();
            }
        }

        private void keyboardMoveUp() {
            if (this.pack.canMoveUp()) {
                this.pack.moveUp();
            }
        }

        private void keyboardMoveDown() {
            if (this.pack.canMoveDown()) {
                this.pack.moveDown();
            }
        }

        private void handlePackSelection() {
            if (this.pack.getCompatibility().isCompatible()) {
                this.pack.select();
            } else {
                Component component = this.pack.getCompatibility().getConfirmation();
                this.minecraft.setScreen(new ConfirmScreen(p_264693_ -> {
                    this.minecraft.setScreen(this.parent.screen);
                    if (p_264693_) {
                        this.pack.select();
                    }
                }, TransferableSelectionList.INCOMPATIBLE_CONFIRM_TITLE, component));
            }
        }

        @Override
        public boolean shouldTakeFocusAfterInteraction() {
            return TransferableSelectionList.this.children().stream().anyMatch(p_438744_ -> p_438744_.getPackId().equals(this.getPackId()));
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
            double d0 = event.x() - this.getX();
            double d1 = event.y() - this.getY();
            if (this.showHoverOverlay() && d0 <= 32.0) {
                this.parent.screen.clearSelected();
                if (this.pack.canSelect()) {
                    this.handlePackSelection();
                    return true;
                }

                if (d0 < 16.0 && this.pack.canUnselect()) {
                    this.pack.unselect();
                    return true;
                }

                if (d0 > 16.0 && d1 < 16.0 && this.pack.canMoveUp()) {
                    this.pack.moveUp();
                    return true;
                }

                if (d0 > 16.0 && d1 > 16.0 && this.pack.canMoveDown()) {
                    this.pack.moveDown();
                    return true;
                }
            }

            return super.mouseClicked(event, isDoubleClick);
        }
    }
}
