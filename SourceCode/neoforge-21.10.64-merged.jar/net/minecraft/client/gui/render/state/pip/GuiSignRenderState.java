package net.minecraft.client.gui.render.state.pip;

import javax.annotation.Nullable;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.model.Model;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record GuiSignRenderState(
    Model.Simple signModel,
    WoodType woodType,
    int x0,
    int y0,
    int x1,
    int y1,
    float scale,
    @Nullable ScreenRectangle scissorArea,
    @Nullable ScreenRectangle bounds
) implements PictureInPictureRenderState {
    public GuiSignRenderState(
        Model.Simple p_435719_,
        WoodType p_421576_,
        int p_421949_,
        int p_421975_,
        int p_422255_,
        int p_421655_,
        float p_422106_,
        @Nullable ScreenRectangle p_421999_
    ) {
        this(
            p_435719_,
            p_421576_,
            p_421949_,
            p_421975_,
            p_422255_,
            p_421655_,
            p_422106_,
            p_421999_,
            PictureInPictureRenderState.getBounds(p_421949_, p_421975_, p_422255_, p_421655_, p_421999_)
        );
    }
}
