package net.goui.cosmicdungeon.block.custom;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class BarrierBlock extends Block {
    public BarrierBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        if (FMLEnvironment.getDist() != Dist.CLIENT) {
            return RenderShape.MODEL;
        }

        return isLocalDeveloperClient() ? RenderShape.MODEL : RenderShape.INVISIBLE;
    }

    private static boolean isLocalDeveloperClient() {
        try {
            Class<?> minecraftClass = Class.forName("net.minecraft.client.Minecraft");
            Method getInstance = minecraftClass.getMethod("getInstance");
            Object minecraft = getInstance.invoke(null);
            if (minecraft == null) return false;

            Field playerField = minecraftClass.getField("player");
            Object player = playerField.get(minecraft);
            if (player == null) return false;

            Method hasPermissions = player.getClass().getMethod("hasPermissions", int.class);
            return Boolean.TRUE.equals(hasPermissions.invoke(player, 2));
        } catch (Throwable ignored) {
            return false;
        }
    }
}
