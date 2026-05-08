package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class OptionsMenuBlurrinessFix extends DataFix {
    public OptionsMenuBlurrinessFix(Schema outputSchema) {
        super(outputSchema, false);
    }

    @Override
    public TypeRewriteRule makeRule() {
        return this.fixTypeEverywhereTyped(
            "OptionsMenuBlurrinessFix",
            this.getInputSchema().getType(References.OPTIONS),
            p_348619_ -> p_348619_.update(DSL.remainderFinder(), p_348652_ -> p_348652_.update("menuBackgroundBlurriness", p_404253_ -> {
                int i = this.convertToIntRange(p_404253_.asString("0.5"));
                return p_404253_.createString(String.valueOf(i));
            }))
        );
    }

    private int convertToIntRange(String value) {
        try {
            return Math.round(Float.parseFloat(value) * 10.0F);
        } catch (NumberFormatException numberformatexception) {
            return 5;
        }
    }
}
