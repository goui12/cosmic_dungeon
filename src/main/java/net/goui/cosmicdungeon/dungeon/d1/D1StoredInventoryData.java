package net.goui.cosmicdungeon.dungeon.d1;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.goui.cosmicdungeon.dungeon.DungeonRunRegistryData;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import java.util.*;

/** Successful runs preserve loot AND pre-entry belongings. Overflow stays durable until claimed. */
public final class D1StoredInventoryData extends SavedData {
    private static final Codec<D1StoredInventoryData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.unboundedMap(Codec.STRING, CompoundTag.CODEC).optionalFieldOf("pending", Map.of())
                    .forGetter((D1StoredInventoryData d) -> d.pending),
            Codec.unboundedMap(Codec.STRING, Codec.LONG).optionalFieldOf("last_stashed_run", Map.of())
                    .forGetter((D1StoredInventoryData d) -> d.lastStashed)
    ).apply(i, D1StoredInventoryData::load));
    private static final SavedDataType<D1StoredInventoryData> TYPE =
            new SavedDataType<>("cosmicdungeon_d1_stored_inventory_v1", D1StoredInventoryData::new, CODEC);
    private final Map<String, CompoundTag> pending = new LinkedHashMap<>();
    private final Map<String, Long> lastStashed = new HashMap<>();
    private D1StoredInventoryData() {}
    private static D1StoredInventoryData load(Map<String, CompoundTag> pending, Map<String, Long> last) {
        var data = new D1StoredInventoryData(); data.pending.putAll(pending); data.lastStashed.putAll(last); return data;
    }
    public static D1StoredInventoryData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }
    public void stash(long runId, UUID player, CompoundTag inventory) {
        String owner = player.toString();
        if (runId <= lastStashed.getOrDefault(owner, 0L)) return;
        pending.put(owner + "|" + runId, inventory.copy());
        lastStashed.put(owner, runId);
        setDirty();
    }
    public boolean hasPending(UUID player) {
        String prefix = player + "|";
        return pending.keySet().stream().anyMatch(k -> k.startsWith(prefix));
    }
    public int claim(ServerPlayer player) {
        if (DungeonRunRegistryData.get(player.level().getServer()).findRunForPlayer(player.getUUID()).isPresent()) return 0;
        String prefix = player.getUUID() + "|";
        int returned = 0;
        for (String key : new ArrayList<>(pending.keySet())) {
            if (!key.startsWith(prefix)) continue;
            NonNullList<ItemStack> items = NonNullList.withSize(player.getInventory().getContainerSize(), ItemStack.EMPTY);
            ContainerHelper.loadAllItems(TagValueInput.create(ProblemReporter.DISCARDING,
                    player.level().registryAccess(), pending.get(key)), items);
            for (int slot = 0; slot < items.size(); slot++) {
                ItemStack stack = items.get(slot);
                if (stack.isEmpty()) continue;
                int before = stack.getCount();
                player.getInventory().add(stack);
                returned += before - stack.getCount();
            }
            if (items.stream().allMatch(ItemStack::isEmpty)) {
                pending.remove(key);
            } else {
                var output = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
                ContainerHelper.saveAllItems(output, items);
                pending.put(key, output.buildResult());
            }
            setDirty();
        }
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
        if (hasPending(player.getUUID())) player.sendSystemMessage(Component.literal(
                "Some stored belongings need inventory space. Use /d1 claim when you have room."));
        return returned;
    }
}
