package com.mojang.blaze3d.textures;

import com.mojang.blaze3d.DontObfuscate;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@DontObfuscate
@net.neoforged.neoforge.internal.NonExhaustiveEnum(reason = "Further address modes may be added")
public enum AddressMode {
    REPEAT,
    CLAMP_TO_EDGE;
}
