package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import java.util.stream.Stream;

public class FoodToConsumableFix extends DataFix {
    public FoodToConsumableFix(Schema outputSchema) {
        super(outputSchema, true);
    }

    @Override
    protected TypeRewriteRule makeRule() {
        return this.writeFixAndRead(
            "Food to consumable fix",
            this.getInputSchema().getType(References.DATA_COMPONENTS),
            this.getOutputSchema().getType(References.DATA_COMPONENTS),
            p_366535_ -> {
                Optional<? extends Dynamic<?>> optional = p_366535_.get("minecraft:food").result();
                if (optional.isPresent()) {
                    float f = optional.get().get("eat_seconds").asFloat(1.6F);
                    Stream<? extends Dynamic<?>> stream = optional.get().get("effects").asStream();
                    Stream<? extends Dynamic<?>> stream1 = stream.map(
                        p_366886_ -> p_366886_.emptyMap()
                            .set("type", p_366886_.createString("minecraft:apply_effects"))
                            .set("effects", p_366886_.createList(p_366886_.get("effect").result().stream()))
                            .set("probability", p_366886_.createFloat(p_366886_.get("probability").asFloat(1.0F)))
                    );
                    p_366535_ = Dynamic.copyField((Dynamic<?>)optional.get(), "using_converts_to", p_366535_, "minecraft:use_remainder");
                    p_366535_ = p_366535_.set("minecraft:food", optional.get().remove("eat_seconds").remove("effects").remove("using_converts_to"));
                    return p_366535_.set(
                        "minecraft:consumable",
                        p_366535_.emptyMap().set("consume_seconds", p_366535_.createFloat(f)).set("on_consume_effects", p_366535_.createList(stream1))
                    );
                } else {
                    return p_366535_;
                }
            }
        );
    }
}
