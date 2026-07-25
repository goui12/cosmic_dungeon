package net.goui.cosmicdungeon.dungeon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.UUID;

public record DungeonReturnTarget(UUID owner, long runId, String dimensionId,
                                  double x, double y, double z, float yaw, float pitch) {
    public static final Codec<DungeonReturnTarget> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.xmap(UUID::fromString, UUID::toString).fieldOf("owner").forGetter(DungeonReturnTarget::owner),
            Codec.LONG.fieldOf("run_id").forGetter(DungeonReturnTarget::runId),
            Codec.STRING.fieldOf("dimension").forGetter(DungeonReturnTarget::dimensionId),
            Codec.DOUBLE.fieldOf("x").forGetter(DungeonReturnTarget::x),
            Codec.DOUBLE.fieldOf("y").forGetter(DungeonReturnTarget::y),
            Codec.DOUBLE.fieldOf("z").forGetter(DungeonReturnTarget::z),
            Codec.FLOAT.fieldOf("yaw").forGetter(DungeonReturnTarget::yaw),
            Codec.FLOAT.fieldOf("pitch").forGetter(DungeonReturnTarget::pitch)
    ).apply(instance, DungeonReturnTarget::new));
}
