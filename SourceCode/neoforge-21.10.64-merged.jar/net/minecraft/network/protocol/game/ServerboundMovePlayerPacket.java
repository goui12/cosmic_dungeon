package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.phys.Vec3;

public abstract class ServerboundMovePlayerPacket implements Packet<ServerGamePacketListener> {
    private static final int FLAG_ON_GROUND = 1;
    private static final int FLAG_HORIZONTAL_COLLISION = 2;
    protected final double x;
    protected final double y;
    protected final double z;
    protected final float yRot;
    protected final float xRot;
    protected final boolean onGround;
    protected final boolean horizontalCollision;
    protected final boolean hasPos;
    protected final boolean hasRot;

    static int packFlags(boolean onGround, boolean horizontalCollision) {
        int i = 0;
        if (onGround) {
            i |= 1;
        }

        if (horizontalCollision) {
            i |= 2;
        }

        return i;
    }

    static boolean unpackOnGround(int flags) {
        return (flags & 1) != 0;
    }

    static boolean unpackHorizontalCollision(int flags) {
        return (flags & 2) != 0;
    }

    protected ServerboundMovePlayerPacket(
        double x,
        double y,
        double z,
        float yRot,
        float xRot,
        boolean onGround,
        boolean horizontalCollision,
        boolean hasPos,
        boolean hasRot
    ) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yRot = yRot;
        this.xRot = xRot;
        this.onGround = onGround;
        this.horizontalCollision = horizontalCollision;
        this.hasPos = hasPos;
        this.hasRot = hasRot;
    }

    @Override
    public abstract PacketType<? extends ServerboundMovePlayerPacket> type();

    /**
     * Passes this Packet on to the NetHandler for processing.
     */
    public void handle(ServerGamePacketListener handler) {
        handler.handleMovePlayer(this);
    }

    public double getX(double defaultValue) {
        return this.hasPos ? this.x : defaultValue;
    }

    public double getY(double defaultValue) {
        return this.hasPos ? this.y : defaultValue;
    }

    public double getZ(double defaultValue) {
        return this.hasPos ? this.z : defaultValue;
    }

    public float getYRot(float defaultValue) {
        return this.hasRot ? this.yRot : defaultValue;
    }

    public float getXRot(float defaultValue) {
        return this.hasRot ? this.xRot : defaultValue;
    }

    public boolean isOnGround() {
        return this.onGround;
    }

    public boolean horizontalCollision() {
        return this.horizontalCollision;
    }

    public boolean hasPosition() {
        return this.hasPos;
    }

    public boolean hasRotation() {
        return this.hasRot;
    }

    public static class Pos extends ServerboundMovePlayerPacket {
        public static final StreamCodec<FriendlyByteBuf, ServerboundMovePlayerPacket.Pos> STREAM_CODEC = Packet.codec(
            ServerboundMovePlayerPacket.Pos::write, ServerboundMovePlayerPacket.Pos::read
        );

        public Pos(Vec3 pos, boolean onGround, boolean horizontalCollision) {
            super(pos.x, pos.y, pos.z, 0.0F, 0.0F, onGround, horizontalCollision, true, false);
        }

        public Pos(double x, double y, double z, boolean onGround, boolean horizontalCollision) {
            super(x, y, z, 0.0F, 0.0F, onGround, horizontalCollision, true, false);
        }

        private static ServerboundMovePlayerPacket.Pos read(FriendlyByteBuf buffer) {
            double d0 = buffer.readDouble();
            double d1 = buffer.readDouble();
            double d2 = buffer.readDouble();
            short short1 = buffer.readUnsignedByte();
            boolean flag = ServerboundMovePlayerPacket.unpackOnGround(short1);
            boolean flag1 = ServerboundMovePlayerPacket.unpackHorizontalCollision(short1);
            return new ServerboundMovePlayerPacket.Pos(d0, d1, d2, flag, flag1);
        }

        /**
         * Writes the raw packet data to the data stream.
         */
        private void write(FriendlyByteBuf buffer) {
            buffer.writeDouble(this.x);
            buffer.writeDouble(this.y);
            buffer.writeDouble(this.z);
            buffer.writeByte(ServerboundMovePlayerPacket.packFlags(this.onGround, this.horizontalCollision));
        }

        @Override
        public PacketType<ServerboundMovePlayerPacket.Pos> type() {
            return GamePacketTypes.SERVERBOUND_MOVE_PLAYER_POS;
        }
    }

    public static class PosRot extends ServerboundMovePlayerPacket {
        public static final StreamCodec<FriendlyByteBuf, ServerboundMovePlayerPacket.PosRot> STREAM_CODEC = Packet.codec(
            ServerboundMovePlayerPacket.PosRot::write, ServerboundMovePlayerPacket.PosRot::read
        );

        public PosRot(Vec3 pos, float yRot, float xRot, boolean onGround, boolean horizontalCollision) {
            super(pos.x, pos.y, pos.z, yRot, xRot, onGround, horizontalCollision, true, true);
        }

        public PosRot(double x, double y, double z, float yRot, float xRot, boolean onGround, boolean horizontalCollision) {
            super(x, y, z, yRot, xRot, onGround, horizontalCollision, true, true);
        }

        private static ServerboundMovePlayerPacket.PosRot read(FriendlyByteBuf buffer) {
            double d0 = buffer.readDouble();
            double d1 = buffer.readDouble();
            double d2 = buffer.readDouble();
            float f = buffer.readFloat();
            float f1 = buffer.readFloat();
            short short1 = buffer.readUnsignedByte();
            boolean flag = ServerboundMovePlayerPacket.unpackOnGround(short1);
            boolean flag1 = ServerboundMovePlayerPacket.unpackHorizontalCollision(short1);
            return new ServerboundMovePlayerPacket.PosRot(d0, d1, d2, f, f1, flag, flag1);
        }

        /**
         * Writes the raw packet data to the data stream.
         */
        private void write(FriendlyByteBuf buffer) {
            buffer.writeDouble(this.x);
            buffer.writeDouble(this.y);
            buffer.writeDouble(this.z);
            buffer.writeFloat(this.yRot);
            buffer.writeFloat(this.xRot);
            buffer.writeByte(ServerboundMovePlayerPacket.packFlags(this.onGround, this.horizontalCollision));
        }

        @Override
        public PacketType<ServerboundMovePlayerPacket.PosRot> type() {
            return GamePacketTypes.SERVERBOUND_MOVE_PLAYER_POS_ROT;
        }
    }

    public static class Rot extends ServerboundMovePlayerPacket {
        public static final StreamCodec<FriendlyByteBuf, ServerboundMovePlayerPacket.Rot> STREAM_CODEC = Packet.codec(
            ServerboundMovePlayerPacket.Rot::write, ServerboundMovePlayerPacket.Rot::read
        );

        public Rot(float yRot, float xRot, boolean onGround, boolean horizontalCollision) {
            super(0.0, 0.0, 0.0, yRot, xRot, onGround, horizontalCollision, false, true);
        }

        private static ServerboundMovePlayerPacket.Rot read(FriendlyByteBuf buffer) {
            float f = buffer.readFloat();
            float f1 = buffer.readFloat();
            short short1 = buffer.readUnsignedByte();
            boolean flag = ServerboundMovePlayerPacket.unpackOnGround(short1);
            boolean flag1 = ServerboundMovePlayerPacket.unpackHorizontalCollision(short1);
            return new ServerboundMovePlayerPacket.Rot(f, f1, flag, flag1);
        }

        /**
         * Writes the raw packet data to the data stream.
         */
        private void write(FriendlyByteBuf buffer) {
            buffer.writeFloat(this.yRot);
            buffer.writeFloat(this.xRot);
            buffer.writeByte(ServerboundMovePlayerPacket.packFlags(this.onGround, this.horizontalCollision));
        }

        @Override
        public PacketType<ServerboundMovePlayerPacket.Rot> type() {
            return GamePacketTypes.SERVERBOUND_MOVE_PLAYER_ROT;
        }
    }

    public static class StatusOnly extends ServerboundMovePlayerPacket {
        public static final StreamCodec<FriendlyByteBuf, ServerboundMovePlayerPacket.StatusOnly> STREAM_CODEC = Packet.codec(
            ServerboundMovePlayerPacket.StatusOnly::write, ServerboundMovePlayerPacket.StatusOnly::read
        );

        public StatusOnly(boolean onGround, boolean horizontalCollision) {
            super(0.0, 0.0, 0.0, 0.0F, 0.0F, onGround, horizontalCollision, false, false);
        }

        private static ServerboundMovePlayerPacket.StatusOnly read(FriendlyByteBuf buffer) {
            short short1 = buffer.readUnsignedByte();
            boolean flag = ServerboundMovePlayerPacket.unpackOnGround(short1);
            boolean flag1 = ServerboundMovePlayerPacket.unpackHorizontalCollision(short1);
            return new ServerboundMovePlayerPacket.StatusOnly(flag, flag1);
        }

        private void write(FriendlyByteBuf buffer) {
            buffer.writeByte(ServerboundMovePlayerPacket.packFlags(this.onGround, this.horizontalCollision));
        }

        @Override
        public PacketType<ServerboundMovePlayerPacket.StatusOnly> type() {
            return GamePacketTypes.SERVERBOUND_MOVE_PLAYER_STATUS_ONLY;
        }
    }
}
