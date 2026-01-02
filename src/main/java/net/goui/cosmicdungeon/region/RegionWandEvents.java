package net.goui.cosmicdungeon.region;

import net.goui.cosmicdungeon.item.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class RegionWandEvents {

    @SubscribeEvent
    public void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START) {
            return;
        }

        final Level level = event.getLevel();
        if (level.isClientSide()) {
            return;
        }

        final Player player = event.getEntity();
        final ItemStack held = player.getMainHandItem();

        if (held.isEmpty() || held.getItem() != ModItems.REGION_WAND.get()) {
            return;
        }

        // Don't let this left click start breaking the block
        event.setCanceled(true);

        final ResourceKey<Level> dim = level.dimension();
        final var pos = event.getPos();

        var result = RegionSelectionStore.setPos1(player, dim, pos);
        if (result.ok()) {
            player.displayClientMessage(
                    Component.literal("Pos1 set: " + pos.getX() + " " + pos.getY() + " " + pos.getZ()
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
    }

    /**
     * Extra safety: if the game tries to continue "mining" anyway, block it.
     */
    @SubscribeEvent
    public void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        final Player player = event.getEntity();
        final Level level = player.level();
        if (level.isClientSide()) {
            return;
        }

        final ItemStack held = player.getMainHandItem();
        if (held.isEmpty() || held.getItem() != ModItems.REGION_WAND.get()) {
            return;
        }

        // Make block breaking effectively impossible while holding the wand
        event.setNewSpeed(0.0F);
    }
}
