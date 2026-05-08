package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.Set;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.Nameable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * LootItemFunction that sets the stack's name by copying it from somewhere else, such as the killing player.
 */
public class CopyNameFunction extends LootItemConditionalFunction {
    private static final ExtraCodecs.LateBoundIdMapper<String, CopyNameFunction.Source> SOURCES = new ExtraCodecs.LateBoundIdMapper<>();
    public static final MapCodec<CopyNameFunction> CODEC;
    private final CopyNameFunction.Source source;

    private CopyNameFunction(List<LootItemCondition> predicates, CopyNameFunction.Source source) {
        super(predicates);
        this.source = source;
    }

    @Override
    public LootItemFunctionType<CopyNameFunction> getType() {
        return LootItemFunctions.COPY_NAME;
    }

    @Override
    public Set<ContextKey<?>> getReferencedContextParams() {
        return Set.of(this.source.param);
    }

    /**
     * Called to perform the actual action of this function, after conditions have been checked.
     */
    @Override
    public ItemStack run(ItemStack stack, LootContext context) {
        if (context.getOptionalParameter(this.source.param) instanceof Nameable nameable) {
            stack.set(DataComponents.CUSTOM_NAME, nameable.getCustomName());
        }

        return stack;
    }

    public static LootItemConditionalFunction.Builder<?> copyName(CopyNameFunction.Source source) {
        return simpleBuilder(p_450909_ -> new CopyNameFunction(p_450909_, source));
    }

    static {
        for (LootContext.EntityTarget lootcontext$entitytarget : LootContext.EntityTarget.values()) {
            SOURCES.put(lootcontext$entitytarget.getSerializedName(), new CopyNameFunction.Source(lootcontext$entitytarget.getParam()));
        }

        for (LootContext.BlockEntityTarget lootcontext$blockentitytarget : LootContext.BlockEntityTarget.values()) {
            SOURCES.put(lootcontext$blockentitytarget.getSerializedName(), new CopyNameFunction.Source(lootcontext$blockentitytarget.getParam()));
        }

        CODEC = RecordCodecBuilder.mapCodec(
            p_450911_ -> commonFields(p_450911_)
                .and(SOURCES.codec(Codec.STRING).fieldOf("source").forGetter(p_450910_ -> p_450910_.source))
                .apply(p_450911_, CopyNameFunction::new)
        );
    }

    public record Source(ContextKey<?> param) {
    }
}
