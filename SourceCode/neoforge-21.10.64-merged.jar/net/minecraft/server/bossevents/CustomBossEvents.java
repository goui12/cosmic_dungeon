package net.minecraft.server.bossevents;

import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import java.util.Collection;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

public class CustomBossEvents {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Codec<Map<ResourceLocation, CustomBossEvent.Packed>> EVENTS_CODEC = Codec.unboundedMap(
        ResourceLocation.CODEC, CustomBossEvent.Packed.CODEC
    );
    private final Map<ResourceLocation, CustomBossEvent> events = Maps.newHashMap();

    @Nullable
    public CustomBossEvent get(ResourceLocation id) {
        return this.events.get(id);
    }

    public CustomBossEvent create(ResourceLocation id, Component name) {
        CustomBossEvent custombossevent = new CustomBossEvent(id, name);
        this.events.put(id, custombossevent);
        return custombossevent;
    }

    public void remove(CustomBossEvent bossbar) {
        this.events.remove(bossbar.getTextId());
    }

    public Collection<ResourceLocation> getIds() {
        return this.events.keySet();
    }

    public Collection<CustomBossEvent> getEvents() {
        return this.events.values();
    }

    public CompoundTag save(HolderLookup.Provider levelRegistry) {
        Map<ResourceLocation, CustomBossEvent.Packed> map = Util.mapValues(this.events, CustomBossEvent::pack);
        return (CompoundTag)EVENTS_CODEC.encodeStart(levelRegistry.createSerializationContext(NbtOps.INSTANCE), map).getOrThrow();
    }

    public void load(CompoundTag tag, HolderLookup.Provider levelRegistry) {
        Map<ResourceLocation, CustomBossEvent.Packed> map = EVENTS_CODEC.parse(levelRegistry.createSerializationContext(NbtOps.INSTANCE), tag)
            .resultOrPartial(p_405050_ -> LOGGER.error("Failed to parse boss bar events: {}", p_405050_))
            .orElse(Map.of());
        map.forEach((p_405180_, p_404753_) -> this.events.put(p_405180_, CustomBossEvent.load(p_405180_, p_404753_)));
    }

    public void onPlayerConnect(ServerPlayer player) {
        for (CustomBossEvent custombossevent : this.events.values()) {
            custombossevent.onPlayerConnect(player);
        }
    }

    public void onPlayerDisconnect(ServerPlayer player) {
        for (CustomBossEvent custombossevent : this.events.values()) {
            custombossevent.onPlayerDisconnect(player);
        }
    }
}
