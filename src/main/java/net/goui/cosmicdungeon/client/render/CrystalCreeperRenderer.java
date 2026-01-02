package net.goui.cosmicdungeon.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.client.model.CrystalCreeperModel;
import net.goui.cosmicdungeon.client.renderstate.CrystalCreeperRenderState;
import net.goui.cosmicdungeon.entity.CrystalCreeperEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

import java.util.EnumMap;
import java.util.Map;

public class CrystalCreeperRenderer
        extends MobRenderer<CrystalCreeperEntity, CrystalCreeperRenderState, CrystalCreeperModel> {

    // One texture per variant
    private static final Map<CrystalCreeperEntity.Variant, ResourceLocation> TEXTURES =
            new EnumMap<>(CrystalCreeperEntity.Variant.class);

    static {
        // assets/cosmicdungeon/textures/entity/crystal_creeper/crystal_creeper_<color>.png
        TEXTURES.put(CrystalCreeperEntity.Variant.TEAL,   tex("crystal_creeper_teal"));
        TEXTURES.put(CrystalCreeperEntity.Variant.BLUE,   tex("crystal_creeper_blue"));
        TEXTURES.put(CrystalCreeperEntity.Variant.GREEN,  tex("crystal_creeper_green"));
        TEXTURES.put(CrystalCreeperEntity.Variant.ORANGE, tex("crystal_creeper_orange"));
        TEXTURES.put(CrystalCreeperEntity.Variant.PURPLE, tex("crystal_creeper_purple"));
        TEXTURES.put(CrystalCreeperEntity.Variant.RED,    tex("crystal_creeper_red"));
    }

    private static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(
                CosmicDungeonMod.MOD_ID,
                "textures/entity/crystal_creeper/" + name + ".png"
        );
    }

    public CrystalCreeperRenderer(EntityRendererProvider.Context ctx) {
        super(
                ctx,
                new CrystalCreeperModel(ctx.bakeLayer(CrystalCreeperModel.LAYER_LOCATION)),
                0.5F // base shadow radius (we can let it stay constant or scale if you want)
        );
    }

    @Override
    public CrystalCreeperRenderState createRenderState() {
        return new CrystalCreeperRenderState();
    }

    @Override
    public void extractRenderState(CrystalCreeperEntity entity,
                                   CrystalCreeperRenderState state,
                                   float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);

        state.eatAnimation = entity.getEatAnimationState();
        state.visualScale  = entity.getVisualScale();
        state.ageInTicks   = entity.tickCount + partialTicks;
        state.variant      = entity.getVariant();
    }

    /**
     * Apply growth-based scale to the whole model.
     */
    @Override
    public void scale(CrystalCreeperRenderState state, PoseStack poseStack) {
        super.scale(state, poseStack);

        float s = state.visualScale;
        poseStack.scale(s, s, s);
    }

    @Override
    public ResourceLocation getTextureLocation(CrystalCreeperRenderState state) {
        CrystalCreeperEntity.Variant variant =
                state.variant != null ? state.variant : CrystalCreeperEntity.Variant.TEAL;

        return TEXTURES.getOrDefault(variant, TEXTURES.get(CrystalCreeperEntity.Variant.TEAL));
    }
}
