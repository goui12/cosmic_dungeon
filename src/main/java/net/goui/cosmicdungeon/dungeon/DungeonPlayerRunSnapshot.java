package net.goui.cosmicdungeon.dungeon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

public record DungeonPlayerRunSnapshot(
        UUID playerId,
        CompoundTag inventoryNbt
) {
    private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);

    public static final Codec<DungeonPlayerRunSnapshot> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUID_CODEC.fieldOf("player_id").forGetter(DungeonPlayerRunSnapshot::playerId),
            CompoundTag.CODEC.fieldOf("inventory_nbt").forGetter(DungeonPlayerRunSnapshot::inventoryNbt)
    ).apply(i, DungeonPlayerRunSnapshot::new));
}