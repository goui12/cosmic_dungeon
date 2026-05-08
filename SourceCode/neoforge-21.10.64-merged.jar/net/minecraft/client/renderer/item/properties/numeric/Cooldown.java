package net.minecraft.client.renderer.item.properties.numeric;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record Cooldown() implements RangeSelectItemModelProperty {
    public static final MapCodec<Cooldown> MAP_CODEC = MapCodec.unit(new Cooldown());

    @Override
    public float get(ItemStack p_387823_, @Nullable ClientLevel p_386825_, @Nullable ItemOwner p_433929_, int p_387909_) {
        return p_433929_ != null && p_433929_.asLivingEntity() instanceof Player player ? player.getCooldowns().getCooldownPercent(p_387823_, 0.0F) : 0.0F;
    }

    @Override
    public MapCodec<Cooldown> type() {
        return MAP_CODEC;
    }
}
