package net.minecraft.client.renderer.item.properties.conditional;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record IsViewEntity() implements ConditionalItemModelProperty {
    public static final MapCodec<IsViewEntity> MAP_CODEC = MapCodec.unit(new IsViewEntity());

    @Override
    public boolean get(ItemStack p_390357_, @Nullable ClientLevel p_390458_, @Nullable LivingEntity p_390497_, int p_390451_, ItemDisplayContext p_390466_) {
        Minecraft minecraft = Minecraft.getInstance();
        Entity entity = minecraft.getCameraEntity();
        return entity != null ? p_390497_ == entity : p_390497_ == minecraft.player;
    }

    @Override
    public MapCodec<IsViewEntity> type() {
        return MAP_CODEC;
    }
}
