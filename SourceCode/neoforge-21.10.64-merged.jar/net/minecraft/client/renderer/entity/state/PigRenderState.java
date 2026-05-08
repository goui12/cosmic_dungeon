package net.minecraft.client.renderer.entity.state;

import javax.annotation.Nullable;
import net.minecraft.world.entity.animal.PigVariant;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PigRenderState extends LivingEntityRenderState {
    public ItemStack saddle = ItemStack.EMPTY;
    @Nullable
    public PigVariant variant;
}
