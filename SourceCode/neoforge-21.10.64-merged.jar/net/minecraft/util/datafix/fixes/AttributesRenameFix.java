package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import net.minecraft.util.datafix.ExtraDataFixUtils;

public class AttributesRenameFix extends DataFix {
    private final String name;
    private final UnaryOperator<String> renames;

    public AttributesRenameFix(Schema outputSchema, String name, UnaryOperator<String> renames) {
        super(outputSchema, false);
        this.name = name;
        this.renames = renames;
    }

    @Override
    protected TypeRewriteRule makeRule() {
        return TypeRewriteRule.seq(
            this.fixTypeEverywhereTyped(this.name + " (Components)", this.getInputSchema().getType(References.DATA_COMPONENTS), this::fixDataComponents),
            this.fixTypeEverywhereTyped(this.name + " (Entity)", this.getInputSchema().getType(References.ENTITY), this::fixEntity),
            this.fixTypeEverywhereTyped(this.name + " (Player)", this.getInputSchema().getType(References.PLAYER), this::fixEntity)
        );
    }

    private Typed<?> fixDataComponents(Typed<?> dataComponents) {
        return dataComponents.update(
            DSL.remainderFinder(),
            p_365366_ -> p_365366_.update(
                "minecraft:attribute_modifiers",
                p_364961_ -> p_364961_.update(
                    "modifiers",
                    p_360482_ -> DataFixUtils.orElse(
                        p_360482_.asStreamOpt().result().map(p_364944_ -> p_364944_.map(this::fixTypeField)).map(p_360482_::createList), p_360482_
                    )
                )
            )
        );
    }

    private Typed<?> fixEntity(Typed<?> data) {
        return data.update(
            DSL.remainderFinder(),
            p_363349_ -> p_363349_.update(
                "attributes",
                p_365176_ -> DataFixUtils.orElse(
                    p_365176_.asStreamOpt().result().map(p_360801_ -> p_360801_.map(this::fixIdField)).map(p_365176_::createList), p_365176_
                )
            )
        );
    }

    private Dynamic<?> fixIdField(Dynamic<?> data) {
        return ExtraDataFixUtils.fixStringField(data, "id", this.renames);
    }

    private Dynamic<?> fixTypeField(Dynamic<?> data) {
        return ExtraDataFixUtils.fixStringField(data, "type", this.renames);
    }
}
