package net.goui.cosmicdungeon.block.custom;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

public class BarrierBlock extends Block {
    public BarrierBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return RenderShape.MODEL;
        }

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            return RenderShape.INVISIBLE;
        }

        return player.hasPermissions(2) ? RenderShape.MODEL : RenderShape.INVISIBLE;
    }
}
