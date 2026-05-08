package net.minecraft.world.item.component;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

public final class CustomData {
    public static final CustomData EMPTY = new CustomData(new CompoundTag());
    public static final Codec<CompoundTag> COMPOUND_TAG_CODEC = Codec.withAlternative(CompoundTag.CODEC, TagParser.FLATTENED_CODEC);
    public static final Codec<CustomData> CODEC = COMPOUND_TAG_CODEC.xmap(CustomData::new, p_331996_ -> p_331996_.tag);
    @Deprecated
    public static final StreamCodec<ByteBuf, CustomData> STREAM_CODEC = ByteBufCodecs.COMPOUND_TAG.map(CustomData::new, p_331280_ -> p_331280_.tag);
    private final CompoundTag tag;

    private CustomData(CompoundTag tag) {
        this.tag = tag;
    }

    public static CustomData of(CompoundTag tag) {
        return new CustomData(tag.copy());
    }

    public boolean matchedBy(CompoundTag tag) {
        return NbtUtils.compareNbt(tag, this.tag, true);
    }

    public static void update(DataComponentType<CustomData> componentType, ItemStack stack, Consumer<CompoundTag> updater) {
        CustomData customdata = stack.getOrDefault(componentType, EMPTY).update(updater);
        if (customdata.tag.isEmpty()) {
            stack.remove(componentType);
        } else {
            stack.set(componentType, customdata);
        }
    }

    public static void set(DataComponentType<CustomData> componentType, ItemStack stack, CompoundTag tag) {
        if (!tag.isEmpty()) {
            stack.set(componentType, of(tag));
        } else {
            stack.remove(componentType);
        }
    }

    public CustomData update(Consumer<CompoundTag> updater) {
        CompoundTag compoundtag = this.tag.copy();
        updater.accept(compoundtag);
        return new CustomData(compoundtag);
    }

    public boolean isEmpty() {
        return this.tag.isEmpty();
    }

    public CompoundTag copyTag() {
        return this.tag.copy();
    }

    public boolean contains(String key) {
        return this.tag.contains(key);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        } else {
            return other instanceof CustomData customdata ? this.tag.equals(customdata.tag) : false;
        }
    }

    @Override
    public int hashCode() {
        return this.tag.hashCode();
    }

    @Override
    public String toString() {
        return this.tag.toString();
    }
}
