package net.minecraft.client.gui.render.state.pip;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.util.profiling.ResultField;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record GuiProfilerChartRenderState(
    List<ResultField> chartData, int x0, int y0, int x1, int y1, @Nullable ScreenRectangle scissorArea, @Nullable ScreenRectangle bounds
) implements PictureInPictureRenderState {
    public GuiProfilerChartRenderState(
        List<ResultField> p_415952_, int p_415838_, int p_415988_, int p_416491_, int p_416487_, @Nullable ScreenRectangle p_415627_
    ) {
        this(
            p_415952_,
            p_415838_,
            p_415988_,
            p_416491_,
            p_416487_,
            p_415627_,
            PictureInPictureRenderState.getBounds(p_415838_, p_415988_, p_416491_, p_416487_, p_415627_)
        );
    }

    @Override
    public float scale() {
        return 1.0F;
    }
}
