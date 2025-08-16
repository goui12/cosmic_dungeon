package net.goui.cosmicdungeon.client.tint;

import com.mojang.serialization.MapCodec;
import net.goui.cosmicdungeon.common.color.AmethystColor;
import net.goui.cosmicdungeon.component.ModDataComponents;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public record AmethystTintSource(int defaultColor) implements ItemTintSource {
    // JSON: { "type": "cosmicdungeon:amethyst_color", "default": <rgb int> }
    public static final MapCodec<AmethystTintSource> CODEC =
            ExtraCodecs.RGB_COLOR_CODEC.fieldOf("default")
                    .xmap(AmethystTintSource::new, AmethystTintSource::defaultColor);

    @Override
    public int calculate(ItemStack stack, ClientLevel level, LivingEntity entity) {
        var c = stack.get(ModDataComponents.AMETHYST_COLOR.get());
        return (c != null ? c.argb : AmethystColor.PURPLE.argb); // return ARGB
    }

    @Override
    public MapCodec<? extends ItemTintSource> type() {
        return CODEC;
    }
}
