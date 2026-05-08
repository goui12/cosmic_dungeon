package com.mojang.realmsclient.gui.screens.configuration;

import com.mojang.realmsclient.dto.RealmsServer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface RealmsConfigurationTab {
    void updateData(RealmsServer server);

    default void onSelected(RealmsServer server) {
    }

    default void onDeselected(RealmsServer server) {
    }
}
