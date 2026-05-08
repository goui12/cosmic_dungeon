package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.function.DoubleUnaryOperator;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class EntityAttributeBaseValueFix extends NamedEntityFix {
    private final String attributeId;
    private final DoubleUnaryOperator valueFixer;

    public EntityAttributeBaseValueFix(Schema outputSchema, String name, String entityName, String attributeId, DoubleUnaryOperator valueFixer) {
        super(outputSchema, false, name, References.ENTITY, entityName);
        this.attributeId = attributeId;
        this.valueFixer = valueFixer;
    }

    @Override
    protected Typed<?> fix(Typed<?> typed) {
        return typed.update(DSL.remainderFinder(), this::fixValue);
    }

    private Dynamic<?> fixValue(Dynamic<?> tag) {
        return tag.update("attributes", p_390656_ -> tag.createList(p_390656_.asStream().map(p_390653_ -> {
            String s = NamespacedSchema.ensureNamespaced(p_390653_.get("id").asString(""));
            if (!s.equals(this.attributeId)) {
                return p_390653_;
            } else {
                double d0 = p_390653_.get("base").asDouble(0.0);
                return p_390653_.set("base", p_390653_.createDouble(this.valueFixer.applyAsDouble(d0)));
            }
        })));
    }
}
