package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import net.minecraft.util.datafix.LegacyComponentDataFixUtils;

public class ItemCustomNameToComponentFix extends DataFix {
    public ItemCustomNameToComponentFix(Schema outputSchema) {
        super(outputSchema, false);
    }

    @Override
    public TypeRewriteRule makeRule() {
        Type<?> type = this.getInputSchema().getType(References.ITEM_STACK);
        Type<Pair<String, String>> type1 = (Type<Pair<String, String>>)this.getInputSchema().getType(References.TEXT_COMPONENT);
        OpticFinder<?> opticfinder = type.findField("tag");
        OpticFinder<?> opticfinder1 = opticfinder.type().findField("display");
        OpticFinder<?> opticfinder2 = opticfinder1.type().findField("Name");
        OpticFinder<Pair<String, String>> opticfinder3 = DSL.typeFinder(type1);
        return this.fixTypeEverywhereTyped(
            "ItemCustomNameToComponentFix",
            type,
            p_392838_ -> p_392838_.updateTyped(
                opticfinder,
                p_392842_ -> p_392842_.updateTyped(
                    opticfinder1,
                    p_15931_ -> p_15931_.updateTyped(
                        opticfinder2,
                        p_392832_ -> p_392832_.update(opticfinder3, p_392833_ -> p_392833_.mapSecond(LegacyComponentDataFixUtils::createTextComponentJson))
                    )
                )
            )
        );
    }
}
