package net.minecraft.client.gui.screens.options.controls;

import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.FocusableTextWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.lang3.ArrayUtils;

@OnlyIn(Dist.CLIENT)
public class KeyBindsList extends ContainerObjectSelectionList<KeyBindsList.Entry> {
    private static final int ITEM_HEIGHT = 20;
    final KeyBindsScreen keyBindsScreen;
    private int maxNameWidth;

    public KeyBindsList(KeyBindsScreen keyBindsScreen, Minecraft minecraft) {
        super(minecraft, keyBindsScreen.width, keyBindsScreen.layout.getContentHeight(), keyBindsScreen.layout.getHeaderHeight(), 20);
        this.keyBindsScreen = keyBindsScreen;
        KeyMapping[] akeymapping = ArrayUtils.clone((KeyMapping[])minecraft.options.keyMappings);
        Arrays.sort((Object[])akeymapping);
        KeyMapping.Category keymapping$category = null;

        for (KeyMapping keymapping : akeymapping) {
            KeyMapping.Category keymapping$category1 = keymapping.getCategory();
            if (keymapping$category1 != keymapping$category) {
                keymapping$category = keymapping$category1;
                this.addEntry(new KeyBindsList.CategoryEntry(keymapping$category1));
            }

            Component component = keymapping.getDisplayName();
            int i = minecraft.font.width(component);
            if (i > this.maxNameWidth) {
                this.maxNameWidth = i;
            }

            this.addEntry(new KeyBindsList.KeyEntry(keymapping, component));
        }
    }

    public void resetMappingAndUpdateButtons() {
        KeyMapping.resetMapping();
        this.refreshEntries();
    }

    public void refreshEntries() {
        this.children().forEach(KeyBindsList.Entry::refreshEntry);
    }

    @Override
    public int getRowWidth() {
        return 340;
    }

    @OnlyIn(Dist.CLIENT)
    public class CategoryEntry extends KeyBindsList.Entry {
        private final FocusableTextWidget categoryName;

        public CategoryEntry(KeyMapping.Category category) {
            this.categoryName = new FocusableTextWidget(
                KeyBindsList.this.getRowWidth(), category.label(), KeyBindsList.this.minecraft.font, false, FocusableTextWidget.BackgroundFill.ON_FOCUS, 4
            );
        }

        @Override
        public void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean isHovering, float partialTick) {
            this.categoryName.setPosition(KeyBindsList.this.width / 2 - this.categoryName.getWidth() / 2, this.getContentBottom() - 9 - 1);
            this.categoryName.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(this.categoryName);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(this.categoryName);
        }

        @Override
        protected void refreshEntry() {
        }
    }

    @OnlyIn(Dist.CLIENT)
    public abstract static class Entry extends ContainerObjectSelectionList.Entry<KeyBindsList.Entry> {
        abstract void refreshEntry();
    }

    @OnlyIn(Dist.CLIENT)
    public class KeyEntry extends KeyBindsList.Entry {
        private static final Component RESET_BUTTON_TITLE = Component.translatable("controls.reset");
        private static final int PADDING = 10;
        private final KeyMapping key;
        private final Component name;
        private final Button changeButton;
        private final Button resetButton;
        private boolean hasCollision = false;

        KeyEntry(KeyMapping key, Component name) {
            this.key = key;
            this.name = name;
            this.changeButton = Button.builder(name, p_345593_ -> {
                    KeyBindsList.this.keyBindsScreen.selectedKey = key;
                    KeyBindsList.this.resetMappingAndUpdateButtons();
                })
                .bounds(0, 0, 75, 20)
                .createNarration(
                    p_346090_ -> key.isUnbound()
                        ? Component.translatable("narrator.controls.unbound", name)
                        : Component.translatable("narrator.controls.bound", name, p_346090_.get())
                )
                .build();
            this.resetButton = Button.builder(RESET_BUTTON_TITLE, p_359096_ -> {
                this.key.setToDefault();
                key.setKey(key.getDefaultKey());
                KeyBindsList.this.resetMappingAndUpdateButtons();
            }).bounds(0, 0, 50, 20).createNarration(p_344899_ -> Component.translatable("narrator.controls.reset", name)).build();
            this.refreshEntry();
        }

        @Override
        public void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean isHovering, float partialTick) {
            int i = KeyBindsList.this.scrollBarX() - this.resetButton.getWidth() - 10;
            int j = this.getContentY() - 2;
            this.resetButton.setPosition(i, j);
            this.resetButton.render(guiGraphics, mouseX, mouseY, partialTick);
            int k = i - 5 - this.changeButton.getWidth();
            this.changeButton.setPosition(k, j);
            this.changeButton.render(guiGraphics, mouseX, mouseY, partialTick);
            guiGraphics.drawString(KeyBindsList.this.minecraft.font, this.name, this.getContentX(), this.getContentYMiddle() - 9 / 2, -1);
            if (this.hasCollision) {
                int l = 3;
                int i1 = this.changeButton.getX() - 6;
                guiGraphics.fill(i1, this.getContentY() - 1, i1 + 3, this.getContentBottom(), -256);
            }
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return ImmutableList.of(this.changeButton, this.resetButton);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return ImmutableList.of(this.changeButton, this.resetButton);
        }

        @Override
        protected void refreshEntry() {
            this.changeButton.setMessage(this.key.getTranslatedKeyMessage());
            this.resetButton.active = !this.key.isDefault();
            this.hasCollision = false;
            MutableComponent mutablecomponent = Component.empty();
            if (!this.key.isUnbound()) {
                for (KeyMapping keymapping : KeyBindsList.this.minecraft.options.keyMappings) {
                    if ((keymapping != this.key && this.key.same(keymapping)) || keymapping.hasKeyModifierConflict(this.key)) { // Neo: gracefully handle conflicts like SHIFT vs SHIFT+G
                        if (this.hasCollision) {
                            mutablecomponent.append(", ");
                        }

                        this.hasCollision = true;
                        mutablecomponent.append(keymapping.getDisplayName());
                    }
                }
            }

            if (this.hasCollision) {
                this.changeButton
                    .setMessage(
                        Component.literal("[ ")
                            .append(this.changeButton.getMessage().copy().withStyle(ChatFormatting.WHITE))
                            .append(" ]")
                            .withStyle(ChatFormatting.YELLOW)
                    );
                this.changeButton.setTooltip(Tooltip.create(Component.translatable("controls.keybinds.duplicateKeybinds", mutablecomponent)));
            } else {
                this.changeButton.setTooltip(null);
            }

            if (KeyBindsList.this.keyBindsScreen.selectedKey == this.key) {
                this.changeButton
                    .setMessage(
                        Component.literal("> ")
                            .append(this.changeButton.getMessage().copy().withStyle(ChatFormatting.WHITE, ChatFormatting.UNDERLINE))
                            .append(" <")
                            .withStyle(ChatFormatting.YELLOW)
                    );
            }
        }
    }
}
