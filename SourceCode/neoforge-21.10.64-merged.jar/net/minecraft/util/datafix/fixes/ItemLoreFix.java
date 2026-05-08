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

public class ItemLoreFix extends DataFix {
    public ItemLoreFix(Schema outputSchema) {
        super(outputSchema, false);
    }

    @Override
    protected TypeRewriteRule makeRule() {
        Type<?> type = this.getInputSchema().getType(References.ITEM_STACK);
        Type<Pair<String, String>> type1 = (Type<Pair<String, String>>)this.getInputSchema().getType(References.TEXT_COMPONENT);
        OpticFinder<?> opticfinder = type.findField("tag");
        OpticFinder<?> opticfinder1 = opticfinder.type().findField("display");
        OpticFinder<?> opticfinder2 = opticfinder1.type().findField("Lore");
        OpticFinder<Pair<String, String>> opticfinder3 = DSL.typeFinder(type1);
        return this.fixTypeEverywhereTyped(
            "Item Lore componentize",
            type,
            p_392847_ -> p_392847_.updateTyped(
                opticfinder,
                p_392856_ -> p_392856_.updateTyped(
                    opticfinder1,
                    p_392852_ -> p_392852_.updateTyped(
                        opticfinder2,
                        p_392849_ -> p_392849_.update(opticfinder3, p_392857_ -> p_392857_.mapSecond(LegacyComponentDataFixUtils::createTextComponentJson))
                    )
                )
            )
        );
    }
}
