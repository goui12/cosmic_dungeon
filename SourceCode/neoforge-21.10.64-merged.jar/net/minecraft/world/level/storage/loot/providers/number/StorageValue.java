package net.minecraft.world.level.storage.loot.providers.number;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootContext;

public record StorageValue(ResourceLocation storage, NbtPathArgument.NbtPath path) implements NumberProvider {
    public static final MapCodec<StorageValue> CODEC = RecordCodecBuilder.mapCodec(
        p_335586_ -> p_335586_.group(
                ResourceLocation.CODEC.fieldOf("storage").forGetter(StorageValue::storage),
                NbtPathArgument.NbtPath.CODEC.fieldOf("path").forGetter(StorageValue::path)
            )
            .apply(p_335586_, StorageValue::new)
    );

    @Override
    public LootNumberProviderType getType() {
        return NumberProviders.STORAGE;
    }

    private Number getNumericTag(LootContext context, Number defaultValue) {
        CompoundTag compoundtag = context.getLevel().getServer().getCommandStorage().get(this.storage);

        try {
            List<Tag> list = this.path.get(compoundtag);
            if (list.size() == 1 && list.getFirst() instanceof NumericTag numerictag) {
                return numerictag.box();
            }
        } catch (CommandSyntaxException commandsyntaxexception) {
        }

        return defaultValue;
    }

    @Override
    public float getFloat(LootContext lootContext) {
        return this.getNumericTag(lootContext, 0.0F).floatValue();
    }

    @Override
    public int getInt(LootContext lootContext) {
        return this.getNumericTag(lootContext, 0).intValue();
    }
}
