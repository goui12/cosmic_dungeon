package net.minecraft.client.renderer.fog;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.fog.environment.AtmosphericFogEnvironment;
import net.minecraft.client.renderer.fog.environment.BlindnessFogEnvironment;
import net.minecraft.client.renderer.fog.environment.DarknessFogEnvironment;
import net.minecraft.client.renderer.fog.environment.DimensionOrBossFogEnvironment;
import net.minecraft.client.renderer.fog.environment.FogEnvironment;
import net.minecraft.client.renderer.fog.environment.LavaFogEnvironment;
import net.minecraft.client.renderer.fog.environment.PowderedSnowFogEnvironment;
import net.minecraft.client.renderer.fog.environment.WaterFogEnvironment;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

@OnlyIn(Dist.CLIENT)
public class FogRenderer implements AutoCloseable {
    public static final int FOG_UBO_SIZE = new Std140SizeCalculator().putVec4().putFloat().putFloat().putFloat().putFloat().putFloat().putFloat().get();
    private static final List<FogEnvironment> FOG_ENVIRONMENTS = Lists.newArrayList(
        new LavaFogEnvironment(),
        new PowderedSnowFogEnvironment(),
        new BlindnessFogEnvironment(),
        new DarknessFogEnvironment(),
        new WaterFogEnvironment(),
        new DimensionOrBossFogEnvironment(),
        new AtmosphericFogEnvironment()
    );
    private static boolean fogEnabled = true;
    private final GpuBuffer emptyBuffer;
    private final MappableRingBuffer regularBuffer;

    public FogRenderer() {
        GpuDevice gpudevice = RenderSystem.getDevice();
        this.regularBuffer = new MappableRingBuffer(() -> "Fog UBO", 130, FOG_UBO_SIZE);

        try (MemoryStack memorystack = MemoryStack.stackPush()) {
            ByteBuffer bytebuffer = memorystack.malloc(FOG_UBO_SIZE);
            this.updateBuffer(
                bytebuffer, 0, new Vector4f(0.0F), Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE
            );
            this.emptyBuffer = gpudevice.createBuffer(() -> "Empty fog", 128, bytebuffer.flip());
        }

        RenderSystem.setShaderFog(this.getBuffer(FogRenderer.FogMode.NONE));
    }

    @Override
    public void close() {
        this.emptyBuffer.close();
        this.regularBuffer.close();
    }

    public void endFrame() {
        this.regularBuffer.rotate();
    }

    public GpuBufferSlice getBuffer(FogRenderer.FogMode fogMode) {
        if (!fogEnabled) {
            return this.emptyBuffer.slice(0, FOG_UBO_SIZE);
        } else {
            return switch (fogMode) {
                case NONE -> this.emptyBuffer.slice(0, FOG_UBO_SIZE);
                case WORLD -> this.regularBuffer.currentBuffer().slice(0, FOG_UBO_SIZE);
            };
        }
    }

    private Vector4f computeFogColor(Camera camera, float partialTick, ClientLevel level, int renderDistance, float darkenWorldAmount, boolean isFoggy) {
        FogType fogtype = this.getFogType(camera, isFoggy);
        Entity entity = camera.getEntity();
        FogEnvironment fogenvironment = null;
        FogEnvironment fogenvironment1 = null;

        for (FogEnvironment fogenvironment2 : FOG_ENVIRONMENTS) {
            if (fogenvironment2.isApplicable(fogtype, entity)) {
                if (fogenvironment == null && fogenvironment2.providesColor()) {
                    fogenvironment = fogenvironment2;
                }

                if (fogenvironment1 == null && fogenvironment2.modifiesDarkness()) {
                    fogenvironment1 = fogenvironment2;
                }
            } else {
                fogenvironment2.onNotApplicable();
            }
        }

        if (fogenvironment == null) {
            throw new IllegalStateException("No color source environment found");
        } else {
            int i = fogenvironment.getBaseColor(level, camera, renderDistance, darkenWorldAmount);
            float f4 = level.getLevelData().voidDarknessOnsetRange();
            float f = Mth.clamp((f4 + level.getMinY() - (float)camera.getPosition().y) / f4, 0.0F, 1.0F);
            if (fogenvironment1 != null) {
                LivingEntity livingentity = (LivingEntity)entity;
                f = fogenvironment1.getModifiedDarkness(livingentity, f, partialTick);
            }

            float f5 = ARGB.redFloat(i);
            float f1 = ARGB.greenFloat(i);
            float f2 = ARGB.blueFloat(i);
            if (f > 0.0F && fogtype != FogType.LAVA && fogtype != FogType.POWDER_SNOW) {
                float f3 = Mth.square(1.0F - f);
                f5 *= f3;
                f1 *= f3;
                f2 *= f3;
            }

            if (darkenWorldAmount > 0.0F) {
                f5 = Mth.lerp(darkenWorldAmount, f5, f5 * 0.7F);
                f1 = Mth.lerp(darkenWorldAmount, f1, f1 * 0.6F);
                f2 = Mth.lerp(darkenWorldAmount, f2, f2 * 0.6F);
            }

            float f6;
            if (fogtype == FogType.WATER) {
                if (entity instanceof LocalPlayer) {
                    f6 = ((LocalPlayer)entity).getWaterVision();
                } else {
                    f6 = 1.0F;
                }
            } else if (entity instanceof LivingEntity livingentity1
                && livingentity1.hasEffect(MobEffects.NIGHT_VISION)
                && !livingentity1.hasEffect(MobEffects.DARKNESS)) {
                f6 = GameRenderer.getNightVisionScale(livingentity1, partialTick);
            } else {
                f6 = 0.0F;
            }

            if (f5 != 0.0F && f1 != 0.0F && f2 != 0.0F) {
                float f7 = 1.0F / Math.max(f5, Math.max(f1, f2));
                f5 = Mth.lerp(f6, f5, f5 * f7);
                f1 = Mth.lerp(f6, f1, f1 * f7);
                f2 = Mth.lerp(f6, f2, f2 * f7);
            }

            return net.neoforged.neoforge.client.ClientHooks.getFogColor(camera, partialTick, level, renderDistance, darkenWorldAmount, f5, f1, f2);
        }
    }

    public static boolean toggleFog() {
        return fogEnabled = !fogEnabled;
    }

    public Vector4f setupFog(Camera camera, int renderDistance, boolean isFoggy, DeltaTracker deltaTracker, float darkenWorldAmount, ClientLevel level) {
        float f = deltaTracker.getGameTimeDeltaPartialTick(false);
        Vector4f vector4f = this.computeFogColor(camera, f, level, renderDistance, darkenWorldAmount, isFoggy);
        float f1 = renderDistance * 16;
        FogType fogtype = this.getFogType(camera, isFoggy);
        Entity entity = camera.getEntity();
        FogData fogdata = new FogData();

        FogEnvironment chosenEnvironment = null;
        for (FogEnvironment fogenvironment : FOG_ENVIRONMENTS) {
            if (fogenvironment.isApplicable(fogtype, entity)) {
                fogenvironment.setupFog(fogdata, entity, camera.getBlockPosition(), level, f1, deltaTracker);
                chosenEnvironment = fogenvironment;
                break;
            }
        }

        float f2 = Mth.clamp(f1 / 10.0F, 4.0F, 64.0F);
        fogdata.renderDistanceStart = f1 - f2;
        fogdata.renderDistanceEnd = f1;

        net.neoforged.neoforge.client.ClientHooks.onSetupFog(chosenEnvironment, fogtype, camera, f, renderDistance, fogdata);

        try (GpuBuffer.MappedView gpubuffer$mappedview = RenderSystem.getDevice()
                .createCommandEncoder()
                .mapBuffer(this.regularBuffer.currentBuffer(), false, true)) {
            this.updateBuffer(
                gpubuffer$mappedview.data(),
                0,
                vector4f,
                fogdata.environmentalStart,
                fogdata.environmentalEnd,
                fogdata.renderDistanceStart,
                fogdata.renderDistanceEnd,
                fogdata.skyEnd,
                fogdata.cloudEnd
            );
        }

        return vector4f;
    }

    private FogType getFogType(Camera camera, boolean isFoggy) {
        FogType fogtype = camera.getFluidInCamera();
        if (fogtype == FogType.NONE) {
            return isFoggy ? FogType.DIMENSION_OR_BOSS : FogType.ATMOSPHERIC;
        } else {
            return fogtype;
        }
    }

    private void updateBuffer(
        ByteBuffer buffer,
        int position,
        Vector4f fogColor,
        float environmentalStart,
        float environmentalEnd,
        float renderDistanceStart,
        float renderDistanceEnd,
        float skyEnd,
        float cloudEnd
    ) {
        buffer.position(position);
        Std140Builder.intoBuffer(buffer)
            .putVec4(fogColor)
            .putFloat(environmentalStart)
            .putFloat(environmentalEnd)
            .putFloat(renderDistanceStart)
            .putFloat(renderDistanceEnd)
            .putFloat(skyEnd)
            .putFloat(cloudEnd);
    }

    @OnlyIn(Dist.CLIENT)
    public static enum FogMode {
        NONE,
        WORLD;
    }
}
