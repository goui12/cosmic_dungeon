package net.minecraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class LpVec3 {
    private static final int DATA_BITS = 15;
    private static final int DATA_BITS_MASK = 32767;
    private static final double MAX_QUANTIZED_VALUE = 32766.0;
    private static final int SCALE_BITS = 2;
    private static final int SCALE_BITS_MASK = 3;
    private static final int CONTINUATION_FLAG = 4;
    private static final int X_OFFSET = 3;
    private static final int Y_OFFSET = 18;
    private static final int Z_OFFSET = 33;
    public static final double ABS_MAX_VALUE = 1.7179869183E10;
    public static final double ABS_MIN_VALUE = 3.051944088384301E-5;

    public static boolean hasContinuationBit(int data) {
        return (data & 4) == 4;
    }

    public static Vec3 read(ByteBuf buffer) {
        int i = buffer.readUnsignedByte();
        if (i == 0) {
            return Vec3.ZERO;
        } else {
            int j = buffer.readUnsignedByte();
            long k = buffer.readUnsignedInt();
            long l = k << 16 | j << 8 | i;
            long i1 = i & 3;
            if (hasContinuationBit(i)) {
                i1 |= (VarInt.read(buffer) & 4294967295L) << 2;
            }

            return new Vec3(unpack(l >> 3) * i1, unpack(l >> 18) * i1, unpack(l >> 33) * i1);
        }
    }

    public static void write(ByteBuf buffer, Vec3 vector) {
        double d0 = sanitize(vector.x);
        double d1 = sanitize(vector.y);
        double d2 = sanitize(vector.z);
        double d3 = Mth.absMax(d0, Mth.absMax(d1, d2));
        if (d3 < 3.051944088384301E-5) {
            buffer.writeByte(0);
        } else {
            long i = Mth.ceilLong(d3);
            boolean flag = (i & 3L) != i;
            long j = flag ? i & 3L | 4L : i;
            long k = pack(d0 / i) << 3;
            long l = pack(d1 / i) << 18;
            long i1 = pack(d2 / i) << 33;
            long j1 = j | k | l | i1;
            buffer.writeByte((byte)j1);
            buffer.writeByte((byte)(j1 >> 8));
            buffer.writeInt((int)(j1 >> 16));
            if (flag) {
                VarInt.write(buffer, (int)(i >> 2));
            }
        }
    }

    private static double sanitize(double value) {
        return Double.isNaN(value) ? 0.0 : Math.clamp(value, -1.7179869183E10, 1.7179869183E10);
    }

    private static long pack(double coord) {
        return Math.round((coord * 0.5 + 0.5) * 32766.0);
    }

    private static double unpack(long coord) {
        return Math.min((double)(coord & 32767L), 32766.0) * 2.0 / 32766.0 - 1.0;
    }
}
