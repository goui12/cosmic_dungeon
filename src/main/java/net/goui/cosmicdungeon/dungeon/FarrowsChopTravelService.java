package net.goui.cosmicdungeon.dungeon;

import net.goui.cosmicdungeon.achievement.CosmicAchievementIds;
import net.goui.cosmicdungeon.achievement.CosmicAdvancementUtil;
import net.goui.cosmicdungeon.component.ModDataComponents;
import net.goui.cosmicdungeon.item.ModItems;
import net.goui.cosmicdungeon.rift.DefaultRiftDestinations;
import net.goui.cosmicdungeon.rift.SafeTeleportUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.Set;

public final class FarrowsChopTravelService {
    private FarrowsChopTravelService() {}

    public static Optional<ItemStack> cookAndLeaveDungeon(ServerPlayer player) {
        Optional<DungeonRunRegistryData.RunRecord> runOpt = DungeonLifecycleService.findActiveRunForPlayer(player);
        if (runOpt.isEmpty() || !runOpt.get().containsDimension(player.level().dimension())) {
            player.sendSystemMessage(Component.literal("Farrow's Chop can only bind while you are inside your active dungeon.")
                    .withStyle(ChatFormatting.RED));
            return Optional.empty();
        }

        DungeonRunRegistryData.RunRecord run = runOpt.get();
        DungeonReturnTarget target = new DungeonReturnTarget(player.getUUID(), run.runId(),
                player.level().dimension().location().toString(), player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot());

        if (!DefaultRiftDestinations.teleportToMainVillage(player)) return Optional.empty();

        ItemStack cooked = new ItemStack(ModItems.FARROWS_CHOP.get());
        cooked.set(ModDataComponents.DUNGEON_RETURN_TARGET.get(), target);
        CosmicAdvancementUtil.grant(player, CosmicAchievementIds.NOSTALGIA_BAIT);
        return Optional.of(cooked);
    }

    public static boolean returnToDungeon(ServerPlayer player, ItemStack chop) {
        DungeonReturnTarget target = chop.get(ModDataComponents.DUNGEON_RETURN_TARGET.get());
        if (target == null) {
            player.sendSystemMessage(Component.literal("This Farrow's Chop has no remembered dungeon location.")
                    .withStyle(ChatFormatting.RED));
            return false;
        }
        if (!player.getUUID().equals(target.owner())) {
            player.sendSystemMessage(Component.literal("This Farrow's Chop remembers another dungeoneer.")
                    .withStyle(ChatFormatting.RED));
            return false;
        }

        DungeonRunRegistryData.RunRecord run = DungeonRunRegistryData.get(player.level().getServer())
                .getRun(target.runId()).orElse(null);
        if (run == null || run.stateEnum() != DungeonRunState.ACTIVE || !run.containsPlayer(player.getUUID())) {
            player.sendSystemMessage(Component.literal("The dungeon lifecycle remembered by this chop is no longer active.")
                    .withStyle(ChatFormatting.RED));
            return false;
        }

        ResourceLocation id = ResourceLocation.tryParse(target.dimensionId());
        if (id == null) return false;
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, id);
        if (!run.containsDimension(key)) {
            player.sendSystemMessage(Component.literal("The remembered location does not belong to your active dungeon.")
                    .withStyle(ChatFormatting.RED));
            return false;
        }
        ServerLevel level = player.level().getServer().getLevel(key);
        if (level == null) return false;
        BlockPos rememberedBlock = BlockPos.containing(target.x(), target.y(), target.z());
        BlockPos safe = SafeTeleportUtil.findSafeTeleportPos(level, rememberedBlock);
        if (safe == null || !safe.equals(rememberedBlock)) {
            player.sendSystemMessage(Component.literal("Your exact remembered dungeon location is no longer safe.")
                    .withStyle(ChatFormatting.RED));
            return false;
        }

        boolean teleported = player.teleportTo(level, target.x(), target.y(), target.z(), Set.of(), target.yaw(), target.pitch(), true);
        if (teleported) player.awardStat(Stats.ITEM_USED.get(ModItems.FARROWS_CHOP.get()));
        return teleported;
    }
}
