package net.goui.cosmicdungeon.block.custom;

import net.goui.cosmicdungeon.block.ModBlocks;
import net.goui.cosmicdungeon.item.ModItems;
import net.goui.cosmicdungeon.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

public class ChickenBlock extends Block {

    private static final SoundEvent[] CHICKEN_SOUNDS = new SoundEvent[]{
            ModSounds.CHICKEN_SOUND_1.get(),
            ModSounds.CHICKEN_SOUND_2.get(),
            ModSounds.CHICKEN_SOUND_3.get(),
            ModSounds.CHICKEN_SOUND_4.get(),
            ModSounds.CHICKEN_SOUND_5.get(),
            ModSounds.CHICKEN_SOUND_6.get(),
            ModSounds.CHICKEN_SOUND_7.get()
    };

    public ChickenBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state,
                              @Nullable BlockEntity blockEntity, ItemStack tool) {
        super.playerDestroy(level, player, pos, state, blockEntity, tool);

        if (!level.isClientSide) {
            SoundEvent randomSound = CHICKEN_SOUNDS[level.random.nextInt(CHICKEN_SOUNDS.length)];
            level.playSound(null, pos, randomSound, SoundSource.BLOCKS, 1.0f, 1.0f);
        }
    }
    @Override
    public void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (!level.isClientSide && player.getMainHandItem().is(ModItems.BROODING_FORK.get())) {
            destroyConnectedChickenBlocks(level, pos, player);
        }

        super.attack(state, level, pos, player);
    }

    private void destroyConnectedChickenBlocks(Level level, BlockPos startPos, Player player) {
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> toVisit = new ArrayDeque<>();
        toVisit.add(startPos);

        while (!toVisit.isEmpty()) {
            BlockPos current = toVisit.poll();
            if (visited.contains(current)) continue;
            visited.add(current);

            BlockState state = level.getBlockState(current);
            if (!state.is(ModBlocks.CHICKEN_BLOCK.get())) continue;

            level.destroyBlock(current, true, player); // break block with drops and animation

            for (Direction dir : Direction.values()) {
                BlockPos neighbor = current.relative(dir);
                if (!visited.contains(neighbor)) {
                    toVisit.add(neighbor);
                }
            }
        }
    }



}
