package net.minecraft.world.level.storage.loot.providers.nbt;

import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.nbt.Tag;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.level.storage.loot.LootContext;

/**
 * A provider for NBT data based on a LootContext.
 *
 * @see NbtProviders
 */
public interface NbtProvider {
    @Nullable
    Tag get(LootContext lootContext);

    Set<ContextKey<?>> getReferencedContextParams();

    LootNbtProviderType getType();
}
