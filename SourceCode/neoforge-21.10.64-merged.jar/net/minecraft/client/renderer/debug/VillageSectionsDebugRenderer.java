package net.minecraft.client.renderer.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Unit;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueAccess;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class VillageSectionsDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
    @Override
    public void render(
        PoseStack p_113701_, MultiBufferSource p_113702_, double p_113703_, double p_113704_, double p_113705_, DebugValueAccess p_449872_, Frustum p_451114_
    ) {
        p_449872_.forEachBlock(DebugSubscriptions.VILLAGE_SECTIONS, (p_448809_, p_448810_) -> {
            SectionPos sectionpos = SectionPos.of(p_448809_);
            DebugRenderer.renderFilledUnitCube(p_113701_, p_113702_, sectionpos.center(), 0.2F, 1.0F, 0.2F, 0.15F);
        });
    }
}
