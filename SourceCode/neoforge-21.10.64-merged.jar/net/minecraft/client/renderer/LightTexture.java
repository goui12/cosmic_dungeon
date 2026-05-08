package net.minecraft.client.renderer;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import java.util.OptionalInt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.dimension.DimensionType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class LightTexture implements AutoCloseable {
    public static final int FULL_BRIGHT = 15728880;
    public static final int FULL_SKY = 15728640;
    public static final int FULL_BLOCK = 240;
    private static final int TEXTURE_SIZE = 16;
    private static final int LIGHTMAP_UBO_SIZE = new Std140SizeCalculator()
        .putFloat()
        .putFloat()
        .putFloat()
        .putFloat()
        .putFloat()
        .putFloat()
        .putFloat()
        .putVec3()
        .putVec3()
        .get();
    private static final Vector3f END_FLASH_SKY_LIGHT_COLOR = new Vector3f(0.9F, 0.5F, 1.0F);
    private final GpuTexture texture;
    private final GpuTextureView textureView;
    private boolean updateLightTexture;
    private float blockLightRedFlicker;
    private final GameRenderer renderer;
    private final Minecraft minecraft;
    private final MappableRingBuffer ubo;

    public LightTexture(GameRenderer renderer, Minecraft minecraft) {
        this.renderer = renderer;
        this.minecraft = minecraft;
        GpuDevice gpudevice = RenderSystem.getDevice();
        this.texture = gpudevice.createTexture("Light Texture", 12, TextureFormat.RGBA8, 16, 16, 1, 1);
        this.texture.setTextureFilter(FilterMode.LINEAR, false);
        this.textureView = gpudevice.createTextureView(this.texture);
        gpudevice.createCommandEncoder().clearColorTexture(this.texture, -1);
        this.ubo = new MappableRingBuffer(() -> "Lightmap UBO", 130, LIGHTMAP_UBO_SIZE);
    }

    public GpuTextureView getTextureView() {
        return this.textureView;
    }

    @Override
    public void close() {
        this.texture.close();
        this.textureView.close();
        this.ubo.close();
    }

    public void tick() {
        this.blockLightRedFlicker = this.blockLightRedFlicker + (float)((Math.random() - Math.random()) * Math.random() * Math.random() * 0.1);
        this.blockLightRedFlicker *= 0.9F;
        this.updateLightTexture = true;
    }

    public void turnOffLightLayer() {
        RenderSystem.setShaderTexture(2, null);
    }

    public void turnOnLightLayer() {
        RenderSystem.setShaderTexture(2, this.textureView);
    }

    private float calculateDarknessScale(LivingEntity entity, float gamma, float partialTick) {
        float f = 0.45F * gamma;
        return Math.max(0.0F, Mth.cos((entity.tickCount - partialTick) * (float) Math.PI * 0.025F) * f);
    }

    public void updateLightTexture(float partialTicks) {
        if (this.updateLightTexture) {
            this.updateLightTexture = false;
            ProfilerFiller profilerfiller = Profiler.get();
            profilerfiller.push("lightTex");
            ClientLevel clientlevel = this.minecraft.level;
            if (clientlevel != null) {
                float f = clientlevel.getSkyDarken(1.0F);
                float f1;
                Vector3f vector3f;
                if (clientlevel.effects().hasEndFlashes()) {
                    vector3f = new Vector3f(0.99F, 1.12F, 1.0F);
                    EndFlashState endflashstate = clientlevel.endFlashState();
                    if (endflashstate != null && !this.minecraft.options.hideLightningFlash().get()) {
                        float f2 = endflashstate.getIntensity(partialTicks);
                        if (this.minecraft.gui.getBossOverlay().shouldCreateWorldFog()) {
                            f1 = f2 / 3.0F;
                        } else {
                            f1 = f2;
                        }
                    } else {
                        f1 = 0.0F;
                    }
                } else {
                    vector3f = new Vector3f(1.0F, 1.0F, 1.0F);
                    if (clientlevel.getSkyFlashTime() > 0) {
                        f1 = 1.0F;
                    } else {
                        f1 = f * 0.95F + 0.05F;
                    }
                }

                float f9 = this.minecraft.options.darknessEffectScale().get().floatValue();
                float f10 = this.minecraft.player.getEffectBlendFactor(MobEffects.DARKNESS, partialTicks) * f9;
                float f3 = this.calculateDarknessScale(this.minecraft.player, f10, partialTicks) * f9;
                float f5 = this.minecraft.player.getWaterVision();
                float f4;
                if (this.minecraft.player.hasEffect(MobEffects.NIGHT_VISION)) {
                    f4 = GameRenderer.getNightVisionScale(this.minecraft.player, partialTicks);
                } else if (f5 > 0.0F && this.minecraft.player.hasEffect(MobEffects.CONDUIT_POWER)) {
                    f4 = f5;
                } else {
                    f4 = 0.0F;
                }

                Vector3f vector3f1;
                if (clientlevel.effects().hasEndFlashes()) {
                    vector3f1 = END_FLASH_SKY_LIGHT_COLOR;
                } else {
                    vector3f1 = new Vector3f(f, f, 1.0F).lerp(new Vector3f(1.0F, 1.0F, 1.0F), 0.35F);
                }

                float f6 = this.blockLightRedFlicker + 1.5F;
                float f7 = clientlevel.dimensionType().ambientLight();
                float f8 = this.minecraft.options.gamma().get().floatValue();
                CommandEncoder commandencoder = RenderSystem.getDevice().createCommandEncoder();

                try (GpuBuffer.MappedView gpubuffer$mappedview = commandencoder.mapBuffer(this.ubo.currentBuffer(), false, true)) {
                    Std140Builder.intoBuffer(gpubuffer$mappedview.data())
                        .putFloat(f7)
                        .putFloat(f1)
                        .putFloat(f6)
                        .putFloat(f4)
                        .putFloat(f3)
                        .putFloat(this.renderer.getDarkenWorldAmount(partialTicks))
                        .putFloat(Math.max(0.0F, f8 - f10))
                        .putVec3(vector3f1)
                        .putVec3(vector3f);
                }

                try (RenderPass renderpass = commandencoder.createRenderPass(() -> "Update light", this.textureView, OptionalInt.empty())) {
                    renderpass.setPipeline(RenderPipelines.LIGHTMAP);
                    RenderSystem.bindDefaultUniforms(renderpass);
                    renderpass.setUniform("LightmapInfo", this.ubo.currentBuffer());
                    renderpass.draw(0, 3);
                }

                this.ubo.rotate();
                profilerfiller.pop();
            }
        }
    }

    public static float getBrightness(DimensionType dimensionType, int lightLevel) {
        return getBrightness(dimensionType.ambientLight(), lightLevel);
    }

    public static float getBrightness(float ambientLight, int lightLevel) {
        float f = lightLevel / 15.0F;
        float f1 = f / (4.0F - 3.0F * f);
        return Mth.lerp(ambientLight, f1, 1.0F);
    }

    public static int pack(int blockLight, int skyLight) {
        return blockLight << 4 | skyLight << 20;
    }

    public static int block(int packedLight) {
        return packedLight >>> 4 & 15;
    }

    public static int sky(int packedLight) {
        return packedLight >>> 20 & 15;
    }

    public static int lightCoordsWithEmission(int packedLight, int emission) {
        if (emission == 0) {
            return packedLight;
        } else {
            int i = Math.max(sky(packedLight), emission);
            int j = Math.max(block(packedLight), emission);
            return pack(j, i);
        }
    }

    // Neo: block and sky only return the integer part. So we add these methods that work with fractional parts too.
    // A full block or sky value is encoded as `0xij` where i is the integer part and j is the fractional part.
    // Shifting by 4 then masking with 15 (= 0xF) only returns the integer part.

    public static int packWithFraction(int block, int sky) {
        return block | sky << 16;
    }

    public static int blockWithFraction(int light) {
        return light & 0xFF;
    }

    public static int skyWithFraction(int light) {
        return light >>> 16 & 0xFF;
    }
}
