package net.minecraft.client.renderer.blockentity.state;

import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.level.block.entity.PotDecorations;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DecoratedPotRenderState extends BlockEntityRenderState {
    public float yRot;
    @Nullable
    public DecoratedPotBlockEntity.WobbleStyle wobbleStyle;
    public float wobbleProgress;
    public PotDecorations decorations = PotDecorations.EMPTY;
    public Direction direction = Direction.NORTH;
}
