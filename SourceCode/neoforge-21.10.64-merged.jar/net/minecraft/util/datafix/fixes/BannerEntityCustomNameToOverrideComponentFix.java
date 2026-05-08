package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.TaggedChoice.TaggedChoiceType;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import java.util.Map;
import java.util.Optional;
import net.minecraft.Util;
import net.minecraft.util.datafix.LegacyComponentDataFixUtils;

public class BannerEntityCustomNameToOverrideComponentFix extends DataFix {
    public BannerEntityCustomNameToOverrideComponentFix(Schema outputSchema) {
        super(outputSchema, false);
    }

    @Override
    public TypeRewriteRule makeRule() {
        Type<?> type = this.getInputSchema().getType(References.BLOCK_ENTITY);
        TaggedChoiceType<?> taggedchoicetype = this.getInputSchema().findChoiceType(References.BLOCK_ENTITY);
        OpticFinder<?> opticfinder = type.findField("CustomName");
        OpticFinder<Pair<String, String>> opticfinder1 = DSL.typeFinder((Type<Pair<String, String>>)this.getInputSchema().getType(References.TEXT_COMPONENT));
        return this.fixTypeEverywhereTyped("Banner entity custom_name to item_name component fix", type, p_392810_ -> {
            Object object = p_392810_.get(taggedchoicetype.finder()).getFirst();
            return object.equals("minecraft:banner") ? this.fix(p_392810_, opticfinder1, opticfinder) : p_392810_;
        });
    }

    private Typed<?> fix(Typed<?> data, OpticFinder<Pair<String, String>> textComponentTypeOptic, OpticFinder<?> customNameOptic) {
        Optional<String> optional = data.getOptionalTyped(customNameOptic).flatMap(p_392806_ -> p_392806_.getOptional(textComponentTypeOptic).map(Pair::getSecond));
        boolean flag = optional.flatMap(LegacyComponentDataFixUtils::extractTranslationString)
            .filter(p_338664_ -> p_338664_.equals("block.minecraft.ominous_banner"))
            .isPresent();
        return flag
            ? Util.writeAndReadTypedOrThrow(
                data,
                data.getType(),
                p_392804_ -> {
                    Dynamic<?> dynamic = p_392804_.createMap(
                        Map.of(
                            p_392804_.createString("minecraft:item_name"),
                            p_392804_.createString(optional.get()),
                            p_392804_.createString("minecraft:hide_additional_tooltip"),
                            p_392804_.emptyMap()
                        )
                    );
                    return p_392804_.set("components", dynamic).remove("CustomName");
                }
            )
            : data;
    }
}
