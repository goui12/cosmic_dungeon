package net.goui.cosmicdungeon.dungeon;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID)
public final class DungeonRespawnEvents {
    private DungeonRespawnEvents() {}

    @SubscribeEvent
    public static void onBedRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        if (DungeonLifecycleService.findActiveRunForPlayer(sp).isEmpty()) return;

        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof BedBlock)) return;

        DungeonLifecycleService.setPlayerRespawnTo(sp, level, pos, sp.getYRot(), sp.getXRot());
    }
}
