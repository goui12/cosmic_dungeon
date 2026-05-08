package com.mojang.realmsclient.gui.screens.configuration;

import com.google.common.collect.ImmutableList;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.RealmsSlot;
import com.mojang.realmsclient.dto.RealmsWorldOptions;
import com.mojang.realmsclient.gui.screens.RealmsPopups;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.PopupScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsLabel;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RealmsSlotOptionsScreen extends RealmsScreen {
    private static final int DEFAULT_DIFFICULTY = 2;
    public static final List<Difficulty> DIFFICULTIES = ImmutableList.of(Difficulty.PEACEFUL, Difficulty.EASY, Difficulty.NORMAL, Difficulty.HARD);
    private static final int DEFAULT_GAME_MODE = 0;
    public static final List<GameType> GAME_MODES = ImmutableList.of(GameType.SURVIVAL, GameType.CREATIVE, GameType.ADVENTURE);
    private static final Component NAME_LABEL = Component.translatable("mco.configure.world.edit.slot.name");
    static final Component SPAWN_PROTECTION_TEXT = Component.translatable("mco.configure.world.spawnProtection");
    private EditBox nameEdit;
    protected final RealmsConfigureWorldScreen parentScreen;
    private int column1X;
    private int columnWidth;
    private final RealmsSlot slot;
    private final RealmsServer.WorldType worldType;
    private Difficulty difficulty;
    private GameType gameMode;
    private final String defaultSlotName;
    private String worldName;
    int spawnProtection;
    private boolean forceGameMode;
    RealmsSlotOptionsScreen.SettingsSlider spawnProtectionButton;

    public RealmsSlotOptionsScreen(RealmsConfigureWorldScreen parentScreen, RealmsSlot slot, RealmsServer.WorldType worldType, int slotIndex) {
        super(Component.translatable("mco.configure.world.buttons.options"));
        this.parentScreen = parentScreen;
        this.slot = slot;
        this.worldType = worldType;
        this.difficulty = findByIndex(DIFFICULTIES, slot.options.difficulty, 2);
        this.gameMode = findByIndex(GAME_MODES, slot.options.gameMode, 0);
        this.defaultSlotName = slot.options.getDefaultSlotName(slotIndex);
        this.setWorldName(slot.options.getSlotName(slotIndex));
        if (worldType == RealmsServer.WorldType.NORMAL) {
            this.spawnProtection = slot.options.spawnProtection;
            this.forceGameMode = slot.options.forceGameMode;
        } else {
            this.spawnProtection = 0;
            this.forceGameMode = false;
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parentScreen);
    }

    private static <T> T findByIndex(List<T> values, int index, int defaultIndex) {
        try {
            return values.get(index);
        } catch (IndexOutOfBoundsException indexoutofboundsexception) {
            return values.get(defaultIndex);
        }
    }

    private static <T> int findIndex(List<T> values, T item, int defaultIndex) {
        int i = values.indexOf(item);
        return i == -1 ? defaultIndex : i;
    }

    @Override
    public void init() {
        this.columnWidth = 170;
        this.column1X = this.width / 2 - this.columnWidth;
        int i = this.width / 2 + 10;
        if (this.worldType != RealmsServer.WorldType.NORMAL) {
            Component component;
            if (this.worldType == RealmsServer.WorldType.ADVENTUREMAP) {
                component = Component.translatable("mco.configure.world.edit.subscreen.adventuremap");
            } else if (this.worldType == RealmsServer.WorldType.INSPIRATION) {
                component = Component.translatable("mco.configure.world.edit.subscreen.inspiration");
            } else {
                component = Component.translatable("mco.configure.world.edit.subscreen.experience");
            }

            this.addLabel(new RealmsLabel(component, this.width / 2, 26, -65536));
        }

        this.nameEdit = this.addWidget(
            new EditBox(this.minecraft.font, this.column1X, row(1), this.columnWidth, 20, null, Component.translatable("mco.configure.world.edit.slot.name"))
        );
        this.nameEdit.setValue(this.worldName);
        this.nameEdit.setResponder(this::setWorldName);
        CycleButton<Difficulty> cyclebutton2 = this.addRenderableWidget(
            CycleButton.builder(Difficulty::getDisplayName)
                .withValues(DIFFICULTIES)
                .withInitialValue(this.difficulty)
                .create(i, row(1), this.columnWidth, 20, Component.translatable("options.difficulty"), (p_448675_, p_448676_) -> this.difficulty = p_448676_)
        );
        CycleButton<GameType> cyclebutton = this.addRenderableWidget(
            CycleButton.builder(GameType::getShortDisplayName)
                .withValues(GAME_MODES)
                .withInitialValue(this.gameMode)
                .create(
                    this.column1X,
                    row(3),
                    this.columnWidth,
                    20,
                    Component.translatable("selectWorld.gameMode"),
                    (p_419688_, p_419516_) -> this.gameMode = p_419516_
                )
        );
        CycleButton<Boolean> cyclebutton1 = this.addRenderableWidget(
            CycleButton.onOffBuilder(this.forceGameMode)
                .create(
                    i,
                    row(3),
                    this.columnWidth,
                    20,
                    Component.translatable("mco.configure.world.forceGameMode"),
                    (p_419549_, p_419556_) -> this.forceGameMode = p_419556_
                )
        );
        this.spawnProtectionButton = this.addRenderableWidget(
            new RealmsSlotOptionsScreen.SettingsSlider(this.column1X, row(5), this.columnWidth, this.spawnProtection, 0.0F, 16.0F)
        );
        if (this.worldType != RealmsServer.WorldType.NORMAL) {
            this.spawnProtectionButton.active = false;
            cyclebutton1.active = false;
        }

        if (this.slot.isHardcore()) {
            cyclebutton2.active = false;
            cyclebutton.active = false;
            cyclebutton1.active = false;
        }

        this.addRenderableWidget(
            Button.builder(Component.translatable("mco.configure.world.buttons.done"), p_419696_ -> this.saveSettings())
                .bounds(this.column1X, row(13), this.columnWidth, 20)
                .build()
        );
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, p_420045_ -> this.onClose()).bounds(i, row(13), this.columnWidth, 20).build());
    }

    private CycleButton.OnValueChange<Boolean> confirmDangerousOption(Component text, Consumer<Boolean> callback) {
        return (p_419961_, p_419623_) -> {
            if (p_419623_) {
                callback.accept(true);
            } else {
                this.minecraft.setScreen(RealmsPopups.warningPopupScreen(this, text, p_419504_ -> {
                    callback.accept(false);
                    p_419504_.onClose();
                }));
            }
        };
    }

    @Override
    public Component getNarrationMessage() {
        return CommonComponents.joinForNarration(this.getTitle(), this.createLabelNarration());
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
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 17, -1);
        guiGraphics.drawString(this.font, NAME_LABEL, this.column1X + this.columnWidth / 2 - this.font.width(NAME_LABEL) / 2, row(0) - 5, -1);
        this.nameEdit.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void setWorldName(String worldName) {
        if (worldName.equals(this.defaultSlotName)) {
            this.worldName = "";
        } else {
            this.worldName = worldName;
        }
    }

    private void saveSettings() {
        int i = findIndex(DIFFICULTIES, this.difficulty, 2);
        int j = findIndex(GAME_MODES, this.gameMode, 0);
        if (this.worldType != RealmsServer.WorldType.ADVENTUREMAP
            && this.worldType != RealmsServer.WorldType.EXPERIENCE
            && this.worldType != RealmsServer.WorldType.INSPIRATION) {
            this.parentScreen
                .saveSlotSettings(
                    new RealmsSlot(
                        this.slot.slotId,
                        new RealmsWorldOptions(
                            this.spawnProtection, i, j, this.forceGameMode, this.worldName, this.slot.options.version, this.slot.options.compatibility
                        ),
                        this.slot.settings
                    )
                );
        } else {
            this.parentScreen
                .saveSlotSettings(
                    new RealmsSlot(
                        this.slot.slotId,
                        new RealmsWorldOptions(
                            this.slot.options.spawnProtection,
                            i,
                            j,
                            this.slot.options.forceGameMode,
                            this.worldName,
                            this.slot.options.version,
                            this.slot.options.compatibility
                        ),
                        this.slot.settings
                    )
                );
        }
    }

    @OnlyIn(Dist.CLIENT)
    class SettingsSlider extends AbstractSliderButton {
        private final double minValue;
        private final double maxValue;

        public SettingsSlider(int x, int y, int width, int value, float minValue, float maxValue) {
            super(x, y, width, 20, CommonComponents.EMPTY, 0.0);
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.value = (Mth.clamp((float)value, minValue, maxValue) - minValue) / (maxValue - minValue);
            this.updateMessage();
        }

        @Override
        public void applyValue() {
            if (RealmsSlotOptionsScreen.this.spawnProtectionButton.active) {
                RealmsSlotOptionsScreen.this.spawnProtection = (int)Mth.lerp(Mth.clamp(this.value, 0.0, 1.0), this.minValue, this.maxValue);
            }
        }

        @Override
        protected void updateMessage() {
            this.setMessage(
                CommonComponents.optionNameValue(
                    RealmsSlotOptionsScreen.SPAWN_PROTECTION_TEXT,
                    (Component)(RealmsSlotOptionsScreen.this.spawnProtection == 0
                        ? CommonComponents.OPTION_OFF
                        : Component.literal(String.valueOf(RealmsSlotOptionsScreen.this.spawnProtection)))
                )
            );
        }
    }
}
