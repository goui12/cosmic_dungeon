package net.goui.cosmicdungeon.client.model;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.client.renderstate.MagmaGlobRenderState;
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

public class Magma_Glob extends EntityModel<MagmaGlobRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, "magma_glob"),
            "main"
    );

    private final ModelPart root;
    private final ModelPart main;
    private final ModelPart eyes;
    private final ModelPart eye1;
    private final ModelPart eye2;
    private final ModelPart body;
    private final ModelPart base;

    // Attack clip timing (manual latch)
    private boolean attackActive = false;
    private float attackStartAge = 0.0F; // ticks

    public Magma_Glob(ModelPart root) {
        super(root);
        this.root = root;
        this.main = root.getChild("main");
        this.eyes = this.main.getChild("eyes");
        this.eye1 = this.eyes.getChild("eye1");
        this.eye2 = this.eyes.getChild("eye2");
        this.body = this.main.getChild("body");
        this.base = this.main.getChild("base");
    }

    // === Geometry with corrected pivots (matches your latest export) ===
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition main = root.addOrReplaceChild("main",
                CubeListBuilder.create(),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );

        // Eyes parent carries forward offset; children carry ±X lateral offsets
        PartDefinition eyes = main.addOrReplaceChild("eyes",
                CubeListBuilder.create(),
                PartPose.offset(0.0F, 8.0F, -7.0F)
        );

        eyes.addOrReplaceChild("eye1",
                CubeListBuilder.create()
                        .texOffs(12, 28).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-2.0F, 0.0F, 0.0F)
        );

        eyes.addOrReplaceChild("eye2",
                CubeListBuilder.create()
                        .texOffs(20, 28).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(2.0F, 0.0F, 0.0F)
        );

        main.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .texOffs(18, 6).addBox(5.0F, 5.0F, -1.0F, 1.0F, 3.0F, 6.0F)
                        .texOffs(16, 4).addBox(4.0F, 4.0F, -2.0F, 1.0F, 5.0F, 8.0F)
                        .texOffs(14, 2).addBox(3.0F, 3.0F, -3.0F, 1.0F, 8.0F, 10.0F)
                        .texOffs(25, 2).addBox(-2.0F, 1.0F, -7.0F, 4.0F, 1.0F, 3.0F)
                        .texOffs(13, 1).addBox(-3.0F, 1.0F, -4.0F, 6.0F, 1.0F, 11.0F)
                        .texOffs(24, 5).addBox(-3.0F, 2.0F, -4.0F, 6.0F, 8.0F, 12.0F)
                        .texOffs(14, 2).addBox(-3.0F, 10.0F, -3.0F, 6.0F, 1.0F, 10.0F)
                        .texOffs(16, 4).addBox(-3.0F, 11.0F, -2.0F, 6.0F, 1.0F, 8.0F)
                        .texOffs(19, 6).addBox(-2.0F, 12.0F, -1.0F, 4.0F, 1.0F, 6.0F)
                        .texOffs(14, 2).addBox(-4.0F, 2.0F, -3.0F, 1.0F, 9.0F, 10.0F)
                        .texOffs(16, 4).addBox(-5.0F, 4.0F, -2.0F, 1.0F, 5.0F, 8.0F)
                        .texOffs(18, 6).addBox(-6.0F, 5.0F, -1.0F, 1.0F, 3.0F, 6.0F)
                        .texOffs(44, 30).addBox(2.0F, 2.0F, -8.0F, 1.0F, 6.0F, 2.0F)
                        .texOffs(42, 28).addBox(1.0F, 2.0F, -8.0F, 1.0F, 3.0F, 4.0F)
                        .texOffs(44, 30).addBox(1.0F, 5.0F, -8.0F, 1.0F, 3.0F, 2.0F)
                        .texOffs(26, 28).addBox(-1.0F, 2.0F, -8.0F, 2.0F, 3.0F, 4.0F)
                        .texOffs(28, 30).addBox(-1.0F, 5.0F, -8.0F, 2.0F, 3.0F, 2.0F)
                        .texOffs(34, 28).addBox(-2.0F, 2.0F, -8.0F, 1.0F, 3.0F, 4.0F)
                        .texOffs(36, 30).addBox(-2.0F, 5.0F, -8.0F, 1.0F, 0.0F, 2.0F)
                        .texOffs(36, 30).addBox(-2.0F, 5.0F, -8.0F, 1.0F, 3.0F, 2.0F)
                        .texOffs(36, 30).addBox(-3.0F, 2.0F, -8.0F, 1.0F, 6.0F, 2.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );

        main.addOrReplaceChild("base",
                CubeListBuilder.create()
                        .texOffs(14, 2).addBox(-3.0F, 0.0F, -7.0F, 6.0F, 1.0F, 13.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(MagmaGlobRenderState state) {
        // Reset to authored pose (also resets per-part scales to 1,1,1)
        this.root.getAllParts().forEach(ModelPart::resetPose);

        // Face the same way the entity is moving (fix "walking backward")
        this.main.yRot = Mth.PI; // 180 degrees

        // === WALK (Blockbench: 1.0 s looping) ===
        if (state.walkAnimation != null && state.walkAnimation.isStarted()) {
            // 20 ticks ≈ 1 second
            float tSec = (state.ageInTicks / 20.0F) % 1.0F; // 0..1
            float baseZ = (tSec <= 0.5F)
                    ? Mth.lerp(tSec / 0.5F, 1.0F, 0.7F)
                    : Mth.lerp((tSec - 0.5F) / 0.5F, 0.7F, 1.0F);

            // Apply SCALE Z to base (matches export)
            this.base.xScale = 1.0F;
            this.base.yScale = 1.0F;
            this.base.zScale = baseZ;
        }

        // === ATTACK (Blockbench: 1.0 s one-shot) ===
        // Restart the clip on EVERY pulse from the entity (even if already playing)
        boolean pulse = state.attackAnimation != null && state.attackAnimation.isStarted();
        if (pulse) {
            attackActive = true;
            attackStartAge = state.ageInTicks; // restart from now
        }

        if (attackActive) {
            float atSec = (state.ageInTicks - attackStartAge) / 20.0F; // seconds since (re)start
            if (atSec < 0.0F) atSec = 0.0F;

            // body.xScale: 0→0.25: 1→2 ; 0.25→0.5: 2→1 ; else 1
            float bodyX;
            if (atSec <= 0.25F) {
                bodyX = Mth.lerp(atSec / 0.25F, 1.0F, 2.0F);
            } else if (atSec <= 0.5F) {
                bodyX = Mth.lerp((atSec - 0.25F) / 0.25F, 2.0F, 1.0F);
            } else {
                bodyX = 1.0F;
            }
            this.body.xScale = bodyX;
            this.body.yScale = 1.0F;
            this.body.zScale = 1.0F;

            // Eyes: eye1 peaks 130° at 0.25s; eye2 peaks 130° at 0.5s
            float d = -Mth.DEG_TO_RAD * 130.0F;

            // eye1: 0→0.25: 0→130 ; 0.25→0.5: 130→0 ; else 0
            if (atSec <= 0.25F) {
                this.eye1.xRot = Mth.lerp(atSec / 0.25F, 0.0F, d);
            } else if (atSec <= 0.5F) {
                this.eye1.xRot = Mth.lerp((atSec - 0.25F) / 0.25F, d, 0.0F);
            } else {
                this.eye1.xRot = 0.0F;
            }

            // eye2: 0→0.5: 0 ; 0.5→0.75: 0→130 ; 0.75→1.0: 130→0
            if (atSec <= 0.5F) {
                this.eye2.xRot = 0.0F;
            } else if (atSec <= 0.75F) {
                this.eye2.xRot = Mth.lerp((atSec - 0.5F) / 0.25F, 0.0F, d);
            } else if (atSec <= 1.0F) {
                this.eye2.xRot = Mth.lerp((atSec - 0.75F) / 0.25F, d, 0.0F);
            } else {
                // clip finished
                this.eye2.xRot = 0.0F;
                attackActive = false;
            }
        }
    }
}
