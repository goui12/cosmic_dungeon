package net.minecraft.world.level.storage.loot.entries;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class NestedLootTable extends LootPoolSingletonContainer {
    public static final MapCodec<NestedLootTable> CODEC = RecordCodecBuilder.mapCodec(
        p_404620_ -> p_404620_.group(Codec.either(LootTable.KEY_CODEC, LootTable.DIRECT_CODEC).fieldOf("value").forGetter(p_331842_ -> p_331842_.contents))
            .and(singletonFields(p_404620_))
            .apply(p_404620_, NestedLootTable::new)
    );
    public static final ProblemReporter.PathElement INLINE_LOOT_TABLE_PATH_ELEMENT = new ProblemReporter.PathElement() {
        @Override
        public String get() {
            return "->{inline}";
        }
    };
    private final Either<ResourceKey<LootTable>, LootTable> contents;

    private NestedLootTable(
        Either<ResourceKey<LootTable>, LootTable> contents, int weight, int quality, List<LootItemCondition> conditions, List<LootItemFunction> functions
    ) {
        super(weight, quality, conditions, functions);
        this.contents = contents;
    }

    @Override
    public LootPoolEntryType getType() {
        return LootPoolEntries.LOOT_TABLE;
    }

    /**
     * Generate the loot stacks of this entry.
     * Contrary to the method name this method does not always generate one stack, it can also generate zero or multiple stacks.
     */
    @Override
    public void createItemStack(Consumer<ItemStack> stackConsumer, LootContext lootContext) {
        this.contents
            .map(
                p_368482_ -> lootContext.getResolver().get((ResourceKey<LootTable>)p_368482_).map(Holder::value).orElse(LootTable.EMPTY),
                p_330212_ -> (LootTable)p_330212_
            )
            .getRandomItemsRaw(lootContext, stackConsumer);
    }

    @Override
    public void validate(ValidationContext validationContext) {
        Optional<ResourceKey<LootTable>> optional = this.contents.left();
        if (optional.isPresent()) {
            ResourceKey<LootTable> resourcekey = optional.get();
            if (!validationContext.allowsReferences()) {
                validationContext.reportProblem(new ValidationContext.ReferenceNotAllowedProblem(resourcekey));
                return;
            }

            if (validationContext.hasVisitedElement(resourcekey)) {
                validationContext.reportProblem(new ValidationContext.RecursiveReferenceProblem(resourcekey));
                return;
            }
        }

        super.validate(validationContext);
        this.contents
            .ifLeft(
                p_368484_ -> validationContext.resolver()
                    .get((ResourceKey<LootTable>)p_368484_)
                    .ifPresentOrElse(
                        p_421462_ -> p_421462_.value()
                            .validate(
                                validationContext.enterElement(new ProblemReporter.ElementReferencePathElement((ResourceKey<?>)p_368484_), (ResourceKey<?>)p_368484_)
                            ),
                        () -> validationContext.reportProblem(new ValidationContext.MissingReferenceProblem((ResourceKey<?>)p_368484_))
                    )
            )
            .ifRight(p_421466_ -> p_421466_.validate(validationContext.forChild(INLINE_LOOT_TABLE_PATH_ELEMENT)));
    }

    public static LootPoolSingletonContainer.Builder<?> lootTableReference(ResourceKey<LootTable> lootTable) {
        return simpleBuilder(
            (p_331271_, p_331120_, p_331361_, p_331392_) -> new NestedLootTable(Either.left(lootTable), p_331271_, p_331120_, p_331361_, p_331392_)
        );
    }

    public static LootPoolSingletonContainer.Builder<?> inlineLootTable(LootTable lootTable) {
        return simpleBuilder(
            (p_330488_, p_330473_, p_330668_, p_331391_) -> new NestedLootTable(Either.right(lootTable), p_330488_, p_330473_, p_330668_, p_331391_)
        );
    }
}
