package com.mojang.blaze3d.pipeline;

import com.mojang.blaze3d.DontObfuscate;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@DontObfuscate
public interface CompiledRenderPipeline {
    boolean isValid();
}
