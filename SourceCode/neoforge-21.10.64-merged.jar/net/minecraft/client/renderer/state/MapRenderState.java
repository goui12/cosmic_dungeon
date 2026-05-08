package net.minecraft.client.renderer.state;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MapRenderState extends net.neoforged.neoforge.client.renderstate.BaseRenderState {
    @Nullable
    public ResourceLocation texture;
    public final List<MapRenderState.MapDecorationRenderState> decorations = new ArrayList<>();

    @OnlyIn(Dist.CLIENT)
    public static class MapDecorationRenderState extends net.neoforged.neoforge.client.renderstate.BaseRenderState {
        public net.minecraft.core.Holder<net.minecraft.world.level.saveddata.maps.MapDecorationType> type;
        @Nullable
        public TextureAtlasSprite atlasSprite;
        public byte x;
        public byte y;
        public byte rot;
        public boolean renderOnFrame;
        @Nullable
        public Component name;
    }
}
