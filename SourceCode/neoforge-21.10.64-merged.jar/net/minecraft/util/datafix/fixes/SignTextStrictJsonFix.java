package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import java.util.List;
import net.minecraft.util.datafix.LegacyComponentDataFixUtils;

public class SignTextStrictJsonFix extends NamedEntityFix {
    private static final List<String> LINE_FIELDS = List.of("Text1", "Text2", "Text3", "Text4");

    public SignTextStrictJsonFix(Schema outputSchema) {
        super(outputSchema, false, "SignTextStrictJsonFix", References.BLOCK_ENTITY, "Sign");
    }

    @Override
    protected Typed<?> fix(Typed<?> typed) {
        for (String s : LINE_FIELDS) {
            OpticFinder<?> opticfinder = typed.getType().findField(s);
            OpticFinder<Pair<String, String>> opticfinder1 = DSL.typeFinder(
                (Type<Pair<String, String>>)this.getInputSchema().getType(References.TEXT_COMPONENT)
            );
            typed = typed.updateTyped(
                opticfinder, p_428707_ -> p_428707_.update(opticfinder1, p_428729_ -> p_428729_.mapSecond(LegacyComponentDataFixUtils::rewriteFromLenient))
            );
        }

        return typed;
    }
}
