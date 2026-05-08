package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.OptionalDynamic;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.Util;
import net.minecraft.util.datafix.LegacyComponentDataFixUtils;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class ItemStackCustomNameToOverrideComponentFix extends DataFix {
    private static final Set<String> MAP_NAMES = Set.of(
        "filled_map.buried_treasure",
        "filled_map.explorer_jungle",
        "filled_map.explorer_swamp",
        "filled_map.mansion",
        "filled_map.monument",
        "filled_map.trial_chambers",
        "filled_map.village_desert",
        "filled_map.village_plains",
        "filled_map.village_savanna",
        "filled_map.village_snowy",
        "filled_map.village_taiga"
    );

    public ItemStackCustomNameToOverrideComponentFix(Schema outputSchema) {
        super(outputSchema, false);
    }

    @Override
    public final TypeRewriteRule makeRule() {
        Type<?> type = this.getInputSchema().getType(References.ITEM_STACK);
        OpticFinder<Pair<String, String>> opticfinder = DSL.fieldFinder("id", DSL.named(References.ITEM_NAME.typeName(), NamespacedSchema.namespacedString()));
        OpticFinder<?> opticfinder1 = type.findField("components");
        return this.fixTypeEverywhereTyped(
            "ItemStack custom_name to item_name component fix",
            type,
            p_338745_ -> {
                Optional<Pair<String, String>> optional = p_338745_.getOptional(opticfinder);
                Optional<String> optional1 = optional.map(Pair::getSecond);
                if (optional1.filter(p_338283_ -> p_338283_.equals("minecraft:white_banner")).isPresent()) {
                    return p_338745_.updateTyped(opticfinder1, ItemStackCustomNameToOverrideComponentFix::fixBanner);
                } else {
                    return optional1.filter(p_338490_ -> p_338490_.equals("minecraft:filled_map")).isPresent()
                        ? p_338745_.updateTyped(opticfinder1, ItemStackCustomNameToOverrideComponentFix::fixMap)
                        : p_338745_;
                }
            }
        );
    }

    private static <T> Typed<T> fixMap(Typed<T> data) {
        return fixCustomName(data, MAP_NAMES::contains);
    }

    private static <T> Typed<T> fixBanner(Typed<T> data) {
        return fixCustomName(data, p_338711_ -> p_338711_.equals("block.minecraft.ominous_banner"));
    }

    private static <T> Typed<T> fixCustomName(Typed<T> data, Predicate<String> namePredicate) {
        return Util.writeAndReadTypedOrThrow(data, data.getType(), p_392859_ -> {
            OptionalDynamic<?> optionaldynamic = p_392859_.get("minecraft:custom_name");
            Optional<String> optional = optionaldynamic.asString().result().flatMap(LegacyComponentDataFixUtils::extractTranslationString).filter(namePredicate);
            return optional.isPresent() ? p_392859_.renameField("minecraft:custom_name", "minecraft:item_name") : p_392859_;
        });
    }
}
