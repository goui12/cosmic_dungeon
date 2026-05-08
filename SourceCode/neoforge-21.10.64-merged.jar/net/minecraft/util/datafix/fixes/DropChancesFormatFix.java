package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.OptionalDynamic;
import java.util.List;

public class DropChancesFormatFix extends DataFix {
    private static final List<String> ARMOR_SLOT_NAMES = List.of("feet", "legs", "chest", "head");
    private static final List<String> HAND_SLOT_NAMES = List.of("mainhand", "offhand");
    private static final float DEFAULT_CHANCE = 0.085F;

    public DropChancesFormatFix(Schema outputSchema) {
        super(outputSchema, false);
    }

    @Override
    protected TypeRewriteRule makeRule() {
        return this.fixTypeEverywhereTyped(
            "DropChancesFormatFix", this.getInputSchema().getType(References.ENTITY), p_394049_ -> p_394049_.update(DSL.remainderFinder(), p_394520_ -> {
                List<Float> list = parseDropChances(p_394520_.get("ArmorDropChances"));
                List<Float> list1 = parseDropChances(p_394520_.get("HandDropChances"));
                float f = p_394520_.get("body_armor_drop_chance").asNumber().result().map(Number::floatValue).orElse(0.085F);
                p_394520_ = p_394520_.remove("ArmorDropChances").remove("HandDropChances").remove("body_armor_drop_chance");
                Dynamic<?> dynamic = p_394520_.emptyMap();
                dynamic = addSlotChances(dynamic, list, ARMOR_SLOT_NAMES);
                dynamic = addSlotChances(dynamic, list1, HAND_SLOT_NAMES);
                if (f != 0.085F) {
                    dynamic = dynamic.set("body", p_394520_.createFloat(f));
                }

                return !dynamic.equals(p_394520_.emptyMap()) ? p_394520_.set("drop_chances", dynamic) : p_394520_;
            })
        );
    }

    private static Dynamic<?> addSlotChances(Dynamic<?> tag, List<Float> chances, List<String> names) {
        for (int i = 0; i < names.size() && i < chances.size(); i++) {
            String s = names.get(i);
            float f = chances.get(i);
            if (f != 0.085F) {
                tag = tag.set(s, tag.createFloat(f));
            }
        }

        return tag;
    }

    private static List<Float> parseDropChances(OptionalDynamic<?> data) {
        return data.asStream().map(p_393989_ -> p_393989_.asFloat(0.085F)).toList();
    }
}
