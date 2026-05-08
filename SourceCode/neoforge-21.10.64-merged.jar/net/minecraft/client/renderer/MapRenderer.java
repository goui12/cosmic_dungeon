package net.minecraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.MapTextureManager;
import net.minecraft.client.resources.model.AtlasManager;
import net.minecraft.data.AtlasIds;
import net.minecraft.util.Mth;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MapRenderer {
    private static final float MAP_Z_OFFSET = -0.01F;
    private static final float DECORATION_Z_OFFSET = -0.001F;
    public static final int WIDTH = 128;
    public static final int HEIGHT = 128;
    private final TextureAtlas decorationSprites;
    private final MapTextureManager mapTextureManager;

    public MapRenderer(AtlasManager atlasManager, MapTextureManager mapTextureManager) {
        this.decorationSprites = atlasManager.getAtlasOrThrow(AtlasIds.MAP_DECORATIONS);
        this.mapTextureManager = mapTextureManager;
    }

    public void render(MapRenderState renderState, PoseStack poseStack, SubmitNodeCollector nodeCollector, boolean active, int packedLight) {
        nodeCollector.submitCustomGeometry(poseStack, RenderType.text(renderState.texture), (p_433720_, p_435645_) -> {
            p_435645_.addVertex(p_433720_, 0.0F, 128.0F, -0.01F).setColor(-1).setUv(0.0F, 1.0F).setLight(packedLight);
            p_435645_.addVertex(p_433720_, 128.0F, 128.0F, -0.01F).setColor(-1).setUv(1.0F, 1.0F).setLight(packedLight);
            p_435645_.addVertex(p_433720_, 128.0F, 0.0F, -0.01F).setColor(-1).setUv(1.0F, 0.0F).setLight(packedLight);
            p_435645_.addVertex(p_433720_, 0.0F, 0.0F, -0.01F).setColor(-1).setUv(0.0F, 0.0F).setLight(packedLight);
        });
        int i = 0;

        for (MapRenderState.MapDecorationRenderState maprenderstate$mapdecorationrenderstate : renderState.decorations) {
            if (!active || maprenderstate$mapdecorationrenderstate.renderOnFrame) {
                if (net.neoforged.neoforge.client.gui.map.MapDecorationRendererManager.render(maprenderstate$mapdecorationrenderstate, poseStack, nodeCollector, renderState, MapRenderer.this.decorationSprites, active, packedLight, i)) {
                    i++;
                    continue;
                }
                poseStack.pushPose();
                poseStack.translate(maprenderstate$mapdecorationrenderstate.x / 2.0F + 64.0F, maprenderstate$mapdecorationrenderstate.y / 2.0F + 64.0F, -0.02F);
                poseStack.mulPose(Axis.ZP.rotationDegrees(maprenderstate$mapdecorationrenderstate.rot * 360 / 16.0F));
                poseStack.scale(4.0F, 4.0F, 3.0F);
                poseStack.translate(-0.125F, 0.125F, 0.0F);
                TextureAtlasSprite textureatlassprite = maprenderstate$mapdecorationrenderstate.atlasSprite;
                if (textureatlassprite != null) {
                    float f = i * -0.001F;
                    nodeCollector.submitCustomGeometry(
                        poseStack,
                        RenderType.text(textureatlassprite.atlasLocation()),
                        (p_433913_, p_434099_) -> {
                            p_434099_.addVertex(p_433913_, -1.0F, 1.0F, f)
                                .setColor(-1)
                                .setUv(textureatlassprite.getU0(), textureatlassprite.getV0())
                                .setLight(packedLight);
                            p_434099_.addVertex(p_433913_, 1.0F, 1.0F, f)
                                .setColor(-1)
                                .setUv(textureatlassprite.getU1(), textureatlassprite.getV0())
                                .setLight(packedLight);
                            p_434099_.addVertex(p_433913_, 1.0F, -1.0F, f)
                                .setColor(-1)
                                .setUv(textureatlassprite.getU1(), textureatlassprite.getV1())
                                .setLight(packedLight);
                            p_434099_.addVertex(p_433913_, -1.0F, -1.0F, f)
                                .setColor(-1)
                                .setUv(textureatlassprite.getU0(), textureatlassprite.getV1())
                                .setLight(packedLight);
                        }
                    );
                    poseStack.popPose();
                }

                if (maprenderstate$mapdecorationrenderstate.name != null) {
                    Font font = Minecraft.getInstance().font;
                    float f1 = font.width(maprenderstate$mapdecorationrenderstate.name);
                    float f2 = Mth.clamp(25.0F / f1, 0.0F, 6.0F / 9.0F);
                    poseStack.pushPose();
                    poseStack.translate(
                        maprenderstate$mapdecorationrenderstate.x / 2.0F + 64.0F - f1 * f2 / 2.0F,
                        maprenderstate$mapdecorationrenderstate.y / 2.0F + 64.0F + 4.0F,
                        -0.025F
                    );
                    poseStack.scale(f2, f2, -1.0F);
                    poseStack.translate(0.0F, 0.0F, 0.1F);
                    nodeCollector.order(1)
                        .submitText(
                            poseStack,
                            0.0F,
                            0.0F,
                            maprenderstate$mapdecorationrenderstate.name.getVisualOrderText(),
                            false,
                            Font.DisplayMode.NORMAL,
                            packedLight,
                            -1,
                            Integer.MIN_VALUE,
                            0
                        );
                    poseStack.popPose();
                }

                i++;
            }
        }
    }

    public void extractRenderState(MapId id, MapItemSavedData savedData, MapRenderState renderState) {
        renderState.texture = this.mapTextureManager.prepareMapTexture(id, savedData);
        renderState.decorations.clear();

        net.neoforged.neoforge.client.renderstate.RenderStateExtensions.onUpdateMapRenderState(savedData, renderState);
        for (MapDecoration mapdecoration : savedData.getDecorations()) {
            renderState.decorations.add(net.neoforged.neoforge.client.renderstate.RenderStateExtensions.onUpdateMapDecorationRenderState(mapdecoration.type(), savedData, renderState, this.extractDecorationRenderState(mapdecoration)));
        }
    }

    private MapRenderState.MapDecorationRenderState extractDecorationRenderState(MapDecoration decoration) {
        MapRenderState.MapDecorationRenderState maprenderstate$mapdecorationrenderstate = new MapRenderState.MapDecorationRenderState();
        maprenderstate$mapdecorationrenderstate.type = decoration.type();
        maprenderstate$mapdecorationrenderstate.atlasSprite = this.decorationSprites.getSprite(decoration.getSpriteLocation());
        maprenderstate$mapdecorationrenderstate.x = decoration.x();
        maprenderstate$mapdecorationrenderstate.y = decoration.y();
        maprenderstate$mapdecorationrenderstate.rot = decoration.rot();
        maprenderstate$mapdecorationrenderstate.name = decoration.name().orElse(null);
        maprenderstate$mapdecorationrenderstate.renderOnFrame = decoration.renderOnFrame();
        return maprenderstate$mapdecorationrenderstate;
    }
}
