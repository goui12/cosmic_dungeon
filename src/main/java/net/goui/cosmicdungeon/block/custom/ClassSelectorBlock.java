// file: src/main/java/net/goui/cosmicdungeon/block/custom/ClassSelectorBlock.java
package net.goui.cosmicdungeon.block.custom;

import net.goui.cosmicdungeon.auth.AccessPolicy;
import net.goui.cosmicdungeon.block.entity.ClassSelectorBlockEntity;
import net.goui.cosmicdungeon.menu.ClassSelectorMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

public class ClassSelectorBlock extends Block implements EntityBlock {

    public ClassSelectorBlock(Properties props) {
        super(props);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ClassSelectorBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
        if (!(level instanceof ServerLevel sl)) return InteractionResult.PASS;

        if (AccessPolicy.isDeveloper(sp)) {
            sendDevMenu(sp, pos);
            return InteractionResult.CONSUME;
        }

        // Dungeoneer: open normal menu and remember which selector it came from
        ClassSelectorTeleportUtil.markPendingSelectorSource(sp, sl, pos);

        MenuProvider provider = new SimpleMenuProvider(
                (containerId, inv, p) -> new ClassSelectorMenu(containerId, inv),
                Component.translatable("menu.cosmicdungeon.class_selector")
        );
        sp.openMenu(provider);
        return InteractionResult.CONSUME;
    }

    private static void sendDevMenu(ServerPlayer dev, BlockPos selectorPos) {
        dev.sendSystemMessage(
                Component.literal("Class Selector @ ").withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(selectorPos.toShortString()).withStyle(ChatFormatting.AQUA))
        );

        String p = selectorPos.getX() + " " + selectorPos.getY() + " " + selectorPos.getZ();

        dev.sendSystemMessage(
                Component.literal("[Rift Destination]")
                        .withStyle(Style.EMPTY.withColor(ChatFormatting.AQUA).withUnderlined(true)
                                .withClickEvent(new ClickEvent.RunCommand("/classselector ui dest " + p)))
        );

        dev.sendSystemMessage(
                Component.literal("[Configure Dungeon Player Count]")
                        .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW).withUnderlined(true)
                                .withClickEvent(new ClickEvent.RunCommand("/classselector ui players " + p)))
        );
    }
}
