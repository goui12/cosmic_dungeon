package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.textures.GpuTexture;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface SpriteTicker extends AutoCloseable {
    void tickAndUpload(int x, int y, GpuTexture texture);

    @Override
    void close();
}
