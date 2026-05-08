package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import java.util.Map;
import java.util.stream.Stream;

public class CustomModelDataExpandFix extends DataFix {
    public CustomModelDataExpandFix(Schema outputSchema) {
        super(outputSchema, false);
    }

    @Override
    protected TypeRewriteRule makeRule() {
        Type<?> type = this.getInputSchema().getType(References.DATA_COMPONENTS);
        return this.fixTypeEverywhereTyped(
            "Custom Model Data expansion",
            type,
            p_388759_ -> p_388759_.update(DSL.remainderFinder(), p_387670_ -> p_387670_.update("minecraft:custom_model_data", p_386554_ -> {
                float f = p_386554_.asNumber(0.0F).floatValue();
                return p_386554_.createMap(Map.of(p_386554_.createString("floats"), p_386554_.createList(Stream.of(p_386554_.createFloat(f)))));
            }))
        );
    }
}
