package net.goui.cosmicdungeon.dungeon;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.achievement.CosmicAdvancementUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import java.util.*;

/** Completion history is durable before the inventory handoff is marked world-ready. */
@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID)
public final class DungeonCompletionAwards {
    private DungeonCompletionAwards() {}
    public static ResourceLocation id(DungeonDifficulty tier) {
        return ResourceLocation.fromNamespaceAndPath("cosmicdungeon", "achievements/dungeon_1_" + tier.name().toLowerCase(Locale.ROOT));
    }
    public static Set<DungeonDifficulty> earned(List<DungeonRunProgressData.CompletionRecord> records) {
        Set<DungeonDifficulty> tiers = EnumSet.noneOf(DungeonDifficulty.class);
        for (var record : records) if ("dungeon_1".equals(record.dungeonId()))
            DungeonDifficulty.parse(record.difficulty()).ifPresent(t -> tiers.addAll(t.completedTiers()));
        return tiers;
    }
    public static void completed(MinecraftServer server, InventoryHandoffPlan plan) {
        if (!plan.kind().equals("cleanup") || !plan.reason().equals("COMPLETED")) return;
        var progress = DungeonRunProgressData.get(server);
        for (var tier : plan.difficulty().completedTiers()) progress.markCompleted(plan.owner(), "dungeon_1", tier.name());
        if (!progress.flushVerified()) throw new IllegalStateException("Completion history could not be verified");
        var player = server.getPlayerList().getPlayer(plan.owner());
        if (player != null) award(player);
    }
    public static void award(ServerPlayer player) {
        for (var tier : earned(DungeonRunProgressData.get(player.level().getServer()).listCompletionsFor(player.getUUID())))
            CosmicAdvancementUtil.grant(player, id(tier));
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) award(player);
    }
}
