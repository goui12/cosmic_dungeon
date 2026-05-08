package com.mojang.blaze3d.buffers;

import com.mojang.blaze3d.DontObfuscate;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@DontObfuscate
public interface GpuFence extends AutoCloseable {
    @Override
    void close();

    boolean awaitCompletion(long timeout);
}
