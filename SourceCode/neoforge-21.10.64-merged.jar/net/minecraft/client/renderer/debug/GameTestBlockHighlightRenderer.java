package net.minecraft.client.renderer.debug;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.Util;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GameTestBlockHighlightRenderer {
    private static final int SHOW_POS_DURATION_MS = 10000;
    private static final float PADDING = 0.02F;
    private final Map<BlockPos, GameTestBlockHighlightRenderer.Marker> markers = Maps.newHashMap();

    public void highlightPos(BlockPos absolutePos, BlockPos relativePos) {
        String s = relativePos.toShortString();
        this.markers.put(absolutePos, new GameTestBlockHighlightRenderer.Marker(-2147418368, s, Util.getMillis() + 10000L));
    }

    public void clear() {
        this.markers.clear();
    }

    public void render(PoseStack poseStack, MultiBufferSource bufferSource) {
        long i = Util.getMillis();
        this.markers.entrySet().removeIf(p_451551_ -> i > p_451551_.getValue().removeAtTime);
        this.markers.forEach((p_451168_, p_451028_) -> this.renderMarker(poseStack, bufferSource, p_451168_, p_451028_));
    }

    private void renderMarker(PoseStack poseStack, MultiBufferSource bufferSource, BlockPos pos, GameTestBlockHighlightRenderer.Marker marker) {
        DebugRenderer.renderFilledBox(poseStack, bufferSource, pos, 0.02F, marker.getR(), marker.getG(), marker.getB(), marker.getA() * 0.75F);
        if (!marker.text.isEmpty()) {
            double d0 = pos.getX() + 0.5;
            double d1 = pos.getY() + 1.2;
            double d2 = pos.getZ() + 0.5;
            DebugRenderer.renderFloatingText(poseStack, bufferSource, marker.text, d0, d1, d2, -1, 0.01F, true, 0.0F, true);
        }
    }

    @OnlyIn(Dist.CLIENT)
    record Marker(int color, String text, long removeAtTime) {
        public float getR() {
            return ARGB.redFloat(this.color);
        }

        public float getG() {
            return ARGB.greenFloat(this.color);
        }

        public float getB() {
            return ARGB.blueFloat(this.color);
        }

        public float getA() {
            return ARGB.alphaFloat(this.color);
        }
    }
}
