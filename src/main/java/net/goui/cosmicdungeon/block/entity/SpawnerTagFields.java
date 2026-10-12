package net.goui.cosmicdungeon.block.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Retain only unknown fields, avoiding a duplicate cache of authored stacks and native spawner data. */
public final class SpawnerTagFields {
    private SpawnerTagFields() {}
    public static CompoundTag unknown(ValueInput in, Set<String> known) {
        var result = new CompoundTag();
        for (String key : in.keySet()) if (!known.contains(key))
            in.read(key, Codec.PASSTHROUGH).ifPresent(value -> result.put(key, value.convert(NbtOps.INSTANCE).getValue().copy()));
        return result;
    }
    public static void write(CompoundTag tag, ValueOutput out) {
        for (String key : tag.keySet()) out.store(key, Codec.PASSTHROUGH, new Dynamic<>(NbtOps.INSTANCE, tag.get(key).copy()));
    }
}
