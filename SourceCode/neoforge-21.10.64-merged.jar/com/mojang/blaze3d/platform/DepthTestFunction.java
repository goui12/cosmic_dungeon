package com.mojang.blaze3d.platform;

import com.mojang.blaze3d.DontObfuscate;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@DontObfuscate
@net.neoforged.neoforge.internal.NonExhaustiveEnum(reason = "Further depth test functions may be added")
public enum DepthTestFunction {
    NO_DEPTH_TEST,
    EQUAL_DEPTH_TEST,
    LEQUAL_DEPTH_TEST,
    LESS_DEPTH_TEST,
    GREATER_DEPTH_TEST;
}
