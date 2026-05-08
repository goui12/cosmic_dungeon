package net.minecraft.client.gui.render.state.pip;

import javax.annotation.Nullable;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record GuiSkinRenderState(
    PlayerModel playerModel,
    ResourceLocation texture,
    float rotationX,
    float rotationY,
    float pivotY,
    int x0,
    int y0,
    int x1,
    int y1,
    float scale,
    @Nullable ScreenRectangle scissorArea,
    @Nullable ScreenRectangle bounds
) implements PictureInPictureRenderState {
    public GuiSkinRenderState(
        PlayerModel p_416539_,
        ResourceLocation p_416375_,
        float p_415701_,
        float p_415592_,
        float p_416289_,
        int p_416447_,
        int p_415995_,
        int p_416186_,
        int p_416598_,
        float p_416100_,
        @Nullable ScreenRectangle p_416137_
    ) {
        this(
            p_416539_,
            p_416375_,
            p_415701_,
            p_415592_,
            p_416289_,
            p_416447_,
            p_415995_,
            p_416186_,
            p_416598_,
            p_416100_,
            p_416137_,
            PictureInPictureRenderState.getBounds(p_416447_, p_415995_, p_416186_, p_416598_, p_416137_)
        );
    }
}
