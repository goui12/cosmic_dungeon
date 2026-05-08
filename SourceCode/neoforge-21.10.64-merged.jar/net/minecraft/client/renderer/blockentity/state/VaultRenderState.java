package net.minecraft.client.renderer.blockentity.state;

import javax.annotation.Nullable;
import net.minecraft.client.renderer.entity.state.ItemClusterRenderState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class VaultRenderState extends BlockEntityRenderState {
    @Nullable
    public ItemClusterRenderState displayItem;
    public float spin;
}
