package net.goui.cosmicdungeon.dungeon;

import net.goui.cosmicdungeon.achievement.CosmicAchievementIds;
import net.goui.cosmicdungeon.achievement.CosmicAdvancementUtil;
import net.goui.cosmicdungeon.component.ModDataComponents;
import net.goui.cosmicdungeon.item.ModItems;
import net.goui.cosmicdungeon.rift.DefaultRiftDestinations;
import net.goui.cosmicdungeon.rift.SafeTeleportUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class FarrowsChopTravelService {
    private FarrowsChopTravelService() {}

    /** Successful delivery is performed here; the empty stack result keeps old callers compatible. */
    public static Optional<ItemStack> cookAndLeaveDungeon(ServerPlayer player, ItemStack rawChop, BlockPos campfire) {
        if(!net.goui.cosmicdungeon.progression.ProgressionService.hasVillageAccess(player))return Optional.empty();
        var runOpt=DungeonLifecycleService.findActiveRunForPlayer(player);
        if(runOpt.isEmpty()||!runOpt.get().dungeonId().equals("dungeon_1")
                ||!runOpt.get().containsDimension(player.level().dimension())
                ||!rawChop.is(ModItems.RAW_FARROWS_CHOP.get())||!ChopOwnershipService.owned(player,rawChop))
            return Optional.empty();
        var state=player.level().getBlockState(campfire);
        if(!(state.getBlock() instanceof net.minecraft.world.level.block.CampfireBlock)
                ||!state.getValue(net.minecraft.world.level.block.CampfireBlock.LIT))return Optional.empty();
        var run=runOpt.get();var escrow=DungeonInventoryEscrowData.get(player.level().getServer());
        var previous=escrow.get(run.runId(),player.getUUID());
        if(previous.filter(DungeonInventoryEscrowData.Entry::outsideActive).isPresent())return Optional.empty();
        if(!net.goui.cosmicdungeon.transaction.InventoryTransactionGuard.beforeInventoryChange(player))return Optional.empty();
        var target=new DungeonReturnTarget(player.getUUID(),run.runId(),player.level().dimension().location().toString(),
                player.getX(),player.getY(),player.getZ(),player.getYRot(),player.getXRot());
        var cooked=new ItemStack(ModItems.FARROWS_CHOP.get());
        cooked.set(ModDataComponents.DUNGEON_RETURN_TARGET.get(),target);
        cooked.set(ModDataComponents.COORDINATES.get(),campfire.immutable());
        cooked.set(ModDataComponents.CHOP_OWNER.get(),player.getUUID());
        cooked.set(ModDataComponents.CHOP_TOKEN.get(),rawChop.get(ModDataComponents.CHOP_TOKEN.get()));
        CompoundTag before=saveInventory(player);
        rawChop.shrink(1);
        CompoundTag dungeonInventory=saveInventory(player);
        CompoundTag outsideInventory=previous.map(DungeonInventoryEscrowData.Entry::outsideInventory)
                .orElseGet(()->run.snapshotFor(player.getUUID()).map(DungeonPlayerRunSnapshot::inventoryNbt)
                        .map(CompoundTag::copy).orElseGet(CompoundTag::new));
        restoreInventory(player,outsideInventory);
        if(!player.getInventory().add(cooked)||!cooked.isEmpty()){
            restoreInventory(player,before);
            player.sendSystemMessage(Component.literal("Your outside inventory needs a free slot for the return Chop."));
            return Optional.empty();
        }
        escrow.put(new DungeonInventoryEscrowData.Entry(run.runId(),player.getUUID(),dungeonInventory,outsideInventory,true));
        if(!DefaultRiftDestinations.teleportToMainVillage(player)){
            restoreInventory(player,before);
            previous.ifPresentOrElse(escrow::put,()->escrow.remove(run.runId(),player.getUUID()));
            return Optional.empty();
        }
        ChopOwnershipData.get(player.level().getServer()).bindRun(player.getUUID(),run.runId());
        CosmicAdvancementUtil.grant(player,CosmicAchievementIds.NOSTALGIA_BAIT);
        return Optional.of(ItemStack.EMPTY);
    }

    public static boolean returnToDungeon(ServerPlayer player, ItemStack chop) {
        DungeonReturnTarget target = validateTarget(player, chop);
        if (target == null) return false;

        DungeonRunRegistryData.RunRecord run = DungeonRunRegistryData.get(player.level().getServer())
                .getRun(target.runId()).orElse(null);
        ResourceLocation id = ResourceLocation.tryParse(target.dimensionId());
        if (run == null || id == null) return false;
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, id);
        ServerLevel level = player.level().getServer().getLevel(key);
        if (level == null) return false;
        BlockPos campfire=chop.get(ModDataComponents.COORDINATES.get());
        if(campfire!=null && !(level.getBlockState(campfire).getBlock() instanceof net.minecraft.world.level.block.CampfireBlock)){
            player.sendSystemMessage(Component.literal("The bound campfire no longer exists."));return false;
        }
        BlockPos rememberedBlock = BlockPos.containing(target.x(), target.y(), target.z());
        BlockPos safe = SafeTeleportUtil.findSafeTeleportPos(level, rememberedBlock);
        if (safe == null || !safe.equals(rememberedBlock)) {
            player.sendSystemMessage(Component.literal("Your exact remembered dungeon location is no longer safe.")
                    .withStyle(ChatFormatting.RED));
            return false;
        }

        DungeonInventoryEscrowData escrow = DungeonInventoryEscrowData.get(player.level().getServer());
        DungeonInventoryEscrowData.Entry entry = escrow.get(run.runId(), player.getUUID()).orElse(null);
        if (entry == null || !entry.outsideActive()) {
            player.sendSystemMessage(Component.literal("Your separated dungeon inventory is unavailable.")
                    .withStyle(ChatFormatting.RED));
            return false;
        }

        if(!net.goui.cosmicdungeon.transaction.InventoryTransactionGuard.beforeInventoryChange(player))return false;
        ItemStack consumedChop=chop.copy();
        CompoundTag outsideBefore = saveInventory(player);
        chop.shrink(1);
        CompoundTag outsideAfter = saveInventory(player);
        restoreInventory(player, entry.dungeonInventory());
        escrow.put(entry.withOutsideInventory(outsideAfter, false));

        boolean teleported = player.teleportTo(level, target.x(), target.y(), target.z(), Set.of(), target.yaw(), target.pitch(), true);
        if (!teleported) {
            restoreInventory(player, outsideBefore);
            escrow.put(entry);
        }
        if(teleported)ChopOwnershipService.consumed(player,consumedChop);
        return teleported;
    }

    public static boolean isOutsideEscrow(ServerPlayer player) {
        if (player == null) return false;
        return DungeonLifecycleService.findActiveRunForPlayer(player)
                .flatMap(run -> DungeonInventoryEscrowData.get(player.level().getServer()).get(run.runId(), player.getUUID()))
                .map(DungeonInventoryEscrowData.Entry::outsideActive).orElse(false);
    }

    public static void syncOutsideInventory(ServerPlayer player) {
        if (player == null) return;
        DungeonLifecycleService.findActiveRunForPlayer(player).ifPresent(run -> {
            DungeonInventoryEscrowData data = DungeonInventoryEscrowData.get(player.level().getServer());
            data.get(run.runId(), player.getUUID()).filter(DungeonInventoryEscrowData.Entry::outsideActive)
                    .ifPresent(entry -> data.put(entry.withOutsideInventory(saveInventory(player), true)));
        });
    }

    public static Optional<CompoundTag> takeOutsideInventoryForCleanup(ServerPlayer onlinePlayer,
                                                                       net.minecraft.server.MinecraftServer server,
                                                                       long runId, UUID playerId) {
        DungeonInventoryEscrowData data = DungeonInventoryEscrowData.get(server);
        DungeonInventoryEscrowData.Entry entry = data.get(runId, playerId).orElse(null);
        if (entry == null) return Optional.empty();
        CompoundTag outside = entry.outsideActive() && onlinePlayer != null
                ? saveInventory(onlinePlayer) : entry.outsideInventory().copy();
        data.remove(runId, playerId);
        return Optional.of(outside);
    }

    private static DungeonReturnTarget validateTarget(ServerPlayer player, ItemStack chop) {
        DungeonReturnTarget target = chop.get(ModDataComponents.DUNGEON_RETURN_TARGET.get());
        if (target == null || !player.getUUID().equals(target.owner()) || !ChopOwnershipService.owned(player,chop)) {
            player.sendSystemMessage(Component.literal(target == null
                    ? "This Farrow's Chop has no remembered dungeon location."
                    : "This Farrow's Chop remembers another dungeoneer.").withStyle(ChatFormatting.RED));
            return null;
        }
        DungeonRunRegistryData.RunRecord run = DungeonRunRegistryData.get(player.level().getServer())
                .getRun(target.runId()).orElse(null);
        ResourceLocation id = ResourceLocation.tryParse(target.dimensionId());
        ResourceKey<Level> key = id == null ? null : ResourceKey.create(Registries.DIMENSION, id);
        if (run == null || run.stateEnum() != DungeonRunState.ACTIVE || !run.containsPlayer(player.getUUID())
                || key == null || !run.containsDimension(key)) {
            player.sendSystemMessage(Component.literal("The dungeon lifecycle remembered by this chop is no longer valid.")
                    .withStyle(ChatFormatting.RED));
            return null;
        }
        return target;
    }

    private static CompoundTag saveInventory(ServerPlayer player) {
        NonNullList<ItemStack> items = NonNullList.withSize(player.getInventory().getContainerSize(), ItemStack.EMPTY);
        for (int i = 0; i < items.size(); i++) items.set(i, player.getInventory().getItem(i).copy());
        TagValueOutput output = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
        ContainerHelper.saveAllItems(output, items);
        return output.buildResult();
    }

    private static void restoreInventory(ServerPlayer player, CompoundTag inventory) {
        NonNullList<ItemStack> items = NonNullList.withSize(player.getInventory().getContainerSize(), ItemStack.EMPTY);
        if (inventory != null && !inventory.isEmpty()) {
            ValueInput input = TagValueInput.create(ProblemReporter.DISCARDING, player.level().registryAccess(), inventory);
            ContainerHelper.loadAllItems(input, items);
        }
        for (int i = 0; i < items.size(); i++) player.getInventory().setItem(i, items.get(i));
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
        player.containerMenu.broadcastChanges();
    }
}
