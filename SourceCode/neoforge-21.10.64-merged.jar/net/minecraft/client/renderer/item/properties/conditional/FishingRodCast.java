package net.minecraft.client.renderer.item.properties.conditional;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.FishingHookRenderer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record FishingRodCast() implements ConditionalItemModelProperty {
    public static final MapCodec<FishingRodCast> MAP_CODEC = MapCodec.unit(new FishingRodCast());

    @Override
    public boolean get(ItemStack p_388347_, @Nullable ClientLevel p_388341_, @Nullable LivingEntity p_386847_, int p_386531_, ItemDisplayContext p_389619_) {
        if (p_386847_ instanceof Player player && player.fishing != null) {
            HumanoidArm humanoidarm = FishingHookRenderer.getHoldingArm(player);
            return p_386847_.getItemHeldByArm(humanoidarm) == p_388347_;
        } else {
            return false;
        }
    }

    @Override
    public MapCodec<FishingRodCast> type() {
        return MAP_CODEC;
    }
}
