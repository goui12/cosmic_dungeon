package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.OptionalDynamic;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;
import net.minecraft.Util;

public class TooltipDisplayComponentFix extends DataFix {
    private static final List<String> CONVERTED_ADDITIONAL_TOOLTIP_TYPES = List.of(
        "minecraft:banner_patterns",
        "minecraft:bees",
        "minecraft:block_entity_data",
        "minecraft:block_state",
        "minecraft:bundle_contents",
        "minecraft:charged_projectiles",
        "minecraft:container",
        "minecraft:container_loot",
        "minecraft:firework_explosion",
        "minecraft:fireworks",
        "minecraft:instrument",
        "minecraft:map_id",
        "minecraft:painting/variant",
        "minecraft:pot_decorations",
        "minecraft:potion_contents",
        "minecraft:tropical_fish/pattern",
        "minecraft:written_book_content"
    );

    public TooltipDisplayComponentFix(Schema outputSchema) {
        super(outputSchema, true);
    }

    @Override
    protected TypeRewriteRule makeRule() {
        Type<?> type = this.getInputSchema().getType(References.DATA_COMPONENTS);
        Type<?> type1 = this.getOutputSchema().getType(References.DATA_COMPONENTS);
        OpticFinder<?> opticfinder = type.findField("minecraft:can_place_on");
        OpticFinder<?> opticfinder1 = type.findField("minecraft:can_break");
        Type<?> type2 = type1.findFieldType("minecraft:can_place_on");
        Type<?> type3 = type1.findFieldType("minecraft:can_break");
        return this.fixTypeEverywhereTyped("TooltipDisplayComponentFix", type, type1, p_399664_ -> fix(p_399664_, opticfinder, opticfinder1, type2, type3));
    }

    private static Typed<?> fix(Typed<?> data, OpticFinder<?> canPlaceOnOptic, OpticFinder<?> canBreakOptic, Type<?> canPlaceOnType, Type<?> canBreakType) {
        Set<String> set = new HashSet<>();
        data = fixAdventureModePredicate(data, canPlaceOnOptic, canPlaceOnType, "minecraft:can_place_on", set);
        data = fixAdventureModePredicate(data, canBreakOptic, canBreakType, "minecraft:can_break", set);
        return data.update(
            DSL.remainderFinder(),
            p_423221_ -> {
                p_423221_ = fixSimpleComponent(p_423221_, "minecraft:trim", set);
                p_423221_ = fixSimpleComponent(p_423221_, "minecraft:unbreakable", set);
                p_423221_ = fixComponentAndUnwrap(p_423221_, "minecraft:dyed_color", "rgb", set);
                p_423221_ = fixComponentAndUnwrap(p_423221_, "minecraft:attribute_modifiers", "modifiers", set);
                p_423221_ = fixComponentAndUnwrap(p_423221_, "minecraft:enchantments", "levels", set);
                p_423221_ = fixComponentAndUnwrap(p_423221_, "minecraft:stored_enchantments", "levels", set);
                p_423221_ = fixComponentAndUnwrap(p_423221_, "minecraft:jukebox_playable", "song", set);
                boolean flag = p_423221_.get("minecraft:hide_tooltip").result().isPresent();
                p_423221_ = p_423221_.remove("minecraft:hide_tooltip");
                boolean flag1 = p_423221_.get("minecraft:hide_additional_tooltip").result().isPresent();
                p_423221_ = p_423221_.remove("minecraft:hide_additional_tooltip");
                if (flag1) {
                    for (String s : CONVERTED_ADDITIONAL_TOOLTIP_TYPES) {
                        if (p_423221_.get(s).result().isPresent()) {
                            set.add(s);
                        }
                    }
                }

                return set.isEmpty() && !flag
                    ? p_423221_
                    : p_423221_.set(
                        "minecraft:tooltip_display",
                        p_423221_.createMap(
                            Map.of(
                                p_423221_.createString("hide_tooltip"),
                                p_423221_.createBoolean(flag),
                                p_423221_.createString("hidden_components"),
                                p_423221_.createList(set.stream().map(p_423221_::createString))
                            )
                        )
                    );
            }
        );
    }

    private static Dynamic<?> fixSimpleComponent(Dynamic<?> data, String name, Set<String> processedComponents) {
        return fixRemainderComponent(data, name, processedComponents, UnaryOperator.identity());
    }

    private static Dynamic<?> fixComponentAndUnwrap(Dynamic<?> data, String name, String innerFieldName, Set<String> processedComponents) {
        return fixRemainderComponent(data, name, processedComponents, p_400276_ -> DataFixUtils.orElse(p_400276_.get(innerFieldName).result(), p_400276_));
    }

    private static Dynamic<?> fixRemainderComponent(Dynamic<?> data, String name, Set<String> processedComponents, UnaryOperator<Dynamic<?>> unwrapper) {
        return data.update(name, p_399611_ -> {
            boolean flag = p_399611_.get("show_in_tooltip").asBoolean(true);
            if (!flag) {
                processedComponents.add(name);
            }

            return unwrapper.apply(p_399611_.remove("show_in_tooltip"));
        });
    }

    private static Typed<?> fixAdventureModePredicate(Typed<?> data, OpticFinder<?> optic, Type<?> type, String name, Set<String> processedComponents) {
        return data.updateTyped(optic, type, p_399580_ -> Util.writeAndReadTypedOrThrow(p_399580_, type, p_399655_ -> {
            OptionalDynamic<?> optionaldynamic = p_399655_.get("predicates");
            if (optionaldynamic.result().isEmpty()) {
                return p_399655_;
            } else {
                boolean flag = p_399655_.get("show_in_tooltip").asBoolean(true);
                if (!flag) {
                    processedComponents.add(name);
                }

                return optionaldynamic.result().get();
            }
        }));
    }
}
