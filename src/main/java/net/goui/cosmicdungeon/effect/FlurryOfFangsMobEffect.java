package net.goui.cosmicdungeon.effect;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.ai.attributes.*;

/** Melee recharge only, without the mining-speed side effect of native Haste. */
public final class FlurryOfFangsMobEffect extends MobEffect {
    public FlurryOfFangsMobEffect(){
        super(MobEffectCategory.BENEFICIAL,0xE4AF39);
        addAttributeModifier(Attributes.ATTACK_SPEED,ResourceLocation.fromNamespaceAndPath("cosmicdungeon","flurry_of_fangs"),
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,amplifier->.1);
    }
}
