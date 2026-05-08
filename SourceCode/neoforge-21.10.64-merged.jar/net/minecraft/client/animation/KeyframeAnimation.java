package net.minecraft.client.animation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.AnimationState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class KeyframeAnimation {
    private final AnimationDefinition definition;
    private final List<KeyframeAnimation.Entry> entries;

    private KeyframeAnimation(AnimationDefinition definition, List<KeyframeAnimation.Entry> entries) {
        this.definition = definition;
        this.entries = entries;
    }

    static KeyframeAnimation bake(ModelPart root, AnimationDefinition definition) {
        List<KeyframeAnimation.Entry> list = new ArrayList<>();
        Function<String, ModelPart> function = root.createPartLookup();

        for (Map.Entry<String, List<AnimationChannel>> entry : definition.boneAnimations().entrySet()) {
            String s = entry.getKey();
            List<AnimationChannel> list1 = entry.getValue();
            ModelPart modelpart = function.apply(s);
            if (modelpart == null) {
                throw new IllegalArgumentException("Cannot animate " + s + ", which does not exist in model");
            }

            for (AnimationChannel animationchannel : list1) {
                list.add(new KeyframeAnimation.Entry(modelpart, animationchannel.target(), animationchannel.keyframes()));
            }
        }

        return new KeyframeAnimation(definition, List.copyOf(list));
    }

    public void applyStatic() {
        this.apply(0L, 1.0F);
    }

    public void applyWalk(float walkAnimationPos, float walkAnimationSpeed, float timeMultiplier, float speedMultiplier) {
        long i = (long)(walkAnimationPos * 50.0F * timeMultiplier);
        float f = Math.min(walkAnimationSpeed * speedMultiplier, 1.0F);
        this.apply(i, f);
    }

    public void apply(AnimationState animationState, float ageInTicks) {
        this.apply(animationState, ageInTicks, 1.0F);
    }

    public void apply(AnimationState animationState, float ageInTicks, float speedMultiplier) {
        animationState.ifStarted(p_427385_ -> this.apply((long)((float)p_427385_.getTimeInMillis(ageInTicks) * speedMultiplier), 1.0F));
    }

    public void apply(long timeInMillis, float scale) {
        float f = this.getElapsedSeconds(timeInMillis);
        Vector3f vector3f = new Vector3f();

        for (KeyframeAnimation.Entry keyframeanimation$entry : this.entries) {
            keyframeanimation$entry.apply(f, scale, vector3f);
        }
    }

    private float getElapsedSeconds(long timeInMillis) {
        float f = (float)timeInMillis / 1000.0F;
        return this.definition.looping() ? f % this.definition.lengthInSeconds() : f;
    }

    @OnlyIn(Dist.CLIENT)
    record Entry(ModelPart part, AnimationChannel.Target target, Keyframe[] keyframes) {
        public void apply(float elapsedSeconds, float scale, Vector3f scratchVector) {
            int i = Math.max(0, Mth.binarySearch(0, this.keyframes.length, p_427328_ -> elapsedSeconds <= this.keyframes[p_427328_].timestamp()) - 1);
            int j = Math.min(this.keyframes.length - 1, i + 1);
            Keyframe keyframe = this.keyframes[i];
            Keyframe keyframe1 = this.keyframes[j];
            float f = elapsedSeconds - keyframe.timestamp();
            float f1;
            if (j != i) {
                f1 = Mth.clamp(f / (keyframe1.timestamp() - keyframe.timestamp()), 0.0F, 1.0F);
            } else {
                f1 = 0.0F;
            }

            keyframe1.interpolation().apply(scratchVector, f1, this.keyframes, i, j, scale);
            this.target.apply(this.part, scratchVector);
        }
    }
}
