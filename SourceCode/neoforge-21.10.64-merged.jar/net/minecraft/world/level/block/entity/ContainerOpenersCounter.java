package net.minecraft.world.level.block.entity;

import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;

public abstract class ContainerOpenersCounter {
    private static final int CHECK_TICK_DELAY = 5;
    private int openCount;
    private double maxInteractionRange;

    protected abstract void onOpen(Level level, BlockPos pos, BlockState state);

    protected abstract void onClose(Level level, BlockPos pos, BlockState state);

    protected abstract void openerCountChanged(Level level, BlockPos pos, BlockState state, int count, int openCount);

    public abstract boolean isOwnContainer(Player player);

    public void incrementOpeners(LivingEntity entity, Level level, BlockPos pos, BlockState state, double maxInteractionRange) {
        int i = this.openCount++;
        if (i == 0) {
            this.onOpen(level, pos, state);
            level.gameEvent(entity, GameEvent.CONTAINER_OPEN, pos);
            scheduleRecheck(level, pos, state);
        }

        this.openerCountChanged(level, pos, state, i, this.openCount);
        this.maxInteractionRange = Math.max(maxInteractionRange, this.maxInteractionRange);
    }

    public void decrementOpeners(LivingEntity entity, Level level, BlockPos pos, BlockState state) {
        int i = this.openCount--;
        if (this.openCount == 0) {
            this.onClose(level, pos, state);
            level.gameEvent(entity, GameEvent.CONTAINER_CLOSE, pos);
            this.maxInteractionRange = 0.0;
        }

        this.openerCountChanged(level, pos, state, i, this.openCount);
    }

    public List<ContainerUser> getEntitiesWithContainerOpen(Level level, BlockPos pos) {
        double d0 = this.maxInteractionRange + 4.0;
        AABB aabb = new AABB(pos).inflate(d0);
        return level.getEntities((Entity)null, aabb, p_435398_ -> this.hasContainerOpen(p_435398_, pos))
            .stream()
            .map(p_433993_ -> (ContainerUser)p_433993_)
            .collect(Collectors.toList());
    }

    private boolean hasContainerOpen(Entity entity, BlockPos pos) {
        return entity instanceof ContainerUser containeruser && !containeruser.getLivingEntity().isSpectator()
            ? containeruser.hasContainerOpen(this, pos)
            : false;
    }

    public void recheckOpeners(Level level, BlockPos pos, BlockState state) {
        List<ContainerUser> list = this.getEntitiesWithContainerOpen(level, pos);
        this.maxInteractionRange = 0.0;

        for (ContainerUser containeruser : list) {
            this.maxInteractionRange = Math.max(containeruser.getContainerInteractionRange(), this.maxInteractionRange);
        }

        int i = list.size();
        int j = this.openCount;
        if (j != i) {
            boolean flag = i != 0;
            boolean flag1 = j != 0;
            if (flag && !flag1) {
                this.onOpen(level, pos, state);
                level.gameEvent(null, GameEvent.CONTAINER_OPEN, pos);
            } else if (!flag) {
                this.onClose(level, pos, state);
                level.gameEvent(null, GameEvent.CONTAINER_CLOSE, pos);
            }

            this.openCount = i;
        }

        this.openerCountChanged(level, pos, state, j, i);
        if (i > 0) {
            scheduleRecheck(level, pos, state);
        }
    }

    public int getOpenerCount() {
        return this.openCount;
    }

    private static void scheduleRecheck(Level level, BlockPos pos, BlockState state) {
        level.scheduleTick(pos, state.getBlock(), 5);
    }
}
