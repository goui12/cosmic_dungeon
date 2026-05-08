package net.minecraft.world.level.storage.loot.providers.score;

import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.scores.ScoreHolder;

/**
 * Provides a scoreboard name based on a {@link LootContext}.
 */
public interface ScoreboardNameProvider {
    @Nullable
    ScoreHolder getScoreHolder(LootContext context);

    LootScoreProviderType getType();

    Set<ContextKey<?>> getReferencedContextParams();
}
