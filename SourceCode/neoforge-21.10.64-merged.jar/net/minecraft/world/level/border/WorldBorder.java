package net.minecraft.world.level.border;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class WorldBorder extends SavedData {
    public static final double MAX_SIZE = 5.999997E7F;
    public static final double MAX_CENTER_COORDINATE = 2.9999984E7;
    public static final Codec<WorldBorder> CODEC = WorldBorder.Settings.CODEC.xmap(WorldBorder.Settings::toWorldBorder, WorldBorder.Settings::new);
    public static final SavedDataType<WorldBorder> TYPE = new SavedDataType<>(
        "world_border", p_445350_ -> WorldBorder.Settings.DEFAULT.toWorldBorder(), p_445351_ -> CODEC, DataFixTypes.SAVED_DATA_WORLD_BORDER
    );
    private final List<BorderChangeListener> listeners = Lists.newArrayList();
    double damagePerBlock = 0.2;
    double safeZone = 5.0;
    int warningTime = 15;
    int warningBlocks = 5;
    double centerX;
    double centerZ;
    int absoluteMaxSize = 29999984;
    WorldBorder.BorderExtent extent = new WorldBorder.StaticBorderExtent(5.999997E7F);

    public boolean isWithinBounds(BlockPos pos) {
        return this.isWithinBounds(pos.getX(), pos.getZ());
    }

    public boolean isWithinBounds(Vec3 pos) {
        return this.isWithinBounds(pos.x, pos.z);
    }

    public boolean isWithinBounds(ChunkPos chunkPos) {
        return this.isWithinBounds(chunkPos.getMinBlockX(), chunkPos.getMinBlockZ()) && this.isWithinBounds(chunkPos.getMaxBlockX(), chunkPos.getMaxBlockZ());
    }

    public boolean isWithinBounds(AABB box) {
        return this.isWithinBounds(box.minX, box.minZ, box.maxX - 1.0E-5F, box.maxZ - 1.0E-5F);
    }

    private boolean isWithinBounds(double x1, double z1, double x2, double z2) {
        return this.isWithinBounds(x1, z1) && this.isWithinBounds(x2, z2);
    }

    public boolean isWithinBounds(double x, double z) {
        return this.isWithinBounds(x, z, 0.0);
    }

    public boolean isWithinBounds(double x, double z, double offset) {
        return x >= this.getMinX() - offset
            && x < this.getMaxX() + offset
            && z >= this.getMinZ() - offset
            && z < this.getMaxZ() + offset;
    }

    public BlockPos clampToBounds(BlockPos pos) {
        return this.clampToBounds(pos.getX(), pos.getY(), pos.getZ());
    }

    public BlockPos clampToBounds(Vec3 pos) {
        return this.clampToBounds(pos.x(), pos.y(), pos.z());
    }

    public BlockPos clampToBounds(double x, double y, double z) {
        return BlockPos.containing(this.clampVec3ToBound(x, y, z));
    }

    public Vec3 clampVec3ToBound(Vec3 vec3) {
        return this.clampVec3ToBound(vec3.x, vec3.y, vec3.z);
    }

    public Vec3 clampVec3ToBound(double x, double y, double z) {
        return new Vec3(
            Mth.clamp(x, this.getMinX(), this.getMaxX() - 1.0E-5F), y, Mth.clamp(z, this.getMinZ(), this.getMaxZ() - 1.0E-5F)
        );
    }

    public double getDistanceToBorder(Entity entity) {
        return this.getDistanceToBorder(entity.getX(), entity.getZ());
    }

    public VoxelShape getCollisionShape() {
        return this.extent.getCollisionShape();
    }

    public double getDistanceToBorder(double x, double z) {
        double d0 = z - this.getMinZ();
        double d1 = this.getMaxZ() - z;
        double d2 = x - this.getMinX();
        double d3 = this.getMaxX() - x;
        double d4 = Math.min(d2, d3);
        d4 = Math.min(d4, d0);
        return Math.min(d4, d1);
    }

    public boolean isInsideCloseToBorder(Entity entity, AABB bounds) {
        double d0 = Math.max(Mth.absMax(bounds.getXsize(), bounds.getZsize()), 1.0);
        return this.getDistanceToBorder(entity) < d0 * 2.0 && this.isWithinBounds(entity.getX(), entity.getZ(), d0);
    }

    public BorderStatus getStatus() {
        return this.extent.getStatus();
    }

    public double getMinX() {
        return this.extent.getMinX();
    }

    public double getMinZ() {
        return this.extent.getMinZ();
    }

    public double getMaxX() {
        return this.extent.getMaxX();
    }

    public double getMaxZ() {
        return this.extent.getMaxZ();
    }

    public double getCenterX() {
        return this.centerX;
    }

    public double getCenterZ() {
        return this.centerZ;
    }

    public void setCenter(double x, double z) {
        this.centerX = x;
        this.centerZ = z;
        this.extent.onCenterChange();
        this.setDirty();

        for (BorderChangeListener borderchangelistener : this.getListeners()) {
            borderchangelistener.onSetCenter(this, x, z);
        }
    }

    public double getSize() {
        return this.extent.getSize();
    }

    public long getLerpTime() {
        return this.extent.getLerpTime();
    }

    public double getLerpTarget() {
        return this.extent.getLerpTarget();
    }

    public void setSize(double size) {
        this.extent = new WorldBorder.StaticBorderExtent(size);
        this.setDirty();

        for (BorderChangeListener borderchangelistener : this.getListeners()) {
            borderchangelistener.onSetSize(this, size);
        }
    }

    public void lerpSizeBetween(double oldSize, double newSize, long time) {
        this.extent = (WorldBorder.BorderExtent)(oldSize == newSize
            ? new WorldBorder.StaticBorderExtent(newSize)
            : new WorldBorder.MovingBorderExtent(oldSize, newSize, time));
        this.setDirty();

        for (BorderChangeListener borderchangelistener : this.getListeners()) {
            borderchangelistener.onLerpSize(this, oldSize, newSize, time);
        }
    }

    protected List<BorderChangeListener> getListeners() {
        return Lists.newArrayList(this.listeners);
    }

    public void addListener(BorderChangeListener listener) {
        this.listeners.add(listener);
    }

    public void removeListener(BorderChangeListener listener) {
        this.listeners.remove(listener);
    }

    public void setAbsoluteMaxSize(int size) {
        this.absoluteMaxSize = size;
        this.extent.onAbsoluteMaxSizeChange();
    }

    public int getAbsoluteMaxSize() {
        return this.absoluteMaxSize;
    }

    public double getSafeZone() {
        return this.safeZone;
    }

    public void setSafeZone(double safeZone) {
        this.safeZone = safeZone;
        this.setDirty();

        for (BorderChangeListener borderchangelistener : this.getListeners()) {
            borderchangelistener.onSetSafeZone(this, safeZone);
        }
    }

    public double getDamagePerBlock() {
        return this.damagePerBlock;
    }

    public void setDamagePerBlock(double damagePerBlock) {
        this.damagePerBlock = damagePerBlock;
        this.setDirty();

        for (BorderChangeListener borderchangelistener : this.getListeners()) {
            borderchangelistener.onSetDamagePerBlock(this, damagePerBlock);
        }
    }

    public double getLerpSpeed() {
        return this.extent.getLerpSpeed();
    }

    public int getWarningTime() {
        return this.warningTime;
    }

    public void setWarningTime(int warningTime) {
        this.warningTime = warningTime;
        this.setDirty();

        for (BorderChangeListener borderchangelistener : this.getListeners()) {
            borderchangelistener.onSetWarningTime(this, warningTime);
        }
    }

    public int getWarningBlocks() {
        return this.warningBlocks;
    }

    public void setWarningBlocks(int warningDistance) {
        this.warningBlocks = warningDistance;
        this.setDirty();

        for (BorderChangeListener borderchangelistener : this.getListeners()) {
            borderchangelistener.onSetWarningBlocks(this, warningDistance);
        }
    }

    public void tick() {
        this.extent = this.extent.update();
    }

    public void applySettings(WorldBorder.Settings serializer) {
        this.setCenter(serializer.centerX(), serializer.centerZ());
        this.setDamagePerBlock(serializer.damagePerBlock());
        this.setSafeZone(serializer.safeZone());
        this.setWarningBlocks(serializer.warningBlocks());
        this.setWarningTime(serializer.warningTime());
        if (serializer.lerpTime() > 0L) {
            this.lerpSizeBetween(serializer.size(), serializer.lerpTarget(), serializer.lerpTime());
        } else {
            this.setSize(serializer.size());
        }
    }

    interface BorderExtent {
        double getMinX();

        double getMaxX();

        double getMinZ();

        double getMaxZ();

        double getSize();

        double getLerpSpeed();

        long getLerpTime();

        double getLerpTarget();

        BorderStatus getStatus();

        void onAbsoluteMaxSizeChange();

        void onCenterChange();

        WorldBorder.BorderExtent update();

        VoxelShape getCollisionShape();
    }

    class MovingBorderExtent implements WorldBorder.BorderExtent {
        private final double from;
        private final double to;
        private final long lerpEnd;
        private final long lerpBegin;
        private final double lerpDuration;

        MovingBorderExtent(double from, double to, long lerpDuration) {
            this.from = from;
            this.to = to;
            this.lerpDuration = lerpDuration;
            this.lerpBegin = Util.getMillis();
            this.lerpEnd = this.lerpBegin + lerpDuration;
        }

        @Override
        public double getMinX() {
            return Mth.clamp(
                WorldBorder.this.getCenterX() - this.getSize() / 2.0, (double)(-WorldBorder.this.absoluteMaxSize), (double)WorldBorder.this.absoluteMaxSize
            );
        }

        @Override
        public double getMinZ() {
            return Mth.clamp(
                WorldBorder.this.getCenterZ() - this.getSize() / 2.0, (double)(-WorldBorder.this.absoluteMaxSize), (double)WorldBorder.this.absoluteMaxSize
            );
        }

        @Override
        public double getMaxX() {
            return Mth.clamp(
                WorldBorder.this.getCenterX() + this.getSize() / 2.0, (double)(-WorldBorder.this.absoluteMaxSize), (double)WorldBorder.this.absoluteMaxSize
            );
        }

        @Override
        public double getMaxZ() {
            return Mth.clamp(
                WorldBorder.this.getCenterZ() + this.getSize() / 2.0, (double)(-WorldBorder.this.absoluteMaxSize), (double)WorldBorder.this.absoluteMaxSize
            );
        }

        @Override
        public double getSize() {
            double d0 = (Util.getMillis() - this.lerpBegin) / this.lerpDuration;
            return d0 < 1.0 ? Mth.lerp(d0, this.from, this.to) : this.to;
        }

        @Override
        public double getLerpSpeed() {
            return Math.abs(this.from - this.to) / (this.lerpEnd - this.lerpBegin);
        }

        @Override
        public long getLerpTime() {
            return this.lerpEnd - Util.getMillis();
        }

        @Override
        public double getLerpTarget() {
            return this.to;
        }

        @Override
        public BorderStatus getStatus() {
            return this.to < this.from ? BorderStatus.SHRINKING : BorderStatus.GROWING;
        }

        @Override
        public void onCenterChange() {
        }

        @Override
        public void onAbsoluteMaxSizeChange() {
        }

        @Override
        public WorldBorder.BorderExtent update() {
            if (this.getLerpTime() <= 0L) {
                WorldBorder.this.setDirty();
                return WorldBorder.this.new StaticBorderExtent(this.to);
            } else {
                return this;
            }
        }

        @Override
        public VoxelShape getCollisionShape() {
            return Shapes.join(
                Shapes.INFINITY,
                Shapes.box(
                    Math.floor(this.getMinX()),
                    Double.NEGATIVE_INFINITY,
                    Math.floor(this.getMinZ()),
                    Math.ceil(this.getMaxX()),
                    Double.POSITIVE_INFINITY,
                    Math.ceil(this.getMaxZ())
                ),
                BooleanOp.ONLY_FIRST
            );
        }
    }

    public record Settings(
        double centerX,
        double centerZ,
        double damagePerBlock,
        double safeZone,
        int warningBlocks,
        int warningTime,
        double size,
        long lerpTime,
        double lerpTarget
    ) {
        public static final WorldBorder.Settings DEFAULT = new WorldBorder.Settings(0.0, 0.0, 0.2, 5.0, 5, 15, 5.999997E7F, 0L, 0.0);
        public static final Codec<WorldBorder.Settings> CODEC = RecordCodecBuilder.create(
            p_446417_ -> p_446417_.group(
                    Codec.doubleRange(-2.9999984E7, 2.9999984E7).fieldOf("center_x").forGetter(WorldBorder.Settings::centerX),
                    Codec.doubleRange(-2.9999984E7, 2.9999984E7).fieldOf("center_z").forGetter(WorldBorder.Settings::centerZ),
                    Codec.DOUBLE.fieldOf("damage_per_block").forGetter(WorldBorder.Settings::damagePerBlock),
                    Codec.DOUBLE.fieldOf("safe_zone").forGetter(WorldBorder.Settings::safeZone),
                    Codec.INT.fieldOf("warning_blocks").forGetter(WorldBorder.Settings::warningBlocks),
                    Codec.INT.fieldOf("warning_time").forGetter(WorldBorder.Settings::warningTime),
                    Codec.DOUBLE.fieldOf("size").forGetter(WorldBorder.Settings::size),
                    Codec.LONG.fieldOf("lerp_time").forGetter(WorldBorder.Settings::lerpTime),
                    Codec.DOUBLE.fieldOf("lerp_target").forGetter(WorldBorder.Settings::lerpTarget)
                )
                .apply(p_446417_, WorldBorder.Settings::new)
        );

        public Settings(WorldBorder p_62032_) {
            this(
                p_62032_.centerX,
                p_62032_.centerZ,
                p_62032_.damagePerBlock,
                p_62032_.safeZone,
                p_62032_.warningBlocks,
                p_62032_.warningTime,
                p_62032_.extent.getSize(),
                p_62032_.extent.getLerpTime(),
                p_62032_.extent.getLerpTarget()
            );
        }

        public WorldBorder toWorldBorder() {
            WorldBorder worldborder = new WorldBorder();
            worldborder.applySettings(this);
            return worldborder;
        }
    }

    class StaticBorderExtent implements WorldBorder.BorderExtent {
        private final double size;
        private double minX;
        private double minZ;
        private double maxX;
        private double maxZ;
        private VoxelShape shape;

        public StaticBorderExtent(double size) {
            this.size = size;
            this.updateBox();
        }

        @Override
        public double getMinX() {
            return this.minX;
        }

        @Override
        public double getMaxX() {
            return this.maxX;
        }

        @Override
        public double getMinZ() {
            return this.minZ;
        }

        @Override
        public double getMaxZ() {
            return this.maxZ;
        }

        @Override
        public double getSize() {
            return this.size;
        }

        @Override
        public BorderStatus getStatus() {
            return BorderStatus.STATIONARY;
        }

        @Override
        public double getLerpSpeed() {
            return 0.0;
        }

        @Override
        public long getLerpTime() {
            return 0L;
        }

        @Override
        public double getLerpTarget() {
            return this.size;
        }

        private void updateBox() {
            this.minX = Mth.clamp(
                WorldBorder.this.getCenterX() - this.size / 2.0, (double)(-WorldBorder.this.absoluteMaxSize), (double)WorldBorder.this.absoluteMaxSize
            );
            this.minZ = Mth.clamp(
                WorldBorder.this.getCenterZ() - this.size / 2.0, (double)(-WorldBorder.this.absoluteMaxSize), (double)WorldBorder.this.absoluteMaxSize
            );
            this.maxX = Mth.clamp(
                WorldBorder.this.getCenterX() + this.size / 2.0, (double)(-WorldBorder.this.absoluteMaxSize), (double)WorldBorder.this.absoluteMaxSize
            );
            this.maxZ = Mth.clamp(
                WorldBorder.this.getCenterZ() + this.size / 2.0, (double)(-WorldBorder.this.absoluteMaxSize), (double)WorldBorder.this.absoluteMaxSize
            );
            this.shape = Shapes.join(
                Shapes.INFINITY,
                Shapes.box(
                    Math.floor(this.getMinX()),
                    Double.NEGATIVE_INFINITY,
                    Math.floor(this.getMinZ()),
                    Math.ceil(this.getMaxX()),
                    Double.POSITIVE_INFINITY,
                    Math.ceil(this.getMaxZ())
                ),
                BooleanOp.ONLY_FIRST
            );
        }

        @Override
        public void onAbsoluteMaxSizeChange() {
            this.updateBox();
        }

        @Override
        public void onCenterChange() {
            this.updateBox();
        }

        @Override
        public WorldBorder.BorderExtent update() {
            return this;
        }

        @Override
        public VoxelShape getCollisionShape() {
            return this.shape;
        }
    }
}
