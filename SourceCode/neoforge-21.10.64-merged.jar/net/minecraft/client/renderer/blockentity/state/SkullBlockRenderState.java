package net.minecraft.client.renderer.blockentity.state;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.SkullBlock;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SkullBlockRenderState extends BlockEntityRenderState {
    public float animationProgress;
    public Direction direction = Direction.NORTH;
    public float rotationDegrees;
    public SkullBlock.Type skullType = SkullBlock.Types.ZOMBIE;
    public RenderType renderType;
}
