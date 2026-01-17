// file: src/main/java/net/goui/cosmicdungeon/client/model/ClassLockedChestModel.java
package net.goui.cosmicdungeon.client.model;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

/**
 * 1:1 Blockbench export (entity-model space).
 * Parts:
 *  - base
 *  - lid
 *  - knob (child of lid)
 *
 * Texture: 64x64
 */
public final class ClassLockedChestModel {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(
                    ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, "class_chest_converted"),
                    "main"
            );

    public final ModelPart base;
    public final ModelPart lid;
    public final ModelPart knob;

    public ClassLockedChestModel(ModelPart root) {
        this.base = root.getChild("base");
        this.lid = root.getChild("lid");
        this.knob = this.lid.getChild("knob");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition base = partdefinition.addOrReplaceChild("base", CubeListBuilder.create(), PartPose.offset(-8.0F, 24.0F, 8.0F));

        base.addOrReplaceChild("base_r1",
                CubeListBuilder.create().texOffs(0, 19)
                        .addBox(-7.0F, -4.0F, -7.0F, 14.0F, 10.0F, 14.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(8.0F, -4.0F, -8.0F, 3.1416F, 0.0F, 0.0F));

        PartDefinition lid = partdefinition.addOrReplaceChild("lid", CubeListBuilder.create(), PartPose.offset(0.0F, 15.0F, 7.0F));

        lid.addOrReplaceChild("lid_r1",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-7.0F, 0.0F, -7.0F, 14.0F, 5.0F, 14.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, -7.0F, 3.1416F, 0.0F, 0.0F));

        PartDefinition knob = lid.addOrReplaceChild("knob", CubeListBuilder.create(), PartPose.offset(0.0F, -1.0F, -14.0F));

        knob.addOrReplaceChild("knob_r1",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-1.0F, -11.0F, 7.0F, 2.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 9.0F, 7.0F, 0.0F, 3.1416F, 0.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    /** Progress 0..1. Applies your Blockbench curve elsewhere; this only sets rotation. */
    public void setLidXRotRadians(float xRotRadians) {
        this.lid.xRot = xRotRadians;
        // knob is child of lid, follows automatically
    }
}
