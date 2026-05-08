package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

public class AddFieldFix extends DataFix {
    private final String name;
    private final TypeReference type;
    private final String fieldName;
    private final String[] path;
    private final Function<Dynamic<?>, Dynamic<?>> fieldGenerator;

    public AddFieldFix(Schema outputSchema, TypeReference type, String fieldName, Function<Dynamic<?>, Dynamic<?>> fieldGenerator, String... path) {
        super(outputSchema, false);
        this.name = String.format(Locale.ROOT, "Adding field `%s` to type `%s`", fieldName, type.typeName().toLowerCase());
        this.type = type;
        this.fieldName = fieldName;
        this.path = path;
        this.fieldGenerator = fieldGenerator;
    }

    @Override
    protected TypeRewriteRule makeRule() {
        return this.fixTypeEverywhereTyped(
            this.name,
            this.getInputSchema().getType(this.type),
            this.getOutputSchema().getType(this.type),
            p_443276_ -> p_443276_.update(DSL.remainderFinder(), p_443221_ -> this.addField(p_443221_, 0))
        );
    }

    private Dynamic<?> addField(Dynamic<?> data, int depth) {
        if (depth >= this.path.length) {
            return data.set(this.fieldName, this.fieldGenerator.apply(data));
        } else {
            Optional<? extends Dynamic<?>> optional = data.get(this.path[depth]).result();
            return optional.isEmpty() ? data : this.addField((Dynamic<?>)optional.get(), depth + 1);
        }
    }
}
