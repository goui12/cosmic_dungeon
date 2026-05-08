package net.minecraft.client.gui.render.state.pip;

import javax.annotation.Nullable;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.model.BookModel;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record GuiBookModelRenderState(
    BookModel bookModel,
    ResourceLocation texture,
    float open,
    float flip,
    int x0,
    int y0,
    int x1,
    int y1,
    float scale,
    @Nullable ScreenRectangle scissorArea,
    @Nullable ScreenRectangle bounds
) implements PictureInPictureRenderState {
    public GuiBookModelRenderState(
        BookModel p_415797_,
        ResourceLocation p_415682_,
        float p_415696_,
        float p_415739_,
        int p_416486_,
        int p_416556_,
        int p_416330_,
        int p_416572_,
        float p_416303_,
        @Nullable ScreenRectangle p_415924_
    ) {
        this(
            p_415797_,
            p_415682_,
            p_415696_,
            p_415739_,
            p_416486_,
            p_416556_,
            p_416330_,
            p_416572_,
            p_416303_,
            p_415924_,
            PictureInPictureRenderState.getBounds(p_416486_, p_416556_, p_416330_, p_416572_, p_415924_)
        );
    }
}
