package net.goui.cosmicdungeon.advancement;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.dungeon.DungeonLifecycleService;
import net.goui.cosmicdungeon.dungeon.DungeonRunProgressData;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.registries.BuiltInRegistries;
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

    private static final Map<UUID, Long> LAST_MASK = new HashMap<>();
    private static final Map<Bloom, AdvancementHolder> ADV_CACHE = new EnumMap<>(Bloom.class);

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, path);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        if (sp.level().isClientSide()) return;
        if ((sp.tickCount % 20) != 0) return;

        var runOpt = DungeonLifecycleService.findActiveRunForPlayer(sp);
        if (runOpt.isEmpty()) {
            LAST_MASK.remove(sp.getUUID());
            return;
        }

        var run = runOpt.get();
        if (!run.containsDimension(sp.level().dimension())) {
            LAST_MASK.remove(sp.getUUID());
            return;
        }

        if(run.dungeonId().equals("dungeon_1")&&!net.goui.cosmicdungeon.dungeon.d1.D1Members.inside(sp,run))return;
        long curr = computeBloomMask(sp);
        long prev = LAST_MASK.getOrDefault(sp.getUUID(), -1L);

        if (curr != prev) {
            MinecraftServer server = sp.level().getServer();
            if (server != null) {
                DungeonRunProgressData.get(server).setBloomMask(run.runId(), sp.getUUID(), curr);
            }
            LAST_MASK.put(sp.getUUID(), curr);
        }
        if (run.dungeonId().equals("dungeon_1")) {
            var server=sp.level().getServer();
            var objectives=net.goui.cosmicdungeon.dungeon.d1.D1RunData.get(server);
            long now=server.overworld().getGameTime();
            for(Bloom bloom:Bloom.values()){
                if((curr&(1L<<bloom.ordinal()))!=0&&objectives.recordUnique(run.runId(),"spectral_seen",bloom.name()))
                    objectives.setValue(run.runId(),"spectral_time:"+bloom.name(),Long.toString(now));
            }
            if(net.goui.cosmicdungeon.dungeon.d1.D1Members.inside(sp,run)){
                long lastActive=net.goui.cosmicdungeon.dungeon.DungeonAfkService.lastActive(sp.getUUID());
                long window=net.goui.cosmicdungeon.Config.BLOOM_ACTIVITY_WINDOW_SECONDS.get()*20L;
                String credit="spectral_credit:"+sp.getUUID();
                for(Bloom bloom:Bloom.values()){
                    var times=objectives.values(run.runId(),"spectral_time:"+bloom.name());
                    if(times.isEmpty())continue;
                    long collected;
                    try{collected=Long.parseLong(times.getFirst());}catch(NumberFormatException bad){continue;}
                    if(lastActive>=collected-window&&lastActive<=collected+window
                            &&objectives.recordUnique(run.runId(),credit,bloom.name())){
                        var holder=getAdvancement(server,bloom);
                        if(holder!=null)sp.getAdvancements().award(holder,"shared");
                    }
                }
                net.goui.cosmicdungeon.dungeon.d1.D1Scoreboards.update(sp,objectives.values(run.runId(),credit).size(),
                        objectives.count(run.runId(),"lesser:"+sp.getUUID()));
            }
        }
    }

    @SubscribeEvent
    public static void stopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event){
        LAST_MASK.clear();ADV_CACHE.clear();
    }

    public static void clearTemporaryBloomProgress(MinecraftServer server, ServerPlayer sp) {
        if (server == null || sp == null) return;

        LAST_MASK.remove(sp.getUUID());

        for (Bloom bloom : Bloom.values()) {
            AdvancementHolder holder = getAdvancement(server, bloom);
            if (holder == null) continue;

            var progress = sp.getAdvancements().getOrStartProgress(holder);
            if (!progress.isDone()) continue;

            sp.getAdvancements().revoke(holder, "shared");
        }
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
            ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
            if (itemId.equals(key)) return true;
        }
        return false;
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