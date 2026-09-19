package net.goui.cosmicdungeon.playerclass.dragoon.repair;

import net.goui.cosmicdungeon.economy.CurrencyDenomination;
import net.goui.cosmicdungeon.economy.CurrencyService;
import net.goui.cosmicdungeon.network.DragoonRepairPayloads;
import net.goui.cosmicdungeon.network.ModNetwork;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.goui.cosmicdungeon.playerclass.api.ClassKeys;
import net.goui.cosmicdungeon.trade.TradeSessionData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.*;

public final class DragoonRepairSessionData {
    private static final long INVITE_EXPIRATION_TICKS = 600L;
    private static long nextWakeup=Long.MAX_VALUE;
    private static final Map<InviteKey, PendingInvite> invites = new HashMap<>(); private static final Map<UUID, RepairSession> sessions = new HashMap<>();
    private DragoonRepairSessionData() {}
    public static void invite(ServerPlayer dragoon, ServerPlayer target) { cleanupExpiredInvites(dragoon.level().getServer()); if (!isDragoon(dragoon)) { dragoon.sendSystemMessage(Component.literal("Only Dragoons can start Repair Affinity.")); return; } if (target==null || dragoon.getUUID().equals(target.getUUID())) { dragoon.sendSystemMessage(Component.literal("Choose another online player to repair for.")); return; } if (!playersReady(dragoon,target)) { dragoon.sendSystemMessage(Component.literal("Both players must be alive, nearby, and in the same dimension.")); return; } if (isBusy(dragoon) || isBusy(target) || TradeSessionData.isBusy(dragoon) || TradeSessionData.isBusy(target)) { dragoon.sendSystemMessage(Component.literal("One of you is already busy.")); return; } long now=dragoon.level().getGameTime(); invites.put(new InviteKey(dragoon.getUUID(), target.getUUID()), new PendingInvite(dragoon.getUUID(), target.getUUID(), now)); target.sendSystemMessage(inviteMessage(dragoon.getName().getString())); dragoon.sendSystemMessage(Component.literal("Repair invite sent to "+target.getName().getString()+".")); }
    public static boolean acceptInvite(ServerPlayer target, ServerPlayer dragoon) { cleanupExpiredInvites(target.level().getServer()); if (dragoon==null) { target.sendSystemMessage(Component.literal("That Dragoon is no longer online.")); return false; } InviteKey key=new InviteKey(dragoon.getUUID(), target.getUUID()); PendingInvite inv=invites.remove(key); if (inv==null) { target.sendSystemMessage(Component.literal("No pending repair invite from "+dragoon.getName().getString()+".")); return false; } if (inv.isExpired(target.level().getGameTime())) { target.sendSystemMessage(Component.literal("That repair invite expired.")); return false; } if (!isDragoon(dragoon) || !playersReady(dragoon,target) || isBusy(dragoon) || isBusy(target) || TradeSessionData.isBusy(dragoon) || TradeSessionData.isBusy(target)) { target.sendSystemMessage(Component.literal("Unable to start Repair Affinity right now.")); return false; } if(!RepairTransactions.reconcile(dragoon)||!RepairTransactions.reconcile(target))return false; removePendingInvitesInvolving(dragoon.getUUID()); removePendingInvitesInvolving(target.getUUID()); RepairSession s=new RepairSession(dragoon,target); sessions.put(dragoon.getUUID(),s); sessions.put(target.getUUID(),s); if (!s.open()) { s.cancel("Unable to open Repair Affinity"); return false; } return true; }
    public static boolean denyInvite(ServerPlayer target, ServerPlayer dragoon) { if (dragoon==null) return false; boolean ok=invites.remove(new InviteKey(dragoon.getUUID(), target.getUUID()))!=null; target.sendSystemMessage(Component.literal(ok?"Denied repair request.":"No pending repair request.")); if (ok) dragoon.sendSystemMessage(Component.literal(target.getName().getString()+" denied your repair request.")); return ok; }
    public static RepairSession get(ServerPlayer p) { return p==null?null:sessions.get(p.getUUID()); } public static boolean isBusy(ServerPlayer p) { return p!=null && (sessions.containsKey(p.getUUID()) || RepairCustody.pending(p)); }
    public static void clearAll() { Set<RepairSession> active = new HashSet<>(sessions.values()); for (RepairSession s : active) s.cancel("server stopping"); invites.clear(); sessions.clear(); nextWakeup=Long.MAX_VALUE; }
    public static void cancel(ServerPlayer p,String reason){ RepairSession s=get(p); if(s!=null)s.cancel(reason); else if(p!=null)p.sendSystemMessage(Component.literal("No active repair session to cancel.")); }
    public static void handleLogout(ServerPlayer p){ if(p==null)return; removePendingInvitesInvolving(p.getUUID()); RepairSession s=get(p); if(s!=null)s.cancelBecausePlayerLeft(p); }
    public static void cleanupExpiredInvites(MinecraftServer server){ if(server==null)return; long now=server.overworld().getGameTime(); invites.entrySet().removeIf(e->e.getValue().isExpired(now)); }
    public static boolean needsTick(MinecraftServer server){
        long now=server.overworld().getGameTime();
        return (!invites.isEmpty()&&now%20==0)||RepairQuoteRules.poll(!sessions.isEmpty(),now,nextWakeup);
    }
    public static void tick(MinecraftServer server){
        cleanupExpiredInvites(server);Set<RepairSession> unique=new HashSet<>(sessions.values());nextWakeup=Long.MAX_VALUE;
        for(RepairSession s:unique){s.checkBreakConditions();if(!s.ended)
            nextWakeup=Math.min(nextWakeup,s.finalizing?s.finishAt:s.targetReady&&s.dragoonReady?s.readyDeadline:Long.MAX_VALUE);}
    }
    private static boolean isDragoon(ServerPlayer p){ return p!=null && ClassKeys.CLASS_ID_DRAGOON.equals(ClassData.getClassId(p)); }
    private static boolean playersReady(ServerPlayer a, ServerPlayer b){ return a!=null&&b!=null&&a.isAlive()&&b.isAlive()&&!a.isSpectator()&&!b.isSpectator()&&CurrencyService.transactionsAllowed(a)&&CurrencyService.transactionsAllowed(b)&&a.level().dimension().equals(b.level().dimension())&&a.distanceToSqr(b)<=Math.pow(net.goui.cosmicdungeon.Config.REPAIR_RANGE.get(),2); }
    private static void removePendingInvitesInvolving(UUID id){ invites.entrySet().removeIf(e->e.getValue().involves(id)); }
    private static Component inviteMessage(String name){ return Component.literal(name+" offers Repair Affinity. ").append(Component.literal("[Accept Repair]").withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN).withClickEvent(new ClickEvent.RunCommand("/repair accept "+name)).withHoverEvent(new HoverEvent.ShowText(Component.literal("Accept repair request"))))).append(" ").append(Component.literal("[Deny Repair]").withStyle(Style.EMPTY.withColor(ChatFormatting.RED).withClickEvent(new ClickEvent.RunCommand("/repair deny "+name)))); }
    private record InviteKey(UUID dragoon, UUID target){} private record PendingInvite(UUID dragoon, UUID target, long created){ boolean isExpired(long now){return now-created>=INVITE_EXPIRATION_TICKS;} boolean involves(UUID id){return dragoon.equals(id)||target.equals(id);} }

    public static final class RepairSession {
        private final UUID sessionId=UUID.randomUUID(), dragoon, target; private final SimpleContainer repairContainer=new SimpleContainer(1); private boolean ended, finalizing, targetReady, dragoonReady; private long readyDeadline, finishAt; private int selectedUnits=1; private long feeTrace;
        private UUID paymentId;
        private net.goui.cosmicdungeon.economy.AccountTransfer.Terms paymentTerms;
        private ItemStack quotedItem=ItemStack.EMPTY;
        private net.goui.cosmicdungeon.economy.PlayerCurrencyData accounts(){
            return net.goui.cosmicdungeon.economy.PlayerCurrencyData.get(server());
        }
        private boolean reservePayment(){
            var srv=server();var data=accounts();paymentId=UUID.randomUUID();
            long run=net.goui.cosmicdungeon.dungeon.DungeonRunRegistryData.get(srv).findRunForPlayer(target)
                    .map(net.goui.cosmicdungeon.dungeon.DungeonRunRegistryData.RunRecord::runId).orElse(0L);
            paymentTerms=new net.goui.cosmicdungeon.economy.AccountTransfer.Terms(target,dragoon,feeTrace,0,
                    "dragoon_repair",run,sessionId+":"+paymentId);
            quotedItem=repairContainer.getItem(0).copy();
            var reserved=data.reserve(paymentId,paymentTerms,System.currentTimeMillis());
            data.reportTransfer(paymentId,reserved);
            if(!reserved.reserved()||!data.flushVerified()){
                return false;
            }
            var d=srv.getPlayerList().getPlayer(dragoon);var t=srv.getPlayerList().getPlayer(target);
            var material=DragoonRepairRules.materialFor(quotedItem).orElseThrow();
            RepairCustody.bindCustomer(t,paymentId);
            if(!RepairCustody.reserveComponents(d,paymentId,material,DragoonRepairRules.componentCount(quotedItem,selectedUnits))){
                return false;
            }
            if(!net.goui.cosmicdungeon.transaction.PlayerSaveProof.save(t)
                    ||!net.goui.cosmicdungeon.transaction.PlayerSaveProof.save(d)){
                return false;
            }
            readyDeadline=srv.overworld().getGameTime()+net.goui.cosmicdungeon.Config.REPAIR_READY_TICKS.get();
            nextWakeup=Math.min(nextWakeup,readyDeadline);
            return RepairTransactions.prepare(t,d,paymentId,selectedUnits,readyDeadline);
        }
        public UUID id(){return sessionId;}
        RepairSession(ServerPlayer d, ServerPlayer t){ dragoon=d.getUUID(); target=t.getUUID(); }
        public SimpleContainer repairContainer(){ return repairContainer; } public boolean isValidFor(Player p){ return p!=null&&!ended&&(p.getUUID().equals(dragoon)||p.getUUID().equals(target)); } public boolean canEditRepairSlot(Player p){ return isTarget(p)&&!targetReady&&!dragoonReady&&!finalizing&&!ended; } private boolean isTarget(Player p){return p!=null&&p.getUUID().equals(target);} private boolean isDragoonPlayer(Player p){return p!=null&&p.getUUID().equals(dragoon);} public boolean canCancelFromMenuClose(ServerPlayer p){ return !ended&&isValidFor(p); }
        public boolean open(){ MinecraftServer srv=server(); if(srv==null)return false; ServerPlayer d=srv.getPlayerList().getPlayer(dragoon), t=srv.getPlayerList().getPlayer(target); if(d==null||t==null)return false; RepairCustody.begin(d,sessionId,false); RepairCustody.begin(t,sessionId,true); d.openMenu(new SimpleMenuProvider((id,inv,pl)->new DragoonRepairMenu(id,inv,this), Component.literal("Repair Affinity: "+t.getName().getString()))); t.openMenu(new SimpleMenuProvider((id,inv,pl)->new DragoonRepairMenu(id,inv,this), Component.literal("Repair Affinity: "+d.getName().getString()))); syncAll("Place a supported damaged item."); return true; }
        private void checkpoint(){var srv=server();if(srv!=null)for(UUID owner:List.of(target,dragoon)){var p=srv.getPlayerList().getPlayer(owner);if(p!=null)RepairCustody.capture(p);}}
        public boolean locked(){ return targetReady || dragoonReady || finalizing; }
        public void onRepairItemChanged(Player p){ if(ended||finalizing)return; if(paymentId!=null){cancel("Repair item changed");return;} targetReady=false; dragoonReady=false; checkpoint(); ItemStack st=repairContainer.getItem(0); selectedUnits = DragoonRepairRules.isSupportedDamagedItem(st) ? DragoonRepairRules.requiredUnitsToFull(st) : 1; syncAll(DragoonRepairRules.isValidRepairItemShape(st)&&!DragoonRepairRules.isSupportedDamagedItem(st)?"This item is not supported by Repair Affinity.":""); }
        public void adjustFee(ServerPlayer p,String denom,int delta){ if(!isTarget(p)||locked())return; CurrencyDenomination d=CurrencyDenomination.fromId(denom); if(d==null)return; feeTrace=RepairQuoteRules.adjustFee(feeTrace,d.traceValue(),delta,CurrencyService.getAvailableTrace(p)); targetReady=false; dragoonReady=false; syncAll(""); }
        public void selectUnits(ServerPlayer p,int units){ if(!isTarget(p)||locked())return; ItemStack st=repairContainer.getItem(0); if(!DragoonRepairRules.isSupportedDamagedItem(st))return; selectedUnits=DragoonRepairRules.clampUnits(st,units); targetReady=false; dragoonReady=false; syncAll(""); }
        public void setReady(ServerPlayer p,boolean ready) {
            if(!isValidFor(p)||finalizing||ended)return;
            if((isTarget(p)?targetReady:dragoonReady)==ready)return;
            if(!ready){
                if(paymentId!=null){cancel("Ready withdrawn");return;} targetReady=false;dragoonReady=false;syncAll("");return;
            }
            String failure=validate(true);
            if(failure!=null){syncAll(failure);return;}
            if(isTarget(p))targetReady=true;else if(isDragoonPlayer(p))dragoonReady=true;
            if(targetReady&&dragoonReady){
                if(!reservePayment()){
                    cancel("Reservation could not be verified");return;
                }
            }
            syncAll(targetReady&&dragoonReady?"Both ready. Dragoon: start within "
                    +(net.goui.cosmicdungeon.Config.REPAIR_READY_TICKS.get()/20.0)+" seconds.":"");
        }
        public void repair(ServerPlayer p) {
            if(!isDragoonPlayer(p)||!targetReady||!dragoonReady||finalizing||ended)return;
            if(RepairQuoteRules.expired(server().overworld().getGameTime(),readyDeadline)){
                cancel("Ready confirmation expired");return;
            }
            String failure=validate(true);if(failure!=null){cancel(failure);return;}
            var plan=accounts().startRepair(paymentId,server().overworld().getGameTime());
            finalizing=true;finishAt=Math.addExact(plan.startedTick(),plan.durationTicks());
            nextWakeup=Math.min(nextWakeup,finishAt);
            syncAll("Repairing for "+(plan.durationTicks()/20.0)+" seconds...");
        }
        private void complete() {
            var srv=server();var d=srv.getPlayerList().getPlayer(dragoon);var t=srv.getPlayerList().getPlayer(target);
            var result=DragoonRepairTransactionService.finalizeRepair(d,t,repairContainer,selectedUnits,paymentId,paymentTerms);
            cancel(result==DragoonRepairTransactionService.Result.SUCCESS?"Repair complete":"Recovering saved repair decision");
        }
        private String validate(boolean requireMenu){ MinecraftServer srv=server(); if(srv==null)return"Server unavailable."; ServerPlayer d=srv.getPlayerList().getPlayer(dragoon), t=srv.getPlayerList().getPlayer(target); if(!playersReady(d,t))return"Both players must stay alive, nearby, and in the same dimension."; if(!isDragoon(d))return"Repair provider is no longer a Dragoon."; if(requireMenu && (!(d.containerMenu instanceof DragoonRepairMenu dm)||!dm.belongsTo(this)||!(t.containerMenu instanceof DragoonRepairMenu tm)||!tm.belongsTo(this)))return"Both players must keep Repair Affinity open."; ItemStack st=repairContainer.getItem(0); Optional<Item> mat=DragoonRepairRules.materialFor(st); if(mat.isEmpty())return"This item is not supported by Repair Affinity."; if(selectedUnits<1||selectedUnits>4||DragoonRepairRules.clampUnits(st,selectedUnits)!=selectedUnits)return"Select a valid repair amount."; if(paymentId==null?DragoonRepairTransactionService.count(d,mat.get())<DragoonRepairRules.componentCount(st,selectedUnits):!RepairCustody.componentsValid(d,paymentId,mat.get(),DragoonRepairRules.componentCount(st,selectedUnits)))return"Dragoon lacks required reserved materials."; if(!CurrencyService.transactionsAllowed(d)||!CurrencyService.transactionsAllowed(t))return"Repair unavailable during dungeon reset."; if(paymentId!=null){ if(!accounts().reservationValid(paymentId,paymentTerms))return"Reserved payment is no longer valid."; if(!ItemStack.matches(st,quotedItem))return"The agreed repair item changed."; } else { if(CurrencyService.getAvailableTrace(t)<feeTrace)return"Customer lacks available labor fee."; if(feeTrace>CurrencyService.getAvailableCapacity(d))return"Dragoon cannot receive that labor fee."; } if(TradeSessionData.isBusy(d)||TradeSessionData.isBusy(t))return"One of you is already in a trade."; return null; }
        public void cancelFromMenuClose(ServerPlayer p){ cancel("menu closed"); } public void checkBreakConditions(){
            if(ended)return;var srv=server();if(srv==null)return;
            String failure=validate(true);
            if(failure!=null && (locked() || !playersReady(srv.getPlayerList().getPlayer(dragoon),srv.getPlayerList().getPlayer(target)))){
                cancel(failure);return;
            }
            long now=srv.overworld().getGameTime();
            if(finalizing && now>=finishAt) complete();
            else if(targetReady&&dragoonReady&&!finalizing&&RepairQuoteRules.expired(now,readyDeadline))cancel("Ready confirmation expired");
        }
        public void cancelBecausePlayerLeft(ServerPlayer p){ cancel("player disconnected", p); }
        public void cancel(String reason){ cancel(reason, null); }
        private void cancel(String reason, ServerPlayer departingPlayer){
            if(ended)return;var srv=server();
            var participants=new ArrayList<ServerPlayer>();
            if(srv!=null)for(UUID owner:List.of(target,dragoon)){
                var player=srv.getPlayerList().getPlayer(owner);
                if(player==null&&departingPlayer!=null&&departingPlayer.getUUID().equals(owner))player=departingPlayer;
                if(player!=null){RepairCustody.capture(player);participants.add(player);}
            }
            if(paymentId!=null&&srv!=null){
                var data=accounts();var transfer=data.transfer(paymentId).orElse(null);
                if(transfer!=null&&transfer.reserved())data.reportTransfer(paymentId,
                        data.cancelTransfer(paymentId,paymentTerms,System.currentTimeMillis(),reason));
            }
            markEnded();repairContainer.clearContent();
            for(var player:participants){
                if(player.containerMenu instanceof DragoonRepairMenu menu&&menu.belongsTo(this))menu.setCarried(ItemStack.EMPTY);
                player.closeContainer();
                if(!RepairTransactions.reconcile(player))continue;
                if(player.isAlive()&&!player.hasDisconnected())RepairCustody.claim(player);
                player.sendSystemMessage(Component.literal("Repair ended: "+reason));
            }
        }
        private void syncAll(String msg){ MinecraftServer srv=server(); if(srv==null||ended)return; ServerPlayer d=srv.getPlayerList().getPlayer(dragoon), t=srv.getPlayerList().getPlayer(target); if(d!=null)syncTo(d,msg); if(t!=null)syncTo(t,msg); }
        private void syncTo(ServerPlayer viewer,String msg){ if(!(viewer.containerMenu instanceof DragoonRepairMenu))return; MinecraftServer srv=server(); ServerPlayer d=srv.getPlayerList().getPlayer(dragoon), t=srv.getPlayerList().getPlayer(target); ItemStack st=repairContainer.getItem(0); Optional<Item> mat=DragoonRepairRules.materialFor(st); Item item=mat.orElse(null); String id=item==null?"":BuiltInRegistries.ITEM.getKey(item).toString(); String name=item==null?"-":DragoonRepairRules.componentKey(st).replace('_',' '); int req=DragoonRepairRules.isSupportedDamagedItem(st)?DragoonRepairRules.requiredUnitsToFull(st):0; boolean has=item!=null&&(paymentId==null?DragoonRepairTransactionService.count(d,item)>=DragoonRepairRules.componentCount(st,selectedUnits):RepairCustody.componentsValid(d,paymentId,item,DragoonRepairRules.componentCount(st,selectedUnits))); ModNetwork.sendTo(viewer,new DragoonRepairPayloads.S2C_State(viewer.containerMenu.containerId,sessionId,d.getName().getString(),t.getName().getString(),viewer.getUUID().equals(dragoon),feeTrace,CurrencyService.getBalanceTrace(t),CurrencyService.getCapacity(d),selectedUnits,req,id,name,item==null?0:DragoonRepairRules.componentCount(st,selectedUnits),has,targetReady,dragoonReady,finalizing,msg==null?"":msg)); }
        private void markEnded(){ ended=true; sessions.remove(dragoon,this); sessions.remove(target,this); } private MinecraftServer server(){ return ServerLifecycleHooks.getCurrentServer(); }
    }
}
