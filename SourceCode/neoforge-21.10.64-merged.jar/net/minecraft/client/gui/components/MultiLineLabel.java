package net.minecraft.client.gui.components;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface MultiLineLabel {
    MultiLineLabel EMPTY = new MultiLineLabel() {
        @Override
        public int render(GuiGraphics p_283645_, MultiLineLabel.Align p_437259_, int p_94389_, int p_94390_, int p_94391_, boolean p_437220_, int p_94392_) {
            return p_94390_;
        }

        @Override
        public Style getStyle(MultiLineLabel.Align p_437348_, int p_437323_, int p_437227_, int p_437413_, double p_437309_, double p_437369_) {
            return null;
        }

        @Override
        public int getLineCount() {
            return 0;
        }

        @Override
        public int getWidth() {
            return 0;
        }
    };

    static MultiLineLabel create(Font font, Component... components) {
        return create(font, Integer.MAX_VALUE, Integer.MAX_VALUE, components);
    }

    static MultiLineLabel create(Font font, int maxWidth, Component... components) {
        return create(font, maxWidth, Integer.MAX_VALUE, components);
    }

    static MultiLineLabel create(Font font, Component component, int maxWidth) {
        return create(font, maxWidth, Integer.MAX_VALUE, component);
    }

    static MultiLineLabel create(final Font font, final int maxWidth, final int maxRows, final Component... components) {
        return components.length == 0
            ? EMPTY
            : new MultiLineLabel() {
                @Nullable
                private List<MultiLineLabel.TextAndWidth> cachedTextAndWidth;
                @Nullable
                private Language splitWithLanguage;

                @Override
                public int render(
                    GuiGraphics p_437254_, MultiLineLabel.Align p_437395_, int p_437400_, int p_437314_, int p_437245_, boolean p_437290_, int p_437256_
                ) {
                    int i = p_437314_;

                    for (MultiLineLabel.TextAndWidth multilinelabel$textandwidth : this.getSplitMessage()) {
                        int j = p_437395_.calculateLeft(p_437400_, multilinelabel$textandwidth.width);
                        p_437254_.drawString(font, multilinelabel$textandwidth.text, j, i, p_437256_);
                        i += p_437245_;
                    }

                    return i;
                }

                @Nullable
                @Override
                public Style getStyle(MultiLineLabel.Align p_437374_, int p_437408_, int p_437385_, int p_437190_, double p_437187_, double p_437216_) {
                    List<MultiLineLabel.TextAndWidth> list = this.getSplitMessage();
                    int i = Mth.floor((p_437216_ - p_437385_) / p_437190_);
                    if (i >= 0 && i < list.size()) {
                        MultiLineLabel.TextAndWidth multilinelabel$textandwidth = list.get(i);
                        int j = p_437374_.calculateLeft(p_437408_, multilinelabel$textandwidth.width);
                        if (p_437187_ < j) {
                            return null;
                        } else {
                            int k = Mth.floor(p_437187_ - j);
                            return font.getSplitter().componentStyleAtWidth(multilinelabel$textandwidth.text, k);
                        }
                    } else {
                        return null;
                    }
                }

                private List<MultiLineLabel.TextAndWidth> getSplitMessage() {
                    Language language = Language.getInstance();
                    if (this.cachedTextAndWidth != null && language == this.splitWithLanguage) {
                        return this.cachedTextAndWidth;
                    } else {
                        this.splitWithLanguage = language;
                        List<FormattedText> list = new ArrayList<>();

                        for (Component component : components) {
                            list.addAll(font.splitIgnoringLanguage(component, maxWidth));
                        }

                        this.cachedTextAndWidth = new ArrayList<>();
                        int i = Math.min(list.size(), maxRows);
                        List<FormattedText> list1 = list.subList(0, i);

                        for (int j = 0; j < list1.size(); j++) {
                            FormattedText formattedtext2 = list1.get(j);
                            FormattedCharSequence formattedcharsequence = Language.getInstance().getVisualOrder(formattedtext2);
                            if (j == list1.size() - 1 && i == maxRows && i != list.size()) {
                                FormattedText formattedtext = font.substrByWidth(
                                    formattedtext2, font.width(formattedtext2) - font.width(CommonComponents.ELLIPSIS)
                                );
                                FormattedText formattedtext1 = FormattedText.composite(formattedtext, CommonComponents.ELLIPSIS);
                                this.cachedTextAndWidth
                                    .add(new MultiLineLabel.TextAndWidth(Language.getInstance().getVisualOrder(formattedtext1), font.width(formattedtext1)));
                            } else {
                                this.cachedTextAndWidth.add(new MultiLineLabel.TextAndWidth(formattedcharsequence, font.width(formattedcharsequence)));
                            }
                        }

                        return this.cachedTextAndWidth;
                    }
                }

                @Override
                public int getLineCount() {
                    return this.getSplitMessage().size();
                }

                @Override
                public int getWidth() {
                    return Math.min(maxWidth, this.getSplitMessage().stream().mapToInt(MultiLineLabel.TextAndWidth::width).max().orElse(0));
                }
            };
    }

    int render(GuiGraphics guiGraphics, MultiLineLabel.Align align, int x, int y, int lineHeight, boolean unused, int color);

    @Nullable
    Style getStyle(MultiLineLabel.Align align, int x, int y, int height, double mouseX, double mouseY);

    int getLineCount();

    int getWidth();

    @OnlyIn(Dist.CLIENT)
    public static enum Align {
        LEFT {
            @Override
            int calculateLeft(int p_437252_, int p_437215_) {
                return p_437252_;
            }
        },
        CENTER {
            @Override
            int calculateLeft(int p_437204_, int p_437426_) {
                return p_437204_ - p_437426_ / 2;
            }
        },
        RIGHT {
            @Override
            int calculateLeft(int p_437336_, int p_437360_) {
                return p_437336_ - p_437360_;
            }
        };

        abstract int calculateLeft(int x, int width);
    }

    @OnlyIn(Dist.CLIENT)
    public record TextAndWidth(FormattedCharSequence text, int width) {
    }
}
