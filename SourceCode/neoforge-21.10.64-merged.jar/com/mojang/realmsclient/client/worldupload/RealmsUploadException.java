package com.mojang.realmsclient.client.worldupload;

import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class RealmsUploadException extends RuntimeException {
    @Nullable
    public Component getStatusMessage() {
        return null;
    }

    @Nullable
    public Component[] getErrorMessages() {
        return null;
    }
}
