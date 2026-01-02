package net.goui.cosmicdungeon.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.client.anim.CrystalCreeperAnimations;
import net.goui.cosmicdungeon.client.renderstate.CrystalCreeperRenderState;
import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class CrystalCreeperModel extends EntityModel<CrystalCreeperRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(
                    ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, "crystal_creeper"),
                    "main"
            );

    // We no longer store a separate root field; we use this.root() like the golem model.
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart leg1;
    private final ModelPart leg2;
    private final ModelPart leg3;
    private final ModelPart leg4;

    private final KeyframeAnimation eatAnimation;

    public CrystalCreeperModel(ModelPart root) {
        // Match MetalmancerGolemModel: pass root to super so EntityModel tracks it.
        super(root);

        // Use the tracked root() just like the golem model does in setupAnim.
        ModelPart rootPart = this.root();

        this.body = rootPart.getChild("body");
        this.head = this.body.getChild("head");
        this.leg1 = rootPart.getChild("leg1");
        this.leg2 = rootPart.getChild("leg2");
        this.leg3 = rootPart.getChild("leg3");
        this.leg4 = rootPart.getChild("leg4");

        // Bake animation from the same root part we’re using for everything else.
        this.eatAnimation = CrystalCreeperAnimations.CRYSTAL_CREEPER_EAT.bake(rootPart);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition body = partdefinition.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .texOffs(16, 16)
                        .addBox(-4.0F, -12.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 18.0F, 0.0F));

        body.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, -12.0F, 0.0F));

        partdefinition.addOrReplaceChild("leg1",
                CubeListBuilder.create()
                        .texOffs(0, 16)
                        .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-2.0F, 18.0F, 4.0F));

        partdefinition.addOrReplaceChild("leg2",
                CubeListBuilder.create()
                        .texOffs(0, 16)
                        .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offset(2.0F, 18.0F, 4.0F));

        partdefinition.addOrReplaceChild("leg3",
                CubeListBuilder.create()
                        .texOffs(0, 16)
                        .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-2.0F, 18.0F, -4.0F));

        partdefinition.addOrReplaceChild("leg4",
                CubeListBuilder.create()
                        .texOffs(0, 16)
                        .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offset(2.0F, 18.0F, -4.0F));

        return LayerDefinition.create(meshdefinition, 64, 32);
    }

    @Override
    public void setupAnim(CrystalCreeperRenderState state) {
        // Match golem: reset starting from this.root()
        this.root().getAllParts().forEach(ModelPart::resetPose);

        // Basic vanilla-style walk anim for legs using walkAnimationPos/speed
        float limbSwing = state.walkAnimationPos;
        float limbSwingAmount = state.walkAnimationSpeed;
        float walkSpeed = 1.5F;
        float walkDegree = 1.5F * limbSwingAmount;

        this.leg1.xRot = Mth.cos(limbSwing * walkSpeed) * walkDegree;
        this.leg2.xRot = Mth.cos(limbSwing * walkSpeed + (float) Math.PI) * walkDegree;
        this.leg3.xRot = Mth.cos(limbSwing * walkSpeed + (float) Math.PI) * walkDegree;
        this.leg4.xRot = Mth.cos(limbSwing * walkSpeed) * walkDegree;

        // Head look
        this.head.yRot = state.yRot * (Mth.PI / 180.0F);
        this.head.xRot = state.xRot * (Mth.PI / 180.0F);

        // Eat animation (keyframe-based)
        if (state.eatAnimation != null) {
            this.eatAnimation.apply(state.eatAnimation, state.ageInTicks, 1.0F);
        }
    }

    // NOTE: No renderToBuffer override here — just like MetalmancerGolemModel.
    // EntityModel’s base implementation will call this.root().render(...),
    // using the ModelPart.render(...) you decompiled.
}
