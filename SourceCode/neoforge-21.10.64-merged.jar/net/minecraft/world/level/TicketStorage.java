package net.minecraft.world.level;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkLevel;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.Ticket;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.slf4j.Logger;

public class TicketStorage extends SavedData {
    private static final int INITIAL_TICKET_LIST_CAPACITY = 4;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Codec<Pair<ChunkPos, Ticket>> TICKET_ENTRY = Codec.mapPair(ChunkPos.CODEC.fieldOf("chunk_pos"), Ticket.CODEC).codec();
    public static final Codec<TicketStorage> CODEC = RecordCodecBuilder.create(
        p_400948_ -> p_400948_.group(TICKET_ENTRY.listOf().optionalFieldOf("tickets", List.of()).forGetter(TicketStorage::packTickets))
            .and(net.neoforged.neoforge.common.world.chunk.ForcedChunkManager.defineExtraStorageParams())
            .apply(p_400948_, (tickets, neoData) -> net.neoforged.neoforge.common.world.chunk.ForcedChunkManager.readStoredTickets(TicketStorage::fromPacked, tickets, neoData))
    );
    public static final SavedDataType<TicketStorage> TYPE = new SavedDataType<>("chunks", TicketStorage::new, CODEC, DataFixTypes.SAVED_DATA_FORCED_CHUNKS);
    private final Long2ObjectOpenHashMap<List<Ticket>> tickets;
    private final Long2ObjectOpenHashMap<List<Ticket>> deactivatedTickets;
    private LongSet chunksWithForcedTickets = new LongOpenHashSet();
    @Nullable
    private TicketStorage.ChunkUpdated loadingChunkUpdatedListener;
    @Nullable
    private TicketStorage.ChunkUpdated simulationChunkUpdatedListener;

    private TicketStorage(Long2ObjectOpenHashMap<List<Ticket>> tickets, Long2ObjectOpenHashMap<List<Ticket>> deactivatedTickets) {
        this.tickets = tickets;
        this.deactivatedTickets = deactivatedTickets;
        this.updateForcedChunks();
        this.updateForcedNaturalSpawning();
    }

    public TicketStorage() {
        this(new Long2ObjectOpenHashMap<>(4), new Long2ObjectOpenHashMap<>());
    }

    private static TicketStorage fromPacked(List<Pair<ChunkPos, Ticket>> packed) {
        Long2ObjectOpenHashMap<List<Ticket>> long2objectopenhashmap = new Long2ObjectOpenHashMap<>();

        for (Pair<ChunkPos, Ticket> pair : packed) {
            ChunkPos chunkpos = pair.getFirst();
            List<Ticket> list = long2objectopenhashmap.computeIfAbsent(chunkpos.toLong(), p_393722_ -> new ObjectArrayList<>(4));
            list.add(pair.getSecond());
        }

        return new TicketStorage(new Long2ObjectOpenHashMap<>(4), long2objectopenhashmap);
    }

    private List<Pair<ChunkPos, Ticket>> packTickets() {
        List<Pair<ChunkPos, Ticket>> list = new ArrayList<>();
        this.forEachTicket((p_400946_, p_400947_) -> {
            if (p_400947_.getType().persist()) {
                list.add(new Pair<>(p_400946_, p_400947_));
            }
        });
        return list;
    }

    private void forEachTicket(BiConsumer<ChunkPos, Ticket> action) {
        forEachTicket(action, this.tickets);
        forEachTicket(action, this.deactivatedTickets);
    }

    private static void forEachTicket(BiConsumer<ChunkPos, Ticket> action, Long2ObjectOpenHashMap<List<Ticket>> tickets) {
        for (Entry<List<Ticket>> entry : Long2ObjectMaps.fastIterable(tickets)) {
            ChunkPos chunkpos = new ChunkPos(entry.getLongKey());

            for (Ticket ticket : entry.getValue()) {
                action.accept(chunkpos, ticket);
            }
        }
    }

    public void activateAllDeactivatedTickets() {
        for (Entry<List<Ticket>> entry : Long2ObjectMaps.fastIterable(this.deactivatedTickets)) {
            for (Ticket ticket : entry.getValue()) {
                this.addTicket(entry.getLongKey(), ticket);
            }
        }

        this.deactivatedTickets.clear();
    }

    public void setLoadingChunkUpdatedListener(@Nullable TicketStorage.ChunkUpdated loadingChunkUpdatedListener) {
        this.loadingChunkUpdatedListener = loadingChunkUpdatedListener;
    }

    public void setSimulationChunkUpdatedListener(@Nullable TicketStorage.ChunkUpdated simulationChunkUpdatedListener) {
        this.simulationChunkUpdatedListener = simulationChunkUpdatedListener;
    }

    public boolean hasTickets() {
        return !this.tickets.isEmpty();
    }

    public boolean shouldKeepDimensionActive() {
        for (List<Ticket> list : this.tickets.values()) {
            for (Ticket ticket : list) {
                if (ticket.getType().shouldKeepDimensionActive()) {
                    return true;
                }
            }
        }

        return false;
    }

    public List<Ticket> getTickets(long chunkPos) {
        return this.tickets.getOrDefault(chunkPos, List.of());
    }

    private List<Ticket> getOrCreateTickets(long chunkPos) {
        return this.tickets.computeIfAbsent(chunkPos, p_393919_ -> new ObjectArrayList<>(4));
    }

    public void addTicketWithRadius(TicketType ticketType, ChunkPos chunkPos, int radius) {
        Ticket ticket = new Ticket(ticketType, ChunkLevel.byStatus(FullChunkStatus.FULL) - radius);
        this.addTicket(chunkPos.toLong(), ticket);
    }

    public void addTicket(Ticket ticket, ChunkPos chunkPos) {
        this.addTicket(chunkPos.toLong(), ticket);
    }

    public boolean addTicket(long chunkPos, Ticket p_ticket) {
        List<Ticket> list = this.getOrCreateTickets(chunkPos);

        for (Ticket ticket : list) {
            if (isTicketSameTypeAndLevel(p_ticket, ticket)) {
                ticket.resetTicksLeft();
                this.setDirty();
                return false;
            }
        }

        int i = getTicketLevelAt(list, true);
        int j = getTicketLevelAt(list, false);
        list.add(p_ticket);
        if (SharedConstants.DEBUG_VERBOSE_SERVER_EVENTS) {
            LOGGER.debug("ATI {} {}", new ChunkPos(chunkPos), p_ticket);
        }

        if (p_ticket.getType().doesSimulate() && p_ticket.getTicketLevel() < i && this.simulationChunkUpdatedListener != null) {
            this.simulationChunkUpdatedListener.update(chunkPos, p_ticket.getTicketLevel(), true);
        }

        if (p_ticket.getType().doesLoad() && p_ticket.getTicketLevel() < j && this.loadingChunkUpdatedListener != null) {
            this.loadingChunkUpdatedListener.update(chunkPos, p_ticket.getTicketLevel(), true);
        }

        if (p_ticket.getType().equals(TicketType.FORCED)) {
            this.chunksWithForcedTickets.add(chunkPos);
        }
        if (p_ticket.getType().forceNaturalSpawning()) chunksWithForceNaturalSpawning.add(chunkPos);

        this.setDirty();
        return true;
    }

    private static boolean isTicketSameTypeAndLevel(Ticket first, Ticket second) {
        return second.getType() == first.getType() && second.getTicketLevel() == first.getTicketLevel();
    }

    public int getTicketLevelAt(long chunkPos, boolean requireSimulation) {
        return getTicketLevelAt(this.getTickets(chunkPos), requireSimulation);
    }

    private static int getTicketLevelAt(List<Ticket> tickets, boolean requireSimulation) {
        Ticket ticket = getLowestTicket(tickets, requireSimulation);
        return ticket == null ? ChunkLevel.MAX_LEVEL + 1 : ticket.getTicketLevel();
    }

    @Nullable
    private static Ticket getLowestTicket(@Nullable List<Ticket> tickets, boolean requireSimulation) {
        if (tickets == null) {
            return null;
        } else {
            Ticket ticket = null;

            for (Ticket ticket1 : tickets) {
                if (ticket == null || ticket1.getTicketLevel() < ticket.getTicketLevel()) {
                    if (requireSimulation && ticket1.getType().doesSimulate()) {
                        ticket = ticket1;
                    } else if (!requireSimulation && ticket1.getType().doesLoad()) {
                        ticket = ticket1;
                    }
                }
            }

            return ticket;
        }
    }

    public void removeTicketWithRadius(TicketType ticketType, ChunkPos chunkPos, int radius) {
        Ticket ticket = new Ticket(ticketType, ChunkLevel.byStatus(FullChunkStatus.FULL) - radius);
        this.removeTicket(chunkPos.toLong(), ticket);
    }

    public void removeTicket(Ticket ticket, ChunkPos chunkPos) {
        this.removeTicket(chunkPos.toLong(), ticket);
    }

    public boolean removeTicket(long chunkPos, Ticket p_ticket) {
        List<Ticket> list = this.tickets.get(chunkPos);
        if (list == null) {
            return false;
        } else {
            boolean flag = false;
            Iterator<Ticket> iterator = list.iterator();

            while (iterator.hasNext()) {
                Ticket ticket = iterator.next();
                if (isTicketSameTypeAndLevel(p_ticket, ticket)) {
                    iterator.remove();
                    if (SharedConstants.DEBUG_VERBOSE_SERVER_EVENTS) {
                        LOGGER.debug("RTI {} {}", new ChunkPos(chunkPos), ticket);
                    }

                    flag = true;
                    break;
                }
            }

            if (!flag) {
                return false;
            } else {
                if (list.isEmpty()) {
                    this.tickets.remove(chunkPos);
                }

                if (p_ticket.getType().doesSimulate() && this.simulationChunkUpdatedListener != null) {
                    this.simulationChunkUpdatedListener.update(chunkPos, getTicketLevelAt(list, true), false);
                }

                if (p_ticket.getType().doesLoad() && this.loadingChunkUpdatedListener != null) {
                    this.loadingChunkUpdatedListener.update(chunkPos, getTicketLevelAt(list, false), false);
                }

                if (p_ticket.getType().equals(TicketType.FORCED)) {
                    this.updateForcedChunks();
                }
                if (p_ticket.getType().forceNaturalSpawning()) this.updateForcedNaturalSpawning();

                this.setDirty();
                return true;
            }
        }
    }

    private void updateForcedChunks() {
        this.chunksWithForcedTickets = this.getAllChunksWithTicketThat(p_393889_ -> p_393889_.getType().equals(TicketType.FORCED));
    }

    public String getTicketDebugString(long chunkPos, boolean requireSimulation) {
        List<Ticket> list = this.getTickets(chunkPos);
        Ticket ticket = getLowestTicket(list, requireSimulation);
        return ticket == null ? "no_ticket" : ticket.toString();
    }

    public void purgeStaleTickets(ChunkMap map) {
        this.removeTicketIf((p_432597_, p_432598_) -> {
            if (this.canTicketExpire(map, p_432597_, p_432598_)) {
                p_432597_.decreaseTicksLeft();
                return p_432597_.isTimedOut();
            } else {
                return false;
            }
        }, null);
        this.setDirty();
    }

    private boolean canTicketExpire(ChunkMap chunkMap, Ticket ticket, long chunkPos) {
        if (!ticket.getType().hasTimeout()) {
            return false;
        } else if (ticket.getType().canExpireIfUnloaded()) {
            return true;
        } else {
            ChunkHolder chunkholder = chunkMap.getUpdatingChunkIfPresent(chunkPos);
            return chunkholder == null || chunkholder.isReadyForSaving();
        }
    }

    public void deactivateTicketsOnClosing() {
        this.removeTicketIf((p_394604_, p_435717_) -> p_394604_.getType() != TicketType.UNKNOWN, this.deactivatedTickets);
        blockForcedChunks.deactivateTicketsOnClosing();
        entityForcedChunks.deactivateTicketsOnClosing();
    }

    public void removeTicketIf(TicketStorage.TicketPredicate predicate, @Nullable Long2ObjectOpenHashMap<List<Ticket>> tickets) {
        ObjectIterator<Entry<List<Ticket>>> objectiterator = this.tickets.long2ObjectEntrySet().fastIterator();
        boolean flag = false;
        boolean updateNaturalSpawning = false;

        while (objectiterator.hasNext()) {
            Entry<List<Ticket>> entry = objectiterator.next();
            Iterator<Ticket> iterator = entry.getValue().iterator();
            long i = entry.getLongKey();
            boolean flag1 = false;
            boolean flag2 = false;

            while (iterator.hasNext()) {
                Ticket ticket = iterator.next();
                if (predicate.test(ticket, i)) {
                    if (tickets != null) {
                        List<Ticket> list = tickets.computeIfAbsent(i, p_394107_ -> new ObjectArrayList<>(entry.getValue().size()));
                        list.add(ticket);
                    }

                    iterator.remove();
                    if (ticket.getType().doesLoad()) {
                        flag2 = true;
                    }

                    if (ticket.getType().doesSimulate()) {
                        flag1 = true;
                    }

                    if (ticket.getType().equals(TicketType.FORCED)) {
                        flag = true;
                    }
                    if (ticket.getType().forceNaturalSpawning()) updateNaturalSpawning = true;
                }
            }

            if (flag2 || flag1) {
                if (flag2 && this.loadingChunkUpdatedListener != null) {
                    this.loadingChunkUpdatedListener.update(i, getTicketLevelAt(entry.getValue(), false), false);
                }

                if (flag1 && this.simulationChunkUpdatedListener != null) {
                    this.simulationChunkUpdatedListener.update(i, getTicketLevelAt(entry.getValue(), true), false);
                }

                this.setDirty();
                if (entry.getValue().isEmpty()) {
                    objectiterator.remove();
                }
            }
        }

        if (flag) {
            this.updateForcedChunks();
        }
        if (updateNaturalSpawning) this.updateForcedNaturalSpawning();
    }

    public void replaceTicketLevelOfType(int level, TicketType type) {
        List<Pair<Ticket, Long>> list = new ArrayList<>();

        for (Entry<List<Ticket>> entry : this.tickets.long2ObjectEntrySet()) {
            for (Ticket ticket : entry.getValue()) {
                if (ticket.getType() == type) {
                    list.add(Pair.of(ticket, entry.getLongKey()));
                }
            }
        }

        for (Pair<Ticket, Long> pair : list) {
            Long olong = pair.getSecond();
            Ticket ticket1 = pair.getFirst();
            this.removeTicket(olong, ticket1);
            TicketType tickettype = ticket1.getType();
            this.addTicket(olong, new Ticket(tickettype, level));
        }
    }

    public boolean updateChunkForced(ChunkPos chunkPos, boolean add) {
        Ticket ticket = new Ticket(TicketType.FORCED, ChunkMap.FORCED_TICKET_LEVEL);
        return add ? this.addTicket(chunkPos.toLong(), ticket) : this.removeTicket(chunkPos.toLong(), ticket);
    }

    public LongSet getForceLoadedChunks() {
        return this.chunksWithForcedTickets;
    }

    private LongSet getAllChunksWithTicketThat(Predicate<Ticket> predicate) {
        LongOpenHashSet longopenhashset = new LongOpenHashSet();

        for (Entry<List<Ticket>> entry : Long2ObjectMaps.fastIterable(this.tickets)) {
            for (Ticket ticket : entry.getValue()) {
                if (predicate.test(ticket)) {
                    longopenhashset.add(entry.getLongKey());
                    break;
                }
            }
        }

        return longopenhashset;
    }

    @FunctionalInterface
    public interface ChunkUpdated {
        void update(long chunkPos, int ticketLevel, boolean isDecreasing);
    }

    public interface TicketPredicate {
        boolean test(Ticket ticket, long chunkPos);
    }

    // Neo: Keep track of forced loaded chunks caused by entities or blocks.
    private final net.neoforged.neoforge.common.world.chunk.ForcedChunkManager.TicketTracker<net.minecraft.core.BlockPos> blockForcedChunks = new net.neoforged.neoforge.common.world.chunk.ForcedChunkManager.TicketTracker<>(this, net.neoforged.neoforge.common.NeoForgeMod.BLOCK_TICKET, net.neoforged.neoforge.common.NeoForgeMod.BLOCK_WITH_NATURAL_SPAWNING_TICKET);
    private final net.neoforged.neoforge.common.world.chunk.ForcedChunkManager.TicketTracker<java.util.UUID> entityForcedChunks = new net.neoforged.neoforge.common.world.chunk.ForcedChunkManager.TicketTracker<>(this, net.neoforged.neoforge.common.NeoForgeMod.ENTITY_TICKET, net.neoforged.neoforge.common.NeoForgeMod.ENTITY_WITH_NATURAL_SPAWNING_TICKET);
    private LongSet chunksWithForceNaturalSpawning = new LongOpenHashSet();

    public net.neoforged.neoforge.common.world.chunk.ForcedChunkManager.TicketTracker<net.minecraft.core.BlockPos> getBlockForcedChunks() {
        return this.blockForcedChunks;
    }

    public net.neoforged.neoforge.common.world.chunk.ForcedChunkManager.TicketTracker<java.util.UUID> getEntityForcedChunks() {
        return this.entityForcedChunks;
    }

    private void updateForcedNaturalSpawning() {
        this.chunksWithForceNaturalSpawning = this.getAllChunksWithTicketThat(ticket -> ticket.getType().forceNaturalSpawning());
    }

    public boolean shouldForceNaturalSpawning(ChunkPos chunkPos) {
        return chunksWithForceNaturalSpawning.contains(chunkPos.toLong());
    }
}
