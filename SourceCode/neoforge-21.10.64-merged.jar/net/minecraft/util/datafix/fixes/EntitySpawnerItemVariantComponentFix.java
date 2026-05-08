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
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.Util;
import net.minecraft.util.datafix.ExtraDataFixUtils;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class EntitySpawnerItemVariantComponentFix extends DataFix {
    public EntitySpawnerItemVariantComponentFix(Schema outputSchema) {
        super(outputSchema, false);
    }

    @Override
    public final TypeRewriteRule makeRule() {
        Type<?> type = this.getInputSchema().getType(References.ITEM_STACK);
        OpticFinder<Pair<String, String>> opticfinder = DSL.fieldFinder("id", DSL.named(References.ITEM_NAME.typeName(), NamespacedSchema.namespacedString()));
        OpticFinder<?> opticfinder1 = type.findField("components");
        return this.fixTypeEverywhereTyped(
            "ItemStack bucket_entity_data variants to separate components",
            type,
            p_397051_ -> {
                String s = p_397051_.getOptional(opticfinder).map(Pair::getSecond).orElse("");

                return switch (s) {
                    case "minecraft:salmon_bucket" -> p_397051_.updateTyped(opticfinder1, (Fixer)EntitySpawnerItemVariantComponentFix::fixSalmonBucket);
                    case "minecraft:axolotl_bucket" -> p_397051_.updateTyped(opticfinder1, (Fixer)EntitySpawnerItemVariantComponentFix::fixAxolotlBucket);
                    case "minecraft:tropical_fish_bucket" -> p_397051_.updateTyped(opticfinder1, (Fixer)EntitySpawnerItemVariantComponentFix::fixTropicalFishBucket);
                    case "minecraft:painting" -> p_397051_.updateTyped(
                        opticfinder1,
                        p_398017_ -> Util.writeAndReadTypedOrThrow(p_398017_, p_398017_.getType(), EntitySpawnerItemVariantComponentFix::fixPainting)
                    );
                    default -> p_397051_;
                };
            }
        );
    }

    private static String getBaseColor(int variant) {
        return ExtraDataFixUtils.dyeColorIdToName(variant >> 16 & 0xFF);
    }

    private static String getPatternColor(int variant) {
        return ExtraDataFixUtils.dyeColorIdToName(variant >> 24 & 0xFF);
    }

    private static String getPattern(int variant) {
        return switch (variant & 65535) {
            case 1 -> "flopper";
            case 256 -> "sunstreak";
            case 257 -> "stripey";
            case 512 -> "snooper";
            case 513 -> "glitter";
            case 768 -> "dasher";
            case 769 -> "blockfish";
            case 1024 -> "brinely";
            case 1025 -> "betty";
            case 1280 -> "spotty";
            case 1281 -> "clayfish";
            default -> "kob";
        };
    }

    private static <T> Dynamic<T> fixTropicalFishBucket(Dynamic<T> data, Dynamic<T> entityData) {
        Optional<Number> optional = entityData.get("BucketVariantTag").asNumber().result();
        if (optional.isEmpty()) {
            return data;
        } else {
            int i = optional.get().intValue();
            String s = getPattern(i);
            String s1 = getBaseColor(i);
            String s2 = getPatternColor(i);
            return data.update("minecraft:bucket_entity_data", p_397278_ -> p_397278_.remove("BucketVariantTag"))
                .set("minecraft:tropical_fish/pattern", data.createString(s))
                .set("minecraft:tropical_fish/base_color", data.createString(s1))
                .set("minecraft:tropical_fish/pattern_color", data.createString(s2));
        }
    }

    private static <T> Dynamic<T> fixAxolotlBucket(Dynamic<T> data, Dynamic<T> entityData) {
        Optional<Number> optional = entityData.get("Variant").asNumber().result();
        if (optional.isEmpty()) {
            return data;
        } else {
            String s = switch (optional.get().intValue()) {
                case 1 -> "wild";
                case 2 -> "gold";
                case 3 -> "cyan";
                case 4 -> "blue";
                default -> "lucy";
            };
            return data.update("minecraft:bucket_entity_data", p_397463_ -> p_397463_.remove("Variant"))
                .set("minecraft:axolotl/variant", data.createString(s));
        }
    }

    private static <T> Dynamic<T> fixSalmonBucket(Dynamic<T> data, Dynamic<T> entityData) {
        Optional<Dynamic<T>> optional = entityData.get("type").result();
        return optional.isEmpty()
            ? data
            : data.update("minecraft:bucket_entity_data", p_397679_ -> p_397679_.remove("type")).set("minecraft:salmon/size", optional.get());
    }

    private static <T> Dynamic<T> fixPainting(Dynamic<T> data) {
        Optional<Dynamic<T>> optional = data.get("minecraft:entity_data").result();
        if (optional.isEmpty()) {
            return data;
        } else if (optional.get().get("id").asString().result().filter(p_397801_ -> p_397801_.equals("minecraft:painting")).isEmpty()) {
            return data;
        } else {
            Optional<Dynamic<T>> optional1 = optional.get().get("variant").result();
            Dynamic<T> dynamic = optional.get().remove("variant");
            if (dynamic.remove("id").equals(dynamic.emptyMap())) {
                data = data.remove("minecraft:entity_data");
            } else {
                data = data.set("minecraft:entity_data", dynamic);
            }

            if (optional1.isPresent()) {
                data = data.set("minecraft:painting/variant", optional1.get());
            }

            return data;
        }
    }

    @FunctionalInterface
    interface Fixer extends Function<Typed<?>, Typed<?>> {
        default Typed<?> apply(Typed<?> data) {
            return data.update(DSL.remainderFinder(), this::fixRemainder);
        }

        default <T> Dynamic<T> fixRemainder(Dynamic<T> data) {
            return data.get("minecraft:bucket_entity_data")
                .result()
                .map(p_397558_ -> this.fixRemainder(data, (Dynamic<T>)p_397558_))
                .orElse(data);
        }

        <T> Dynamic<T> fixRemainder(Dynamic<T> data, Dynamic<T> entityData);
    }
}
