package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import net.minecraft.util.datafix.LegacyComponentDataFixUtils;

public class WrittenBookPagesStrictJsonFix extends ItemStackTagFix {
    public WrittenBookPagesStrictJsonFix(Schema outputSchema) {
        super(outputSchema, "WrittenBookPagesStrictJsonFix", p_428681_ -> p_428681_.equals("minecraft:written_book"));
    }

    @Override
    protected Typed<?> fixItemStackTag(Typed<?> data) {
        Type<Pair<String, String>> type = (Type<Pair<String, String>>)this.getInputSchema().getType(References.TEXT_COMPONENT);
        Type<?> type1 = this.getInputSchema().getType(References.ITEM_STACK);
        OpticFinder<?> opticfinder = type1.findField("tag");
        OpticFinder<?> opticfinder1 = opticfinder.type().findField("pages");
        OpticFinder<Pair<String, String>> opticfinder2 = DSL.typeFinder(type);
        return data.updateTyped(
            opticfinder1, p_428708_ -> p_428708_.update(opticfinder2, p_428725_ -> p_428725_.mapSecond(LegacyComponentDataFixUtils::rewriteFromLenient))
        );
    }
}
