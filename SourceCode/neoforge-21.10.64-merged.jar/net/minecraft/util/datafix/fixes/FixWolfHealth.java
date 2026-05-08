package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import net.minecraft.util.datafix.schemas.NamespacedSchema;
import org.apache.commons.lang3.mutable.MutableBoolean;

public class FixWolfHealth extends NamedEntityFix {
    private static final String WOLF_ID = "minecraft:wolf";
    private static final String WOLF_HEALTH = "minecraft:generic.max_health";

    public FixWolfHealth(Schema outputSchema) {
        super(outputSchema, false, "FixWolfHealth", References.ENTITY, "minecraft:wolf");
    }

    @Override
    protected Typed<?> fix(Typed<?> typed) {
        return typed.update(
            DSL.remainderFinder(),
            p_397173_ -> {
                MutableBoolean mutableboolean = new MutableBoolean(false);
                p_397173_ = p_397173_.update(
                    "Attributes",
                    p_397542_ -> p_397542_.createList(
                        p_397542_.asStream()
                            .map(
                                p_397249_ -> "minecraft:generic.max_health".equals(NamespacedSchema.ensureNamespaced(p_397249_.get("Name").asString("")))
                                    ? p_397249_.update("Base", p_397210_ -> {
                                        if (p_397210_.asDouble(0.0) == 20.0) {
                                            mutableboolean.setTrue();
                                            return p_397210_.createDouble(40.0);
                                        } else {
                                            return p_397210_;
                                        }
                                    })
                                    : p_397249_
                            )
                    )
                );
                if (mutableboolean.isTrue()) {
                    p_397173_ = p_397173_.update("Health", p_397951_ -> p_397951_.createFloat(p_397951_.asFloat(0.0F) * 2.0F));
                }

                return p_397173_;
            }
        );
    }
}
