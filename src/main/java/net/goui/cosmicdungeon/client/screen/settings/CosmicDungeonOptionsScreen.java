package net.goui.cosmicdungeon.client.screen.settings;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public final class CosmicDungeonOptionsScreen extends Screen {
    private final Screen parent;

    public CosmicDungeonOptionsScreen(Screen parent) {
        super(Component.literal("Cosmic Dungeon Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;
        int y = this.height / 2 - 22;
        addRenderableWidget(Button.builder(Component.literal("Cosmic Spawner HUD"), b ->
                Minecraft.getInstance().setScreen(new CosmicSpawnerHudOptionsScreen(this)))
                .bounds(centerX - 100, y, 200, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(centerX - 100, this.height - 28, 200, 20)
                .build());
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFFFF);
        graphics.drawCenteredString(this.font, Component.literal("Client display preferences"), this.width / 2, 42, 0xFFAAAAAA);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }
}
