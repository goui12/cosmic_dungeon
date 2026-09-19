package net.goui.cosmicdungeon.npc.inn;
import com.mojang.brigadier.CommandDispatcher;
import net.goui.cosmicdungeon.auth.AccessPolicy;
import net.goui.cosmicdungeon.config.VendorPricesConfig;
import net.goui.cosmicdungeon.economy.*;
import net.goui.cosmicdungeon.dungeon.*;
import net.goui.cosmicdungeon.progression.ProgressionService;
import net.goui.cosmicdungeon.vendor.VendorAssignmentService;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import java.util.*;
public final class InnService {
    private static final ResourceLocation PROFILE=ResourceLocation.fromNamespaceAndPath("cosmicdungeon","d1/save_teleport_npc");
    private record Offer(UUID vendor,long price,long expires,UUID token){}
    private static final Map<UUID,Offer> OFFERS=new HashMap<>();
    private InnService(){}
    public static boolean isBeluzon(Entity entity){return entity!=null&&PROFILE.equals(VendorAssignmentService.getProfileId(entity));}
    public static boolean bonded(ServerPlayer player){return PlayerCurrencyData.get(player.level().getServer()).hasReceipt("inn:bond",player.getUUID());}
    public static void offer(ServerPlayer player,Entity vendor){
        if(!ProgressionService.hasVillageAccess(player)){say(player,"Village access is required.");return;}
        if(vendor.getType()!=EntityType.CREAKING){say(player,"Beluzon's native Creaking placement needs developer setup.");return;}
        if(!InnData.get(player.level().getServer()).contains(player.level(),vendor.blockPosition())){say(player,"The Inn region needs developer setup.");return;}
        if(bonded(player)){say(player,"The Heart remembers you. Right-click an approved Inn bed to choose your home.");return;}
        var offer=new Offer(vendor.getUUID(),VendorPricesConfig.INN_BOND.get(),player.level().getServer().overworld().getGameTime()+600,UUID.randomUUID());
        OFFERS.put(player.getUUID(),offer);
        player.sendSystemMessage(Component.literal("Bind yourself to the First Heart for "+offer.price()+" Trace, paid once? ")
                .append(Component.literal("[Establish bond]").withStyle(s->s.withClickEvent(new ClickEvent.RunCommand("/inn bond "+offer.token())))));
    }
    private static int confirm(ServerPlayer player,String token){
        var offer=OFFERS.remove(player.getUUID());
        if(offer==null||!offer.token().toString().equals(token)||offer.expires()<player.level().getServer().overworld().getGameTime())return fail(player,"That offer expired. Speak to Beluzon again.");
        if(bonded(player))return 1;
        Entity vendor=player.level().getEntity(offer.vendor());
        if(vendor==null||!isBeluzon(vendor)||vendor.getType()!=EntityType.CREAKING||player.distanceToSqr(vendor)>64
                ||!InnData.get(player.level().getServer()).contains(player.level(),vendor.blockPosition())
                ||!ProgressionService.hasVillageAccess(player)||!CurrencyService.transactionsAllowed(player)
                ||offer.price()!=VendorPricesConfig.INN_BOND.get())return fail(player,"The Inn bond offer is no longer available.");
        long result=PlayerCurrencyData.get(player.level().getServer()).change(player.getUUID(),player.getName().getString(),
                -offer.price(),"inn_bond",vendor.getUUID().toString(),0,"inn:bond",false);
        if(result<0)return fail(player,"Not enough Trace for the Inn bond.");
        say(player,"The Heart remembers you. Right-click an approved Inn bed to choose your home.");return 1;
    }
    public static boolean claim(ServerPlayer player,BlockPos pos){
        var data=InnData.get(player.level().getServer());
        if(!data.contains(player.level(),pos)||!(player.level().getBlockState(pos).getBlock() instanceof BedBlock))return false;
        if(!bonded(player)){say(player,"Speak to Beluzon to establish your Inn bond first.");return true;}
        data.bed(player.getUUID(),pos);say(player,"Inn home registered. Use /home outside an active dungeon.");return true;
    }
    public static Optional<TeleportTransition> destination(ServerPlayer player,TeleportTransition.PostTeleportTransition after){
        if(!bonded(player))return Optional.empty();
        var data=InnData.get(player.level().getServer());var pos=data.bed(player.getUUID());
        var id=ResourceLocation.tryParse(data.dimension());if(pos==null||id==null)return Optional.empty();
        var level=player.level().getServer().getLevel(ResourceKey.create(Registries.DIMENSION,id));if(level==null)return Optional.empty();
        level.getChunkAt(pos);
        if(!data.contains(level,pos)||!(level.getBlockState(pos).getBlock() instanceof BedBlock))return Optional.empty();
        return BedBlock.findStandUpPosition(EntityType.PLAYER,level,pos,level.getBlockState(pos).getValue(BedBlock.FACING),player.getYRot()).map(place->
                new TeleportTransition(level,place,Vec3.ZERO,player.getYRot(),player.getXRot(),after));
    }
    public static TeleportTransition fallback(ServerPlayer player,TeleportTransition current,TeleportTransition.PostTeleportTransition after){
        if(DungeonRunRegistryData.get(player.level().getServer()).findRunForPlayer(player.getUUID()).isPresent())return current;
        if(player.getRespawnConfig()!=null&&!current.missingRespawnBlock())return current;
        return destination(player,after).orElse(current);
    }
    private static int home(ServerPlayer player){
        if(!player.isAlive()||player.isPassenger()||DungeonRunRegistryData.get(player.level().getServer()).findRunForPlayer(player.getUUID()).isPresent())
            return fail(player,"Finish your dungeon journey before using /home.");
        var target=destination(player,TeleportTransition.DO_NOTHING).orElse(null);
        if(target==null)return fail(player,"Choose a usable approved Inn bed after establishing your bond.");
        player.closeContainer();var p=target.position();
        return player.teleportTo(target.newLevel(),p.x,p.y,p.z,Set.of(),target.yRot(),target.xRot(),false)?1:0;
    }
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher){
        dispatcher.register(Commands.literal("home").executes(c->home(c.getSource().getPlayerOrException())));
        dispatcher.register(Commands.literal("inn")
                .then(Commands.literal("bond").then(Commands.argument("token",com.mojang.brigadier.arguments.StringArgumentType.word())
                    .executes(c->confirm(c.getSource().getPlayerOrException(),com.mojang.brigadier.arguments.StringArgumentType.getString(c,"token")))))
                .then(Commands.literal("region").requires(AccessPolicy::requireDeveloperOrConsole)
                    .then(Commands.argument("minimum",BlockPosArgument.blockPos()).then(Commands.argument("maximum",BlockPosArgument.blockPos())
                        .executes(c->{
                            var source=c.getSource();
                            if(DungeonDefinitions.byDimension(source.getLevel().dimension()).isPresent()
                                ||DungeonRunRegistryData.get(source.getServer()).findRunForInstanceDimension(source.getLevel().dimension()).isPresent()){
                                source.sendFailure(Component.literal("The Inn belongs outside dungeon instances."));return 0;
                            }
                            var a=BlockPosArgument.getLoadedBlockPos(c,"minimum");var b=BlockPosArgument.getLoadedBlockPos(c,"maximum");
                            long volume=((long)Math.abs(a.getX()-b.getX())+1)*(Math.abs((long)a.getY()-b.getY())+1)*(Math.abs((long)a.getZ()-b.getZ())+1);
                            if(volume>1_000_000){source.sendFailure(Component.literal("Inn region must be at most one million blocks."));return 0;}
                            InnData.get(source.getServer()).region(source.getLevel(),a,b);
                            source.sendSuccess(()->Component.literal("Inn region saved; existing beds are approved and protected."),true);return 1;
                        }))))
                .then(Commands.literal("status").executes(c->{
                    var player=c.getSource().getPlayerOrException();say(player,bonded(player)?"Inn bond established.":"No Inn bond yet.");return 1;
                })));
    }
    public static void clear(){OFFERS.clear();}
    private static void say(ServerPlayer player,String text){player.sendSystemMessage(Component.literal(text));}
    private static int fail(ServerPlayer player,String text){say(player,text);return 0;}
    // TODO(M40, world validation): NPC Beluzon Internal 1FT6k2MFKgQf_tQ5UcBn0wmqJ9-yY_Wdpjna4USdVOZA
    // requires a native Creaking, First Heart in the protected Pale Oak pillar, and a full-night
    // activation check. Bind the authored Inn region and the existing Creaking using developer tools.
    // Do not replace villagers, align logs, place beds, or modify the user's authored world automatically.
}
