package net.goui.cosmicdungeon.dungeon;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.goui.cosmicdungeon.transaction.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.storage.*;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import java.util.*;

/** One synchronous owner-local transfer. Reconnect settles its saved decision before gameplay. */
@EventBusSubscriber(modid=CosmicDungeonMod.MOD_ID)
public final class ChopTravelRecovery {
    public static final String KEY="chop_travel_custody_v1",RECEIPT="chop_travel_receipt_v1";
    private static final Set<ServerPlayer> HOLDS=Collections.newSetFromMap(new WeakHashMap<>());
    private ChopTravelRecovery(){}
    private static CompoundTag root(ServerPlayer p){return p.getPersistentData().getCompoundOrEmpty(ClassData.ROOT_TAG);}
    private static DungeonInventoryEscrowData data(ServerPlayer p){return DungeonInventoryEscrowData.get(p.level().getServer());}
    public static boolean blocked(ServerPlayer p){return HOLDS.contains(p)||root(p).contains(KEY)||data(p).transition(p.getUUID())!=null;}
    public static boolean pending(MinecraftServer server,UUID owner){return DungeonInventoryEscrowData.get(server).transition(owner)!=null;}
    public static boolean readyForCleanup(MinecraftServer server,List<UUID> owners){
        for(UUID owner:owners){
            var p=server.getPlayerList().getPlayer(owner);
            if(p!=null&&blocked(p)&&!reconcile(p))return false;
            if(pending(server,owner))return false;
        }
        return true;
    }
    private static void hold(ServerPlayer p,Exception failure){
        HOLDS.add(p);com.mojang.logging.LogUtils.getLogger().error("Chop recovery held for {}; saved images preserved",p.getUUID(),failure);
        p.connection.disconnect(Component.literal("Your Chop journey needs save recovery. Reconnect to recover; if it repeats, ask a developer to review the saved journey."));
    }
    public static CompoundTag pose(ServerPlayer p){
        return ChopTravelPlan.pose(p.level().dimension().location().toString(),p.getX(),p.getY(),p.getZ(),p.getYRot(),p.getXRot());
    }
    public static boolean execute(ServerPlayer p,ChopTravelPlan plan){
        if(blocked(p)||!p.isAlive()||p.isPassenger()||!p.getUUID().equals(plan.owner()))return false;
        try{
            if(!saveInventory(p).equals(plan.tag("before"))||!pose(p).equals(plan.tag("source")))
                throw new IllegalStateException("Player changed before Chop reservation");
            decode(p,plan.tag("after"));validateDestination(p,plan);
            var data=data(p);
            if(!ChopOwnershipData.get(p.level().getServer()).image(p.getUUID()).equals(plan.tag("ownership_before"))
                    ||plan.run()>0&&!DungeonInventoryEscrowData.image(data.get(plan.run(),p.getUUID()).orElse(null)).equals(plan.tag("escrow_before")))
                throw new IllegalStateException("Chop authority changed before reservation");
            data.reserve(plan);
            if(!data.flushVerified())throw new IllegalStateException("Chop reservation save not verified");
            var next=root(p).copy();next.put(KEY,plan.reservation());p.getPersistentData().put(ClassData.ROOT_TAG,next);
            if(!PlayerSaveProof.saveWithLocation(p))throw new IllegalStateException("Chop owner preparation not verified");
            data.commit(p.getUUID(),plan.id());
            if(!data.flushVerified())throw new IllegalStateException("Chop commit save not verified");
            return reconcile(p);
        }catch(RuntimeException failure){hold(p,failure);return false;}
    }
    public static boolean reconcile(ServerPlayer p){
        if(HOLDS.contains(p))return false;
        try{
            var data=data(p);var plan=data.transition(p.getUUID());
            if(plan==null){
                if(root(p).contains(KEY))throw new IllegalStateException("Chop owner custody has no world decision");
                return true;
            }
            var custody=root(p).getCompoundOrEmpty(KEY);var receipt=root(p).getCompoundOrEmpty(RECEIPT);
            if(!plan.recoverable(custody,receipt))throw new IllegalStateException("Chop custody/receipt differs from world decision");
            if(!data.flushVerified())throw new IllegalStateException("Chop decision readback failed");
            if(plan.committed()){
                var ownership=ChopOwnershipData.get(p.level().getServer());
                if(!ownership.compareAndSetVerified(p.getUUID(),plan.tag("ownership_before"),plan.tag("ownership_after")))
                    throw new IllegalStateException("Chop ownership save not verified");
                data.applyEscrow(plan);
                if(!data.flushVerified())throw new IllegalStateException("Chop inventory escrow save not verified");
            }else if(!ChopOwnershipData.get(p.level().getServer()).image(p.getUUID()).equals(plan.tag("ownership_before"))
                    ||plan.run()>0&&!DungeonInventoryEscrowData.image(data.get(plan.run(),p.getUUID()).orElse(null)).equals(plan.tag("escrow_before")))
                throw new IllegalStateException("Cancelled Chop reservation has changed authority");
            if(!plan.receipted(receipt)){
                if(!p.isAlive())throw new IllegalStateException("Chop recovery requires a living owner");
                // Source inventory remains frozen until the commit. Unknown changes require review, never replacement.
                if(!saveInventory(p).equals(plan.tag("before")))throw new IllegalStateException("Chop source inventory differs from prepared image");
                if(plan.committed()){
                    var items=decode(p,plan.tag("after"));
                    ServerLevel destination=validateDestination(p,plan);
                    if(!plan.tag("source").equals(plan.tag("destination"))){
                        var target=plan.tag("destination");
                        if(!p.teleportTo(destination,target.getDoubleOr("x",0),target.getDoubleOr("y",0),target.getDoubleOr("z",0),
                                Set.of(),target.getFloatOr("yaw",0),target.getFloatOr("pitch",0),true))
                            throw new IllegalStateException("Chop destination refused teleport");
                    }
                    install(p,items);
                }
                var next=root(p).copy();next.remove(KEY);next.put(RECEIPT,plan.receipt());p.getPersistentData().put(ClassData.ROOT_TAG,next);
            }
            if(!PlayerSaveProof.saveWithLocation(p))throw new IllegalStateException("Chop owner receipt/position save not verified");
            if(plan.committed()&&plan.kind().equals("leave"))
                net.goui.cosmicdungeon.achievement.CosmicAdvancementUtil.grant(p,net.goui.cosmicdungeon.achievement.CosmicAchievementIds.NOSTALGIA_BAIT);
            data.acknowledge(p.getUUID(),plan.id());
            if(!data.flushVerified())throw new IllegalStateException("Chop acknowledgement save not verified");
            p.inventoryMenu.broadcastChanges();return true;
        }catch(RuntimeException failure){hold(p,failure);return false;}
    }
    private static ServerLevel validateDestination(ServerPlayer p,ChopTravelPlan plan){
        var target=plan.tag("destination");
        var key=ResourceKey.create(Registries.DIMENSION,ResourceLocation.parse(target.getStringOr("dimension","")));
        var level=p.level().getServer().getLevel(key);
        if(level==null)throw new IllegalStateException("Chop destination world missing");
        if(plan.kind().equals("adopt")||plan.kind().equals("refresh"))return level;
        var run=DungeonRunRegistryData.get(p.level().getServer()).getRun(plan.run()).orElseThrow();
        if(run.stateEnum()!=DungeonRunState.ACTIVE||!run.containsPlayer(p.getUUID()))
            throw new IllegalStateException("Chop run membership unavailable; retain journal for review");
        if(plan.kind().equals("return")){
            if(!run.containsDimension(key))throw new IllegalStateException("Return targets another instance");
            var fire=BlockPos.CODEC.parse(NbtOps.INSTANCE,plan.tag("campfire").get("pos")).getOrThrow();
            if(!(level.getBlockState(fire).getBlock() instanceof CampfireBlock))
                throw new IllegalStateException("Bound campfire missing; saved inventories retained");
        }else if(DungeonInstanceSlots.slotOf(key).isPresent()||DungeonDefinitions.byDimension(key).isPresent())
            throw new IllegalStateException("Village destination points into a dungeon");
        var pos=BlockPos.containing(target.getDoubleOr("x",0),target.getDoubleOr("y",0),target.getDoubleOr("z",0));
        var safe=net.goui.cosmicdungeon.rift.SafeTeleportUtil.findSafeTeleportPos(level,pos);
        if(!pos.equals(safe))throw new IllegalStateException("Frozen Chop destination is no longer safe");
        return level;
    }
    public static NonNullList<ItemStack> inventory(ServerPlayer p){
        var items=NonNullList.withSize(p.getInventory().getContainerSize(),ItemStack.EMPTY);
        for(int i=0;i<items.size();i++)items.set(i,p.getInventory().getItem(i).copy());return items;
    }
    public static CompoundTag encode(ServerPlayer player,NonNullList<ItemStack> items){
        var errors=new ProblemReporter.Collector();var output=TagValueOutput.createWithContext(errors,player.registryAccess());
        ContainerHelper.saveAllItems(output,items);
        if(!errors.isEmpty())throw new IllegalStateException(errors.getReport());return output.buildResult();
    }
    public static CompoundTag saveInventory(ServerPlayer p){return encode(p,inventory(p));}
    public static NonNullList<ItemStack> decode(ServerPlayer p,CompoundTag image){
        var errors=new ProblemReporter.Collector();var items=NonNullList.withSize(p.getInventory().getContainerSize(),ItemStack.EMPTY);
        ContainerHelper.loadAllItems(TagValueInput.create(errors,p.registryAccess(),image),items);
        if(!errors.isEmpty())throw new IllegalStateException(errors.getReport());
        // Reject duplicate/out-of-range slots or silently ignored native fields instead of losing items.
        if(!encode(p,items).equals(image)&&!(image.isEmpty()&&items.stream().allMatch(ItemStack::isEmpty)))
            throw new IllegalStateException("Inventory image does not round-trip exactly");
        return items;
    }
    private static void install(ServerPlayer p,NonNullList<ItemStack> items){
        for(int i=0;i<items.size();i++)p.getInventory().setItem(i,items.get(i).copy());p.getInventory().setChanged();
    }
    public static int slot(ServerPlayer p,ItemStack stack){
        for(int i=0;i<p.getInventory().getContainerSize();i++)if(p.getInventory().getItem(i)==stack)return i;return -1;
    }
    /** Insert only into the 36 ordinary slots; never place a return Chop into armor/offhand slots. */
    public static boolean insert(NonNullList<ItemStack> items,ItemStack stack){
        for(int i=0;i<Math.min(36,items.size());i++)if(items.get(i).isEmpty()){items.set(i,stack.copy());return true;}return false;
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void login(PlayerEvent.PlayerLoggedInEvent event){
        if(event.getEntity() instanceof ServerPlayer p)reconcile(p);
    }
    // TODO(M43/M102, licensed TEST): interrupt native dedicated and integrated saves at every
    // journal/player/ownership/teleport boundary. Never clear orphan custody or force a destination
    // when a partial backup lost its run/campfire. Review a complete save copy and matching images.
}
