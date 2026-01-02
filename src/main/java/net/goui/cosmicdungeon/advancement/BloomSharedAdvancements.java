package net.goui.cosmicdungeon.advancement;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID)
public final class BloomSharedAdvancements {
    private BloomSharedAdvancements() {}

    /**
     * These MUST be ITEM ids (what appears in inventories).
     * For normal blocks, the block id and the block-item id are usually identical.
     */
    private enum Bloom {
        QUIET_ASSURANCE(
                rl("bloom_of_quiet_assurance"),
                rl("blooms/bloom_of_quiet_assurance")
        ),
        GENTLE_LIES(
                rl("bloom_of_gentle_lies"),
                rl("blooms/bloom_of_gentle_lies")
        ),
        WANING_MERCY(
                rl("bloom_of_waning_mercy"),
                rl("blooms/bloom_of_waning_mercy")
        ),
        CONSTRICTING_BONDS(
                rl("bloom_of_constricting_bonds"),
                rl("blooms/bloom_of_constricting_bonds")
        ),
        UNSPOKEN_RESIGNATION(
                rl("bloom_of_unspoken_resignation"),
                rl("blooms/bloom_of_unspoken_resignation")
        ),
        ELEGY(
                rl("bloom_of_elegy"),
                rl("blooms/bloom_of_elegy")
        );

        final ResourceLocation itemId;
        final ResourceLocation advancementId;

        Bloom(ResourceLocation itemId, ResourceLocation advancementId) {
            this.itemId = itemId;
            this.advancementId = advancementId;
        }
    }

    // per-player bitmask of which blooms they had last time we checked
    private static final Map<UUID, Long> LAST_MASK = new HashMap<>();

    // cache advancement holders (rebuilt lazily)
    private static final Map<Bloom, AdvancementHolder> ADV_CACHE = new EnumMap<>(Bloom.class);

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, path);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        if (sp.level().isClientSide()) return;

        // Throttle: check once per second per player.
        if ((sp.tickCount % 20) != 0) return;

        long prev = LAST_MASK.getOrDefault(sp.getUUID(), 0L);
        long curr = computeBloomMask(sp);

        long newlyAcquired = curr & ~prev;
        if (newlyAcquired != 0L) {
            MinecraftServer server = sp.level().getServer();
            if (server != null) {
                for (Bloom bloom : Bloom.values()) {
                    long bit = (1L << bloom.ordinal());
                    if ((newlyAcquired & bit) != 0L) {
                        awardToAllOnlinePlayers(server, bloom);
                    }
                }
            }
        }

        LAST_MASK.put(sp.getUUID(), curr);
    }

    private static long computeBloomMask(ServerPlayer sp) {
        long mask = 0L;
        for (Bloom bloom : Bloom.values()) {
            if (hasItem(sp, bloom.itemId)) {
                mask |= (1L << bloom.ordinal());
            }
        }
        return mask;
    }

    private static boolean hasItem(ServerPlayer sp, ResourceLocation itemId) {
        for (int i = 0; i < sp.getInventory().getContainerSize(); i++) {
            var stack = sp.getInventory().getItem(i);
            if (stack.isEmpty()) continue;

            Item item = stack.getItem();
            ResourceLocation key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
            if (itemId.equals(key)) return true;
        }
        return false;
    }

    private static void awardToAllOnlinePlayers(MinecraftServer server, Bloom bloom) {
        AdvancementHolder holder = getAdvancement(server, bloom);
        if (holder == null) return;

        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            var progress = p.getAdvancements().getOrStartProgress(holder);
            if (progress.isDone()) continue;

            // Our JSON criteria name is "shared"
            p.getAdvancements().award(holder, "shared");
        }
    }

    private static AdvancementHolder getAdvancement(MinecraftServer server, Bloom bloom) {
        AdvancementHolder cached = ADV_CACHE.get(bloom);
        if (cached != null) return cached;

        AdvancementHolder holder = server.getAdvancements().get(bloom.advancementId);
        if (holder != null) {
            ADV_CACHE.put(bloom, holder);
        }
        return holder;
    }
}
