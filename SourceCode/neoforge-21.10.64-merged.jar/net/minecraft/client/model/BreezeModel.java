package net.minecraft.client.model;

import java.util.Set;
import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.animation.definitions.BreezeAnimation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.state.BreezeRenderState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BreezeModel extends EntityModel<BreezeRenderState> {
    private static final float WIND_TOP_SPEED = 0.6F;
    private static final float WIND_MIDDLE_SPEED = 0.8F;
    private static final float WIND_BOTTOM_SPEED = 1.0F;
    private final ModelPart head;
    private final ModelPart eyes;
    private final ModelPart wind;
    private final ModelPart windTop;
    private final ModelPart windMid;
    private final ModelPart windBottom;
    private final ModelPart rods;
    private final KeyframeAnimation idleAnimation;
    private final KeyframeAnimation shootAnimation;
    private final KeyframeAnimation slideAnimation;
    private final KeyframeAnimation slideBackAnimation;
    private final KeyframeAnimation inhaleAnimation;
    private final KeyframeAnimation jumpAnimation;

    public BreezeModel(ModelPart root) {
        super(root, RenderType::entityTranslucent);
        this.wind = root.getChild("wind_body");
        this.windBottom = this.wind.getChild("wind_bottom");
        this.windMid = this.windBottom.getChild("wind_mid");
        this.windTop = this.windMid.getChild("wind_top");
        this.head = root.getChild("body").getChild("head");
        this.eyes = this.head.getChild("eyes");
        this.rods = root.getChild("body").getChild("rods");
        this.idleAnimation = BreezeAnimation.IDLE.bake(root);
        this.shootAnimation = BreezeAnimation.SHOOT.bake(root);
        this.slideAnimation = BreezeAnimation.SLIDE.bake(root);
        this.slideBackAnimation = BreezeAnimation.SLIDE_BACK.bake(root);
        this.inhaleAnimation = BreezeAnimation.INHALE.bake(root);
        this.jumpAnimation = BreezeAnimation.JUMP.bake(root);
    }

    private static MeshDefinition createBaseMesh() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        PartDefinition partdefinition1 = partdefinition.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition partdefinition2 = partdefinition1.addOrReplaceChild("rods", CubeListBuilder.create(), PartPose.offset(0.0F, 8.0F, 0.0F));
        partdefinition2.addOrReplaceChild(
            "rod_1",
            CubeListBuilder.create().texOffs(0, 17).addBox(-1.0F, 0.0F, -3.0F, 2.0F, 8.0F, 2.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(2.5981F, -3.0F, 1.5F, -2.7489F, -1.0472F, 3.1416F)
        );
        partdefinition2.addOrReplaceChild(
            "rod_2",
            CubeListBuilder.create().texOffs(0, 17).addBox(-1.0F, 0.0F, -3.0F, 2.0F, 8.0F, 2.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(-2.5981F, -3.0F, 1.5F, -2.7489F, 1.0472F, 3.1416F)
        );
        partdefinition2.addOrReplaceChild(
            "rod_3",
            CubeListBuilder.create().texOffs(0, 17).addBox(-1.0F, 0.0F, -3.0F, 2.0F, 8.0F, 2.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(0.0F, -3.0F, -3.0F, 0.3927F, 0.0F, 0.0F)
        );
        PartDefinition partdefinition3 = partdefinition1.addOrReplaceChild(
            "head",
            CubeListBuilder.create()
                .texOffs(4, 24)
                .addBox(-5.0F, -5.0F, -4.2F, 10.0F, 3.0F, 4.0F, new CubeDeformation(0.0F))
                .texOffs(0, 0)
                .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)),
            PartPose.offset(0.0F, 4.0F, 0.0F)
        );
        partdefinition3.addOrReplaceChild(
            "eyes",
            CubeListBuilder.create()
                .texOffs(4, 24)
                .addBox(-5.0F, -5.0F, -4.2F, 10.0F, 3.0F, 4.0F, new CubeDeformation(0.0F))
                .texOffs(0, 0)
                .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)),
            PartPose.offset(0.0F, 0.0F, 0.0F)
        );
        PartDefinition partdefinition4 = partdefinition.addOrReplaceChild("wind_body", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition partdefinition5 = partdefinition4.addOrReplaceChild(
            "wind_bottom",
            CubeListBuilder.create().texOffs(1, 83).addBox(-2.5F, -7.0F, -2.5F, 5.0F, 7.0F, 5.0F, new CubeDeformation(0.0F)),
            PartPose.offset(0.0F, 24.0F, 0.0F)
        );
        PartDefinition partdefinition6 = partdefinition5.addOrReplaceChild(
            "wind_mid",
            CubeListBuilder.create()
                .texOffs(74, 28)
                .addBox(-6.0F, -6.0F, -6.0F, 12.0F, 6.0F, 12.0F, new CubeDeformation(0.0F))
                .texOffs(78, 32)
                .addBox(-4.0F, -6.0F, -4.0F, 8.0F, 6.0F, 8.0F, new CubeDeformation(0.0F))
                .texOffs(49, 71)
                .addBox(-2.5F, -6.0F, -2.5F, 5.0F, 6.0F, 5.0F, new CubeDeformation(0.0F)),
            PartPose.offset(0.0F, -7.0F, 0.0F)
        );
        partdefinition6.addOrReplaceChild(
            "wind_top",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-9.0F, -8.0F, -9.0F, 18.0F, 8.0F, 18.0F, new CubeDeformation(0.0F))
                .texOffs(6, 6)
                .addBox(-6.0F, -8.0F, -6.0F, 12.0F, 8.0F, 12.0F, new CubeDeformation(0.0F))
                .texOffs(105, 57)
                .addBox(-2.5F, -8.0F, -2.5F, 5.0F, 8.0F, 5.0F, new CubeDeformation(0.0F)),
            PartPose.offset(0.0F, -6.0F, 0.0F)
        );
        return meshdefinition;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = createBaseMesh();
        meshdefinition.getRoot().retainPartsAndChildren(Set.of("head", "rods"));
        return LayerDefinition.create(meshdefinition, 32, 32);
    }

    public static LayerDefinition createWindLayer() {
        MeshDefinition meshdefinition = createBaseMesh();
        meshdefinition.getRoot().retainPartsAndChildren(Set.of("wind_body"));
        return LayerDefinition.create(meshdefinition, 128, 128);
    }

    public static LayerDefinition createEyesLayer() {
        MeshDefinition meshdefinition = createBaseMesh();
        meshdefinition.getRoot().retainPartsAndChildren(Set.of("eyes"));
        return LayerDefinition.create(meshdefinition, 32, 32);
    }

    public void setupAnim(BreezeRenderState renderState) {
        super.setupAnim(renderState);
        this.idleAnimation.apply(renderState.idle, renderState.ageInTicks);
        this.shootAnimation.apply(renderState.shoot, renderState.ageInTicks);
        this.slideAnimation.apply(renderState.slide, renderState.ageInTicks);
        this.slideBackAnimation.apply(renderState.slideBack, renderState.ageInTicks);
        this.inhaleAnimation.apply(renderState.inhale, renderState.ageInTicks);
        this.jumpAnimation.apply(renderState.longJump, renderState.ageInTicks);
    }

    public ModelPart head() {
        return this.head;
    }

    public ModelPart eyes() {
        return this.eyes;
    }

    public ModelPart rods() {
        return this.rods;
    }

    public ModelPart wind() {
        return this.wind;
    }
}
