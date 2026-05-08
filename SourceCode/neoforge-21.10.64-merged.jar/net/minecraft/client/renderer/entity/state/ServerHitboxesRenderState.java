package net.minecraft.client.renderer.entity.state;

import javax.annotation.Nullable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record ServerHitboxesRenderState(
    boolean missing,
    double serverEntityX,
    double serverEntityY,
    double serverEntityZ,
    double deltaMovementX,
    double deltaMovementY,
    double deltaMovementZ,
    float eyeHeight,
    @Nullable HitboxesRenderState hitboxes
) {
    public ServerHitboxesRenderState(boolean p_412106_) {
        this(p_412106_, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0F, null);
    }
}
