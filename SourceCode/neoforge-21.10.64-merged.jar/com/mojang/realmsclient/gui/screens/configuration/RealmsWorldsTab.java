package com.mojang.realmsclient.gui.screens.configuration;

import com.google.common.collect.Lists;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.WorldTemplate;
import com.mojang.realmsclient.gui.RealmsWorldSlotButton;
import com.mojang.realmsclient.gui.screens.RealmsLongRunningMcoTaskScreen;
import com.mojang.realmsclient.gui.screens.RealmsPopups;
import com.mojang.realmsclient.gui.screens.RealmsResetWorldScreen;
import com.mojang.realmsclient.gui.screens.RealmsSelectWorldTemplateScreen;
import com.mojang.realmsclient.util.task.SwitchMinigameTask;
import com.mojang.realmsclient.util.task.SwitchSlotTask;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PopupScreen;
import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
class RealmsWorldsTab extends GridLayoutTab implements RealmsConfigurationTab {
    static final Component TITLE = Component.translatable("mco.configure.worlds.title");
    private final RealmsConfigureWorldScreen configurationScreen;
    private final Minecraft minecraft;
    private RealmsServer serverData;
    private final Button optionsButton;
    private final Button backupButton;
    private final Button resetWorldButton;
    private final List<RealmsWorldSlotButton> slotButtonList = Lists.newArrayList();

    RealmsWorldsTab(RealmsConfigureWorldScreen configurationScreen, Minecraft minecraft, RealmsServer serverData) {
        super(TITLE);
        this.configurationScreen = configurationScreen;
        this.minecraft = minecraft;
        this.serverData = serverData;
        GridLayout.RowHelper gridlayout$rowhelper = this.layout.spacing(20).createRowHelper(1);
        GridLayout.RowHelper gridlayout$rowhelper1 = new GridLayout().spacing(16).createRowHelper(4);
        this.slotButtonList.clear();

        for (int i = 1; i < 5; i++) {
            this.slotButtonList.add(gridlayout$rowhelper1.addChild(this.createSlotButton(i), LayoutSettings.defaults().alignVerticallyBottom()));
        }

        gridlayout$rowhelper.addChild(gridlayout$rowhelper1.getGrid());
        GridLayout.RowHelper gridlayout$rowhelper2 = new GridLayout().spacing(8).createRowHelper(1);
        this.optionsButton = gridlayout$rowhelper2.addChild(
            Button.builder(
                    Component.translatable("mco.configure.world.buttons.options"),
                    p_420075_ -> minecraft.setScreen(
                        new RealmsSlotOptionsScreen(configurationScreen, serverData.slots.get(serverData.activeSlot).clone(), serverData.worldType, serverData.activeSlot)
                    )
                )
                .bounds(0, 0, 150, 20)
                .build()
        );
        this.backupButton = gridlayout$rowhelper2.addChild(
            Button.builder(
                    Component.translatable("mco.configure.world.backup"),
                    p_419786_ -> minecraft.setScreen(new RealmsBackupScreen(configurationScreen, serverData.clone(), serverData.activeSlot))
                )
                .bounds(0, 0, 150, 20)
                .build()
        );
        this.resetWorldButton = gridlayout$rowhelper2.addChild(
            Button.builder(Component.empty(), p_419578_ -> this.resetButtonPressed()).bounds(0, 0, 150, 20).build()
        );
        gridlayout$rowhelper.addChild(gridlayout$rowhelper2.getGrid(), LayoutSettings.defaults().alignHorizontallyCenter());
        this.backupButton.active = true;
        this.updateData(serverData);
    }

    private void resetButtonPressed() {
        if (this.isMinigame()) {
            this.minecraft
                .setScreen(
                    new RealmsSelectWorldTemplateScreen(
                        Component.translatable("mco.template.title.minigame"), this::templateSelectionCallback, RealmsServer.WorldType.MINIGAME, null
                    )
                );
        } else {
            this.minecraft
                .setScreen(
                    RealmsResetWorldScreen.forResetSlot(
                        this.configurationScreen,
                        this.serverData.clone(),
                        () -> this.minecraft.execute(() -> this.minecraft.setScreen(this.configurationScreen.getNewScreen()))
                    )
                );
        }
    }

    private void templateSelectionCallback(@Nullable WorldTemplate template) {
        if (template != null && WorldTemplate.WorldTemplateType.MINIGAME == template.type) {
            this.configurationScreen.stateChanged();
            RealmsConfigureWorldScreen realmsconfigureworldscreen = this.configurationScreen.getNewScreen();
            this.minecraft
                .setScreen(
                    new RealmsLongRunningMcoTaskScreen(
                        realmsconfigureworldscreen, new SwitchMinigameTask(this.serverData.id, template, realmsconfigureworldscreen)
                    )
                );
        } else {
            this.minecraft.setScreen(this.configurationScreen);
        }
    }

    private boolean isMinigame() {
        return this.serverData.isMinigameActive();
    }

    @Override
    public void onSelected(RealmsServer server) {
        this.updateData(server);
    }

    @Override
    public void updateData(RealmsServer server) {
        this.serverData = server;
        this.optionsButton.active = !server.expired && !this.isMinigame();
        this.resetWorldButton.active = !server.expired;
        if (this.isMinigame()) {
            this.resetWorldButton.setMessage(Component.translatable("mco.configure.world.buttons.switchminigame"));
        } else {
            boolean flag = server.slots.containsKey(server.activeSlot) && server.slots.get(server.activeSlot).options.empty;
            if (flag) {
                this.resetWorldButton.setMessage(Component.translatable("mco.configure.world.buttons.newworld"));
            } else {
                this.resetWorldButton.setMessage(Component.translatable("mco.configure.world.buttons.resetworld"));
            }
        }

        this.backupButton.active = !this.isMinigame();

        for (RealmsWorldSlotButton realmsworldslotbutton : this.slotButtonList) {
            RealmsWorldSlotButton.State realmsworldslotbutton$state = realmsworldslotbutton.setServerData(server);
            if (realmsworldslotbutton$state.activeSlot) {
                realmsworldslotbutton.setSize(80, 80);
            } else {
                realmsworldslotbutton.setSize(50, 50);
            }
        }
    }

    private RealmsWorldSlotButton createSlotButton(int slotIndex) {
        return new RealmsWorldSlotButton(0, 0, 80, 80, slotIndex, this.serverData, p_419905_ -> {
            RealmsWorldSlotButton.State realmsworldslotbutton$state = ((RealmsWorldSlotButton)p_419905_).getState();
            switch (realmsworldslotbutton$state.action) {
                case SWITCH_SLOT:
                    if (realmsworldslotbutton$state.minigame) {
                        this.switchToMinigame();
                    } else if (realmsworldslotbutton$state.empty) {
                        this.switchToEmptySlot(slotIndex, this.serverData);
                    } else {
                        this.switchToFullSlot(slotIndex, this.serverData);
                    }
                case NOTHING:
                    return;
                default:
                    throw new IllegalStateException("Unknown action " + realmsworldslotbutton$state.action);
            }
        });
    }

    private void switchToMinigame() {
        RealmsSelectWorldTemplateScreen realmsselectworldtemplatescreen = new RealmsSelectWorldTemplateScreen(
            Component.translatable("mco.template.title.minigame"),
            this::templateSelectionCallback,
            RealmsServer.WorldType.MINIGAME,
            null,
            List.of(Component.translatable("mco.minigame.world.info.line1"), Component.translatable("mco.minigame.world.info.line2"))
        );
        this.minecraft.setScreen(realmsselectworldtemplatescreen);
    }

    private void switchToFullSlot(int slotIndex, RealmsServer serverData) {
        this.minecraft
            .setScreen(
                RealmsPopups.infoPopupScreen(
                    this.configurationScreen,
                    Component.translatable("mco.configure.world.slot.switch.question.line1"),
                    p_428669_ -> {
                        RealmsConfigureWorldScreen realmsconfigureworldscreen = this.configurationScreen.getNewScreen();
                        this.configurationScreen.stateChanged();
                        this.minecraft
                            .setScreen(
                                new RealmsLongRunningMcoTaskScreen(
                                    realmsconfigureworldscreen,
                                    new SwitchSlotTask(
                                        serverData.id, slotIndex, () -> this.minecraft.execute(() -> this.minecraft.setScreen(realmsconfigureworldscreen))
                                    )
                                )
                            );
                    }
                )
            );
    }

    private void switchToEmptySlot(int slotIndex, RealmsServer serverData) {
        this.minecraft
            .setScreen(
                RealmsPopups.infoPopupScreen(
                    this.configurationScreen,
                    Component.translatable("mco.configure.world.slot.switch.question.line1"),
                    p_419876_ -> {
                        this.configurationScreen.stateChanged();
                        RealmsResetWorldScreen realmsresetworldscreen = RealmsResetWorldScreen.forEmptySlot(
                            this.configurationScreen,
                            slotIndex,
                            serverData,
                            () -> this.minecraft.execute(() -> this.minecraft.setScreen(this.configurationScreen.getNewScreen()))
                        );
                        this.minecraft.setScreen(realmsresetworldscreen);
                    }
                )
            );
    }
}
