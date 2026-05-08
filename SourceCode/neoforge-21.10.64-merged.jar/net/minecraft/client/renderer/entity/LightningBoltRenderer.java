package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.LightningBoltRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LightningBolt;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class LightningBoltRenderer extends EntityRenderer<LightningBolt, LightningBoltRenderState> {
    public LightningBoltRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public void submit(LightningBoltRenderState renderState, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState) {
        float[] afloat = new float[8];
        float[] afloat1 = new float[8];
        float f = 0.0F;
        float f1 = 0.0F;
        RandomSource randomsource = RandomSource.create(renderState.seed);

        for (int i = 7; i >= 0; i--) {
            afloat[i] = f;
            afloat1[i] = f1;
            f += randomsource.nextInt(11) - 5;
            f1 += randomsource.nextInt(11) - 5;
        }

        float f2 = f;
        float f3 = f1;
        nodeCollector.submitCustomGeometry(poseStack, RenderType.lightning(), (p_434636_, p_432984_) -> {
            Matrix4f matrix4f = p_434636_.pose();

            for (int j = 0; j < 4; j++) {
                RandomSource randomsource1 = RandomSource.create(renderState.seed);

                for (int k = 0; k < 3; k++) {
                    int l = 7;
                    int i1 = 0;
                    if (k > 0) {
                        l = 7 - k;
                    }

                    if (k > 0) {
                        i1 = l - 2;
                    }

                    float f4 = afloat[l] - f2;
                    float f5 = afloat1[l] - f3;

                    for (int j1 = l; j1 >= i1; j1--) {
                        float f6 = f4;
                        float f7 = f5;
                        if (k == 0) {
                            f4 += randomsource1.nextInt(11) - 5;
                            f5 += randomsource1.nextInt(11) - 5;
                        } else {
                            f4 += randomsource1.nextInt(31) - 15;
                            f5 += randomsource1.nextInt(31) - 15;
                        }

                        float f8 = 0.5F;
                        float f9 = 0.45F;
                        float f10 = 0.45F;
                        float f11 = 0.5F;
                        float f12 = 0.1F + j * 0.2F;
                        if (k == 0) {
                            f12 *= j1 * 0.1F + 1.0F;
                        }

                        float f13 = 0.1F + j * 0.2F;
                        if (k == 0) {
                            f13 *= (j1 - 1.0F) * 0.1F + 1.0F;
                        }

                        quad(matrix4f, p_432984_, f4, f5, j1, f6, f7, 0.45F, 0.45F, 0.5F, f12, f13, false, false, true, false);
                        quad(matrix4f, p_432984_, f4, f5, j1, f6, f7, 0.45F, 0.45F, 0.5F, f12, f13, true, false, true, true);
                        quad(matrix4f, p_432984_, f4, f5, j1, f6, f7, 0.45F, 0.45F, 0.5F, f12, f13, true, true, false, true);
                        quad(matrix4f, p_432984_, f4, f5, j1, f6, f7, 0.45F, 0.45F, 0.5F, f12, f13, false, true, false, false);
                    }
                }
            }
        });
    }

    private static void quad(
        Matrix4f pose,
        VertexConsumer buffer,
        float x1,
        float z1,
        int sectionY,
        float x2,
        float z2,
        float red,
        float green,
        float blue,
        float innerThickness,
        float outerThickness,
        boolean addThicknessLeftSideX,
        boolean addThicknessLeftSideZ,
        boolean addThicknessRightSideX,
        boolean addThicknessRightSideZ
    ) {
        buffer.addVertex(
                pose, x1 + (addThicknessLeftSideX ? outerThickness : -outerThickness), (float)(sectionY * 16), z1 + (addThicknessLeftSideZ ? outerThickness : -outerThickness)
            )
            .setColor(red, green, blue, 0.3F);
        buffer.addVertex(
                pose, x2 + (addThicknessLeftSideX ? innerThickness : -innerThickness), (float)((sectionY + 1) * 16), z2 + (addThicknessLeftSideZ ? innerThickness : -innerThickness)
            )
            .setColor(red, green, blue, 0.3F);
        buffer.addVertex(
                pose, x2 + (addThicknessRightSideX ? innerThickness : -innerThickness), (float)((sectionY + 1) * 16), z2 + (addThicknessRightSideZ ? innerThickness : -innerThickness)
            )
            .setColor(red, green, blue, 0.3F);
        buffer.addVertex(
                pose, x1 + (addThicknessRightSideX ? outerThickness : -outerThickness), (float)(sectionY * 16), z1 + (addThicknessRightSideZ ? outerThickness : -outerThickness)
            )
            .setColor(red, green, blue, 0.3F);
    }

    public LightningBoltRenderState createRenderState() {
        return new LightningBoltRenderState();
    }

    public void extractRenderState(LightningBolt entity, LightningBoltRenderState reusedState, float partialTick) {
        super.extractRenderState(entity, reusedState, partialTick);
        reusedState.seed = entity.seed;
    }

    protected boolean affectedByCulling(LightningBolt display) {
        return false;
    }
}
