package net.minecraft.client.gui.screens.inventory;

import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.FittingMultiLineTextWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ServerboundTestInstanceBlockActionPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.TestInstanceBlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TestInstanceBlockEditScreen extends Screen {
    private static final Component ID_LABEL = Component.translatable("test_instance_block.test_id");
    private static final Component SIZE_LABEL = Component.translatable("test_instance_block.size");
    private static final Component INCLUDE_ENTITIES_LABEL = Component.translatable("test_instance_block.entities");
    private static final Component ROTATION_LABEL = Component.translatable("test_instance_block.rotation");
    private static final int BUTTON_PADDING = 8;
    private static final int WIDTH = 316;
    private final TestInstanceBlockEntity blockEntity;
    @Nullable
    private EditBox idEdit;
    @Nullable
    private EditBox sizeXEdit;
    @Nullable
    private EditBox sizeYEdit;
    @Nullable
    private EditBox sizeZEdit;
    @Nullable
    private FittingMultiLineTextWidget infoWidget;
    @Nullable
    private Button saveButton;
    @Nullable
    private Button exportButton;
    @Nullable
    private CycleButton<Boolean> includeEntitiesButton;
    @Nullable
    private CycleButton<Rotation> rotationButton;

    public TestInstanceBlockEditScreen(TestInstanceBlockEntity blockEntity) {
        super(blockEntity.getBlockState().getBlock().getName());
        this.blockEntity = blockEntity;
    }

    @Override
    protected void init() {
        int i = this.width / 2 - 158;
        boolean flag = SharedConstants.IS_RUNNING_IN_IDE;
        int j = flag ? 3 : 2;
        int k = widgetSize(j);
        this.idEdit = new EditBox(this.font, i, 40, 316, 20, Component.translatable("test_instance_block.test_id"));
        this.idEdit.setMaxLength(128);
        Optional<ResourceKey<GameTestInstance>> optional = this.blockEntity.test();
        if (optional.isPresent()) {
            this.idEdit.setValue(optional.get().location().toString());
        }

        this.idEdit.setResponder(p_397657_ -> this.updateTestInfo(false));
        this.addRenderableWidget(this.idEdit);
        this.infoWidget = new FittingMultiLineTextWidget(i, 70, 316, 8 * 9, Component.literal(""), this.font);
        this.addRenderableWidget(this.infoWidget);
        Vec3i vec3i = this.blockEntity.getSize();
        int l = 0;
        this.sizeXEdit = new EditBox(this.font, this.widgetX(l++, 5), 160, widgetSize(5), 20, Component.translatable("structure_block.size.x"));
        this.sizeXEdit.setMaxLength(15);
        this.addRenderableWidget(this.sizeXEdit);
        this.sizeYEdit = new EditBox(this.font, this.widgetX(l++, 5), 160, widgetSize(5), 20, Component.translatable("structure_block.size.y"));
        this.sizeYEdit.setMaxLength(15);
        this.addRenderableWidget(this.sizeYEdit);
        this.sizeZEdit = new EditBox(this.font, this.widgetX(l++, 5), 160, widgetSize(5), 20, Component.translatable("structure_block.size.z"));
        this.sizeZEdit.setMaxLength(15);
        this.addRenderableWidget(this.sizeZEdit);
        this.setSize(vec3i);
        this.rotationButton = this.addRenderableWidget(
            CycleButton.builder(TestInstanceBlockEditScreen::rotationDisplay)
                .withValues(Rotation.values())
                .withInitialValue(this.blockEntity.getRotation())
                .displayOnlyValue()
                .create(this.widgetX(l++, 5), 160, widgetSize(5), 20, ROTATION_LABEL, (p_397117_, p_397434_) -> this.updateSaveState())
        );
        this.includeEntitiesButton = this.addRenderableWidget(
            CycleButton.onOffBuilder(!this.blockEntity.ignoreEntities())
                .displayOnlyValue()
                .create(this.widgetX(l++, 5), 160, widgetSize(5), 20, INCLUDE_ENTITIES_LABEL)
        );
        l = 0;
        this.addRenderableWidget(Button.builder(Component.translatable("test_instance.action.reset"), p_397794_ -> {
            this.sendToServer(ServerboundTestInstanceBlockActionPacket.Action.RESET);
            this.minecraft.setScreen(null);
        }).bounds(this.widgetX(l++, j), 185, k, 20).build());
        this.saveButton = this.addRenderableWidget(Button.builder(Component.translatable("test_instance.action.save"), p_397643_ -> {
            this.sendToServer(ServerboundTestInstanceBlockActionPacket.Action.SAVE);
            this.minecraft.setScreen(null);
        }).bounds(this.widgetX(l++, j), 185, k, 20).build());
        if (flag) {
            this.exportButton = this.addRenderableWidget(Button.builder(Component.literal("Export Structure"), p_397603_ -> {
                this.sendToServer(ServerboundTestInstanceBlockActionPacket.Action.EXPORT);
                this.minecraft.setScreen(null);
            }).bounds(this.widgetX(l++, j), 185, k, 20).build());
        }

        this.addRenderableWidget(Button.builder(Component.translatable("test_instance.action.run"), p_397205_ -> {
            this.sendToServer(ServerboundTestInstanceBlockActionPacket.Action.RUN);
            this.minecraft.setScreen(null);
        }).bounds(this.widgetX(0, 3), 210, widgetSize(3), 20).build());
        this.addRenderableWidget(
            Button.builder(CommonComponents.GUI_DONE, p_397628_ -> this.onDone()).bounds(this.widgetX(1, 3), 210, widgetSize(3), 20).build()
        );
        this.addRenderableWidget(
            Button.builder(CommonComponents.GUI_CANCEL, p_397964_ -> this.onCancel()).bounds(this.widgetX(2, 3), 210, widgetSize(3), 20).build()
        );
        this.updateTestInfo(true);
    }

    private void updateSaveState() {
        boolean flag = this.rotationButton.getValue() == Rotation.NONE && ResourceLocation.tryParse(this.idEdit.getValue()) != null;
        this.saveButton.active = flag;
        if (this.exportButton != null) {
            this.exportButton.active = flag;
        }
    }

    private static Component rotationDisplay(Rotation rotation) {
        return Component.literal(switch (rotation) {
            case NONE -> "0";
            case CLOCKWISE_90 -> "90";
            case CLOCKWISE_180 -> "180";
            case COUNTERCLOCKWISE_90 -> "270";
        });
    }

    private void setSize(Vec3i size) {
        this.sizeXEdit.setValue(Integer.toString(size.getX()));
        this.sizeYEdit.setValue(Integer.toString(size.getY()));
        this.sizeZEdit.setValue(Integer.toString(size.getZ()));
    }

    private int widgetX(int index, int widgetsInRow) {
        int i = this.width / 2 - 158;
        float f = exactWidgetSize(widgetsInRow);
        return (int)(i + index * (8.0F + f));
    }

    private static int widgetSize(int widgetsInRow) {
        return (int)exactWidgetSize(widgetsInRow);
    }

    private static float exactWidgetSize(int widgetsInRow) {
        return (float)(316 - (widgetsInRow - 1) * 8) / widgetsInRow;
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
        int i = this.width / 2 - 158;
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 10, -1);
        guiGraphics.drawString(this.font, ID_LABEL, i, 30, -6250336);
        guiGraphics.drawString(this.font, SIZE_LABEL, i, 150, -6250336);
        guiGraphics.drawString(this.font, ROTATION_LABEL, this.rotationButton.getX(), 150, -6250336);
        guiGraphics.drawString(this.font, INCLUDE_ENTITIES_LABEL, this.includeEntitiesButton.getX(), 150, -6250336);
    }

    private void updateTestInfo(boolean load) {
        boolean flag = this.sendToServer(
            load ? ServerboundTestInstanceBlockActionPacket.Action.INIT : ServerboundTestInstanceBlockActionPacket.Action.QUERY
        );
        if (!flag) {
            this.infoWidget.setMessage(Component.translatable("test_instance.description.invalid_id").withStyle(ChatFormatting.RED));
        }

        this.updateSaveState();
    }

    private void onDone() {
        this.sendToServer(ServerboundTestInstanceBlockActionPacket.Action.SET);
        this.onClose();
    }

    private boolean sendToServer(ServerboundTestInstanceBlockActionPacket.Action action) {
        Optional<ResourceLocation> optional = Optional.ofNullable(ResourceLocation.tryParse(this.idEdit.getValue()));
        Optional<ResourceKey<GameTestInstance>> optional1 = optional.map(p_397501_ -> ResourceKey.create(Registries.TEST_INSTANCE, p_397501_));
        Vec3i vec3i = new Vec3i(parseSize(this.sizeXEdit.getValue()), parseSize(this.sizeYEdit.getValue()), parseSize(this.sizeZEdit.getValue()));
        boolean flag = !this.includeEntitiesButton.getValue();
        this.minecraft
            .getConnection()
            .send(
                new ServerboundTestInstanceBlockActionPacket(this.blockEntity.getBlockPos(), action, optional1, vec3i, this.rotationButton.getValue(), flag)
            );
        return optional.isPresent();
    }

    public void setStatus(Component status, Optional<Vec3i> size) {
        MutableComponent mutablecomponent = Component.empty();
        this.blockEntity
            .errorMessage()
            .ifPresent(
                p_397370_ -> mutablecomponent.append(
                        Component.translatable("test_instance.description.failed", Component.empty().withStyle(ChatFormatting.RED).append(p_397370_))
                    )
                    .append("\n\n")
            );
        mutablecomponent.append(status);
        this.infoWidget.setMessage(mutablecomponent);
        size.ifPresent(this::setSize);
    }

    private void onCancel() {
        this.onClose();
    }

    private static int parseSize(String size) {
        try {
            return Mth.clamp(Integer.parseInt(size), 1, 48);
        } catch (NumberFormatException numberformatexception) {
            return 1;
        }
    }

    @Override
    public boolean isInGameUi() {
        return true;
    }
}
