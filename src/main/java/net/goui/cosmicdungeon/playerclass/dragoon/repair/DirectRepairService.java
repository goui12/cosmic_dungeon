package net.goui.cosmicdungeon.playerclass.dragoon.repair;
import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.config.VendorCatalog;
import net.goui.cosmicdungeon.config.VendorPricesConfig;
import net.goui.cosmicdungeon.dungeon.DungeonLifecycleService;
import net.goui.cosmicdungeon.economy.CurrencyService;
import net.goui.cosmicdungeon.faction.NpcFactionService;
import net.goui.cosmicdungeon.vendor.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ClickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import java.util.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** Explicit server quote -> single-use confirmation; never repairs inside a dungeon instance. */
@EventBusSubscriber(modid=CosmicDungeonMod.MOD_ID)
public final class DirectRepairService {
    private DirectRepairService() {}
    private record Quote(UUID token,UUID vendor,ItemStack item,int units,long price,long expires) {}
    private static final Map<UUID,Quote> QUOTES=new HashMap<>();
    public static long listCost(ItemStack item,int units) {
        String key=DragoonRepairRules.componentKey(item);
        var entry=key==null?null:VendorCatalog.item("repair__"+key);
        if(entry==null||entry.retail().get()<0)return -1;
        return BigDecimal.valueOf(entry.retail().get()).multiply(BigDecimal.valueOf(DragoonRepairRules.componentCount(item,units)))
                .multiply(BigDecimal.valueOf(VendorPricesConfig.DIRECT_REPAIR_MULTIPLIER.get()))
                .setScale(0,RoundingMode.CEILING).longValueExact();
    }
    private static boolean allowed(ServerPlayer player,Entity vendor) {
        if(vendor==null||!vendor.isAlive()||player.distanceToSqr(vendor)>64||!player.isAlive()||player.isSpectator()
                || DragoonRepairSessionData.isBusy(player) || net.goui.cosmicdungeon.trade.TradeSessionData.isBusy(player))return false;
        if(DungeonLifecycleService.findActiveRunForPlayer(player).filter(r->r.containsDimension(player.level().dimension())).isPresent())return false;
        var id=VendorAssignmentService.getProfileId(vendor);
        if(id==null||!id.toString().equals("cosmicdungeon:d1/weapon_supplier"))return false;
        var profile=VendorProfileManager.INSTANCE.get(id);
        return profile!=null && VendorAccessService.evaluate(player,profile).allowed() && NpcFactionService.canRetail(player);
    }
    public static int quote(ServerPlayer player,int requestedUnits) {
        var vendor=player.level().getEntities(player,player.getBoundingBox().inflate(8),e->allowed(player,e))
                .stream().min(Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
        var item=player.getMainHandItem();
        if(vendor==null||!DragoonRepairRules.isSupportedDamagedItem(item)){
            player.sendSystemMessage(Component.literal("Hold one damaged supported item near Elias in the settlement."));return 0;
        }
        int units=DragoonRepairRules.clampUnits(item,requestedUnits);
        long list=listCost(item,units), price=NpcFactionService.retail(player,list);
        if(price<0)return 0;
        var quote=new Quote(UUID.randomUUID(),vendor.getUUID(),item.copy(),units,price,
                Math.addExact(player.level().getServer().overworld().getGameTime(),net.goui.cosmicdungeon.economy.D1EconomyConfig.VENDOR_QUOTE_TICKS.get()));
        QUOTES.put(player.getUUID(),quote);
        player.sendSystemMessage(Component.literal("Repair "+item.getHoverName().getString()+" by up to "+(units*25)
                +"% for "+price+" Trace. ").append(Component.literal("[Confirm]").withStyle(s->s.withClickEvent(
                new ClickEvent.RunCommand("/repair shop confirm "+quote.token())))));
        return 1;
    }
    public static int confirm(ServerPlayer player,String token) {
        var quote=QUOTES.remove(player.getUUID());
        if(quote==null||!quote.token().toString().equals(token)||quote.expires()<player.level().getServer().overworld().getGameTime())return 0;
        var vendor=player.level().getEntity(quote.vendor());
        var item=player.getMainHandItem();
        if(!allowed(player,vendor)||!ItemStack.matches(item,quote.item())
                ||NpcFactionService.retail(player,listCost(item,quote.units()))!=quote.price()){
            player.sendSystemMessage(Component.literal("Repair quote changed. Request a new quote."));return 0;
        }
        if(player.containerMenu!=player.inventoryMenu||!player.inventoryMenu.getCarried().isEmpty()||!CurrencyService.transactionsAllowed(player)){
            player.sendSystemMessage(Component.literal("Close other interfaces before confirming a shop repair."));return 0;
        }
        int slot=player.getInventory().getSelectedSlot();var inputs=new net.minecraft.nbt.ListTag();var outputs=new net.minecraft.nbt.ListTag();
        inputs.add(CommerceCustodyImages.lot(slot,RepairCustody.encode(player,item)));
        var repaired=item.copy();repaired.setDamageValue(Math.max(0,item.getDamageValue()-DragoonRepairRules.projectedRepair(item,quote.units())));
        outputs.add(CommerceCustodyImages.lot(slot,RepairCustody.encode(player,repaired)));
        var details=new net.minecraft.nbt.CompoundTag();details.putInt("units",quote.units());details.putLong("price",quote.price());details.putLong("list_cost",listCost(item,quote.units()));
        if(!CommerceTransactions.execute(player,quote.token(),-quote.price(),"direct_repair",quote.vendor().toString(),
                CommerceTransactions.plan(player,quote.token(),inputs,outputs,details))){player.sendSystemMessage(Component.literal("Repair did not complete; any pending receipt is preserved."));return 0;}
        player.getInventory().setChanged();player.inventoryMenu.broadcastChanges();
        player.sendSystemMessage(Component.literal("Equipment repaired for "+quote.price()+" Trace."));
        com.mojang.logging.LogUtils.getLogger().info("[Economy] direct_repair transaction={} player={} vendor={} price={} item={}",
                quote.token(),player.getUUID(),vendor.getUUID(),quote.price(),quote.item());
        return 1;
    }
    @SubscribeEvent public static void stop(ServerStoppedEvent event){QUOTES.clear();}
}
