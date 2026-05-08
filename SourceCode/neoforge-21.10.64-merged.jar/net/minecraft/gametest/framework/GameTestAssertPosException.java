package net.minecraft.gametest.framework;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public class GameTestAssertPosException extends GameTestAssertException {
    private final BlockPos absolutePos;
    private final BlockPos relativePos;

    public GameTestAssertPosException(Component message, BlockPos absolutePos, BlockPos relativePos, int tick) {
        super(message, tick);
        this.absolutePos = absolutePos;
        this.relativePos = relativePos;
    }

    @Override
    public Component getDescription() {
        return Component.translatable(
            "test.error.position",
            this.message,
            this.absolutePos.getX(),
            this.absolutePos.getY(),
            this.absolutePos.getZ(),
            this.relativePos.getX(),
            this.relativePos.getY(),
            this.relativePos.getZ(),
            this.tick
        );
    }

    public Component getMessageToShowAtBlock() {
        return this.message;
    }

    @Nullable
    public BlockPos getRelativePos() {
        return this.relativePos;
    }

    @Nullable
    public BlockPos getAbsolutePos() {
        return this.absolutePos;
    }
}
