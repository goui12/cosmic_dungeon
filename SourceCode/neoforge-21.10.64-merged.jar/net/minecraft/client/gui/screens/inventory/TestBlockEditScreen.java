package net.minecraft.client.gui.screens.inventory;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSetTestBlockPacket;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.TestBlockEntity;
import net.minecraft.world.level.block.state.properties.TestBlockMode;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TestBlockEditScreen extends Screen {
    private static final List<TestBlockMode> MODES = List.of(TestBlockMode.values());
    private static final Component TITLE = Component.translatable(Blocks.TEST_BLOCK.getDescriptionId());
    private static final Component MESSAGE_LABEL = Component.translatable("test_block.message");
    private final BlockPos position;
    private TestBlockMode mode;
    private String message;
    @Nullable
    private EditBox messageEdit;

    public TestBlockEditScreen(TestBlockEntity blockEntity) {
        super(TITLE);
        this.position = blockEntity.getBlockPos();
        this.mode = blockEntity.getMode();
        this.message = blockEntity.getMessage();
    }

    @Override
    public void init() {
        this.messageEdit = new EditBox(this.font, this.width / 2 - 152, 80, 240, 20, Component.translatable("test_block.message"));
        this.messageEdit.setMaxLength(128);
        this.messageEdit.setValue(this.message);
        this.addRenderableWidget(this.messageEdit);
        this.setInitialFocus(this.messageEdit);
        this.updateMode(this.mode);
        this.addRenderableWidget(
            CycleButton.builder(TestBlockMode::getDisplayName)
                .withValues(MODES)
                .displayOnlyValue()
                .withInitialValue(this.mode)
                .create(this.width / 2 - 4 - 150, 185, 50, 20, TITLE, (p_397349_, p_397276_) -> this.updateMode(p_397276_))
        );
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, p_397828_ -> this.onDone()).bounds(this.width / 2 - 4 - 150, 210, 150, 20).build());
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, p_397482_ -> this.onCancel()).bounds(this.width / 2 + 4, 210, 150, 20).build());
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
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 10, -1);
        if (this.mode != TestBlockMode.START) {
            guiGraphics.drawString(this.font, MESSAGE_LABEL, this.width / 2 - 153, 70, -6250336);
        }

        guiGraphics.drawString(this.font, this.mode.getDetailedMessage(), this.width / 2 - 153, 174, -6250336);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean isInGameUi() {
        return true;
    }

    private void onDone() {
        this.message = this.messageEdit.getValue();
        this.minecraft.getConnection().send(new ServerboundSetTestBlockPacket(this.position, this.mode, this.message));
        this.onClose();
    }

    @Override
    public void onClose() {
        this.onCancel();
    }

    private void onCancel() {
        this.minecraft.setScreen(null);
    }

    private void updateMode(TestBlockMode mode) {
        this.mode = mode;
        this.messageEdit.visible = mode != TestBlockMode.START;
    }
}
