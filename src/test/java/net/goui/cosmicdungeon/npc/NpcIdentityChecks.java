package net.goui.cosmicdungeon.npc;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.nbt.*;
import java.nio.file.Files;
import java.util.*;

/** Production identity transitions and actual compressed NBT, without launching a world. */
public final class NpcIdentityChecks {
    private static int checks;
    private static final String INN = "vendor:cosmicdungeon:d1/save_teleport_npc";
    private static void check(boolean ok, String label) { checks++; if (!ok) throw new AssertionError(label); }
    private static NpcIdentityData restart(NpcIdentityData data) throws Exception {
        var path = Files.createTempFile("d1-npc-identity-", ".nbt");
        try {
            var encoded = NpcIdentityData.CODEC.encodeStart(NbtOps.INSTANCE, data).getOrThrow();
            NbtIo.writeCompressed((CompoundTag)encoded, path);
            return NpcIdentityData.CODEC.parse(NbtOps.INSTANCE,
                    NbtIo.readCompressed(path, NbtAccounter.create(1024 * 1024))).getOrThrow();
        } finally { Files.deleteIfExists(path); }
    }
    public static void main(String[] args) throws Exception {
        var data = NpcIdentityData.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{}")).getOrThrow();
        UUID first = new UUID(0, 1), second = new UUID(0, 2), third = new UUID(0, 3);
        check(data.owner(INN) == null, "Existing world starts without a fabricated NPC");
        check(data.admit(INN, first), "First loaded legacy binding adopted");
        check(data.admit(INN, first), "Owner may unload and return");
        check(!data.admit(INN, second), "Legacy duplicate cannot become a second Beluzon");
        check(data.admit("tamsin", third), "Different roles are independent");
        var initial = data;
        check(data.install(INN, second, () -> {
            check(initial.admit(INN, second), "Replacement reserved before entity join");
            check(!initial.admit(INN, first), "Old chunk cannot win during replacement");
            return true;
        }), "Successful spawn commits replacement");
        data = restart(data);
        check(!data.admit(INN, first) && data.admit(INN, second), "Restart rejects old chunk UUID");
        check(data.admit("tamsin", third), "Replacing Beluzon preserves Tamsin");
        check(!data.install(INN, third, () -> false), "Canceled spawn is reported");
        check(data.admit(INN, second) && !data.admit(INN, third), "Canceled spawn retains original owner");
        boolean thrown = false;
        try { data.install(INN, third, () -> { throw new IllegalStateException("Simulated insertion failure"); }); }
        catch (IllegalStateException expected) { thrown = true; }
        check(thrown && data.admit(INN, second), "Throwing insertion restores original owner");
        check(!data.install("new-role", third, () -> false) && data.owner("new-role") == null,
                "Failed first spawn leaves no ghost owner");
        data.clear(INN, first);
        check(data.admit(INN, second), "Clearing stale NPC cannot clear replacement");
        data.clear(INN, second);
        data = restart(data);
        check(data.owner(INN).isEmpty(), "Explicit unbind retirement persists");
        check(!data.admit(INN, first) && !data.admit(INN, second), "Unbind cannot resurrect an older NPC");
        check(!data.install(INN, third, () -> false) && data.owner(INN).isEmpty(), "Failure preserves retirement marker");
        check(data.install(INN, third, () -> true), "Explicit spawn can replace retirement marker");
        check(data.admit(INN, third) && !data.admit(INN, second), "Only newest explicit placement survives");
        for (int n = 4; n <= 40; n++) {
            UUID next = new UUID(0, n);
            data.replace(INN, next);
            data = restart(data);
            for (int old = 1; old < n; old++)
                check(!data.admit(INN, new UUID(0, old)), "Out-of-order retired chunks stay retired");
            check(data.admit(INN, next), "Newest copy survives restart");
        }
        for (String invalid : List.of("garbage", "1-1-1-1-1", "00000000-0000-0000-0000-000000000001 ")) {
            var malformed = new com.google.gson.JsonObject();
            var owners = new com.google.gson.JsonObject(); owners.addProperty(INN, invalid); malformed.add("owners", owners);
            check(NpcIdentityData.CODEC.parse(JsonOps.INSTANCE, malformed).error().isPresent(), "Damaged UUID is rejected");
        }
        var emptyRole = new com.google.gson.JsonObject();
        var invalidOwners = new com.google.gson.JsonObject(); invalidOwners.addProperty("", "");
        emptyRole.add("owners", invalidOwners);
        check(NpcIdentityData.CODEC.parse(JsonOps.INSTANCE, emptyRole).error().isPresent(), "Empty role is rejected");
        System.out.println(checks + " NPC identity replacement/save checks passed");
    }
}
