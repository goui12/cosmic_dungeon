package net.minecraft.world.level.storage.loot.functions;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.slf4j.Logger;

public class FunctionReference extends LootItemConditionalFunction {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final MapCodec<FunctionReference> CODEC = RecordCodecBuilder.mapCodec(
        p_335341_ -> commonFields(p_335341_)
            .and(ResourceKey.codec(Registries.ITEM_MODIFIER).fieldOf("name").forGetter(p_335337_ -> p_335337_.name))
            .apply(p_335341_, FunctionReference::new)
    );
    private final ResourceKey<LootItemFunction> name;

    private FunctionReference(List<LootItemCondition> conditions, ResourceKey<LootItemFunction> name) {
        super(conditions);
        this.name = name;
    }

    @Override
    public LootItemFunctionType<FunctionReference> getType() {
        return LootItemFunctions.REFERENCE;
    }

    /**
     * Validate that this object is used correctly according to the given ValidationContext.
     */
    @Override
    public void validate(ValidationContext context) {
        if (!context.allowsReferences()) {
            context.reportProblem(new ValidationContext.ReferenceNotAllowedProblem(this.name));
        } else if (context.hasVisitedElement(this.name)) {
            context.reportProblem(new ValidationContext.RecursiveReferenceProblem(this.name));
        } else {
            super.validate(context);
            context.resolver()
                .get(this.name)
                .ifPresentOrElse(
                    p_421469_ -> p_421469_.value().validate(context.enterElement(new ProblemReporter.ElementReferencePathElement(this.name), this.name)),
                    () -> context.reportProblem(new ValidationContext.MissingReferenceProblem(this.name))
                );
        }
    }

    /**
     * Called to perform the actual action of this function, after conditions have been checked.
     */
    @Override
    protected ItemStack run(ItemStack stack, LootContext context) {
        LootItemFunction lootitemfunction = context.getResolver().get(this.name).map(Holder::value).orElse(null);
        if (lootitemfunction == null) {
            LOGGER.warn("Unknown function: {}", this.name.location());
            return stack;
        } else {
            LootContext.VisitedEntry<?> visitedentry = LootContext.createVisitedEntry(lootitemfunction);
            if (context.pushVisitedElement(visitedentry)) {
                ItemStack itemstack;
                try {
                    itemstack = lootitemfunction.apply(stack, context);
                } finally {
                    context.popVisitedElement(visitedentry);
                }

                return itemstack;
            } else {
                LOGGER.warn("Detected infinite loop in loot tables");
                return stack;
            }
        }
    }

    public static LootItemConditionalFunction.Builder<?> functionReference(ResourceKey<LootItemFunction> key) {
        return simpleBuilder(p_335336_ -> new FunctionReference(p_335336_, key));
    }
}
