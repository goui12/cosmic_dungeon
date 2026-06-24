package net.goui.cosmicdungeon.playerclass.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Stores class attunement metadata for placed blocks so the item keeps it when broken. */
public final class PlacedAttunementData extends SavedData {
    private static final String STORAGE_NAME = "cosmicdungeon_placed_attunements_v1";

    private record Key(ResourceLocation dimension, BlockPos pos) {}

    private record Attunement(String classId, int dungeon, int tier, long trace) {
        static Attunement from(ItemStack stack) {
            String classId = ClassItemUtil.getClassAttunement(stack);
            Integer dungeon = ClassItemUtil.getDungeon(stack);
            Integer tier = ClassItemUtil.getTier(stack);
            Long trace = ClassItemUtil.getTraceValue(stack);
            if (classId == null || dungeon == null || tier == null || trace == null) return null;
            return new Attunement(classId, dungeon, tier, trace);
        }

        void applyTo(ItemStack stack) {
            ClassItemUtil.attune(stack, classId, dungeon, tier, trace);
        }
    }

    private record Entry(ResourceLocation dimension, int x, int y, int z, String classId, int dungeon, int tier, long trace) {
        Key key() {
            return new Key(dimension, new BlockPos(x, y, z));
        }

        Attunement attunement() {
            return new Attunement(classId, dungeon, tier, trace);
        }
    }

    private static final Codec<Entry> ENTRY_CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("dimension").forGetter(Entry::dimension),
            Codec.INT.fieldOf("x").forGetter(Entry::x),
            Codec.INT.fieldOf("y").forGetter(Entry::y),
            Codec.INT.fieldOf("z").forGetter(Entry::z),
            Codec.STRING.fieldOf("classId").forGetter(Entry::classId),
            Codec.INT.fieldOf("dungeon").forGetter(Entry::dungeon),
            Codec.INT.fieldOf("tier").forGetter(Entry::tier),
            Codec.LONG.fieldOf("trace").forGetter(Entry::trace)
    ).apply(i, Entry::new));

    public static final Codec<PlacedAttunementData> CODEC = RecordCodecBuilder.create(i -> i.group(
            ENTRY_CODEC.listOf().optionalFieldOf("entries", List.of()).forGetter(PlacedAttunementData::entriesView)
    ).apply(i, PlacedAttunementData::fromEntries));

    private static final SavedDataType<PlacedAttunementData> TYPE =
            new SavedDataType<>(STORAGE_NAME, PlacedAttunementData::new, CODEC);

    private final Map<Key, Attunement> attunements = new HashMap<>();

    public PlacedAttunementData() {}

    public static PlacedAttunementData get(Level level) {
        if (!(level instanceof ServerLevel sl)) {
            throw new IllegalStateException("PlacedAttunementData accessed on client or non-server level");
        }
        return sl.getDataStorage().computeIfAbsent(TYPE);
    }

    public void put(Level level, BlockPos pos, ItemStack stack) {
        Attunement attunement = Attunement.from(stack);
        if (attunement == null) return;
        attunements.put(key(level, pos), attunement);
        setDirty();
    }

    public boolean applyAndRemove(Level level, BlockPos pos, ItemStack stack) {
        Attunement attunement = attunements.remove(key(level, pos));
        if (attunement == null || stack == null || stack.isEmpty()) return false;
        attunement.applyTo(stack);
        setDirty();
        return true;
    }

    public void remove(Level level, BlockPos pos) {
        if (attunements.remove(key(level, pos)) != null) setDirty();
    }

    private static Key key(Level level, BlockPos pos) {
        return new Key(level.dimension().location(), pos.immutable());
    }

    private List<Entry> entriesView() {
        List<Entry> out = new ArrayList<>(attunements.size());
        for (Map.Entry<Key, Attunement> e : attunements.entrySet()) {
            Key key = e.getKey();
            Attunement attunement = e.getValue();
            out.add(new Entry(key.dimension(), key.pos().getX(), key.pos().getY(), key.pos().getZ(),
                    attunement.classId(), attunement.dungeon(), attunement.tier(), attunement.trace()));
        }
        return out;
    }

    private static PlacedAttunementData fromEntries(List<Entry> entries) {
        PlacedAttunementData data = new PlacedAttunementData();
        for (Entry entry : entries) {
            if (!ClassItemUtil.isPlayableClass(entry.classId())) continue;
            data.attunements.put(entry.key(), entry.attunement());
        }
        return data;
    }
}
