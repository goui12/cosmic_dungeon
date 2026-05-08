package net.minecraft.client.gui.render.state.pip;

import javax.annotation.Nullable;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.model.BannerFlagModel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record GuiBannerResultRenderState(
    BannerFlagModel flag,
    DyeColor baseColor,
    BannerPatternLayers resultBannerPatterns,
    int x0,
    int y0,
    int x1,
    int y1,
    @Nullable ScreenRectangle scissorArea,
    @Nullable ScreenRectangle bounds
) implements PictureInPictureRenderState {
    public GuiBannerResultRenderState(
        BannerFlagModel p_449939_,
        DyeColor p_416489_,
        BannerPatternLayers p_416057_,
        int p_416374_,
        int p_416493_,
        int p_415894_,
        int p_416726_,
        @Nullable ScreenRectangle p_416459_
    ) {
        this(
            p_449939_,
            p_416489_,
            p_416057_,
            p_416374_,
            p_416493_,
            p_415894_,
            p_416726_,
            p_416459_,
            PictureInPictureRenderState.getBounds(p_416374_, p_416493_, p_415894_, p_416726_, p_416459_)
        );
    }

    @Override
    public float scale() {
        return 16.0F;
    }
}
