package net.minecraft.world.level.storage.loot.providers.nbt;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Set;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.level.storage.loot.LootContext;

/**
 * An NbtProvider that provides NBT data from a named {@link CommandStorage}.
 */
public record StorageNbtProvider(ResourceLocation id) implements NbtProvider {
    public static final MapCodec<StorageNbtProvider> CODEC = RecordCodecBuilder.mapCodec(
        p_298341_ -> p_298341_.group(ResourceLocation.CODEC.fieldOf("source").forGetter(StorageNbtProvider::id)).apply(p_298341_, StorageNbtProvider::new)
    );

    @Override
    public LootNbtProviderType getType() {
        return NbtProviders.STORAGE;
    }

    @Override
    public Tag get(LootContext lootContext) {
        return lootContext.getLevel().getServer().getCommandStorage().get(this.id);
    }

    @Override
    public Set<ContextKey<?>> getReferencedContextParams() {
        return Set.of();
    }
}
