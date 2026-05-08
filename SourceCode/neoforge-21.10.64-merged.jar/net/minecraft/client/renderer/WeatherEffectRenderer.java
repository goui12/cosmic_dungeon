package net.minecraft.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.state.WeatherRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WeatherEffectRenderer {
    private static final int RAIN_RADIUS = 10;
    private static final int RAIN_DIAMETER = 21;
    private static final ResourceLocation RAIN_LOCATION = ResourceLocation.withDefaultNamespace("textures/environment/rain.png");
    private static final ResourceLocation SNOW_LOCATION = ResourceLocation.withDefaultNamespace("textures/environment/snow.png");
    private static final int RAIN_TABLE_SIZE = 32;
    private static final int HALF_RAIN_TABLE_SIZE = 16;
    private int rainSoundTime;
    private final float[] columnSizeX = new float[1024];
    private final float[] columnSizeZ = new float[1024];

    public WeatherEffectRenderer() {
        for (int i = 0; i < 32; i++) {
            for (int j = 0; j < 32; j++) {
                float f = j - 16;
                float f1 = i - 16;
                float f2 = Mth.length(f, f1);
                this.columnSizeX[i * 32 + j] = -f1 / f2;
                this.columnSizeZ[i * 32 + j] = f / f2;
            }
        }
    }

    public void extractRenderState(Level level, int ticks, float partialTick, Vec3 cameraPosition, WeatherRenderState reusedState) {
        reusedState.intensity = level.getRainLevel(partialTick);
        if (!(reusedState.intensity <= 0.0F)) {
            reusedState.radius = Minecraft.useFancyGraphics() ? 10 : 5;
            int i = Mth.floor(cameraPosition.x);
            int j = Mth.floor(cameraPosition.y);
            int k = Mth.floor(cameraPosition.z);
            BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();
            RandomSource randomsource = RandomSource.create();

            for (int l = k - reusedState.radius; l <= k + reusedState.radius; l++) {
                for (int i1 = i - reusedState.radius; i1 <= i + reusedState.radius; i1++) {
                    int j1 = level.getHeight(Heightmap.Types.MOTION_BLOCKING, i1, l);
                    int k1 = Math.max(j - reusedState.radius, j1);
                    int l1 = Math.max(j + reusedState.radius, j1);
                    if (l1 - k1 != 0) {
                        Biome.Precipitation biome$precipitation = this.getPrecipitationAt(level, blockpos$mutableblockpos.set(i1, j, l));
                        if (biome$precipitation != Biome.Precipitation.NONE) {
                            int i2 = i1 * i1 * 3121 + i1 * 45238971 ^ l * l * 418711 + l * 13761;
                            randomsource.setSeed(i2);
                            int j2 = Math.max(j, j1);
                            int k2 = LevelRenderer.getLightColor(level, blockpos$mutableblockpos.set(i1, j2, l));
                            if (biome$precipitation == Biome.Precipitation.RAIN) {
                                reusedState.rainColumns.add(this.createRainColumnInstance(randomsource, ticks, i1, k1, l1, l, k2, partialTick));
                            } else if (biome$precipitation == Biome.Precipitation.SNOW) {
                                reusedState.snowColumns.add(this.createSnowColumnInstance(randomsource, ticks, i1, k1, l1, l, k2, partialTick));
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * @deprecated Neo: use {@link #render(MultiBufferSource, Vec3, WeatherRenderState
     *             , net.minecraft.client.renderer.state.LevelRenderState)} instead
     */
    @Deprecated
    public void render(MultiBufferSource bufferSource, Vec3 cameraPosition, WeatherRenderState renderState) {
        this.render(bufferSource, cameraPosition, renderState, null);
    }

    public void render(MultiBufferSource bufferSource, Vec3 cameraPosition, WeatherRenderState renderState, @org.jetbrains.annotations.Nullable net.minecraft.client.renderer.state.LevelRenderState levelRenderState) {
        if (levelRenderState != null && levelRenderState.dimensionSpecialEffects.renderSnowAndRain(levelRenderState, renderState, bufferSource, cameraPosition)) {
            return;
        }

        if (!renderState.rainColumns.isEmpty()) {
            RenderType rendertype = RenderType.weather(RAIN_LOCATION, Minecraft.useShaderTransparency());
            this.renderInstances(bufferSource.getBuffer(rendertype), renderState.rainColumns, cameraPosition, 1.0F, renderState.radius, renderState.intensity);
        }

        if (!renderState.snowColumns.isEmpty()) {
            RenderType rendertype1 = RenderType.weather(SNOW_LOCATION, Minecraft.useShaderTransparency());
            this.renderInstances(bufferSource.getBuffer(rendertype1), renderState.snowColumns, cameraPosition, 0.8F, renderState.radius, renderState.intensity);
        }
    }

    private WeatherEffectRenderer.ColumnInstance createRainColumnInstance(
        RandomSource random, int ticks, int x, int bottomY, int topY, int z, int lightCoords, float partialTick
    ) {
        int i = ticks & 131071;
        int j = x * x * 3121 + x * 45238971 + z * z * 418711 + z * 13761 & 0xFF;
        float f = 3.0F + random.nextFloat();
        float f1 = -(i + j + partialTick) / 32.0F * f;
        float f2 = f1 % 32.0F;
        return new WeatherEffectRenderer.ColumnInstance(x, z, bottomY, topY, 0.0F, f2, lightCoords);
    }

    private WeatherEffectRenderer.ColumnInstance createSnowColumnInstance(
        RandomSource random, int ticks, int x, int bottomY, int topY, int z, int lightCoords, float partialTick
    ) {
        float f = ticks + partialTick;
        float f1 = (float)(random.nextDouble() + f * 0.01F * (float)random.nextGaussian());
        float f2 = (float)(random.nextDouble() + f * (float)random.nextGaussian() * 0.001F);
        float f3 = -((ticks & 511) + partialTick) / 512.0F;
        int i = LightTexture.pack((LightTexture.block(lightCoords) * 3 + 15) / 4, (LightTexture.sky(lightCoords) * 3 + 15) / 4);
        return new WeatherEffectRenderer.ColumnInstance(x, z, bottomY, topY, f1, f3 + f2, i);
    }

    private void renderInstances(
        VertexConsumer buffer, List<WeatherEffectRenderer.ColumnInstance> columnInstances, Vec3 cameraPosition, float amount, int radius, float rainLevel
    ) {
        for (WeatherEffectRenderer.ColumnInstance weathereffectrenderer$columninstance : columnInstances) {
            float f = (float)(weathereffectrenderer$columninstance.x + 0.5 - cameraPosition.x);
            float f1 = (float)(weathereffectrenderer$columninstance.z + 0.5 - cameraPosition.z);
            float f2 = (float)Mth.lengthSquared(f, f1);
            float f3 = Mth.lerp(f2 / (radius * radius), amount, 0.5F) * rainLevel;
            int i = ARGB.white(f3);
            int j = (weathereffectrenderer$columninstance.z - Mth.floor(cameraPosition.z) + 16) * 32
                + weathereffectrenderer$columninstance.x
                - Mth.floor(cameraPosition.x)
                + 16;
            float f4 = this.columnSizeX[j] / 2.0F;
            float f5 = this.columnSizeZ[j] / 2.0F;
            float f6 = f - f4;
            float f7 = f + f4;
            float f8 = (float)(weathereffectrenderer$columninstance.topY - cameraPosition.y);
            float f9 = (float)(weathereffectrenderer$columninstance.bottomY - cameraPosition.y);
            float f10 = f1 - f5;
            float f11 = f1 + f5;
            float f12 = weathereffectrenderer$columninstance.uOffset + 0.0F;
            float f13 = weathereffectrenderer$columninstance.uOffset + 1.0F;
            float f14 = weathereffectrenderer$columninstance.bottomY * 0.25F + weathereffectrenderer$columninstance.vOffset;
            float f15 = weathereffectrenderer$columninstance.topY * 0.25F + weathereffectrenderer$columninstance.vOffset;
            buffer.addVertex(f6, f8, f10).setUv(f12, f14).setColor(i).setLight(weathereffectrenderer$columninstance.lightCoords);
            buffer.addVertex(f7, f8, f11).setUv(f13, f14).setColor(i).setLight(weathereffectrenderer$columninstance.lightCoords);
            buffer.addVertex(f7, f9, f11).setUv(f13, f15).setColor(i).setLight(weathereffectrenderer$columninstance.lightCoords);
            buffer.addVertex(f6, f9, f10).setUv(f12, f15).setColor(i).setLight(weathereffectrenderer$columninstance.lightCoords);
        }
    }

    public void tickRainParticles(ClientLevel level, Camera camera, int ticks, ParticleStatus particleStatus) {
        if (level.effects().tickRain(level, ticks, camera))
            return;
        float f = level.getRainLevel(1.0F) / (Minecraft.useFancyGraphics() ? 1.0F : 2.0F);
        if (!(f <= 0.0F)) {
            RandomSource randomsource = RandomSource.create(ticks * 312987231L);
            BlockPos blockpos = BlockPos.containing(camera.getPosition());
            BlockPos blockpos1 = null;
            int i = (int)(100.0F * f * f) / (particleStatus == ParticleStatus.DECREASED ? 2 : 1);

            for (int j = 0; j < i; j++) {
                int k = randomsource.nextInt(21) - 10;
                int l = randomsource.nextInt(21) - 10;
                BlockPos blockpos2 = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, blockpos.offset(k, 0, l));
                if (blockpos2.getY() > level.getMinY()
                    && blockpos2.getY() <= blockpos.getY() + 10
                    && blockpos2.getY() >= blockpos.getY() - 10
                    && this.getPrecipitationAt(level, blockpos2) == Biome.Precipitation.RAIN) {
                    blockpos1 = blockpos2.below();
                    if (particleStatus == ParticleStatus.MINIMAL) {
                        break;
                    }

                    double d0 = randomsource.nextDouble();
                    double d1 = randomsource.nextDouble();
                    BlockState blockstate = level.getBlockState(blockpos1);
                    FluidState fluidstate = level.getFluidState(blockpos1);
                    VoxelShape voxelshape = blockstate.getCollisionShape(level, blockpos1);
                    double d2 = voxelshape.max(Direction.Axis.Y, d0, d1);
                    double d3 = fluidstate.getHeight(level, blockpos1);
                    double d4 = Math.max(d2, d3);
                    ParticleOptions particleoptions = !fluidstate.is(FluidTags.LAVA)
                            && !blockstate.is(Blocks.MAGMA_BLOCK)
                            && !CampfireBlock.isLitCampfire(blockstate)
                        ? ParticleTypes.RAIN
                        : ParticleTypes.SMOKE;
                    level.addParticle(particleoptions, blockpos1.getX() + d0, blockpos1.getY() + d4, blockpos1.getZ() + d1, 0.0, 0.0, 0.0);
                }
            }

            if (blockpos1 != null && randomsource.nextInt(3) < this.rainSoundTime++) {
                this.rainSoundTime = 0;
                if (blockpos1.getY() > blockpos.getY() + 1
                    && level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, blockpos).getY() > Mth.floor((float)blockpos.getY())) {
                    level.playLocalSound(blockpos1, SoundEvents.WEATHER_RAIN_ABOVE, SoundSource.WEATHER, 0.1F, 0.5F, false);
                } else {
                    level.playLocalSound(blockpos1, SoundEvents.WEATHER_RAIN, SoundSource.WEATHER, 0.2F, 1.0F, false);
                }
            }
        }
    }

    private Biome.Precipitation getPrecipitationAt(Level level, BlockPos pos) {
        if (!level.getChunkSource().hasChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()))) {
            return Biome.Precipitation.NONE;
        } else {
            Biome biome = level.getBiome(pos).value();
            return biome.getPrecipitationAt(pos, level.getSeaLevel());
        }
    }

    @OnlyIn(Dist.CLIENT)
    public record ColumnInstance(int x, int z, int bottomY, int topY, float uOffset, float vOffset, int lightCoords) {
    }
}
