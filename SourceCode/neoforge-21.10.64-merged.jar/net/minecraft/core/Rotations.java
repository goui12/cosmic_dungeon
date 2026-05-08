package net.minecraft.core;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.network.codec.StreamCodec;

/**
 * @param x Rotation on the X axis
 * @param y Rotation on the Y axis
 * @param z Rotation on the Z axis
 */
public record Rotations(float x, float y, float z) {
    public static final Codec<Rotations> CODEC = Codec.FLOAT
        .listOf()
        .comapFlatMap(
            p_405461_ -> Util.fixedSize((List<Float>)p_405461_, 3).map(p_404764_ -> new Rotations(p_404764_.get(0), p_404764_.get(1), p_404764_.get(2))),
            p_405231_ -> List.of(p_405231_.x(), p_405231_.y(), p_405231_.z())
        );
    public static final StreamCodec<ByteBuf, Rotations> STREAM_CODEC = new StreamCodec<ByteBuf, Rotations>() {
        public Rotations decode(ByteBuf p_320504_) {
            return new Rotations(p_320504_.readFloat(), p_320504_.readFloat(), p_320504_.readFloat());
        }

        public void encode(ByteBuf p_320561_, Rotations p_320041_) {
            p_320561_.writeFloat(p_320041_.x);
            p_320561_.writeFloat(p_320041_.y);
            p_320561_.writeFloat(p_320041_.z);
        }
    };

    public Rotations(float x, float y, float z) {
        x = !Float.isInfinite(x) && !Float.isNaN(x) ? x % 360.0F : 0.0F;
        y = !Float.isInfinite(y) && !Float.isNaN(y) ? y % 360.0F : 0.0F;
        z = !Float.isInfinite(z) && !Float.isNaN(z) ? z % 360.0F : 0.0F;
        this.x = x;
        this.y = y;
        this.z = z;
    }
}
