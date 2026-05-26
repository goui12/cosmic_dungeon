package net.goui.cosmicdungeon.trade;
import net.goui.cosmicdungeon.economy.CurrencyService;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.SimpleMenuProvider;
import java.util.*;

public final class TradeSessionData {
    private static final Map<UUID, UUID> invites = new HashMap<>();
    private static final Map<UUID, TradeSession> sessions = new HashMap<>();
    public static void invite(ServerPlayer from, ServerPlayer to){ invites.put(to.getUUID(), from.getUUID()); to.sendSystemMessage(net.minecraft.network.chat.Component.literal(from.getName().getString()+" invited you to trade. Use /trade accept "+from.getName().getString())); }
    public static boolean acceptInvite(ServerPlayer accepter, ServerPlayer inviter){ UUID u=invites.get(accepter.getUUID()); if(u==null || !u.equals(inviter.getUUID())) return false; invites.remove(accepter.getUUID()); TradeSession s = new TradeSession(inviter,accepter); sessions.put(inviter.getUUID(),s); sessions.put(accepter.getUUID(),s); s.open(); return true; }
    public static TradeSession get(ServerPlayer p){ return sessions.get(p.getUUID()); }
    public static void cancel(ServerPlayer p, String reason){ TradeSession s=get(p); if(s!=null) s.cancel(reason); }

    public static final class TradeSession {
        private final UUID a,b; private final SimpleContainer aOffer=new SimpleContainer(6), bOffer=new SimpleContainer(6);
        private long aCurrency,bCurrency; private boolean aReady,bReady,aConfirm,bConfirm;
        public TradeSession(ServerPlayer a, ServerPlayer b){this.a=a.getUUID();this.b=b.getUUID();}
        public boolean contains(net.minecraft.world.entity.player.Player p){UUID u=p.getUUID(); return u.equals(a)||u.equals(b);}        
        public SimpleContainer getContainerFor(net.minecraft.world.entity.player.Player p, boolean own){ boolean isA=p.getUUID().equals(a); return own?(isA?aOffer:bOffer):(isA?bOffer:aOffer); }
        public void open(){ var sa=server().getPlayerList().getPlayer(a); var sb=server().getPlayerList().getPlayer(b); if(sa==null||sb==null) return; sa.openMenu(new SimpleMenuProvider((id,inv,pl)->new TradeMenu(id,inv,this), net.minecraft.network.chat.Component.literal("Trade"))); sb.openMenu(new SimpleMenuProvider((id,inv,pl)->new TradeMenu(id,inv,this), net.minecraft.network.chat.Component.literal("Trade"))); }
        public void onOfferChanged(net.minecraft.world.entity.player.Player p){ if(p.getUUID().equals(a)){aReady=bReady=aConfirm=bConfirm=false;} else {aReady=bReady=aConfirm=bConfirm=false;} }
        public void setCurrency(ServerPlayer p,long amt){ if(amt<0) amt=0; if(p.getUUID().equals(a)) aCurrency=amt; else bCurrency=amt; onOfferChanged(p); }
        public void setReady(ServerPlayer p, boolean v){ if(p.getUUID().equals(a)) aReady=v; else bReady=v; if(!v){aConfirm=bConfirm=false;} }
        public void setConfirm(ServerPlayer p, boolean v){ if(!(aReady&&bReady)) return; if(p.getUUID().equals(a)) aConfirm=v; else bConfirm=v; if(aConfirm&&bConfirm) finalizeTrade(); }
        private void finalizeTrade(){ var sa=server().getPlayerList().getPlayer(a); var sb=server().getPlayerList().getPlayer(b); if(sa==null||sb==null){cancel("Player offline"); return;} if(CurrencyService.getBalanceTrace(sa)<aCurrency||CurrencyService.getBalanceTrace(sb)<bCurrency){cancel("Insufficient balance"); return;} if(!hasCapacity(sa,bOffer)||!hasCapacity(sb,aOffer)){cancel("Not enough inventory space"); return;}
            if(!CurrencyService.tryWithdraw(sa,aCurrency)||!CurrencyService.tryWithdraw(sb,bCurrency)){cancel("Currency withdraw failed"); return;} CurrencyService.tryDeposit(sa,bCurrency); CurrencyService.tryDeposit(sb,aCurrency);
            moveAll(aOffer,sb); moveAll(bOffer,sa); end(); }
        private boolean hasCapacity(ServerPlayer p, SimpleContainer src){ int empty=0, items=0; for(int i=0;i<p.getInventory().getContainerSize();i++) if(p.getInventory().getItem(i).isEmpty()) empty++; for(int i=0;i<src.getContainerSize();i++) if(!src.getItem(i).isEmpty()) items++; return empty>=items; }
        private void moveAll(SimpleContainer c, ServerPlayer to){ for(int i=0;i<c.getContainerSize();i++){ var s=c.removeItemNoUpdate(i); if(!s.isEmpty()) to.getInventory().placeItemBackInInventory(s);} }
        private MinecraftServer server(){ var p=net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer(); return p; }
        public void cancel(String reason){ var sa=server().getPlayerList().getPlayer(a); var sb=server().getPlayerList().getPlayer(b); if(sa!=null) moveAll(aOffer,sa); if(sb!=null) moveAll(bOffer,sb); if(sa!=null) sa.closeContainer(); if(sb!=null) sb.closeContainer(); end(); }
        private void end(){ sessions.remove(a); sessions.remove(b); }
    }
}
