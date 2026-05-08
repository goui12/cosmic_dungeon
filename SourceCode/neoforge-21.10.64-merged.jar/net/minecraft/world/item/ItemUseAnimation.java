package net.minecraft.world.item;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.function.IntFunction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

@net.neoforged.fml.common.asm.enumextension.IndexedEnum
@net.neoforged.fml.common.asm.enumextension.NamedEnum(1)
@net.neoforged.fml.common.asm.enumextension.NetworkedEnum(net.neoforged.fml.common.asm.enumextension.NetworkedEnum.NetworkCheck.BIDIRECTIONAL)
public enum ItemUseAnimation implements StringRepresentable, net.neoforged.fml.common.asm.enumextension.IExtensibleEnum {
    NONE(0, "none"),
    EAT(1, "eat"),
    DRINK(2, "drink"),
    BLOCK(3, "block"),
    BOW(4, "bow"),
    SPEAR(5, "spear"),
    CROSSBOW(6, "crossbow"),
    SPYGLASS(7, "spyglass"),
    TOOT_HORN(8, "toot_horn"),
    BRUSH(9, "brush"),
    BUNDLE(10, "bundle");

    private static final IntFunction<ItemUseAnimation> BY_ID = ByIdMap.continuous(ItemUseAnimation::getId, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
    public static final Codec<ItemUseAnimation> CODEC = StringRepresentable.fromEnum(ItemUseAnimation::values);
    public static final StreamCodec<ByteBuf, ItemUseAnimation> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, ItemUseAnimation::getId);
    private final int id;
    private final String name;

    private ItemUseAnimation(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return this.id;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public static net.neoforged.fml.common.asm.enumextension.ExtensionInfo getExtensionInfo() {
        return net.neoforged.fml.common.asm.enumextension.ExtensionInfo.nonExtended(ItemUseAnimation.class);
    }
}
