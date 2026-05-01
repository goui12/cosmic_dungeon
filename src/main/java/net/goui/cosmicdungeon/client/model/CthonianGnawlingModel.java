package net.goui.cosmicdungeon.client.model;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.client.anim.CthonianGnawlingAnimation;
import net.goui.cosmicdungeon.client.renderstate.CthonianGnawlingRenderState;
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

public class CthonianGnawlingModel extends EntityModel<CthonianGnawlingRenderState> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, "cthonian_gnawling"), "main");
	private final ModelPart root;
	private final ModelPart Main_Bone;
	private final ModelPart Head_Main;
	private final ModelPart Head_1;
	private final ModelPart Head_2;
	private final ModelPart Head_3;
	private final ModelPart Head_4;
	private final ModelPart Head_5;
	private final ModelPart Head_6;
	private final ModelPart Head_7;
	private final ModelPart Head_8;
	private final ModelPart Head_9;
	private final ModelPart Head_10;
	private final ModelPart Teeth;
	private final ModelPart Tooth_1;
	private final ModelPart Tooth_2;
	private final ModelPart Tooth_3;
	private final ModelPart Tooth_7;
	private final ModelPart Tooth_8;
	private final ModelPart Tooth_4;
	private final ModelPart Tooth_5;
	private final ModelPart Tooth_6;
	private final ModelPart Head_Seg;
	private final ModelPart Body_1;
	private final ModelPart Body1;
	private final ModelPart Body_1_Seg;
	private final ModelPart Body_2;
	private final ModelPart Body2;
	private final ModelPart Body_2_Seg;
	private final ModelPart Body_3;
	private final ModelPart Body3;
	private final ModelPart Body_3_Seg;
	private final ModelPart Body_4;
	private final ModelPart Body4;
	private final ModelPart Body_4_Seg;
	private final ModelPart Body_5;
	private final ModelPart Body5;
	private final ModelPart Body_5_Seg;
	private final ModelPart Body_6;
	private final ModelPart Body6;
	private final ModelPart Body_6_Seg;
	private final ModelPart Body_7;
	private final ModelPart Body7;
	private final ModelPart Body_7_Seg;
	private final ModelPart Body_8;
	private final ModelPart Body8;
	private final ModelPart Body_8_Seg;
	private final ModelPart Body_9;
	private final ModelPart Body9;
	private final ModelPart Body_9_Seg;
	private final ModelPart Body_10;
	private final ModelPart Body10;
	private final KeyframeAnimation walkingAnimation;
	private final KeyframeAnimation chompAnimation;

	public CthonianGnawlingModel(ModelPart root) {
		super(root);
		this.root = root;
		this.walkingAnimation = CthonianGnawlingAnimation.WALKING_BODY.bake(root);
		this.chompAnimation = CthonianGnawlingAnimation.CHOMP.bake(root);
		this.Main_Bone = root.getChild("Main_Bone");
		this.Head_Main = this.Main_Bone.getChild("Head_Main");
		this.Head_1 = this.Head_Main.getChild("Head_1");
		this.Head_2 = this.Head_Main.getChild("Head_2");
		this.Head_3 = this.Head_Main.getChild("Head_3");
		this.Head_4 = this.Head_Main.getChild("Head_4");
		this.Head_5 = this.Head_Main.getChild("Head_5");
		this.Head_6 = this.Head_Main.getChild("Head_6");
		this.Head_7 = this.Head_Main.getChild("Head_7");
		this.Head_8 = this.Head_Main.getChild("Head_8");
		this.Head_9 = this.Head_Main.getChild("Head_9");
		this.Head_10 = this.Head_Main.getChild("Head_10");
		this.Teeth = this.Head_Main.getChild("Teeth");
		this.Tooth_1 = this.Teeth.getChild("Tooth_1");
		this.Tooth_2 = this.Teeth.getChild("Tooth_2");
		this.Tooth_3 = this.Teeth.getChild("Tooth_3");
		this.Tooth_7 = this.Teeth.getChild("Tooth_7");
		this.Tooth_8 = this.Teeth.getChild("Tooth_8");
		this.Tooth_4 = this.Teeth.getChild("Tooth_4");
		this.Tooth_5 = this.Teeth.getChild("Tooth_5");
		this.Tooth_6 = this.Teeth.getChild("Tooth_6");
		this.Head_Seg = this.Head_Main.getChild("Head_Seg");
		this.Body_1 = this.Head_Seg.getChild("Body_1");
		this.Body1 = this.Body_1.getChild("Body1");
		this.Body_1_Seg = this.Body_1.getChild("Body_1_Seg");
		this.Body_2 = this.Body_1_Seg.getChild("Body_2");
		this.Body2 = this.Body_2.getChild("Body2");
		this.Body_2_Seg = this.Body_2.getChild("Body_2_Seg");
		this.Body_3 = this.Body_2_Seg.getChild("Body_3");
		this.Body3 = this.Body_3.getChild("Body3");
		this.Body_3_Seg = this.Body_3.getChild("Body_3_Seg");
		this.Body_4 = this.Body_3_Seg.getChild("Body_4");
		this.Body4 = this.Body_4.getChild("Body4");
		this.Body_4_Seg = this.Body_4.getChild("Body_4_Seg");
		this.Body_5 = this.Body_4_Seg.getChild("Body_5");
		this.Body5 = this.Body_5.getChild("Body5");
		this.Body_5_Seg = this.Body_5.getChild("Body_5_Seg");
		this.Body_6 = this.Body_5_Seg.getChild("Body_6");
		this.Body6 = this.Body_6.getChild("Body6");
		this.Body_6_Seg = this.Body_6.getChild("Body_6_Seg");
		this.Body_7 = this.Body_6_Seg.getChild("Body_7");
		this.Body7 = this.Body_7.getChild("Body7");
		this.Body_7_Seg = this.Body_7.getChild("Body_7_Seg");
		this.Body_8 = this.Body_7_Seg.getChild("Body_8");
		this.Body8 = this.Body_8.getChild("Body8");
		this.Body_8_Seg = this.Body_8.getChild("Body_8_Seg");
		this.Body_9 = this.Body_8_Seg.getChild("Body_9");
		this.Body9 = this.Body_9.getChild("Body9");
		this.Body_9_Seg = this.Body_9.getChild("Body_9_Seg");
		this.Body_10 = this.Body_9_Seg.getChild("Body_10");
		this.Body10 = this.Body_10.getChild("Body10");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition Main_Bone = partdefinition.addOrReplaceChild("Main_Bone", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition Head_Main = Main_Bone.addOrReplaceChild("Head_Main", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition Head_1 = Head_Main.addOrReplaceChild("Head_1", CubeListBuilder.create().texOffs(49, 31).addBox(-5.0F, -19.0F, -8.0F, 10.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(90, 61).addBox(-5.0F, -1.0F, -8.0F, 10.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(158, 76).addBox(-8.0F, -18.0F, -8.0F, 4.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(72, 31).addBox(-8.0F, -2.0F, -8.0F, 6.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(187, 195).addBox(9.0F, -13.0F, -8.0F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(191, 235).addBox(8.0F, -17.0F, -8.0F, 1.0F, 5.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(235, 124).addBox(8.0F, -8.0F, -8.0F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(162, 15).addBox(4.0F, -18.0F, -8.0F, 4.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(160, 46).addBox(2.0F, -2.0F, -8.0F, 6.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(186, 235).addBox(-9.0F, -17.0F, -8.0F, 1.0F, 5.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(181, 232).addBox(-9.0F, -8.0F, -8.0F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(29, 117).addBox(-10.0F, -13.0F, -8.0F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition Head_2 = Head_Main.addOrReplaceChild("Head_2", CubeListBuilder.create().texOffs(173, 145).addBox(-5.0F, -19.0F, -8.0F, 10.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(112, 212).addBox(-5.0F, -3.0F, -8.0F, 10.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(29, 98).addBox(-8.0F, -18.0F, -8.0F, 1.0F, 17.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(235, 114).addBox(-7.0F, -18.0F, -8.0F, 2.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(196, 235).addBox(-7.0F, -4.0F, -8.0F, 2.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(163, 205).addBox(9.0F, -13.0F, -8.0F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(180, 215).addBox(7.0F, -17.0F, -8.0F, 2.0F, 15.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(228, 33).addBox(5.0F, -4.0F, -8.0F, 3.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(230, 143).addBox(5.0F, -18.0F, -8.0F, 3.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(94, 216).addBox(-9.0F, -17.0F, -8.0F, 1.0F, 15.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(215, 198).addBox(-10.0F, -13.0F, -8.0F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 1.0F));

		PartDefinition Head_3 = Head_Main.addOrReplaceChild("Head_3", CubeListBuilder.create().texOffs(206, 68).addBox(-5.0F, -4.0F, -8.0F, 10.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(46, 207).addBox(-5.0F, -19.0F, -8.0F, 10.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(173, 215).addBox(-8.0F, -18.0F, -8.0F, 2.0F, 17.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(167, 227).addBox(9.0F, -13.0F, -8.0F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(123, 217).addBox(8.0F, -17.0F, -8.0F, 1.0F, 15.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(215, 179).addBox(6.0F, -18.0F, -8.0F, 2.0F, 17.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(217, 49).addBox(5.0F, -18.0F, -8.0F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(236, 76).addBox(-6.0F, -18.0F, -8.0F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(167, 236).addBox(-6.0F, -5.0F, -8.0F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(236, 70).addBox(5.0F, -5.0F, -8.0F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(128, 217).addBox(-9.0F, -17.0F, -8.0F, 1.0F, 15.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(228, 38).addBox(-10.0F, -13.0F, -8.0F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 2.0F));

		PartDefinition Head_4 = Head_Main.addOrReplaceChild("Head_4", CubeListBuilder.create().texOffs(0, 201).addBox(-5.0F, -5.0F, -8.0F, 10.0F, 5.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(23, 201).addBox(-5.0F, -19.0F, -8.0F, 10.0F, 5.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(35, 66).addBox(-8.0F, -18.0F, -8.0F, 3.0F, 17.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(228, 92).addBox(9.0F, -13.0F, -8.0F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(133, 217).addBox(8.0F, -17.0F, -8.0F, 1.0F, 15.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(54, 127).addBox(5.0F, -18.0F, -8.0F, 3.0F, 17.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(138, 217).addBox(-9.0F, -17.0F, -8.0F, 1.0F, 15.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(229, 0).addBox(-10.0F, -13.0F, -8.0F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 3.0F));

		PartDefinition Head_5 = Head_Main.addOrReplaceChild("Head_5", CubeListBuilder.create().texOffs(145, 197).addBox(-5.0F, -6.0F, -8.0F, 10.0F, 6.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(50, 199).addBox(-5.0F, -19.0F, -8.0F, 10.0F, 6.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(187, 207).addBox(-8.0F, -18.0F, -8.0F, 3.0F, 17.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(99, 231).addBox(-5.0F, -13.0F, -8.0F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(104, 231).addBox(4.0F, -13.0F, -8.0F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(65, 229).addBox(9.0F, -13.0F, -8.0F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(143, 217).addBox(8.0F, -17.0F, -8.0F, 1.0F, 15.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(196, 207).addBox(5.0F, -18.0F, -8.0F, 3.0F, 17.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(148, 217).addBox(-9.0F, -17.0F, -8.0F, 1.0F, 15.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(229, 65).addBox(-10.0F, -13.0F, -8.0F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 4.0F));

		PartDefinition Head_6 = Head_Main.addOrReplaceChild("Head_6", CubeListBuilder.create().texOffs(196, 139).addBox(-5.0F, -7.0F, -8.0F, 10.0F, 7.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(230, 148).addBox(-5.0F, -12.0F, -8.0F, 2.0F, 5.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(230, 166).addBox(3.0F, -12.0F, -8.0F, 2.0F, 5.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(196, 148).addBox(-5.0F, -19.0F, -8.0F, 10.0F, 7.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(205, 207).addBox(-8.0F, -18.0F, -8.0F, 3.0F, 17.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(70, 229).addBox(9.0F, -13.0F, -8.0F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(153, 217).addBox(8.0F, -17.0F, -8.0F, 1.0F, 15.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(0, 208).addBox(5.0F, -18.0F, -8.0F, 3.0F, 17.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(218, 14).addBox(-9.0F, -17.0F, -8.0F, 1.0F, 15.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(75, 229).addBox(-10.0F, -13.0F, -8.0F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 5.0F));

		PartDefinition Head_7 = Head_Main.addOrReplaceChild("Head_7", CubeListBuilder.create().texOffs(160, 36).addBox(-5.0F, -8.0F, -8.0F, 10.0F, 8.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(196, 129).addBox(-5.0F, -19.0F, -8.0F, 10.0F, 8.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(140, 200).addBox(-3.0F, -11.0F, -8.0F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(236, 178).addBox(2.0F, -11.0F, -8.0F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(9, 208).addBox(-8.0F, -18.0F, -8.0F, 3.0F, 17.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(80, 229).addBox(9.0F, -13.0F, -8.0F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(45, 218).addBox(8.0F, -17.0F, -8.0F, 1.0F, 15.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(18, 208).addBox(5.0F, -18.0F, -8.0F, 3.0F, 17.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(50, 218).addBox(-9.0F, -17.0F, -8.0F, 1.0F, 15.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(85, 229).addBox(-10.0F, -13.0F, -8.0F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 6.0F));

		PartDefinition Head_8 = Head_Main.addOrReplaceChild("Head_8", CubeListBuilder.create().texOffs(121, 49).addBox(-5.0F, -9.0F, -8.0F, 10.0F, 9.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(123, 19).addBox(-5.0F, -19.0F, -8.0F, 10.0F, 9.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(175, 46).addBox(-2.0F, -10.0F, -8.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(198, 94).addBox(1.0F, -10.0F, -8.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(27, 208).addBox(-8.0F, -18.0F, -8.0F, 3.0F, 17.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(230, 101).addBox(9.0F, -13.0F, -8.0F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(55, 218).addBox(8.0F, -17.0F, -8.0F, 1.0F, 15.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(36, 208).addBox(5.0F, -18.0F, -8.0F, 3.0F, 17.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(60, 218).addBox(-9.0F, -17.0F, -8.0F, 1.0F, 15.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(230, 110).addBox(-10.0F, -13.0F, -8.0F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 7.0F));

		PartDefinition Head_9 = Head_Main.addOrReplaceChild("Head_9", CubeListBuilder.create().texOffs(145, 176).addBox(-5.0F, -19.0F, -8.0F, 10.0F, 19.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(208, 36).addBox(-8.0F, -18.0F, -8.0F, 3.0F, 17.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(112, 230).addBox(9.0F, -13.0F, -8.0F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(219, 127).addBox(8.0F, -17.0F, -8.0F, 1.0F, 15.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(103, 212).addBox(5.0F, -18.0F, -8.0F, 3.0F, 17.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(222, 179).addBox(-9.0F, -17.0F, -8.0F, 1.0F, 15.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(117, 230).addBox(-10.0F, -13.0F, -8.0F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 8.0F));

		PartDefinition Head_10 = Head_Main.addOrReplaceChild("Head_10", CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -19.0F, -8.0F, 10.0F, 19.0F, 14.0F, new CubeDeformation(0.0F))
		.texOffs(47, 65).addBox(-8.0F, -18.0F, -8.0F, 3.0F, 17.0F, 14.0F, new CubeDeformation(0.0F))
		.texOffs(142, 106).addBox(9.0F, -13.0F, -8.0F, 1.0F, 7.0F, 14.0F, new CubeDeformation(0.0F))
		.texOffs(90, 31).addBox(8.0F, -17.0F, -8.0F, 1.0F, 15.0F, 14.0F, new CubeDeformation(0.0F))
		.texOffs(0, 66).addBox(5.0F, -18.0F, -8.0F, 3.0F, 17.0F, 14.0F, new CubeDeformation(0.0F))
		.texOffs(92, 0).addBox(-9.0F, -17.0F, -8.0F, 1.0F, 15.0F, 14.0F, new CubeDeformation(0.0F))
		.texOffs(142, 128).addBox(-10.0F, -13.0F, -8.0F, 1.0F, 7.0F, 14.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 9.0F));

		PartDefinition Teeth = Head_Main.addOrReplaceChild("Teeth", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition Tooth_1 = Teeth.addOrReplaceChild("Tooth_1", CubeListBuilder.create().texOffs(113, 61).addBox(0.0F, -1.0F, 0.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(122, 172).addBox(-1.0F, -1.0F, -0.25F, 2.0F, 2.0F, 1.0F, new CubeDeformation(-0.18F))
		.texOffs(230, 173).addBox(-0.95F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.28F))
		.texOffs(231, 194).addBox(-0.8F, -1.0F, -1.65F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.4F))
		.texOffs(231, 199).addBox(-0.6F, -1.0F, -2.4F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.55F))
		.texOffs(27, 227).addBox(-0.5F, -1.0F, -2.89F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.65F))
		.texOffs(235, 104).addBox(-0.37F, -1.0F, -3.2F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.75F)), PartPose.offset(-6.0F, -10.0F, -6.0F));

		PartDefinition cube_r1 = Tooth_1.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(36, 227).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.75F)), PartPose.offsetAndRotation(0.63F, 0.0F, -2.3F, 0.0F, -1.2217F, 0.0F));

		PartDefinition Tooth_2 = Teeth.addOrReplaceChild("Tooth_2", CubeListBuilder.create().texOffs(136, 172).addBox(0.0F, -1.0F, 0.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(129, 172).addBox(-1.0F, -1.0F, -0.25F, 2.0F, 2.0F, 1.0F, new CubeDeformation(-0.18F))
		.texOffs(232, 9).addBox(-0.95F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.28F))
		.texOffs(232, 14).addBox(-0.8F, -1.0F, -1.65F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.4F))
		.texOffs(232, 19).addBox(-0.6F, -1.0F, -2.4F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.55F))
		.texOffs(158, 227).addBox(-0.5F, -1.0F, -2.89F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.65F))
		.texOffs(235, 109).addBox(-0.37F, -1.0F, -3.2F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.75F)), PartPose.offsetAndRotation(6.0F, -10.0F, -6.0F, 0.0F, 0.0F, -3.1416F));

		PartDefinition cube_r2 = Tooth_2.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(227, 179).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.75F)), PartPose.offsetAndRotation(0.63F, 0.0F, -2.3F, 0.0F, -1.2217F, 0.0F));

		PartDefinition Tooth_3 = Teeth.addOrReplaceChild("Tooth_3", CubeListBuilder.create().texOffs(203, 167).addBox(0.0F, -1.0F, 0.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(173, 15).addBox(-1.0F, -1.0F, -0.25F, 2.0F, 2.0F, 1.0F, new CubeDeformation(-0.18F))
		.texOffs(232, 24).addBox(-0.95F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.28F))
		.texOffs(27, 232).addBox(-0.8F, -1.0F, -1.65F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.4F))
		.texOffs(36, 232).addBox(-0.6F, -1.0F, -2.4F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.55F))
		.texOffs(227, 184).addBox(-0.5F, -1.0F, -2.89F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.65F))
		.texOffs(232, 47).addBox(-0.37F, -1.0F, -3.2F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.75F)), PartPose.offsetAndRotation(0.0F, -3.0F, -7.0F, 0.0F, -0.3927F, -1.5708F));

		PartDefinition cube_r3 = Tooth_3.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(232, 52).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.75F)), PartPose.offsetAndRotation(0.63F, 0.0F, -2.3F, 0.0F, -1.2217F, 0.0F));

		PartDefinition Tooth_7 = Teeth.addOrReplaceChild("Tooth_7", CubeListBuilder.create().texOffs(234, 5).addBox(0.0F, -1.0F, 0.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(187, 15).addBox(-1.0F, -1.0F, -0.25F, 2.0F, 2.0F, 1.0F, new CubeDeformation(-0.18F))
		.texOffs(234, 0).addBox(-0.95F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.28F))
		.texOffs(234, 65).addBox(-0.8F, -1.0F, -1.65F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.4F))
		.texOffs(234, 83).addBox(-0.6F, -1.0F, -2.4F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.55F))
		.texOffs(227, 217).addBox(-0.5F, -1.0F, -2.89F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.65F))
		.texOffs(234, 119).addBox(-0.37F, -1.0F, -3.2F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.75F)), PartPose.offsetAndRotation(-4.0F, -4.0F, -6.0F, 0.0F, -0.3927F, -1.5708F));

		PartDefinition cube_r4 = Tooth_7.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(122, 234).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.75F)), PartPose.offsetAndRotation(0.63F, 0.0F, -2.3F, 0.0F, -1.2217F, 0.0F));

		PartDefinition Tooth_8 = Teeth.addOrReplaceChild("Tooth_8", CubeListBuilder.create().texOffs(234, 88).addBox(0.0F, -1.0F, 0.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(196, 167).addBox(-1.0F, -1.0F, -0.25F, 2.0F, 2.0F, 1.0F, new CubeDeformation(-0.18F))
		.texOffs(131, 234).addBox(-0.95F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.28F))
		.texOffs(140, 234).addBox(-0.8F, -1.0F, -1.65F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.4F))
		.texOffs(149, 234).addBox(-0.6F, -1.0F, -2.4F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.55F))
		.texOffs(223, 227).addBox(-0.5F, -1.0F, -2.89F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.65F))
		.texOffs(172, 234).addBox(-0.37F, -1.0F, -3.2F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.75F)), PartPose.offsetAndRotation(4.0F, -4.0F, -6.0F, 0.0F, -0.3927F, -1.5708F));

		PartDefinition cube_r5 = Tooth_8.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(45, 235).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.75F)), PartPose.offsetAndRotation(0.63F, 0.0F, -2.3F, 0.0F, -1.2217F, 0.0F));

		PartDefinition Tooth_4 = Teeth.addOrReplaceChild("Tooth_4", CubeListBuilder.create().texOffs(208, 167).addBox(0.0F, -1.0F, 0.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(79, 174).addBox(-1.0F, -1.0F, -0.25F, 2.0F, 2.0F, 1.0F, new CubeDeformation(-0.18F))
		.texOffs(232, 155).addBox(-0.95F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.28F))
		.texOffs(158, 232).addBox(-0.8F, -1.0F, -1.65F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.4F))
		.texOffs(232, 160).addBox(-0.6F, -1.0F, -2.4F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.55F))
		.texOffs(227, 189).addBox(-0.5F, -1.0F, -2.89F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.65F))
		.texOffs(205, 232).addBox(-0.37F, -1.0F, -3.2F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.75F)), PartPose.offsetAndRotation(0.0F, -16.0F, -7.0F, 0.0F, -0.3927F, 1.5708F));

		PartDefinition cube_r6 = Tooth_4.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(223, 232).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.75F)), PartPose.offsetAndRotation(0.63F, 0.0F, -2.3F, 0.0F, -1.2217F, 0.0F));

		PartDefinition Tooth_5 = Teeth.addOrReplaceChild("Tooth_5", CubeListBuilder.create().texOffs(232, 29).addBox(0.0F, -1.0F, 0.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(86, 174).addBox(-1.0F, -1.0F, -0.25F, 2.0F, 2.0F, 1.0F, new CubeDeformation(-0.18F))
		.texOffs(232, 227).addBox(-0.95F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.28F))
		.texOffs(232, 232).addBox(-0.8F, -1.0F, -1.65F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.4F))
		.texOffs(0, 233).addBox(-0.6F, -1.0F, -2.4F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.55F))
		.texOffs(227, 207).addBox(-0.5F, -1.0F, -2.89F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.65F))
		.texOffs(9, 233).addBox(-0.37F, -1.0F, -3.2F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.75F)), PartPose.offsetAndRotation(-4.0F, -15.0F, -6.0F, 0.0F, -0.3927F, 1.5708F));

		PartDefinition cube_r7 = Tooth_5.addOrReplaceChild("cube_r7", CubeListBuilder.create().texOffs(18, 233).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.75F)), PartPose.offsetAndRotation(0.63F, 0.0F, -2.3F, 0.0F, -1.2217F, 0.0F));

		PartDefinition Tooth_6 = Teeth.addOrReplaceChild("Tooth_6", CubeListBuilder.create().texOffs(233, 43).addBox(0.0F, -1.0F, 0.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(180, 15).addBox(-1.0F, -1.0F, -0.25F, 2.0F, 2.0F, 1.0F, new CubeDeformation(-0.18F))
		.texOffs(233, 38).addBox(-0.95F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.28F))
		.texOffs(90, 233).addBox(-0.8F, -1.0F, -1.65F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.4F))
		.texOffs(233, 92).addBox(-0.6F, -1.0F, -2.4F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.55F))
		.texOffs(227, 212).addBox(-0.5F, -1.0F, -2.89F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.65F))
		.texOffs(233, 136).addBox(-0.37F, -1.0F, -3.2F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.75F)), PartPose.offsetAndRotation(4.0F, -15.0F, -6.0F, 0.0F, -0.3927F, 1.5708F));

		PartDefinition cube_r8 = Tooth_6.addOrReplaceChild("cube_r8", CubeListBuilder.create().texOffs(214, 233).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.75F)), PartPose.offsetAndRotation(0.63F, 0.0F, -2.3F, 0.0F, -1.2217F, 0.0F));

		PartDefinition Head_Seg = Head_Main.addOrReplaceChild("Head_Seg", CubeListBuilder.create().texOffs(119, 61).addBox(-8.0F, -7.0F, -1.0F, 16.0F, 15.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(195, 31).addBox(-5.0F, 8.0F, -1.0F, 10.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(196, 157).addBox(-5.0F, -8.0F, -1.0F, 10.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(220, 0).addBox(-9.0F, -3.0F, -1.0F, 1.0F, 7.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(222, 196).addBox(8.0F, -3.0F, -1.0F, 1.0F, 7.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -10.0F, 15.0F));

		PartDefinition Body_1 = Head_Seg.addOrReplaceChild("Body_1", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 1.0F));

		PartDefinition Body1 = Body_1.addOrReplaceChild("Body1", CubeListBuilder.create().texOffs(47, 34).addBox(-5.0F, -19.0F, -8.0F, 10.0F, 19.0F, 11.0F, new CubeDeformation(0.0F))
		.texOffs(0, 98).addBox(-8.0F, -18.0F, -8.0F, 3.0F, 17.0F, 11.0F, new CubeDeformation(0.0F))
		.texOffs(50, 180).addBox(9.0F, -13.0F, -8.0F, 1.0F, 7.0F, 11.0F, new CubeDeformation(0.0F))
		.texOffs(54, 153).addBox(8.0F, -17.0F, -8.0F, 1.0F, 15.0F, 11.0F, new CubeDeformation(0.0F))
		.texOffs(113, 94).addBox(5.0F, -18.0F, -8.0F, 3.0F, 17.0F, 11.0F, new CubeDeformation(0.0F))
		.texOffs(0, 155).addBox(-9.0F, -17.0F, -8.0F, 1.0F, 15.0F, 11.0F, new CubeDeformation(0.0F))
		.texOffs(0, 182).addBox(-10.0F, -13.0F, -8.0F, 1.0F, 7.0F, 11.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 10.0F, 8.0F));

		PartDefinition Body_1_Seg = Body_1.addOrReplaceChild("Body_1_Seg", CubeListBuilder.create().texOffs(121, 30).addBox(-8.0F, -7.0F, -1.0F, 16.0F, 15.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(196, 162).addBox(-5.0F, 8.0F, -1.0F, 10.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(198, 74).addBox(-5.0F, -8.0F, -1.0F, 10.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(214, 222).addBox(-9.0F, -3.0F, -1.0F, 1.0F, 7.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(223, 11).addBox(8.0F, -3.0F, -1.0F, 1.0F, 7.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 11.0F));

		PartDefinition Body_2 = Body_1_Seg.addOrReplaceChild("Body_2", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 1.0F));

		PartDefinition Body2 = Body_2.addOrReplaceChild("Body2", CubeListBuilder.create().texOffs(0, 34).addBox(-6.0F, -20.0F, -8.0F, 12.0F, 20.0F, 11.0F, new CubeDeformation(0.0F))
		.texOffs(82, 94).addBox(-9.0F, -19.0F, -8.0F, 4.0F, 18.0F, 11.0F, new CubeDeformation(0.0F))
		.texOffs(169, 150).addBox(9.0F, -14.0F, -8.0F, 2.0F, 9.0F, 11.0F, new CubeDeformation(0.0F))
		.texOffs(0, 127).addBox(8.0F, -18.0F, -8.0F, 2.0F, 16.0F, 11.0F, new CubeDeformation(0.0F))
		.texOffs(35, 97).addBox(5.0F, -19.0F, -8.0F, 4.0F, 18.0F, 11.0F, new CubeDeformation(0.0F))
		.texOffs(27, 127).addBox(-10.0F, -18.0F, -8.0F, 2.0F, 16.0F, 11.0F, new CubeDeformation(0.0F))
		.texOffs(171, 76).addBox(-11.0F, -14.0F, -8.0F, 2.0F, 9.0F, 11.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 10.0F, 8.0F));

		PartDefinition Body_2_Seg = Body_2.addOrReplaceChild("Body_2_Seg", CubeListBuilder.create().texOffs(123, 0).addBox(-8.0F, -7.0F, -1.0F, 16.0F, 15.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(198, 79).addBox(-5.0F, 8.0F, -1.0F, 10.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(198, 84).addBox(-5.0F, -8.0F, -1.0F, 10.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(223, 22).addBox(-9.0F, -3.0F, -1.0F, 1.0F, 7.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(223, 49).addBox(8.0F, -3.0F, -1.0F, 1.0F, 7.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 11.0F));

		PartDefinition Body_3 = Body_2_Seg.addOrReplaceChild("Body_3", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 1.0F));

		PartDefinition Body3 = Body_3.addOrReplaceChild("Body3", CubeListBuilder.create().texOffs(49, 0).addBox(-5.0F, -19.0F, -8.0F, 10.0F, 19.0F, 11.0F, new CubeDeformation(0.0F))
		.texOffs(113, 123).addBox(-8.0F, -18.0F, -8.0F, 3.0F, 17.0F, 11.0F, new CubeDeformation(0.0F))
		.texOffs(25, 182).addBox(9.0F, -13.0F, -8.0F, 1.0F, 7.0F, 11.0F, new CubeDeformation(0.0F))
		.texOffs(25, 155).addBox(8.0F, -17.0F, -8.0F, 1.0F, 15.0F, 11.0F, new CubeDeformation(0.0F))
		.texOffs(66, 124).addBox(5.0F, -18.0F, -8.0F, 3.0F, 17.0F, 11.0F, new CubeDeformation(0.0F))
		.texOffs(158, 49).addBox(-9.0F, -17.0F, -8.0F, 1.0F, 15.0F, 11.0F, new CubeDeformation(0.0F))
		.texOffs(183, 36).addBox(-10.0F, -13.0F, -8.0F, 1.0F, 7.0F, 11.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 10.0F, 8.0F));

		PartDefinition Body_3_Seg = Body_3.addOrReplaceChild("Body_3_Seg", CubeListBuilder.create().texOffs(160, 19).addBox(-7.0F, -6.0F, -1.0F, 14.0F, 13.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(198, 89).addBox(-5.0F, 7.0F, -1.0F, 10.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(135, 212).addBox(-4.0F, -7.0F, -1.0F, 8.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(223, 155).addBox(-8.0F, -3.0F, -1.0F, 1.0F, 7.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(225, 83).addBox(7.0F, -2.0F, -1.0F, 1.0F, 5.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 11.0F));

		PartDefinition Body_4 = Body_3_Seg.addOrReplaceChild("Body_4", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 1.0F));

		PartDefinition Body4 = Body_4.addOrReplaceChild("Body4", CubeListBuilder.create().texOffs(82, 65).addBox(-4.0F, -18.0F, -8.0F, 8.0F, 18.0F, 10.0F, new CubeDeformation(0.0F))
		.texOffs(142, 150).addBox(-7.0F, -17.0F, -8.0F, 3.0F, 15.0F, 10.0F, new CubeDeformation(0.0F))
		.texOffs(192, 171).addBox(8.0F, -13.0F, -8.0F, 1.0F, 7.0F, 10.0F, new CubeDeformation(0.0F))
		.texOffs(169, 171).addBox(7.0F, -16.0F, -8.0F, 1.0F, 13.0F, 10.0F, new CubeDeformation(0.0F))
		.texOffs(95, 152).addBox(4.0F, -17.0F, -8.0F, 3.0F, 15.0F, 10.0F, new CubeDeformation(0.0F))
		.texOffs(173, 97).addBox(-8.0F, -16.0F, -8.0F, 1.0F, 13.0F, 10.0F, new CubeDeformation(0.0F))
		.texOffs(192, 189).addBox(-9.0F, -13.0F, -8.0F, 1.0F, 7.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 10.0F, 8.0F));

		PartDefinition Body_4_Seg = Body_4.addOrReplaceChild("Body_4_Seg", CubeListBuilder.create().texOffs(162, 0).addBox(-6.0F, -5.0F, -1.0F, 12.0F, 11.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(45, 213).addBox(-4.0F, 6.0F, -1.0F, 8.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(140, 205).addBox(-4.0F, -8.0F, -1.0F, 8.0F, 3.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(196, 226).addBox(-7.0F, -2.0F, -1.0F, 1.0F, 5.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(187, 226).addBox(6.0F, -2.0F, -1.0F, 1.0F, 5.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 2.0F, 10.0F));

		PartDefinition Body_5 = Body_4_Seg.addOrReplaceChild("Body_5", CubeListBuilder.create(), PartPose.offset(0.0F, -1.0F, 1.0F));

		PartDefinition Body5 = Body_5.addOrReplaceChild("Body5", CubeListBuilder.create().texOffs(196, 113).addBox(-8.0F, -11.0F, 27.0F, 1.0F, 7.0F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(142, 80).addBox(-3.0F, -17.0F, 27.0F, 6.0F, 17.0F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(122, 176).addBox(-6.0F, -16.0F, 27.0F, 3.0F, 15.0F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(196, 97).addBox(7.0F, -11.0F, 27.0F, 1.0F, 7.0F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(168, 195).addBox(6.0F, -13.0F, 27.0F, 1.0F, 11.0F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(173, 121).addBox(3.0F, -16.0F, 27.0F, 3.0F, 15.0F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(122, 152).addBox(-7.0F, -13.0F, 27.0F, 1.0F, 11.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 9.0F, -27.0F));

		PartDefinition Body_5_Seg = Body_5.addOrReplaceChild("Body_5_Seg", CubeListBuilder.create().texOffs(193, 0).addBox(-5.0F, -4.0F, -1.0F, 10.0F, 10.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(95, 143).addBox(-2.0F, 6.0F, -1.0F, 4.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(215, 121).addBox(-3.0F, -6.0F, -1.0F, 6.0F, 2.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(224, 136).addBox(-6.0F, -1.0F, -1.0F, 1.0F, 3.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(54, 146).addBox(5.0F, -1.0F, -1.0F, 1.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 1.0F, 8.0F));

		PartDefinition Body_6 = Body_5_Seg.addOrReplaceChild("Body_6", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 1.0F));

		PartDefinition Body6 = Body_6.addOrReplaceChild("Body6", CubeListBuilder.create().texOffs(79, 153).addBox(-6.0F, -11.0F, 36.0F, 1.0F, 9.0F, 6.0F, new CubeDeformation(0.0F))
		.texOffs(158, 215).addBox(-7.0F, -9.0F, 36.0F, 1.0F, 5.0F, 6.0F, new CubeDeformation(0.0F))
		.texOffs(79, 178).addBox(-3.0F, -15.0F, 36.0F, 6.0F, 15.0F, 6.0F, new CubeDeformation(0.0F))
		.texOffs(104, 178).addBox(-5.0F, -13.0F, 36.0F, 2.0F, 12.0F, 6.0F, new CubeDeformation(0.0F))
		.texOffs(215, 109).addBox(6.0F, -9.0F, 36.0F, 1.0F, 5.0F, 6.0F, new CubeDeformation(0.0F))
		.texOffs(66, 97).addBox(5.0F, -11.0F, 36.0F, 1.0F, 9.0F, 6.0F, new CubeDeformation(0.0F))
		.texOffs(95, 124).addBox(3.0F, -13.0F, 36.0F, 2.0F, 12.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 8.0F, -36.0F));

		PartDefinition Body_6_Seg = Body_6.addOrReplaceChild("Body_6_Seg", CubeListBuilder.create().texOffs(0, 227).addBox(4.0F, -1.0F, -1.0F, 1.0F, 2.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(119, 80).addBox(-4.0F, -4.0F, -1.0F, 8.0F, 9.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(223, 60).addBox(-2.0F, 5.0F, -1.0F, 4.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(79, 169).addBox(-2.0F, -5.0F, -1.0F, 4.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(205, 226).addBox(-5.0F, -1.0F, -1.0F, 1.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 1.0F, 6.0F));

		PartDefinition Body_7 = Body_6_Seg.addOrReplaceChild("Body_7", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 1.0F));

		PartDefinition Body7 = Body_7.addOrReplaceChild("Body7", CubeListBuilder.create().texOffs(88, 200).addBox(3.0F, -11.0F, 43.0F, 2.0F, 10.0F, 5.0F, new CubeDeformation(0.0F))
		.texOffs(81, 216).addBox(-6.0F, -9.0F, 43.0F, 1.0F, 7.0F, 5.0F, new CubeDeformation(0.0F))
		.texOffs(183, 55).addBox(-3.0F, -13.0F, 43.0F, 6.0F, 13.0F, 5.0F, new CubeDeformation(0.0F))
		.texOffs(73, 200).addBox(-5.0F, -11.0F, 43.0F, 2.0F, 10.0F, 5.0F, new CubeDeformation(0.0F))
		.texOffs(68, 216).addBox(5.0F, -9.0F, 43.0F, 1.0F, 7.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 7.0F, -43.0F));

		PartDefinition Body_7_Seg = Body_7.addOrReplaceChild("Body_7_Seg", CubeListBuilder.create().texOffs(18, 227).addBox(-4.0F, -1.0F, -1.0F, 1.0F, 2.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(9, 227).addBox(3.0F, -1.0F, -1.0F, 1.0F, 2.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(121, 200).addBox(-3.0F, -4.0F, -1.0F, 6.0F, 8.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(223, 222).addBox(-2.0F, 4.0F, -1.0F, 4.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(95, 148).addBox(-2.0F, -4.0F, -1.0F, 4.0F, 0.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 1.0F, 5.0F));

		PartDefinition Body_8 = Body_7_Seg.addOrReplaceChild("Body_8", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 1.0F));

		PartDefinition Body8 = Body_8.addOrReplaceChild("Body8", CubeListBuilder.create().texOffs(146, 19).addBox(4.0F, -8.0F, 49.0F, 1.0F, 5.0F, 5.0F, new CubeDeformation(0.0F))
		.texOffs(215, 94).addBox(3.0F, -10.0F, 49.0F, 1.0F, 9.0F, 5.0F, new CubeDeformation(0.0F))
		.texOffs(144, 49).addBox(-5.0F, -8.0F, 49.0F, 1.0F, 5.0F, 5.0F, new CubeDeformation(0.0F))
		.texOffs(195, 14).addBox(-3.0F, -11.0F, 49.0F, 6.0F, 11.0F, 5.0F, new CubeDeformation(0.0F))
		.texOffs(214, 207).addBox(-4.0F, -10.0F, 49.0F, 1.0F, 9.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 6.0F, -49.0F));

		PartDefinition Body_8_Seg = Body_8.addOrReplaceChild("Body_8_Seg", CubeListBuilder.create().texOffs(215, 167).addBox(-2.0F, -4.0F, -1.0F, 4.0F, 8.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(54, 235).addBox(-3.0F, -2.0F, -1.0F, 1.0F, 4.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(235, 97).addBox(2.0F, -2.0F, -1.0F, 1.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 1.0F, 5.0F));

		PartDefinition Body_9 = Body_8_Seg.addOrReplaceChild("Body_9", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 1.0F));

		PartDefinition Body9 = Body_9.addOrReplaceChild("Body9", CubeListBuilder.create().texOffs(112, 217).addBox(-3.0F, -9.0F, 55.0F, 1.0F, 8.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(225, 74).addBox(3.0F, -7.0F, 55.0F, 1.0F, 4.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(217, 36).addBox(2.0F, -9.0F, 55.0F, 1.0F, 8.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(224, 127).addBox(-4.0F, -7.0F, 55.0F, 1.0F, 4.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(104, 197).addBox(-2.0F, -10.0F, 55.0F, 4.0F, 10.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 5.0F, -55.0F));

		PartDefinition Body_9_Seg = Body_9.addOrReplaceChild("Body_9_Seg", CubeListBuilder.create().texOffs(66, 113).addBox(-2.0F, -3.0F, -1.0F, 4.0F, 6.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 1.0F, 4.0F));

		PartDefinition Body_10 = Body_9_Seg.addOrReplaceChild("Body_10", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 1.0F));

		PartDefinition Body10 = Body_10.addOrReplaceChild("Body10", CubeListBuilder.create().texOffs(219, 144).addBox(-3.0F, -7.0F, 60.0F, 1.0F, 6.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(35, 85).addBox(2.0F, -7.0F, 60.0F, 1.0F, 6.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(206, 55).addBox(-2.0F, -8.0F, 60.0F, 4.0F, 8.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 4.0F, -60.0F));

		return LayerDefinition.create(meshdefinition, 256, 256);
	}

	@Override
	public void setupAnim(CthonianGnawlingRenderState state) {
		this.root.getAllParts().forEach(ModelPart::resetPose);
		applyRestingTailCurlPose();

		// Temporary crash-debug bypass: keep gnawling in a static, non-animated pose.
		this.Main_Bone.yRot = (float) Math.PI;
	}

	private void applyRestingTailCurlPose() {
		this.Head_Seg.yRot = 2.5F * net.minecraft.util.Mth.DEG_TO_RAD;
		this.Body_1.yRot = 2.5F * net.minecraft.util.Mth.DEG_TO_RAD;
		this.Body_1_Seg.yRot = 2.5F * net.minecraft.util.Mth.DEG_TO_RAD;
		this.Body_2.yRot = 2.5F * net.minecraft.util.Mth.DEG_TO_RAD;
		this.Body_2_Seg.yRot = 2.5F * net.minecraft.util.Mth.DEG_TO_RAD;
		this.Body_3.yRot = 2.5F * net.minecraft.util.Mth.DEG_TO_RAD;
		this.Body_3_Seg.yRot = 2.5F * net.minecraft.util.Mth.DEG_TO_RAD;
		this.Body_4.yRot = 2.5F * net.minecraft.util.Mth.DEG_TO_RAD;
		this.Body_4_Seg.yRot = 2.5F * net.minecraft.util.Mth.DEG_TO_RAD;

		this.Body_5.yRot = -2.5F * net.minecraft.util.Mth.DEG_TO_RAD;
		this.Body_5_Seg.yRot = -2.5F * net.minecraft.util.Mth.DEG_TO_RAD;
		this.Body_6.yRot = -2.5F * net.minecraft.util.Mth.DEG_TO_RAD;
		this.Body_6_Seg.yRot = -2.5F * net.minecraft.util.Mth.DEG_TO_RAD;
		this.Body_7.yRot = -2.5F * net.minecraft.util.Mth.DEG_TO_RAD;
		this.Body_7_Seg.yRot = -2.5F * net.minecraft.util.Mth.DEG_TO_RAD;
		this.Body_8.yRot = -2.5F * net.minecraft.util.Mth.DEG_TO_RAD;
		this.Body_8_Seg.yRot = -2.5F * net.minecraft.util.Mth.DEG_TO_RAD;
		this.Body_9.yRot = -2.5F * net.minecraft.util.Mth.DEG_TO_RAD;
		this.Body_9_Seg.yRot = -2.5F * net.minecraft.util.Mth.DEG_TO_RAD;
		this.Body_10.yRot = -2.5F * net.minecraft.util.Mth.DEG_TO_RAD;
	}
}
