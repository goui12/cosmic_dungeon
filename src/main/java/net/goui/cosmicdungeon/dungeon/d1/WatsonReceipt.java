package net.goui.cosmicdungeon.dungeon.d1;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.*;
import java.util.*;

/** One bounded per-owner cursor, written in the same native file as its projection. */
public record WatsonReceipt(UUID owner, long run, UUID decision, boolean success) {
    private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);
    public static final Codec<WatsonReceipt> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUID_CODEC.fieldOf("owner").forGetter(WatsonReceipt::owner),
            Codec.LONG.fieldOf("run").forGetter(WatsonReceipt::run),
            UUID_CODEC.fieldOf("decision").forGetter(WatsonReceipt::decision),
            Codec.BOOL.fieldOf("success").forGetter(WatsonReceipt::success)
    ).apply(i, WatsonReceipt::new));
    public static final Codec<Map<UUID, WatsonReceipt>> MAP_CODEC = Codec.unboundedMap(UUID_CODEC, CODEC);
    public WatsonReceipt {
        Objects.requireNonNull(owner); Objects.requireNonNull(decision);
        if (run <= 0) throw new IllegalArgumentException("Invalid Watson receipt run");
    }
    public CompoundTag image() { return (CompoundTag) CODEC.encodeStart(NbtOps.INSTANCE, this).getOrThrow(); }
    public boolean matches(CompoundTag tag) { return image().equals(tag); }
    public boolean shouldApply(WatsonReceipt previous) {
        if (previous == null) return true;
        if (!owner.equals(previous.owner)) throw new IllegalStateException("Foreign Watson receipt");
        if (equals(previous)) return false;
        if (run <= previous.run) throw new IllegalStateException("Stale or conflicting Watson decision");
        return true;
    }
    public static void validate(Map<UUID, WatsonReceipt> receipts) {
        receipts.forEach((owner, receipt) -> {
            if (!owner.equals(receipt.owner)) throw new IllegalArgumentException("Watson receipt owner differs from map key");
        });
    }
}
