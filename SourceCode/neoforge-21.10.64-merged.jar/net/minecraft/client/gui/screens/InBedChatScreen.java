package net.minecraft.client.gui.screens;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class InBedChatScreen extends ChatScreen {
    private Button leaveBedButton;

    public InBedChatScreen(String p_437222_, boolean p_437213_) {
        super(p_437222_, p_437213_);
    }

    @Override
    protected void init() {
        super.init();
        this.leaveBedButton = Button.builder(Component.translatable("multiplayer.stopSleeping"), p_96074_ -> this.sendWakeUp())
            .bounds(this.width / 2 - 100, this.height - 40, 200, 20)
            .build();
        this.addRenderableWidget(this.leaveBedButton);
    }

    @Override
    public void render(GuiGraphics p_281659_, int p_283403_, int p_281737_, float p_282201_) {
        if (!this.minecraft.getChatStatus().isChatAllowed(this.minecraft.isLocalServer())) {
            this.leaveBedButton.render(p_281659_, p_283403_, p_281737_, p_282201_);
        } else {
            super.render(p_281659_, p_283403_, p_281737_, p_282201_);
        }
    }

    @Override
    public void onClose() {
        this.sendWakeUp();
    }

    @Override
    public boolean charTyped(CharacterEvent p_446925_) {
        return !this.minecraft.getChatStatus().isChatAllowed(this.minecraft.isLocalServer()) ? true : super.charTyped(p_446925_);
    }

    @Override
    public boolean keyPressed(KeyEvent p_445775_) {
        if (p_445775_.isEscape()) {
            this.sendWakeUp();
        }

        if (!this.minecraft.getChatStatus().isChatAllowed(this.minecraft.isLocalServer())) {
            return true;
        } else if (p_445775_.isConfirmation()) {
            this.handleChatInput(this.input.getValue(), true);
            this.input.setValue("");
            this.minecraft.gui.getChat().resetChatScroll();
            return true;
        } else {
            return super.keyPressed(p_445775_);
        }
    }

    private void sendWakeUp() {
        ClientPacketListener clientpacketlistener = this.minecraft.player.connection;
        clientpacketlistener.send(new ServerboundPlayerCommandPacket(this.minecraft.player, ServerboundPlayerCommandPacket.Action.STOP_SLEEPING));
    }

    public void onPlayerWokeUp() {
        String s = this.input.getValue();
        if (!this.isDraft && !s.isEmpty()) {
            this.exitReason = ChatScreen.ExitReason.DONE;
            this.minecraft.setScreen(new ChatScreen(s, false));
        } else {
            this.exitReason = ChatScreen.ExitReason.INTERRUPTED;
            this.minecraft.setScreen(null);
        }
    }
}
