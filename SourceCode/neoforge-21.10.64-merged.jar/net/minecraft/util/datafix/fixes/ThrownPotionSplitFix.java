package net.minecraft.util.datafix.fixes;

import com.google.common.base.Suppliers;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import java.util.function.Supplier;
import net.minecraft.util.datafix.ExtraDataFixUtils;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class ThrownPotionSplitFix extends EntityRenameFix {
    private final Supplier<ThrownPotionSplitFix.ItemIdFinder> itemIdFinder = Suppliers.memoize(
        () -> {
            Type<?> type = this.getInputSchema().getChoiceType(References.ENTITY, "minecraft:potion");
            Type<?> type1 = ExtraDataFixUtils.patchSubType(
                type, this.getInputSchema().getType(References.ENTITY), this.getOutputSchema().getType(References.ENTITY)
            );
            OpticFinder<?> opticfinder = type1.findField("Item");
            OpticFinder<Pair<String, String>> opticfinder1 = DSL.fieldFinder(
                "id", DSL.named(References.ITEM_NAME.typeName(), NamespacedSchema.namespacedString())
            );
            return new ThrownPotionSplitFix.ItemIdFinder(opticfinder, opticfinder1);
        }
    );

    public ThrownPotionSplitFix(Schema outputSchema) {
        super("ThrownPotionSplitFix", outputSchema, true);
    }

    @Override
    protected Pair<String, Typed<?>> fix(String entityName, Typed<?> typed) {
        if (!entityName.equals("minecraft:potion")) {
            return Pair.of(entityName, typed);
        } else {
            String s = this.itemIdFinder.get().getItemId(typed);
            return "minecraft:lingering_potion".equals(s) ? Pair.of("minecraft:lingering_potion", typed) : Pair.of("minecraft:splash_potion", typed);
        }
    }

    record ItemIdFinder(OpticFinder<?> itemFinder, OpticFinder<Pair<String, String>> itemIdFinder) {
        public String getItemId(Typed<?> data) {
            return data.getOptionalTyped(this.itemFinder)
                .flatMap(p_400219_ -> p_400219_.getOptional(this.itemIdFinder))
                .map(Pair::getSecond)
                .map(NamespacedSchema::ensureNamespaced)
                .orElse("");
        }
    }
}
