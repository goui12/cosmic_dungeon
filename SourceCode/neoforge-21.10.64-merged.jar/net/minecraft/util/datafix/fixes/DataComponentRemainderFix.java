package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import javax.annotation.Nullable;

public abstract class DataComponentRemainderFix extends DataFix {
    private final String name;
    private final String componentId;
    private final String newComponentId;

    public DataComponentRemainderFix(Schema outputSchema, String name, String componentId) {
        this(outputSchema, name, componentId, componentId);
    }

    public DataComponentRemainderFix(Schema outputSchema, String name, String componentId, String newComponentId) {
        super(outputSchema, false);
        this.name = name;
        this.componentId = componentId;
        this.newComponentId = newComponentId;
    }

    @Override
    public final TypeRewriteRule makeRule() {
        Type<?> type = this.getInputSchema().getType(References.DATA_COMPONENTS);
        return this.fixTypeEverywhereTyped(this.name, type, p_386780_ -> p_386780_.update(DSL.remainderFinder(), p_387394_ -> {
            Optional<? extends Dynamic<?>> optional = p_387394_.get(this.componentId).result();
            if (optional.isEmpty()) {
                return p_387394_;
            } else {
                Dynamic<?> dynamic = this.fixComponent((Dynamic<?>)optional.get());
                return p_387394_.remove(this.componentId).setFieldIfPresent(this.newComponentId, Optional.ofNullable(dynamic));
            }
        }));
    }

    @Nullable
    protected abstract <T> Dynamic<T> fixComponent(Dynamic<T> component);
}
