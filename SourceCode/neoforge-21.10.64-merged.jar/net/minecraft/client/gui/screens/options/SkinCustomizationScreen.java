package net.minecraft.client.gui.screens.options;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SkinCustomizationScreen extends OptionsSubScreen {
    private static final Component TITLE = Component.translatable("options.skinCustomisation.title");

    public SkinCustomizationScreen(Screen lastScreen, Options options) {
        super(lastScreen, options, TITLE);
    }

    @Override
    protected void addOptions() {
        List<AbstractWidget> list = new ArrayList<>();

        for (PlayerModelPart playermodelpart : PlayerModelPart.values()) {
            list.add(
                CycleButton.onOffBuilder(this.options.isModelPartEnabled(playermodelpart))
                    .create(playermodelpart.getName(), (p_359093_, p_359094_) -> this.options.setModelPart(playermodelpart, p_359094_))
            );
        }

        list.add(this.options.mainHand().createButton(this.options));
        this.list.addSmall(list);
    }
}
