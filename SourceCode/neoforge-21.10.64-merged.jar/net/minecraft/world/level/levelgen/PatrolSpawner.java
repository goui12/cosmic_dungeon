package net.minecraft.world.level.levelgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.PatrollingMonster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;

public class PatrolSpawner implements CustomSpawner {
    private int nextTick;

    @Override
    public void tick(ServerLevel level, boolean spawnEnemies) {
        if (spawnEnemies) {
            if (level.getGameRules().getBoolean(GameRules.RULE_DO_PATROL_SPAWNING)) {
                RandomSource randomsource = level.random;
                this.nextTick--;
                if (this.nextTick <= 0) {
                    this.nextTick = this.nextTick + 12000 + randomsource.nextInt(1200);
                    long i = level.getDayTime() / 24000L;
                    if (i >= 5L && level.isBrightOutside()) {
                        if (randomsource.nextInt(5) == 0) {
                            int j = level.players().size();
                            if (j >= 1) {
                                Player player = level.players().get(randomsource.nextInt(j));
                                if (!player.isSpectator()) {
                                    if (!level.isCloseToVillage(player.blockPosition(), 2)) {
                                        int k = (24 + randomsource.nextInt(24)) * (randomsource.nextBoolean() ? -1 : 1);
                                        int l = (24 + randomsource.nextInt(24)) * (randomsource.nextBoolean() ? -1 : 1);
                                        BlockPos.MutableBlockPos blockpos$mutableblockpos = player.blockPosition().mutable().move(k, 0, l);
                                        int i1 = 10;
                                        if (level.hasChunksAt(
                                            blockpos$mutableblockpos.getX() - 10,
                                            blockpos$mutableblockpos.getZ() - 10,
                                            blockpos$mutableblockpos.getX() + 10,
                                            blockpos$mutableblockpos.getZ() + 10
                                        )) {
                                            Holder<Biome> holder = level.getBiome(blockpos$mutableblockpos);
                                            if (!holder.is(BiomeTags.WITHOUT_PATROL_SPAWNS)) {
                                                int j1 = (int)Math.ceil(level.getCurrentDifficultyAt(blockpos$mutableblockpos).getEffectiveDifficulty()) + 1;

                                                for (int k1 = 0; k1 < j1; k1++) {
                                                    blockpos$mutableblockpos.setY(
                                                        level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, blockpos$mutableblockpos).getY()
                                                    );
                                                    if (k1 == 0) {
                                                        if (!this.spawnPatrolMember(level, blockpos$mutableblockpos, randomsource, true)) {
                                                            break;
                                                        }
                                                    } else {
                                                        this.spawnPatrolMember(level, blockpos$mutableblockpos, randomsource, false);
                                                    }

                                                    blockpos$mutableblockpos.setX(
                                                        blockpos$mutableblockpos.getX() + randomsource.nextInt(5) - randomsource.nextInt(5)
                                                    );
                                                    blockpos$mutableblockpos.setZ(
                                                        blockpos$mutableblockpos.getZ() + randomsource.nextInt(5) - randomsource.nextInt(5)
                                                    );
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean spawnPatrolMember(ServerLevel level, BlockPos pos, RandomSource random, boolean leader) {
        BlockState blockstate = level.getBlockState(pos);
        if (!NaturalSpawner.isValidEmptySpawnBlock(level, pos, blockstate, blockstate.getFluidState(), EntityType.PILLAGER)) {
            return false;
        } else if (!PatrollingMonster.checkPatrollingMonsterSpawnRules(EntityType.PILLAGER, level, EntitySpawnReason.PATROL, pos, random)) {
            return false;
        } else {
            PatrollingMonster patrollingmonster = EntityType.PILLAGER.create(level, EntitySpawnReason.PATROL);
            if (patrollingmonster != null) {
                if (leader) {
                    patrollingmonster.setPatrolLeader(true);
                    patrollingmonster.findPatrolTarget();
                }

                patrollingmonster.setPos(pos.getX(), pos.getY(), pos.getZ());
                patrollingmonster.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.PATROL, null);
                level.addFreshEntityWithPassengers(patrollingmonster);
                return true;
            } else {
                return false;
            }
        }
    }
}
