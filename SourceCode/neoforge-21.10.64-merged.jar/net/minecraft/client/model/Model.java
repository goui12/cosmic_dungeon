package net.minecraft.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import java.util.function.Function;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Unit;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class Model<S> {
    protected final ModelPart root;
    protected final Function<ResourceLocation, RenderType> renderType;
    private final List<ModelPart> allParts;

    public Model(ModelPart root, Function<ResourceLocation, RenderType> renderType) {
        this.root = root;
        this.renderType = renderType;
        this.allParts = root.getAllParts();
    }

    protected static net.neoforged.neoforge.client.entity.animation.json.AnimationHolder getAnimation(ResourceLocation key) {
        return net.neoforged.neoforge.client.entity.animation.json.AnimationLoader.INSTANCE.getAnimationHolder(key);
    }

    public final RenderType renderType(ResourceLocation location) {
        return this.renderType.apply(location);
    }

    public final void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        this.root().render(poseStack, buffer, packedLight, packedOverlay, color);
    }

    public final void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay) {
        this.renderToBuffer(poseStack, buffer, packedLight, packedOverlay, -1);
    }

    public final ModelPart root() {
        return this.root;
    }

    public final List<ModelPart> allParts() {
        return this.allParts;
    }

    public void setupAnim(S renderState) {
        this.resetPose();
    }

    public final void resetPose() {
        for (ModelPart modelpart : this.allParts) {
            modelpart.resetPose();
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class Simple extends Model<Unit> {
        public Simple(ModelPart root, Function<ResourceLocation, RenderType> renderType) {
            super(root, renderType);
        }

        public void setupAnim(Unit renderState) {
        }
    }
}
