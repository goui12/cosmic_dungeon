package net.minecraft.world.ticks;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import it.unimi.dsi.fastutil.Hash.Strategy;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ChunkPos;

public record SavedTick<T>(T type, BlockPos pos, int delay, TickPriority priority) {
    public static final Strategy<SavedTick<?>> UNIQUE_TICK_HASH = new Strategy<SavedTick<?>>() {
        public int hashCode(SavedTick<?> savedTick) {
            return 31 * savedTick.pos().hashCode() + savedTick.type().hashCode();
        }

        public boolean equals(@Nullable SavedTick<?> first, @Nullable SavedTick<?> second) {
            if (first == second) {
                return true;
            } else {
                return first != null && second != null ? first.type() == second.type() && first.pos().equals(second.pos()) : false;
            }
        }
    };

    public static <T> Codec<SavedTick<T>> codec(Codec<T> codec) {
        MapCodec<BlockPos> mapcodec = RecordCodecBuilder.mapCodec(
            p_404625_ -> p_404625_.group(
                    Codec.INT.fieldOf("x").forGetter(Vec3i::getX), Codec.INT.fieldOf("y").forGetter(Vec3i::getY), Codec.INT.fieldOf("z").forGetter(Vec3i::getZ)
                )
                .apply(p_404625_, BlockPos::new)
        );
        return RecordCodecBuilder.create(
            p_404628_ -> p_404628_.group(
                    codec.fieldOf("i").forGetter(SavedTick::type),
                    mapcodec.forGetter(SavedTick::pos),
                    Codec.INT.fieldOf("t").forGetter(SavedTick::delay),
                    TickPriority.CODEC.fieldOf("p").forGetter(SavedTick::priority)
                )
                .apply(p_404628_, SavedTick::new)
        );
    }

    public static <T> List<SavedTick<T>> filterTickListForChunk(List<SavedTick<T>> tickList, ChunkPos chunkPos) {
        long i = chunkPos.toLong();
        return tickList.stream().filter(p_404630_ -> ChunkPos.asLong(p_404630_.pos()) == i).toList();
    }

    public ScheduledTick<T> unpack(long gameTime, long subTickOrder) {
        return new ScheduledTick<>(this.type, this.pos, gameTime + this.delay, this.priority, subTickOrder);
    }

    public static <T> SavedTick<T> probe(T type, BlockPos pos) {
        return new SavedTick<>(type, pos, 0, TickPriority.NORMAL);
    }
}
