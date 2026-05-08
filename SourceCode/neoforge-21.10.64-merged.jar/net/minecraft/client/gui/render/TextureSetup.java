package net.minecraft.client.gui.render;

import com.mojang.blaze3d.textures.GpuTextureView;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record TextureSetup(@Nullable GpuTextureView texure0, @Nullable GpuTextureView texure1, @Nullable GpuTextureView texure2) {
    private static final TextureSetup NO_TEXTURE_SETUP = new TextureSetup(null, null, null);
    private static int sortKeySeed;

    public static TextureSetup singleTexture(GpuTextureView texture) {
        return new TextureSetup(texture, null, null);
    }

    public static TextureSetup singleTextureWithLightmap(GpuTextureView texture) {
        return new TextureSetup(texture, null, Minecraft.getInstance().gameRenderer.lightTexture().getTextureView());
    }

    public static TextureSetup doubleTexture(GpuTextureView texture1, GpuTextureView texture2) {
        return new TextureSetup(texture1, texture2, null);
    }

    public static TextureSetup noTexture() {
        return NO_TEXTURE_SETUP;
    }

    public int getSortKey() {
        return SharedConstants.DEBUG_SHUFFLE_UI_RENDERING_ORDER ? this.hashCode() * (sortKeySeed + 1) : this.hashCode();
    }

    public static void updateSortKeySeed() {
        sortKeySeed = Math.round(100000.0F * (float)Math.random());
    }
}
