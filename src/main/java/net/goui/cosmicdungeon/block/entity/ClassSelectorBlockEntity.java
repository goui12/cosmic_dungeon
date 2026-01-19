// file: src/main/java/net/goui/cosmicdungeon/block/entity/ClassSelectorBlockEntity.java
package net.goui.cosmicdungeon.block.entity;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.HashMap;
import java.util.Map;

public final class ClassSelectorBlockEntity extends BlockEntity {

    private static final String TAG_DEST = "DestinationName"; // legacy/global fallback (optional)
    private static final String TAG_MAX_PLAYERS = "MaxPlayers";
    private static final String TAG_SLOT_DESTS = "SlotDestinations"; // new

    private static final Codec<Map<String, String>> SLOT_DESTS_CODEC =
            Codec.unboundedMap(Codec.STRING, Codec.STRING);

    private String destinationName = ""; // legacy/fallback
    private int maxPlayers = 2; // default

    /** slotNumber (1..64) -> destination name */
    private final Map<String, String> slotDestinations = new HashMap<>();

    public ClassSelectorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CLASS_SELECTOR.get(), pos, state);
    }

    /* ---------------- Basic destination (legacy/fallback) ---------------- */

    public String getDestinationName() {
        return destinationName == null ? "" : destinationName;
    }

    public void setDestinationName(String name) {
        this.destinationName = name == null ? "" : name.trim();
        markChangedAndSync();
    }

    public boolean hasDestination() {
        return !getDestinationName().isBlank();
    }

    /* ---------------- Max players ---------------- */

    public int getMaxPlayers() {
        return Math.max(1, maxPlayers);
    }

    public void setMaxPlayers(int count) {
        this.maxPlayers = Math.max(1, Math.min(64, count));
        markChangedAndSync();
    }

    /* ---------------- Per-slot destinations ---------------- */

    private static int clampSlot(int slot) {
        if (slot < 1) return 1;
        if (slot > 64) return 64;
        return slot;
    }

    /** Returns destination configured for this slot, or "" if not set. */
    public String getSlotDestination(int slotNumber) {
        int s = clampSlot(slotNumber);
        String v = slotDestinations.get(String.valueOf(s));
        return v == null ? "" : v;
    }

    /**
     * Sets destination for a specific slot.
     * NOTE: Validity (exists in RiftRegistryData) should be enforced by the command/UI.
     */
    public void setSlotDestination(int slotNumber, String destinationName) {
        int s = clampSlot(slotNumber);
        String clean = destinationName == null ? "" : destinationName.trim();
        if (clean.isBlank()) {
            slotDestinations.remove(String.valueOf(s));
        } else {
            slotDestinations.put(String.valueOf(s), clean);
        }
        markChangedAndSync();
    }

    public void clearSlotDestination(int slotNumber) {
        int s = clampSlot(slotNumber);
        slotDestinations.remove(String.valueOf(s));
        markChangedAndSync();
    }

    /** Snapshot for UI/debug. */
    public Map<String, String> snapshotSlotDestinations() {
        return Map.copyOf(slotDestinations);
    }

    /* ---------------- Internals ---------------- */

    private void markChangedAndSync() {
        setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /* ----------------- Persistence (1.21.10) ----------------- */

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);

        output.putString(TAG_DEST, getDestinationName());
        output.putInt(TAG_MAX_PLAYERS, getMaxPlayers());

        if (!slotDestinations.isEmpty()) {
            output.store(TAG_SLOT_DESTS, SLOT_DESTS_CODEC, slotDestinations);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        this.destinationName = input.getString(TAG_DEST).orElse("");
        this.maxPlayers = input.getIntOr(TAG_MAX_PLAYERS, 2);
        if (this.maxPlayers < 1) this.maxPlayers = 1;
        if (this.maxPlayers > 64) this.maxPlayers = 64;

        this.slotDestinations.clear();
        Map<String, String> loaded = input.read(TAG_SLOT_DESTS, SLOT_DESTS_CODEC).orElse(Map.of());
        if (loaded != null && !loaded.isEmpty()) {
            // sanitize keys/values
            for (var e : loaded.entrySet()) {
                if (e.getKey() == null || e.getValue() == null) continue;
                String k = e.getKey().trim();
                String v = e.getValue().trim();
                if (k.isBlank() || v.isBlank()) continue;
                slotDestinations.put(k, v);
            }
        }
    }
}
