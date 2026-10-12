package net.goui.cosmicdungeon.network;

import java.util.*;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.*;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Bounded display quotes; actions carry only server-defined target identity and exact generation. */
public final class PotionCraftPayloads {
    public static final int MAX_ENTRIES = 96;
    public static final int MAX_MATERIALS = 18, MAX_KEY = 264;
    private PotionCraftPayloads() {}
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("cosmicdungeon", path); }
    private static void target(String id) {
        if (id == null || id.length() > 128 || !id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+"))
            throw new IllegalArgumentException("Invalid potion target");
    }
    /** Stock-key identities only: no arbitrary ingredient components or client recipes. */
    public record Material(String key, int needed, int own, int shared) {
        public Material {
            if (key == null || key.length() > MAX_KEY) throw new IllegalArgumentException("Invalid material key");
            var parts = key.split("/", -1);
            if (!(parts.length == 2 && parts[0].equals("item") || parts.length == 3 && parts[0].equals("potion")))
                throw new IllegalArgumentException("Invalid material identity");
            for (int i = 1; i < parts.length; i++) {
                target(parts[i]);
                if (parts[i].contains("/")) throw new IllegalArgumentException("Invalid vanilla identity");
            }
            if (needed < 1 || needed > 17 || own < 0 || own > 4096 || shared < own || shared > 4096)
                throw new IllegalArgumentException("Invalid material quantity");
        }
    }
    public record Entry(String targetId, ItemStack output, int own, int shared, int steps, int powders, List<Material> materials) {
        public Entry {
            target(targetId); output = output.copy(); materials = List.copyOf(materials);
            if (output.isEmpty() || output.getCount() != 1 || own < 0 || own > 64 || shared < own || shared > 64
                    || steps < 0 || steps > 16 || powders < 0 || powders > 1 || materials.size() > MAX_MATERIALS
                    || materials.stream().map(Material::key).distinct().count() != materials.size())
                throw new IllegalArgumentException("Invalid potion quote");
            if (shared == 0) {
                if (steps != 0 || powders != 0 || !materials.isEmpty()) throw new IllegalArgumentException("Unavailable route");
            } else if (steps == 0 || materials.size() < 2 || !materials.getFirst().key().startsWith("potion/")
                    || materials.getFirst().needed() != 1
                    || materials.subList(1, materials.size()).stream().anyMatch(m -> !m.key().startsWith("item/"))
                    || materials.subList(1, materials.size()).stream().mapToInt(Material::needed).sum() != steps + powders
                    || materials.stream().anyMatch(m -> m.shared() < m.needed() || own > 0 && m.own() < m.needed())
                    || powders > 0 && materials.stream().noneMatch(m -> m.key().equals("item/minecraft:blaze_powder") && m.needed() >= powders))
                throw new IllegalArgumentException("Invalid selected route");
        }
        @Override public ItemStack output() { return output.copy(); }
    }
    public record View(long run, long revision, String classId, int charges, int perPowder, List<Entry> entries) implements CustomPacketPayload {
        public View {
            entries = List.copyOf(entries);
            if (run < 0 || revision < 0 || classId == null || !Set.of("", "theurgist", "venefex").contains(classId)
                    || charges < 0 || charges >= 20 || perPowder != 20 || entries.size() > MAX_ENTRIES
                    || entries.stream().map(Entry::targetId).distinct().count() != entries.size()
                    || run == 0 && (!classId.isEmpty() || !entries.isEmpty() || charges != 0)
                    || run > 0 && classId.isEmpty()) throw new IllegalArgumentException("Invalid potion view");
        }
        public static View empty(long revision) { return new View(0, revision, "", 0, 20, List.of()); }
        public static final Type<View> TYPE = new Type<>(id("potion_craft_view"));
        public static final StreamCodec<RegistryFriendlyByteBuf, View> STREAM_CODEC = new StreamCodec<>() {
            public void encode(RegistryFriendlyByteBuf b, View v) {
                b.writeVarLong(v.run); b.writeVarLong(v.revision); b.writeUtf(v.classId, 16);
                b.writeVarInt(v.charges); b.writeVarInt(v.perPowder); b.writeVarInt(v.entries.size());
                for (var e : v.entries) {
                    b.writeUtf(e.targetId, 128); ItemStack.STREAM_CODEC.encode(b, e.output);
                    b.writeVarInt(e.own); b.writeVarInt(e.shared); b.writeVarInt(e.steps); b.writeVarInt(e.powders);
                    b.writeVarInt(e.materials.size());
                    for (var m : e.materials) {
                        b.writeUtf(m.key, MAX_KEY); b.writeVarInt(m.needed); b.writeVarInt(m.own); b.writeVarInt(m.shared);
                    }
                }
            }
            public View decode(RegistryFriendlyByteBuf b) {
                long run = b.readVarLong(), revision = b.readVarLong(); String cls = b.readUtf(16);
                int charges = b.readVarInt(), perPowder = b.readVarInt(), count = b.readVarInt();
                if (count < 0 || count > MAX_ENTRIES) throw new IllegalArgumentException("Too many potion quotes");
                var entries = new ArrayList<Entry>(count);
                for (int i = 0; i < count; i++) {
                    String target = b.readUtf(128); var output = ItemStack.STREAM_CODEC.decode(b);
                    int own = b.readVarInt(), shared = b.readVarInt(), steps = b.readVarInt(), powders = b.readVarInt(), size = b.readVarInt();
                    if (size < 0 || size > MAX_MATERIALS) throw new IllegalArgumentException("Too many route materials");
                    var materials = new ArrayList<Material>(size);
                    for (int j = 0; j < size; j++) materials.add(new Material(b.readUtf(MAX_KEY), b.readVarInt(), b.readVarInt(), b.readVarInt()));
                    entries.add(new Entry(target, output, own, shared, steps, powders, materials));
                }
                return new View(run, revision, cls, charges, perPowder, entries);
            }
        };
        public Type<View> type() { return TYPE; }
    }
    public record Action(long run, long revision, String targetId, boolean shift) implements CustomPacketPayload {
        public Action { target(targetId); if (run <= 0 || revision < 0) throw new IllegalArgumentException("Invalid potion action"); }
        public static final Type<Action> TYPE = new Type<>(id("potion_craft_action"));
        public static final StreamCodec<ByteBuf, Action> STREAM_CODEC = new StreamCodec<>() {
            public void encode(ByteBuf raw, Action a) { var b = new FriendlyByteBuf(raw); b.writeVarLong(a.run); b.writeVarLong(a.revision); b.writeUtf(a.targetId, 128); b.writeBoolean(a.shift); }
            public Action decode(ByteBuf raw) { var b = new FriendlyByteBuf(raw); return new Action(b.readVarLong(), b.readVarLong(), b.readUtf(128), b.readBoolean()); }
        };
        public Type<Action> type() { return TYPE; }
    }
    public record Request(boolean open) implements CustomPacketPayload {
        public static final Type<Request> TYPE = new Type<>(id("potion_craft_request"));
        public static final StreamCodec<ByteBuf, Request> STREAM_CODEC = new StreamCodec<>() {
            public void encode(ByteBuf b, Request r) { b.writeBoolean(r.open); }
            public Request decode(ByteBuf b) { return new Request(b.readBoolean()); }
        };
        public Type<Request> type() { return TYPE; }
    }
}
