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
public record IsSelected() implements ConditionalItemModelProperty {
    public static final MapCodec<IsSelected> MAP_CODEC = MapCodec.unit(new IsSelected());

    @Override
    public boolean get(ItemStack p_387724_, @Nullable ClientLevel p_387118_, @Nullable LivingEntity p_387251_, int p_388905_, ItemDisplayContext p_389631_) {
        return p_387251_ instanceof LocalPlayer localplayer && localplayer.getInventory().getSelectedItem() == p_387724_;
    }

    @Override
    public MapCodec<IsSelected> type() {
        return MAP_CODEC;
    }
}
