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
        try { return cookAndLeaveDungeonChecked(player,rawChop,campfire); }
        catch(RuntimeException failure){
            com.mojang.logging.LogUtils.getLogger().error("Chop travel refused; inventory evidence retained for {}",player.getUUID(),failure);
            player.sendSystemMessage(Component.literal("This Chop journey needs a developer save check; your stored items are retained."));
            return Optional.empty();
        }
    }
    private static Optional<ItemStack> cookAndLeaveDungeonChecked(ServerPlayer player, ItemStack rawChop, BlockPos campfire) {
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
        var cooked=rawChop.transmuteCopy(ModItems.FARROWS_CHOP.get(),1);
        cooked.set(ModDataComponents.DUNGEON_RETURN_TARGET.get(),target);
        cooked.set(ModDataComponents.COORDINATES.get(),campfire.immutable());
        cooked.set(ModDataComponents.CHOP_OWNER.get(),player.getUUID());
        cooked.set(ModDataComponents.CHOP_TOKEN.get(),rawChop.get(ModDataComponents.CHOP_TOKEN.get()));
        int slot=ChopTravelRecovery.slot(player,rawChop);if(slot<0)return Optional.empty();
        var before=ChopTravelRecovery.saveInventory(player);
        var dungeonItems=ChopTravelRecovery.inventory(player);dungeonItems.get(slot).shrink(1);
        var dungeonInventory=ChopTravelRecovery.encode(player,dungeonItems);
        var outsideInventory=previous.map(DungeonInventoryEscrowData.Entry::outsideInventory)
                .orElseGet(()->run.snapshotFor(player.getUUID()).map(DungeonPlayerRunSnapshot::inventoryNbt).map(CompoundTag::copy).orElse(null));
        if(outsideInventory==null)return Optional.empty();
        var outsideItems=ChopTravelRecovery.decode(player,outsideInventory);
        if(outsideItems.stream().anyMatch(ChopOwnershipService::isChop)){
            player.sendSystemMessage(Component.literal("Your outside inventory contains an older Chop; a developer must review it before travel."));
            return Optional.empty();
        }
        if(!ChopTravelRecovery.insert(outsideItems,cooked)){
            player.sendSystemMessage(Component.literal("Your outside inventory needs a free slot for the return Chop."));
            return Optional.empty();
        }
        var village=DefaultRiftDestinations.resolveMainVillage(player.level().getServer()).orElse(null);
        if(village==null)return Optional.empty();
        var safe=SafeTeleportUtil.findSafeTeleportPos(village.level(),village.pos());if(safe==null)return Optional.empty();
        var after=ChopTravelRecovery.encode(player,outsideItems);
        var nextEscrow=new DungeonInventoryEscrowData.Entry(run.runId(),player.getUUID(),dungeonInventory,after,true);
        var owners=ChopOwnershipData.get(player.level().getServer());var owner=owners.entry(player.getUUID());
        var plan=ChopTravelPlan.create(player.getUUID(),run.runId(),"leave",before,after,ChopTravelRecovery.pose(player),
                ChopTravelPlan.pose(village.level().dimension().location().toString(),safe.getX()+0.5,safe.getY(),safe.getZ()+0.5,player.getYRot(),player.getXRot()),
                DungeonInventoryEscrowData.image(previous.orElse(null)),DungeonInventoryEscrowData.image(nextEscrow),
                owners.image(player.getUUID()),ChopOwnershipData.entryImage(new ChopOwnershipData.Entry(owner.token(),run.runId(),false)),null);
        return ChopTravelRecovery.execute(player,plan)?Optional.of(ItemStack.EMPTY):Optional.empty();
    }

    public static boolean returnToDungeon(ServerPlayer player, ItemStack chop) {
        try { return returnToDungeonChecked(player,chop); }
        catch(RuntimeException failure){
            com.mojang.logging.LogUtils.getLogger().error("Chop travel refused; inventory evidence retained for {}",player.getUUID(),failure);
            player.sendSystemMessage(Component.literal("This Chop journey needs a developer save check; your stored items are retained."));
            return false;
        }
    }
    private static boolean returnToDungeonChecked(ServerPlayer player, ItemStack chop) {
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
        if(campfire==null || !(level.getBlockState(campfire).getBlock() instanceof net.minecraft.world.level.block.CampfireBlock)){
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
        int slot=ChopTravelRecovery.slot(player,chop);if(slot<0)return false;
        var before=ChopTravelRecovery.saveInventory(player);
        var outsideItems=ChopTravelRecovery.inventory(player);outsideItems.get(slot).shrink(1);
        var outsideAfter=ChopTravelRecovery.encode(player,outsideItems);
        // Decode before reservation so a bad legacy image cannot consume the return entitlement.
        var after=ChopTravelRecovery.encode(player,ChopTravelRecovery.decode(player,entry.dungeonInventory()));
        var fire=new CompoundTag();fire.put("pos",BlockPos.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE,campfire).getOrThrow());
        var plan=ChopTravelPlan.create(player.getUUID(),run.runId(),"return",before,after,ChopTravelRecovery.pose(player),
                ChopTravelPlan.pose(target.dimensionId(),target.x(),target.y(),target.z(),target.yaw(),target.pitch()),
                DungeonInventoryEscrowData.image(entry),DungeonInventoryEscrowData.image(entry.withOutsideInventory(outsideAfter,false)),
                ChopOwnershipData.get(player.level().getServer()).image(player.getUUID()),new CompoundTag(),fire);
        return ChopTravelRecovery.execute(player,plan);
    }

    public static boolean isOutsideEscrow(ServerPlayer player) {
        if (player == null) return false;
        return DungeonLifecycleService.findActiveRunForPlayer(player)
                .flatMap(run -> DungeonInventoryEscrowData.get(player.level().getServer()).get(run.runId(), player.getUUID()))
                .map(DungeonInventoryEscrowData.Entry::outsideActive).orElse(false);
    }

    public static void syncOutsideInventory(ServerPlayer player) {
        if (player == null || net.goui.cosmicdungeon.dungeon.d1.D1WatsonRecovery.blocked(player) || ChopTravelRecovery.blocked(player) || DungeonInventoryHandoffs.blocked(player)) return;
        DungeonLifecycleService.findActiveRunForPlayer(player).ifPresent(run -> {
            DungeonInventoryEscrowData data = DungeonInventoryEscrowData.get(player.level().getServer());
            data.get(run.runId(), player.getUUID()).filter(DungeonInventoryEscrowData.Entry::outsideActive)
                    .ifPresent(entry -> {
                        var current=saveInventory(player);
                        if(!entry.outsideInventory().equals(current))data.put(entry.withOutsideInventory(current,true));
                    });
        });
    }

    public static Optional<CompoundTag> takeOutsideInventoryForCleanup(ServerPlayer onlinePlayer,
                                                                       net.minecraft.server.MinecraftServer server,
                                                                       long runId, UUID playerId) {
        if (DungeonRunRegistryData.get(server).getRun(runId).map(run -> run.dungeonId().equals("dungeon_1")).orElse(false))
            throw new IllegalStateException("D1 cleanup must use its durable inventory handoff");
        DungeonInventoryEscrowData data = DungeonInventoryEscrowData.get(server);
        DungeonInventoryEscrowData.Entry entry = data.get(runId, playerId).orElse(null);
        if (entry == null) return Optional.empty();
        CompoundTag outside = entry.outsideActive() && onlinePlayer != null
                ? saveInventory(onlinePlayer) : entry.outsideInventory().copy();
        if(ChopTravelRecovery.pending(server,playerId))throw new IllegalStateException("Chop journey must settle before cleanup");
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

    private static CompoundTag saveInventory(ServerPlayer player){return ChopTravelRecovery.saveInventory(player);}
    // TODO(M43/M102, licensed TEST): Batch28 routes D1 cleanup through DungeonInventoryHandoffs.
    // Q&A D20/D24 requires success/failure/offline delivery and Raw entitlement across native
    // restarts. Inject failure at journal/stash/owner/escrow/player/ack writes on complete save copies.
    // The old helper above is retained only for deferred non-D1 lifecycle compatibility.
}
