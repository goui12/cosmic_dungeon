package net.goui.cosmicdungeon.block.entity;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Optional chest metadata. A present but invalid assignment must never become public. */
public record ClassChestOwnership(@Nullable UUID playerId, String playerName) {
    public static final String TAG = "CosmicSlotOwner";
    public static final ClassChestOwnership UNASSIGNED = new ClassChestOwnership(null, "");

    public ClassChestOwnership {
        playerName = playerName == null ? "" : playerName.strip();
        if (playerName.length() > 16) playerName = playerName.substring(0, 16);
    }

    public boolean permits(UUID visitor) {
        return playerId != null && playerId.equals(visitor);
    }

    public Component displayName() {
        return playerName.isEmpty() ? Component.translatable("chest.cosmicdungeon.unassigned")
                : Component.literal(playerName);
    }

    @Nullable
    public static ClassChestOwnership load(ValueInput input) {
        if (!input.keySet().contains(TAG)) return null; // Existing unbound/template chest.
        ValueInput data = input.childOrEmpty(TAG);
        UUID owner = null;
        try { owner = UUID.fromString(data.getStringOr("uuid", "")); }
        catch (IllegalArgumentException ignored) { /* Keep the assignment locked. */ }
        return new ClassChestOwnership(owner, data.getStringOr("name", ""));
    }

    public void save(ValueOutput output) {
        ValueOutput data = output.child(TAG);
        data.putString("uuid", playerId == null ? "" : playerId.toString());
        data.putString("name", playerName);
    }
}
