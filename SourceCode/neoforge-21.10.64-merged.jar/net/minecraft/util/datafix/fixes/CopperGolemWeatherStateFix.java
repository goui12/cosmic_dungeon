package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class CopperGolemWeatherStateFix extends NamedEntityFix {
    public CopperGolemWeatherStateFix(Schema outputSchema) {
        super(outputSchema, false, "CopperGolemWeatherStateFix", References.ENTITY, "minecraft:copper_golem");
    }

    @Override
    protected Typed<?> fix(Typed<?> typed) {
        return typed.update(DSL.remainderFinder(), p_436673_ -> p_436673_.update("weather_state", CopperGolemWeatherStateFix::fixWeatherState));
    }

    private static Dynamic<?> fixWeatherState(Dynamic<?> data) {
        return switch (data.asInt(0)) {
            case 1 -> data.createString("exposed");
            case 2 -> data.createString("weathered");
            case 3 -> data.createString("oxidized");
            default -> data.createString("unaffected");
        };
    }
}
