// file: src/main/java/net/goui/cosmicdungeon/block/custom/ClassSelectorBlock.java
package net.goui.cosmicdungeon.block.custom;

import net.goui.cosmicdungeon.menu.ClassSelectorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class ClassSelectorBlock extends Block {

    public ClassSelectorBlock(Properties props) {
        super(props);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        // Client says "handled" so the hand anim plays; server does the real menu open.
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (!(player instanceof ServerPlayer sp)) {
            return InteractionResult.PASS;
        }

        MenuProvider provider = new SimpleMenuProvider(
                (containerId, inv, p) -> new ClassSelectorMenu(containerId, inv),
                Component.translatable("menu.cosmicdungeon.class_selector")
        );

        sp.openMenu(provider);
        return InteractionResult.CONSUME;
    }
}
