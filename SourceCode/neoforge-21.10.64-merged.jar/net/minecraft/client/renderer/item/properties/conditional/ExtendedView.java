package net.minecraft.client.renderer.item.properties.conditional;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record ExtendedView() implements ConditionalItemModelProperty {
    public static final MapCodec<ExtendedView> MAP_CODEC = MapCodec.unit(new ExtendedView());

    @Override
    public boolean get(ItemStack p_389501_, @Nullable ClientLevel p_389409_, @Nullable LivingEntity p_389583_, int p_389502_, ItemDisplayContext p_389679_) {
        return p_389679_ == ItemDisplayContext.GUI && Minecraft.getInstance().hasShiftDown();
    }

    @Override
    public MapCodec<ExtendedView> type() {
        return MAP_CODEC;
    }
}
