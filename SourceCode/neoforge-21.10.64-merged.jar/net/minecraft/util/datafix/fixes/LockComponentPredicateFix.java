package net.minecraft.util.datafix.fixes;

import com.google.common.escape.Escaper;
import com.google.common.escape.Escapers;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import javax.annotation.Nullable;

public class LockComponentPredicateFix extends DataComponentRemainderFix {
    public static final Escaper ESCAPER = Escapers.builder().addEscape('"', "\\\"").addEscape('\\', "\\\\").build();

    public LockComponentPredicateFix(Schema outputSchema) {
        super(outputSchema, "LockComponentPredicateFix", "minecraft:lock");
    }

    @Nullable
    @Override
    protected <T> Dynamic<T> fixComponent(Dynamic<T> component) {
        return fixLock(component);
    }

    @Nullable
    public static <T> Dynamic<T> fixLock(Dynamic<T> tag) {
        Optional<String> optional = tag.asString().result();
        if (optional.isEmpty()) {
            return null;
        } else if (optional.get().isEmpty()) {
            return null;
        } else {
            Dynamic<T> dynamic = tag.createString("\"" + ESCAPER.escape(optional.get()) + "\"");
            Dynamic<T> dynamic1 = tag.emptyMap().set("minecraft:custom_name", dynamic);
            return tag.emptyMap().set("components", dynamic1);
        }
    }
}
