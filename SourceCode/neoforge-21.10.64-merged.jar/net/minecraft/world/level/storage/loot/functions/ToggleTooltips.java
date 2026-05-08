package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class ToggleTooltips extends LootItemConditionalFunction {
    public static final MapCodec<ToggleTooltips> CODEC = RecordCodecBuilder.mapCodec(
        p_399463_ -> commonFields(p_399463_)
            .and(Codec.unboundedMap(DataComponentType.CODEC, Codec.BOOL).fieldOf("toggles").forGetter(p_335699_ -> p_335699_.values))
            .apply(p_399463_, ToggleTooltips::new)
    );
    private final Map<DataComponentType<?>, Boolean> values;

    public ToggleTooltips(List<LootItemCondition> conditions, Map<DataComponentType<?>, Boolean> values) {
        super(conditions);
        this.values = values;
    }

    /**
     * Called to perform the actual action of this function, after conditions have been checked.
     */
    @Override
    protected ItemStack run(ItemStack stack, LootContext context) {
        stack.update(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT, p_399462_ -> {
            for (Entry<DataComponentType<?>, Boolean> entry : this.values.entrySet()) {
                boolean flag = entry.getValue();
                p_399462_ = p_399462_.withHidden(entry.getKey(), !flag);
            }

            return p_399462_;
        });
        return stack;
    }

    @Override
    public LootItemFunctionType<ToggleTooltips> getType() {
        return LootItemFunctions.TOGGLE_TOOLTIPS;
    }
}
