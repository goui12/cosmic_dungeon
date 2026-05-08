package net.minecraft.client.renderer.blockentity.state;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ShelfRenderState extends BlockEntityRenderState {
    public ItemStackRenderState[] items = new ItemStackRenderState[3];
    public boolean alignToBottom;
}
