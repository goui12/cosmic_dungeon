package net.goui.cosmicdungeon.door;

import net.goui.cosmicdungeon.component.ModDataComponents;
import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.UUID;

/**
 * Enforces door locks:
 * - Blocks opening locked vanilla doors.
 * - If player holds a matching key, CONSUMES 1 key, UNLOCKS the door, shows a message,
 *   and allows the interaction to proceed (door opens).
 */
@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID)
public final class DoorLockHandler {
    private DoorLockHandler() {}

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (level.isClientSide()) return;

        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof DoorBlock)) return;

        // Normalize to LOWER half
        if (state.getOptionalValue(DoorBlock.HALF).orElse(DoubleBlockHalf.LOWER) == DoubleBlockHalf.UPPER) {
            pos = pos.below();
        }

        DoorLockData data = DoorLockData.get(level);
        DoorLockData.LockInfo info = data.getLock(level, pos);
        if (info == null) return; // not locked

        // Check both hands for a matching key
        UUID need = info.lockId;
        InteractionHand matchingHand = null;

        if (matchesKey(event.getItemStack(), need)) {
            matchingHand = event.getHand(); // the hand used for the click
        } else if (matchesKey(event.getEntity().getOffhandItem(), need)) {
            matchingHand = InteractionHand.OFF_HAND;
        }

        if (matchingHand != null) {
            // Consume ONE key from that hand
            ItemStack stack = matchingHand == InteractionHand.MAIN_HAND
                    ? event.getEntity().getMainHandItem()
                    : event.getEntity().getOffhandItem();
            stack.shrink(1);

            // Permanently unlock
            data.unlock(level, pos);

            // Feedback
            event.getEntity().displayClientMessage(Component.literal("You've unlocked a door."), true);

            // Let the default interaction proceed (door will open)
            event.setCancellationResult(InteractionResult.PASS);
            // Do NOT cancel — allow vanilla/open logic to run
            return;
        }

        // No matching key -> block opening
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
        event.getEntity().displayClientMessage(Component.literal("This door is locked."), true);
    }

    private static boolean matchesKey(ItemStack stack, UUID need) {
        if (stack.isEmpty() || need == null) return false;
        UUID have = stack.get(ModDataComponents.DOOR_LOCK_ID.get());
        return need.equals(have);
    }
}
