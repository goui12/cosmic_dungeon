package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Instrument;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.InstrumentComponent;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class SetInstrumentFunction extends LootItemConditionalFunction {
    public static final MapCodec<SetInstrumentFunction> CODEC = RecordCodecBuilder.mapCodec(
        p_298123_ -> commonFields(p_298123_)
            .and(TagKey.hashedCodec(Registries.INSTRUMENT).fieldOf("options").forGetter(p_298122_ -> p_298122_.options))
            .apply(p_298123_, SetInstrumentFunction::new)
    );
    private final TagKey<Instrument> options;

    private SetInstrumentFunction(List<LootItemCondition> conditions, TagKey<Instrument> options) {
        super(conditions);
        this.options = options;
    }

    @Override
    public LootItemFunctionType<SetInstrumentFunction> getType() {
        return LootItemFunctions.SET_INSTRUMENT;
    }

    /**
     * Called to perform the actual action of this function, after conditions have been checked.
     */
    @Override
    public ItemStack run(ItemStack stack, LootContext context) {
        Registry<Instrument> registry = context.getLevel().registryAccess().lookupOrThrow(Registries.INSTRUMENT);
        Optional<Holder<Instrument>> optional = registry.getRandomElementOf(this.options, context.getRandom());
        if (optional.isPresent()) {
            stack.set(DataComponents.INSTRUMENT, new InstrumentComponent(optional.get()));
        }

        return stack;
    }

    public static LootItemConditionalFunction.Builder<?> setInstrumentOptions(TagKey<Instrument> instrumentOptions) {
        return simpleBuilder(p_298125_ -> new SetInstrumentFunction(p_298125_, instrumentOptions));
    }
}
