package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class AreaEffectCloudDurationScaleFix extends NamedEntityFix {
    public AreaEffectCloudDurationScaleFix(Schema outputSchema) {
        super(outputSchema, false, "AreaEffectCloudDurationScaleFix", References.ENTITY, "minecraft:area_effect_cloud");
    }

    @Override
    protected Typed<?> fix(Typed<?> typed) {
        return typed.update(DSL.remainderFinder(), p_394341_ -> p_394341_.set("potion_duration_scale", p_394341_.createFloat(0.25F)));
    }
}
