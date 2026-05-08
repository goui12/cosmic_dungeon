package net.minecraft.world.item.consume_effects;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public record ApplyStatusEffectsConsumeEffect(List<MobEffectInstance> effects, float probability) implements ConsumeEffect {
    public static final MapCodec<ApplyStatusEffectsConsumeEffect> CODEC = RecordCodecBuilder.mapCodec(
        p_366712_ -> p_366712_.group(
                MobEffectInstance.CODEC.listOf().fieldOf("effects").forGetter(ApplyStatusEffectsConsumeEffect::effects),
                Codec.floatRange(0.0F, 1.0F).optionalFieldOf("probability", 1.0F).forGetter(ApplyStatusEffectsConsumeEffect::probability)
            )
            .apply(p_366712_, ApplyStatusEffectsConsumeEffect::new)
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ApplyStatusEffectsConsumeEffect> STREAM_CODEC = StreamCodec.composite(
        MobEffectInstance.STREAM_CODEC.apply(ByteBufCodecs.list()),
        ApplyStatusEffectsConsumeEffect::effects,
        ByteBufCodecs.FLOAT,
        ApplyStatusEffectsConsumeEffect::probability,
        ApplyStatusEffectsConsumeEffect::new
    );

    public ApplyStatusEffectsConsumeEffect(MobEffectInstance p_366514_, float p_366567_) {
        this(List.of(p_366514_), p_366567_);
    }

    public ApplyStatusEffectsConsumeEffect(List<MobEffectInstance> p_366447_) {
        this(p_366447_, 1.0F);
    }

    public ApplyStatusEffectsConsumeEffect(MobEffectInstance p_366678_) {
        this(p_366678_, 1.0F);
    }

    @Override
    public ConsumeEffect.Type<ApplyStatusEffectsConsumeEffect> getType() {
        return ConsumeEffect.Type.APPLY_EFFECTS;
    }

    @Override
    public boolean apply(Level level, ItemStack stack, LivingEntity entity) {
        if (entity.getRandom().nextFloat() >= this.probability) {
            return false;
        } else {
            boolean flag = false;

            for (MobEffectInstance mobeffectinstance : this.effects) {
                if (entity.addEffect(new MobEffectInstance(mobeffectinstance))) {
                    flag = true;
                }
            }

            return flag;
        }
    }
}
