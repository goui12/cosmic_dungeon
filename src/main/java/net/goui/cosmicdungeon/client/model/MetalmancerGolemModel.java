package net.goui.cosmicdungeon.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.client.anim.MetalmancerGolemAnimations;
import net.goui.cosmicdungeon.client.renderstate.MetalmancerGolemRenderState;

import net.goui.cosmicdungeon.client.renderstate.StoneWardenRenderState;
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

public class MetalmancerGolemModel extends EntityModel<MetalmancerGolemRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, "metalmancer_golem"), "main");

    private final ModelPart main;
    private final ModelPart body;
    private final ModelPart right_arm;
    private final ModelPart left_arm;
    private final ModelPart head;
    private final ModelPart right_leg;
    private final ModelPart left_leg;

    // Animations baked from tutorial-style constants
    private final KeyframeAnimation walkingAnimation;
    private final KeyframeAnimation attackAnimation;
    private final KeyframeAnimation summonAnimation;
    private final KeyframeAnimation deathAnimation;
    private final KeyframeAnimation idle1Animation;
    private final KeyframeAnimation idle2Animation;
    private final KeyframeAnimation idle3Animation;

    public MetalmancerGolemModel(ModelPart root) {
        super(root);
        this.main = root.getChild("main");
        this.body = this.main.getChild("body");
        this.right_arm = this.body.getChild("right_arm");
        this.left_arm = this.body.getChild("left_arm");
        this.head = this.body.getChild("head");
        this.right_leg = this.main.getChild("right_leg");
        this.left_leg = this.main.getChild("left_leg");

        this.walkingAnimation = MetalmancerGolemAnimations.METALMANCER_GOLEM_WALKING.bake(root);
        this.attackAnimation = MetalmancerGolemAnimations.METALMANCER_GOLEM_ATTACK.bake(root);
        this.summonAnimation = MetalmancerGolemAnimations.METALMANCER_GOLEM_SUMMON.bake(root);
        this.deathAnimation = MetalmancerGolemAnimations.METALMANCER_GOLEM_DEATH.bake(root);
        this.idle1Animation = MetalmancerGolemAnimations.METALMANCER_GOLEM_IDLE1.bake(root);
        this.idle2Animation = MetalmancerGolemAnimations.METALMANCER_GOLEM_IDLE2.bake(root);
        this.idle3Animation = MetalmancerGolemAnimations.METALMANCER_GOLEM_IDLE3.bake(root);

    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition main = partdefinition.addOrReplaceChild("main", CubeListBuilder.create(), PartPose.offset(0.0F, 25.0F, 0.0F));

        PartDefinition body = main.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-6.0F, -14.0F, -3.0F, 12.0F, 9.0F, 6.0F, new CubeDeformation(0.0F))
                .texOffs(26, 30).addBox(-4.5F, -5.0F, -2.0F, 9.0F, 5.0F, 4.0F, new CubeDeformation(0.5F))
                .texOffs(0, 15).addBox(-5.0F, -13.0F, -4.0F, 10.0F, 7.0F, 8.0F, new CubeDeformation(0.0F))
                .texOffs(36, 24).addBox(2.0F, -8.0F, -2.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(36, 26).addBox(2.0F, -8.0F, -2.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(36, 28).addBox(2.0F, -8.0F, -2.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(40, 24).addBox(2.0F, -8.0F, -2.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(40, 26).addBox(2.0F, -8.0F, -2.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(40, 28).addBox(2.0F, -8.0F, -2.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(16, 42).addBox(2.0F, -8.0F, -2.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(20, 42).addBox(2.0F, -8.0F, -2.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(42, 21).addBox(2.0F, -8.0F, -2.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(16, 44).addBox(2.0F, -8.0F, -2.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(20, 44).addBox(2.0F, -8.0F, -2.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(44, 23).addBox(2.0F, -8.0F, -2.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(44, 25).addBox(2.0F, -8.0F, -2.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(44, 27).addBox(2.0F, -8.0F, -2.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(16, 46).addBox(2.0F, -8.0F, -2.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -11.0F, -1.0F));

        PartDefinition right_arm = body.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(36, 0).addBox(-3.0F, -1.5F, -3.0F, 3.0F, 18.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(-6.0F, -13.0F, 1.0F));

        PartDefinition left_arm = body.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(26, 39).addBox(0.0F, -1.5F, -3.0F, 3.0F, 18.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(6.0F, -13.0F, 1.0F));

        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 30).addBox(-4.0F, -7.0F, -1.5F, 8.0F, 7.0F, 5.0F, new CubeDeformation(0.0F))
                .texOffs(36, 21).addBox(-1.0F, -3.0F, -2.5F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -14.0F, -1.0F));

        PartDefinition right_leg = main.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(38, 39).addBox(-2.5F, 0.0F, -3.0F, 4.0F, 10.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(-3.0F, -11.0F, 0.0F));

        PartDefinition left_leg = main.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 42).addBox(-1.5F, 0.0F, -3.0F, 4.0F, 10.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(3.0F, -11.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public void setupAnim(MetalmancerGolemRenderState state) {
        this.root().getAllParts().forEach(ModelPart::resetPose);
        this.walkingAnimation.applyWalk(state.walkAnimationPos, state.walkAnimationSpeed, 2.5f, 2.5f);
        this.summonAnimation.apply(state.summonAnimation, state.ageInTicks, 1f);
        this.idle1Animation.apply(state.idle1Animation, state.ageInTicks, 1f);
        this.idle2Animation.apply(state.idle2Animation, state.ageInTicks, 1f);
        this.idle3Animation.apply(state.idle3Animation, state.ageInTicks, 1f);
        this.deathAnimation.apply(state.deathAnimation, state.ageInTicks, 1f);
        this.attackAnimation.apply(state.attackAnimation, state.ageInTicks, 1f);

    }
}
