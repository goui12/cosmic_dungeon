package net.minecraft.client.renderer.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LightDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
    private final Minecraft minecraft;
    private static final int MAX_RENDER_DIST = 10;

    public LightDebugRenderer(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    @Override
    public void render(
        PoseStack poseStack, MultiBufferSource bufferSource, double camX, double camY, double camZ, DebugValueAccess debugValueAccess, Frustum frustum
    ) {
        Level level = this.minecraft.level;
        BlockPos blockpos = BlockPos.containing(camX, camY, camZ);
        LongSet longset = new LongOpenHashSet();

        for (BlockPos blockpos1 : BlockPos.betweenClosed(blockpos.offset(-10, -10, -10), blockpos.offset(10, 10, 10))) {
            int i = level.getBrightness(LightLayer.SKY, blockpos1);
            float f = (15 - i) / 15.0F * 0.5F + 0.16F;
            int j = Mth.hsvToRgb(f, 0.9F, 0.9F);
            long k = SectionPos.blockToSection(blockpos1.asLong());
            if (longset.add(k)) {
                DebugRenderer.renderFloatingText(
                    poseStack,
                    bufferSource,
                    level.getChunkSource().getLightEngine().getDebugData(LightLayer.SKY, SectionPos.of(k)),
                    SectionPos.sectionToBlockCoord(SectionPos.x(k), 8),
                    SectionPos.sectionToBlockCoord(SectionPos.y(k), 8),
                    SectionPos.sectionToBlockCoord(SectionPos.z(k), 8),
                    -65536,
                    0.3F
                );
            }

            if (i != 15) {
                DebugRenderer.renderFloatingText(
                    poseStack, bufferSource, String.valueOf(i), blockpos1.getX() + 0.5, blockpos1.getY() + 0.25, blockpos1.getZ() + 0.5, j
                );
            }
        }
    }
}
