package net.goui.cosmicdungeon.npc;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.goui.cosmicdungeon.transaction.SavedDataProof;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import java.util.*;
import java.util.function.BooleanSupplier;

/** Global NPC identities, never positions. An empty owner is a deliberate retirement marker. */
public final class NpcIdentityData extends SavedData {
    private static final String ID = "cosmicdungeon_npc_identities_v1";
    private static final Codec<String> OWNER = Codec.STRING.validate(value -> {
        if (value.isEmpty()) return DataResult.success(value);
        try {
            return UUID.fromString(value).toString().equals(value)
                    ? DataResult.success(value) : DataResult.error(() -> "Noncanonical NPC UUID");
        } catch (IllegalArgumentException bad) { return DataResult.error(() -> "Invalid NPC UUID"); }
    });
    static final Codec<NpcIdentityData> CODEC = Codec.unboundedMap(
            Codec.STRING.validate(key -> !key.isBlank() && key.length() <= 256
                    ? DataResult.success(key) : DataResult.error(() -> "Invalid NPC role")), OWNER)
            .optionalFieldOf("owners", Map.of()).xmap(NpcIdentityData::new, d -> Map.copyOf(d.owners)).codec();
    private static final SavedDataType<NpcIdentityData> TYPE =
            new SavedDataType<>(ID, NpcIdentityData::new, CODEC);
    private final Map<String, String> owners = new HashMap<>();
    NpcIdentityData() {}
    private NpcIdentityData(Map<String, String> owners) { this.owners.putAll(owners); }

    public static NpcIdentityData get(MinecraftServer server) {
        SavedDataProof.validate(server, ID, CODEC);
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }
    /** Null means never recorded; empty means explicitly cleared. Neither means "unloaded". */
    public String owner(String role) { return owners.get(role); }
    public boolean admit(String role, UUID entity) {
        if (!owners.containsKey(role)) {
            replace(role, entity);
            return true;
        }
        return entity.toString().equals(owners.get(role));
    }
    public String replace(String role, UUID entity) {
        Objects.requireNonNull(entity);
        if (role == null || role.isBlank() || role.length() > 256) throw new IllegalArgumentException("NPC role");
        String previous = owners.put(role, entity.toString());
        if (!entity.toString().equals(previous)) setDirty();
        return previous;
    }
    /** Reserve before EntityJoinLevelEvent, roll back on refused/throwing insertion. */
    public boolean install(String role, UUID entity, BooleanSupplier insertion) {
        String previous = replace(role, entity);
        boolean committed = false;
        try {
            committed = insertion.getAsBoolean();
            return committed;
        } finally {
            if (!committed) {
                if (previous == null) owners.remove(role); else owners.put(role, previous);
                setDirty();
            }
        }
    }
    public void clear(String role, UUID entity) {
        if (entity.toString().equals(owners.get(role))) {
            owners.put(role, "");
            setDirty();
        }
    }
}
