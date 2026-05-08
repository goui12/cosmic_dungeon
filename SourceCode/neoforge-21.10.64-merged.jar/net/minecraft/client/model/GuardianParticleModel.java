package net.minecraft.client.model;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Unit;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GuardianParticleModel extends Model<Unit> {
    public GuardianParticleModel(ModelPart root) {
        super(root, RenderType::entityCutoutNoCull);
    }
}
