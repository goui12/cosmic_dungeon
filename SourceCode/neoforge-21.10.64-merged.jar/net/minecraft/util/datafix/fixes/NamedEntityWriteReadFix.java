package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import net.minecraft.Util;
import net.minecraft.util.datafix.ExtraDataFixUtils;

public abstract class NamedEntityWriteReadFix extends DataFix {
    private final String name;
    private final String entityName;
    private final TypeReference type;

    public NamedEntityWriteReadFix(Schema outputSchema, boolean changesType, String name, TypeReference type, String entityName) {
        super(outputSchema, changesType);
        this.name = name;
        this.type = type;
        this.entityName = entityName;
    }

    @Override
    public TypeRewriteRule makeRule() {
        Type<?> type = this.getInputSchema().getType(this.type);
        Type<?> type1 = this.getInputSchema().getChoiceType(this.type, this.entityName);
        Type<?> type2 = this.getOutputSchema().getType(this.type);
        OpticFinder<?> opticfinder = DSL.namedChoice(this.entityName, type1);
        Type<?> type3 = ExtraDataFixUtils.patchSubType(type, type, type2);
        return this.fix(type, type2, type3, opticfinder);
    }

    private <S, T, A> TypeRewriteRule fix(Type<S> inputType, Type<T> outputType, Type<?> type, OpticFinder<A> optic) {
        return this.fixTypeEverywhereTyped(this.name, inputType, outputType, p_396589_ -> {
            if (p_396589_.getOptional(optic).isEmpty()) {
                return ExtraDataFixUtils.cast(outputType, p_396589_);
            } else {
                Typed<?> typed = ExtraDataFixUtils.cast(type, p_396589_);
                return Util.writeAndReadTypedOrThrow(typed, outputType, this::fix);
            }
        });
    }

    protected abstract <T> Dynamic<T> fix(Dynamic<T> tag);
}
