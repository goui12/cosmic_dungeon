package net.minecraft.client.renderer.item.properties.conditional;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record IsCarried() implements ConditionalItemModelProperty {
    public static final MapCodec<IsCarried> MAP_CODEC = MapCodec.unit(new IsCarried());

    @Override
    public boolean get(ItemStack p_387247_, @Nullable ClientLevel p_387933_, @Nullable LivingEntity p_388427_, int p_386998_, ItemDisplayContext p_389534_) {
        return p_388427_ instanceof LocalPlayer localplayer && localplayer.containerMenu.getCarried() == p_387247_;
    }

    @Override
    public MapCodec<IsCarried> type() {
        return MAP_CODEC;
    }
}
