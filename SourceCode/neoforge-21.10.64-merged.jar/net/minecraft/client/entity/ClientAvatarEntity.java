package net.minecraft.client.entity;

import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.player.PlayerSkin;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface ClientAvatarEntity {
    ClientAvatarState avatarState();

    PlayerSkin getSkin();

    @Nullable
    Component belowNameDisplay();

    @Nullable
    Parrot.Variant getParrotVariantOnShoulder(boolean left);

    boolean showExtraEars();
}
