package net.minecraft.client.gui.screens.multiplayer;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CodeOfConductScreen extends WarningScreen {
    private static final Component TITLE = Component.translatable("multiplayer.codeOfConduct.title").withStyle(ChatFormatting.BOLD);
    private static final Component CHECK = Component.translatable("multiplayer.codeOfConduct.check");
    @Nullable
    private final ServerData serverData;
    private final String codeOfConductText;
    private final BooleanConsumer resultConsumer;
    private final Screen parent;

    private CodeOfConductScreen(@Nullable ServerData serverData, Screen parent, Component content, String codeOfConductScreen, BooleanConsumer resultConsumer) {
        super(TITLE, content, CHECK, TITLE.copy().append("\n").append(content));
        this.serverData = serverData;
        this.parent = parent;
        this.codeOfConductText = codeOfConductScreen;
        this.resultConsumer = resultConsumer;
    }

    public CodeOfConductScreen(@Nullable ServerData serverData, Screen parent, String codeOfConductText, BooleanConsumer resultConsumer) {
        this(serverData, parent, Component.literal(codeOfConductText), codeOfConductText, resultConsumer);
    }

    @Override
    protected Layout addFooterButtons() {
        LinearLayout linearlayout = LinearLayout.horizontal().spacing(8);
        linearlayout.addChild(Button.builder(CommonComponents.GUI_ACKNOWLEDGE, p_439148_ -> this.onResult(true)).build());
        linearlayout.addChild(Button.builder(CommonComponents.GUI_DISCONNECT, p_439910_ -> this.onResult(false)).build());
        return linearlayout;
    }

    private void onResult(boolean accepted) {
        this.resultConsumer.accept(accepted);
        if (this.serverData != null) {
            if (accepted && this.stopShowing.selected()) {
                this.serverData.acceptCodeOfConduct(this.codeOfConductText);
            } else {
                this.serverData.clearCodeOfConduct();
            }

            ServerList.saveSingleServer(this.serverData);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.parent instanceof ConnectScreen || this.parent instanceof ServerReconfigScreen) {
            this.parent.tick();
        }
    }
}
