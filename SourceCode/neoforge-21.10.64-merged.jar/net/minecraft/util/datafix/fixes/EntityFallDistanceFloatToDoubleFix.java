package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class EntityFallDistanceFloatToDoubleFix extends DataFix {
    private TypeReference type;

    public EntityFallDistanceFloatToDoubleFix(Schema outputSchema, TypeReference type) {
        super(outputSchema, false);
        this.type = type;
    }

    @Override
    protected TypeRewriteRule makeRule() {
        return this.fixTypeEverywhereTyped(
            "EntityFallDistanceFloatToDoubleFixFor" + this.type.typeName(),
            this.getOutputSchema().getType(this.type),
            EntityFallDistanceFloatToDoubleFix::fixEntity
        );
    }

    private static Typed<?> fixEntity(Typed<?> data) {
        return data.update(
            DSL.remainderFinder(),
            p_397901_ -> p_397901_.renameAndFixField("FallDistance", "fall_distance", p_398000_ -> p_398000_.createDouble(p_398000_.asFloat(0.0F)))
        );
    }
}
