package net.minecraft.client.model;

import java.util.Set;
import java.util.Map.Entry;
import java.util.function.UnaryOperator;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.MeshTransformer;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record BabyModelTransform(
    boolean scaleHead, float babyYHeadOffset, float babyZHeadOffset, float babyHeadScale, float babyBodyScale, float bodyYOffset, Set<String> headParts
) implements MeshTransformer {
    public BabyModelTransform(Set<String> p_363672_) {
        this(false, 5.0F, 2.0F, p_363672_);
    }

    public BabyModelTransform(boolean p_363243_, float p_361705_, float p_364325_, Set<String> p_360719_) {
        this(p_363243_, p_361705_, p_364325_, 2.0F, 2.0F, 24.0F, p_360719_);
    }

    @Override
    public MeshDefinition apply(MeshDefinition mesh) {
        float f = this.scaleHead ? 1.5F / this.babyHeadScale : 1.0F;
        float f1 = 1.0F / this.babyBodyScale;
        UnaryOperator<PartPose> unaryoperator = p_363725_ -> p_363725_.translated(0.0F, this.babyYHeadOffset, this.babyZHeadOffset).scaled(f);
        UnaryOperator<PartPose> unaryoperator1 = p_362082_ -> p_362082_.translated(0.0F, this.bodyYOffset, 0.0F).scaled(f1);
        MeshDefinition meshdefinition = new MeshDefinition();

        for (Entry<String, PartDefinition> entry : mesh.getRoot().getChildren()) {
            String s = entry.getKey();
            PartDefinition partdefinition = entry.getValue();
            meshdefinition.getRoot().addOrReplaceChild(s, partdefinition.transformed(this.headParts.contains(s) ? unaryoperator : unaryoperator1));
        }

        return meshdefinition;
    }
}
