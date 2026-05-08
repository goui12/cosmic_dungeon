package net.minecraft.client.gui.screens.dialog;

import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Holder;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ServerLinks;
import net.minecraft.server.dialog.Dialog;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface DialogConnectionAccess {
    void disconnect(Component message);

    void runCommand(String command, @Nullable Screen previousScreen);

    void openDialog(Holder<Dialog> dialog, @Nullable Screen previousScreen);

    void sendCustomAction(ResourceLocation id, Optional<Tag> payload);

    ServerLinks serverLinks();
}
