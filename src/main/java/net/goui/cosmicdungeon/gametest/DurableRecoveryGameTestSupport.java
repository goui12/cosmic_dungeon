package net.goui.cosmicdungeon.gametest;

import com.mojang.serialization.Codec;
import java.io.IOException;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.goui.cosmicdungeon.transaction.SavedDataProof;
import net.minecraft.server.MinecraftServer;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** CI fixture support: replace only a named journal with its actual native disk-decoded instance. */
public final class DurableRecoveryGameTestSupport {
    private DurableRecoveryGameTestSupport() {}

    /** Model only the fixture transport; the real production hold must still request disconnection. */
    public static final class Disconnects {
        private final Set<ServerPlayer> expected = Collections.newSetFromMap(new IdentityHashMap<>());
        private final Map<ServerPlayer,Component> reasons = new IdentityHashMap<>();
        public void expect(ServerPlayer owner) { expected.add(owner); }
        public void record(ServerPlayer owner, Component reason) {
            reasons.put(owner, reason.copy()); owner.disconnect();
            if (!expected.contains(owner))
                throw new IllegalStateException("Unexpected native fixture disconnect for " + owner.getUUID() + ": " + reason.getString());
        }
        public boolean recoveryHold(ServerPlayer owner) {
            var reason = reasons.get(owner);
            return owner.hasDisconnected() && reason != null && reason.getString().contains("save check")
                    && reason.getString().contains("recover");
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends SavedData> SavedDataType<T> type(Class<T> owner) {
        try {
            var field = owner.getDeclaredField("TYPE"); field.setAccessible(true);
            return (SavedDataType<T>) field.get(null);
        } catch (ReflectiveOperationException ex) { throw new IllegalStateException(ex); }
    }
    @SuppressWarnings("unchecked")
    private static <T extends SavedData> Codec<T> codec(Class<T> owner) {
        try {
            var field = owner.getDeclaredField("CODEC"); field.setAccessible(true);
            return (Codec<T>) field.get(null);
        } catch (ReflectiveOperationException ex) { throw new IllegalStateException(ex); }
    }
    public static <T extends SavedData> Tag image(Class<T> owner, T journal) {
        return codec(owner).encodeStart(NbtOps.INSTANCE, journal).getOrThrow();
    }
    public static <T extends SavedData> T disk(MinecraftServer server, Class<T> owner, String id) {
        try { return SavedDataProof.read(server, id, codec(owner)); }
        catch (IOException ex) { throw new IllegalStateException("Fixture cannot read durable journal " + id, ex); }
    }
    @SuppressWarnings("unchecked")
    private static <T extends SavedData> T productionGet(MinecraftServer server, Class<T> owner) {
        try { return (T) owner.getMethod("get", MinecraftServer.class).invoke(null, server); }
        catch (ReflectiveOperationException ex) { throw new IllegalStateException(ex); }
    }
    public static <T extends SavedData> T reload(MinecraftServer server, Class<T> owner, String id) {
        var previous = productionGet(server, owner);
        var loaded = disk(server, owner, id);
        server.overworld().getDataStorage().set(type(owner), loaded);
        var actual = productionGet(server, owner);
        if (actual != loaded || actual == previous)
            throw new IllegalStateException("Native disk journal did not replace production instance: " + id);
        return actual;
    }
    /** Install an earlier observed disk image with newer native owner files to model a save-order cut. */
    public static <T extends SavedData> T restoreCut(MinecraftServer server, Class<T> owner, String id, T diskCut) {
        server.overworld().getDataStorage().set(type(owner), diskCut);
        if (productionGet(server, owner) != diskCut || !SavedDataProof.save(server, id, codec(owner), diskCut))
            throw new IllegalStateException("Fixture cutoff image was not durably installed: " + id);
        return reload(server, owner, id);
    }
}
