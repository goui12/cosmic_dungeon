package net.minecraft.client.gui.render.state.pip;

import javax.annotation.Nullable;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public record GuiEntityRenderState(
    EntityRenderState renderState,
    Vector3f translation,
    Quaternionf rotation,
    @Nullable Quaternionf overrideCameraAngle,
    int x0,
    int y0,
    int x1,
    int y1,
    float scale,
    @Nullable ScreenRectangle scissorArea,
    @Nullable ScreenRectangle bounds
) implements PictureInPictureRenderState {
    public GuiEntityRenderState(
        EntityRenderState p_415613_,
        Vector3f p_416191_,
        Quaternionf p_416283_,
        @Nullable Quaternionf p_415811_,
        int p_416659_,
        int p_415754_,
        int p_416420_,
        int p_415949_,
        float p_416526_,
        @Nullable ScreenRectangle p_416040_
    ) {
        this(
            p_415613_,
            p_416191_,
            p_416283_,
            p_415811_,
            p_416659_,
            p_415754_,
            p_416420_,
            p_415949_,
            p_416526_,
            p_416040_,
            PictureInPictureRenderState.getBounds(p_416659_, p_415754_, p_416420_, p_415949_, p_416040_)
        );
    }
}
