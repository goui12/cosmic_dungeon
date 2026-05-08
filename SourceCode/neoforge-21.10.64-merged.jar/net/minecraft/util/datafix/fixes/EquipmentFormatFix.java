package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public class EquipmentFormatFix extends DataFix {
    public EquipmentFormatFix(Schema outputSchema) {
        super(outputSchema, true);
    }

    @Override
    protected TypeRewriteRule makeRule() {
        Type<?> type = this.getInputSchema().getTypeRaw(References.ITEM_STACK);
        Type<?> type1 = this.getOutputSchema().getTypeRaw(References.ITEM_STACK);
        OpticFinder<?> opticfinder = type.findField("id");
        return this.fix(type, type1, opticfinder);
    }

    private <ItemStackOld, ItemStackNew> TypeRewriteRule fix(Type<ItemStackOld> oldItemStackType, Type<ItemStackNew> newItemStackType, OpticFinder<?> optic) {
        Type<Pair<String, Pair<Either<List<ItemStackOld>, Unit>, Pair<Either<List<ItemStackOld>, Unit>, Pair<Either<ItemStackOld, Unit>, Either<ItemStackOld, Unit>>>>>> type = DSL.named(
            References.ENTITY_EQUIPMENT.typeName(),
            DSL.and(
                DSL.optional(DSL.field("ArmorItems", DSL.list(oldItemStackType))),
                DSL.optional(DSL.field("HandItems", DSL.list(oldItemStackType))),
                DSL.optional(DSL.field("body_armor_item", oldItemStackType)),
                DSL.optional(DSL.field("saddle", oldItemStackType))
            )
        );
        Type<Pair<String, Either<Pair<Either<ItemStackNew, Unit>, Pair<Either<ItemStackNew, Unit>, Pair<Either<ItemStackNew, Unit>, Pair<Either<ItemStackNew, Unit>, Pair<Either<ItemStackNew, Unit>, Pair<Either<ItemStackNew, Unit>, Pair<Either<ItemStackNew, Unit>, Pair<Either<ItemStackNew, Unit>, Dynamic<?>>>>>>>>>, Unit>>> type1 = DSL.named(
            References.ENTITY_EQUIPMENT.typeName(),
            DSL.optional(
                DSL.field(
                    "equipment",
                    DSL.and(
                        DSL.optional(DSL.field("mainhand", newItemStackType)),
                        DSL.optional(DSL.field("offhand", newItemStackType)),
                        DSL.optional(DSL.field("feet", newItemStackType)),
                        DSL.and(
                            DSL.optional(DSL.field("legs", newItemStackType)),
                            DSL.optional(DSL.field("chest", newItemStackType)),
                            DSL.optional(DSL.field("head", newItemStackType)),
                            DSL.and(DSL.optional(DSL.field("body", newItemStackType)), DSL.optional(DSL.field("saddle", newItemStackType)), DSL.remainderType())
                        )
                    )
                )
            )
        );
        if (!type.equals(this.getInputSchema().getType(References.ENTITY_EQUIPMENT))) {
            throw new IllegalStateException("Input entity_equipment type does not match expected");
        } else if (!type1.equals(this.getOutputSchema().getType(References.ENTITY_EQUIPMENT))) {
            throw new IllegalStateException("Output entity_equipment type does not match expected");
        } else {
            return this.fixTypeEverywhere(
                "EquipmentFormatFix",
                type,
                type1,
                p_397464_ -> {
                    Predicate<ItemStackOld> predicate = p_397199_ -> {
                        Typed<ItemStackOld> typed = new Typed<>(oldItemStackType, p_397464_, p_397199_);
                        return typed.getOptional(optic).isEmpty();
                    };
                    return p_397594_ -> {
                        String s = p_397594_.getFirst();
                        Pair<Either<List<ItemStackOld>, Unit>, Pair<Either<List<ItemStackOld>, Unit>, Pair<Either<ItemStackOld, Unit>, Either<ItemStackOld, Unit>>>> pair = p_397594_.getSecond();
                        List<ItemStackOld> list = pair.getFirst().map(Function.identity(), p_397225_ -> List.of());
                        List<ItemStackOld> list1 = pair.getSecond().getFirst().map(Function.identity(), p_398024_ -> List.of());
                        Either<ItemStackOld, Unit> either = pair.getSecond().getSecond().getFirst();
                        Either<ItemStackOld, Unit> either1 = pair.getSecond().getSecond().getSecond();
                        Either<ItemStackOld, Unit> either2 = getItemFromList(0, list, predicate);
                        Either<ItemStackOld, Unit> either3 = getItemFromList(1, list, predicate);
                        Either<ItemStackOld, Unit> either4 = getItemFromList(2, list, predicate);
                        Either<ItemStackOld, Unit> either5 = getItemFromList(3, list, predicate);
                        Either<ItemStackOld, Unit> either6 = getItemFromList(0, list1, predicate);
                        Either<ItemStackOld, Unit> either7 = getItemFromList(1, list1, predicate);
                        return areAllEmpty(either, either1, either2, either3, either4, either5, either6, either7)
                            ? Pair.of(s, Either.right(Unit.INSTANCE))
                            : Pair.of(
                                s,
                                Either.left(
                                    Pair.of(
                                        (Either<ItemStackNew, Unit>)either6,
                                        Pair.of(
                                            (Either<ItemStackNew, Unit>)either7,
                                            Pair.of(
                                                (Either<ItemStackNew, Unit>)either2,
                                                Pair.of(
                                                    (Either<ItemStackNew, Unit>)either3,
                                                    Pair.of(
                                                        (Either<ItemStackNew, Unit>)either4,
                                                        Pair.of(
                                                            (Either<ItemStackNew, Unit>)either5,
                                                            Pair.of(
                                                                (Either<ItemStackNew, Unit>)either,
                                                                Pair.of((Either<ItemStackNew, Unit>)either1, new Dynamic(p_397464_))
                                                            )
                                                        )
                                                    )
                                                )
                                            )
                                        )
                                    )
                                )
                            );
                    };
                }
            );
        }
    }

    @SafeVarargs
    private static boolean areAllEmpty(Either<?, Unit>... items) {
        for (Either<?, Unit> either : items) {
            if (either.right().isEmpty()) {
                return false;
            }
        }

        return true;
    }

    private static <ItemStack> Either<ItemStack, Unit> getItemFromList(int index, List<ItemStack> list, Predicate<ItemStack> predicate) {
        if (index >= list.size()) {
            return Either.right(Unit.INSTANCE);
        } else {
            ItemStack itemstack = list.get(index);
            return predicate.test(itemstack) ? Either.right(Unit.INSTANCE) : Either.left(itemstack);
        }
    }
}
