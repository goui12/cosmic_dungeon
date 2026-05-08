package net.minecraft.world.level.storage.loot;

import java.util.Set;
import net.minecraft.util.context.ContextKey;

/**
 * An object that will use some parameters from a LootContext. Used for validation purposes to validate that the correct parameters are present.
 */
public interface LootContextUser {
    default Set<ContextKey<?>> getReferencedContextParams() {
        return Set.of();
    }

    /**
     * Validate that this object is used correctly according to the given ValidationContext.
     */
    default void validate(ValidationContext context) {
        context.validateContextUsage(this);
    }
}
