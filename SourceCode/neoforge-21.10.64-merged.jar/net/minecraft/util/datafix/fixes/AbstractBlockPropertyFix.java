package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public abstract class AbstractBlockPropertyFix extends DataFix {
    private final String name;

    public AbstractBlockPropertyFix(Schema outputSchema, String name) {
        super(outputSchema, false);
        this.name = name;
    }

    @Override
    protected TypeRewriteRule makeRule() {
        return this.fixTypeEverywhereTyped(
            this.name, this.getInputSchema().getType(References.BLOCK_STATE), p_393664_ -> p_393664_.update(DSL.remainderFinder(), this::fixBlockState)
        );
    }

    private Dynamic<?> fixBlockState(Dynamic<?> tag) {
        Optional<String> optional = tag.get("Name").asString().result().map(NamespacedSchema::ensureNamespaced);
        return optional.isPresent() && this.shouldFix(optional.get())
            ? tag.update("Properties", p_394507_ -> this.fixProperties(optional.get(), p_394507_))
            : tag;
    }

    protected abstract boolean shouldFix(String name);

    protected abstract <T> Dynamic<T> fixProperties(String name, Dynamic<T> properties);
}
