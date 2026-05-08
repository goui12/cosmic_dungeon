package net.minecraft.client.gui.screens;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ManageServerScreen extends Screen {
    private static final Component NAME_LABEL = Component.translatable("manageServer.enterName");
    private static final Component IP_LABEL = Component.translatable("manageServer.enterIp");
    private static final Component DEFAULT_SERVER_NAME = Component.translatable("selectServer.defaultName");
    private Button addButton;
    private final BooleanConsumer callback;
    private final ServerData serverData;
    private EditBox ipEdit;
    private EditBox nameEdit;
    private final Screen lastScreen;

    public ManageServerScreen(Screen lastScreen, Component title, BooleanConsumer callback, ServerData serverData) {
        super(title);
        this.lastScreen = lastScreen;
        this.callback = callback;
        this.serverData = serverData;
    }

    @Override
    protected void init() {
        this.nameEdit = new EditBox(this.font, this.width / 2 - 100, 66, 200, 20, NAME_LABEL);
        this.nameEdit.setValue(this.serverData.name);
        this.nameEdit.setHint(DEFAULT_SERVER_NAME);
        this.nameEdit.setResponder(p_445790_ -> this.updateAddButtonStatus());
        this.addWidget(this.nameEdit);
        this.ipEdit = new EditBox(this.font, this.width / 2 - 100, 106, 200, 20, IP_LABEL);
        this.ipEdit.setMaxLength(128);
        this.ipEdit.setValue(this.serverData.ip);
        this.ipEdit.setResponder(p_447100_ -> this.updateAddButtonStatus());
        this.addWidget(this.ipEdit);
        this.addRenderableWidget(
            CycleButton.builder(ServerData.ServerPackStatus::getName)
                .withValues(ServerData.ServerPackStatus.values())
                .withInitialValue(this.serverData.getResourcePackStatus())
                .create(
                    this.width / 2 - 100,
                    this.height / 4 + 72,
                    200,
                    20,
                    Component.translatable("manageServer.resourcePack"),
                    (p_446128_, p_445771_) -> this.serverData.setResourcePackStatus(p_445771_)
                )
        );
        this.addButton = this.addRenderableWidget(
            Button.builder(CommonComponents.GUI_DONE, p_446986_ -> this.onAdd()).bounds(this.width / 2 - 100, this.height / 4 + 96 + 18, 200, 20).build()
        );
        this.addRenderableWidget(
            Button.builder(CommonComponents.GUI_CANCEL, p_445476_ -> this.callback.accept(false))
                .bounds(this.width / 2 - 100, this.height / 4 + 120 + 18, 200, 20)
                .build()
        );
        this.updateAddButtonStatus();
    }

    @Override
    protected void setInitialFocus() {
        this.setInitialFocus(this.nameEdit);
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        String s = this.ipEdit.getValue();
        String s1 = this.nameEdit.getValue();
        this.init(minecraft, width, height);
        this.ipEdit.setValue(s);
        this.nameEdit.setValue(s1);
    }

    private void onAdd() {
        String s = this.nameEdit.getValue();
        this.serverData.name = s.isEmpty() ? DEFAULT_SERVER_NAME.getString() : s;
        this.serverData.ip = this.ipEdit.getValue();
        this.callback.accept(true);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.lastScreen);
    }

    private void updateAddButtonStatus() {
        this.addButton.active = ServerAddress.isValidAddress(this.ipEdit.getValue());
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
        guiGraphics.drawString(this.font, NAME_LABEL, this.width / 2 - 100 + 1, 53, -6250336);
        guiGraphics.drawString(this.font, IP_LABEL, this.width / 2 - 100 + 1, 94, -6250336);
        this.nameEdit.render(guiGraphics, mouseX, mouseY, partialTick);
        this.ipEdit.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}
