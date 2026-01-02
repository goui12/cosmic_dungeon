package net.goui.cosmicdungeon.item.custom;

import net.goui.cosmicdungeon.region.RegionSelectionStore;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class RegionWandItem extends Item {

    public RegionWandItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        final Level level = context.getLevel();

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        final Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }

        final BlockPos pos = context.getClickedPos();
        final ResourceKey<Level> dim = level.dimension();

        var result = RegionSelectionStore.setPos2(player, dim, pos);

        if (result.ok()) {
            player.displayClientMessage(
                    Component.literal("Pos2 set: " + pos.getX() + " " + pos.getY() + " " + pos.getZ()
                                    + " (" + result.dimensionId() + ")")
                            .withStyle(ChatFormatting.GREEN),
                    false
            );
        } else {
            player.displayClientMessage(
                    Component.literal(result.message()).withStyle(ChatFormatting.RED),
                    false
            );
        }

        return InteractionResult.SUCCESS;
    }
}
