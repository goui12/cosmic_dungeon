package com.mojang.blaze3d.shaders;

import com.mojang.blaze3d.DontObfuscate;
import javax.annotation.Nullable;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@DontObfuscate
@net.neoforged.neoforge.internal.NonExhaustiveEnum(reason = "Further shader types such as Compute, Task, and Mesh may be added")
public enum ShaderType {
    VERTEX("vertex", ".vsh"),
    FRAGMENT("fragment", ".fsh");

    private static final ShaderType[] TYPES = values();
    private final String name;
    private final String extension;

    private ShaderType(String name, String extension) {
        this.name = name;
        this.extension = extension;
    }

    @Nullable
    public static ShaderType byLocation(ResourceLocation location) {
        for (ShaderType shadertype : TYPES) {
            if (location.getPath().endsWith(shadertype.extension)) {
                return shadertype;
            }
        }

        return null;
    }

    public String getName() {
        return this.name;
    }

    public FileToIdConverter idConverter() {
        return new FileToIdConverter("shaders", this.extension);
    }
}
