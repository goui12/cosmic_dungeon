package net.minecraft.client.particle;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.state.ParticleGroupRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ItemPickupParticleGroup extends ParticleGroup<ItemPickupParticle> {
    public ItemPickupParticleGroup(ParticleEngine engine) {
        super(engine);
    }

    @Override
    public ParticleGroupRenderState extractRenderState(Frustum frustum, Camera camera, float partialTick) {
        return new ItemPickupParticleGroup.State(
            this.particles.stream().map(p_445748_ -> ItemPickupParticleGroup.ParticleInstance.fromParticle(p_445748_, camera, partialTick)).toList()
        );
    }

    @OnlyIn(Dist.CLIENT)
    record ParticleInstance(EntityRenderState itemRenderState, double xOffset, double yOffset, double zOffset) {
        public static ItemPickupParticleGroup.ParticleInstance fromParticle(ItemPickupParticle particle, Camera camera, float partialTick) {
            float f = (particle.life + partialTick) / 3.0F;
            f *= f;
            double d0 = Mth.lerp((double)partialTick, particle.targetXOld, particle.targetX);
            double d1 = Mth.lerp((double)partialTick, particle.targetYOld, particle.targetY);
            double d2 = Mth.lerp((double)partialTick, particle.targetZOld, particle.targetZ);
            double d3 = Mth.lerp((double)f, particle.itemRenderState.x, d0);
            double d4 = Mth.lerp((double)f, particle.itemRenderState.y, d1);
            double d5 = Mth.lerp((double)f, particle.itemRenderState.z, d2);
            Vec3 vec3 = camera.getPosition();
            return new ItemPickupParticleGroup.ParticleInstance(particle.itemRenderState, d3 - vec3.x(), d4 - vec3.y(), d5 - vec3.z());
        }
    }

    @OnlyIn(Dist.CLIENT)
    record State(List<ItemPickupParticleGroup.ParticleInstance> instances) implements ParticleGroupRenderState {
        @Override
        public void submit(SubmitNodeCollector p_446688_, CameraRenderState p_451251_) {
            PoseStack posestack = new PoseStack();
            EntityRenderDispatcher entityrenderdispatcher = Minecraft.getInstance().getEntityRenderDispatcher();

            for (ItemPickupParticleGroup.ParticleInstance itempickupparticlegroup$particleinstance : this.instances) {
                entityrenderdispatcher.submit(
                    itempickupparticlegroup$particleinstance.itemRenderState,
                    p_451251_,
                    itempickupparticlegroup$particleinstance.xOffset,
                    itempickupparticlegroup$particleinstance.yOffset,
                    itempickupparticlegroup$particleinstance.zOffset,
                    posestack,
                    p_446688_
                );
            }
        }
    }
}
