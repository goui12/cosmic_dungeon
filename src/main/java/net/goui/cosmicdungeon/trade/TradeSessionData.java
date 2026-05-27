package net.goui.cosmicdungeon.trade;

import net.goui.cosmicdungeon.economy.CurrencyService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class TradeSessionData {
    private static final Map<UUID, UUID> invites = new HashMap<>();
    private static final Map<UUID, TradeSession> sessions = new HashMap<>();

    public static void invite(ServerPlayer from, ServerPlayer to) {
        invites.put(to.getUUID(), from.getUUID());
        to.sendSystemMessage(Component.literal(from.getName().getString() + " invited you to trade. Use /trade accept " + from.getName().getString()));
        from.sendSystemMessage(Component.literal("Trade invite sent to " + to.getName().getString() + "."));
    }

    public static boolean acceptInvite(ServerPlayer accepter, ServerPlayer inviter) {
        UUID u = invites.get(accepter.getUUID());
        if (u == null || !u.equals(inviter.getUUID())) {
            accepter.sendSystemMessage(Component.literal("No pending trade invite from " + inviter.getName().getString() + "."));
            return false;
        }
        invites.remove(accepter.getUUID());
        TradeSession s = new TradeSession(inviter, accepter);
        sessions.put(inviter.getUUID(), s);
        sessions.put(accepter.getUUID(), s);
        s.open();
        return true;
    }

    public static TradeSession get(ServerPlayer p) {
        return sessions.get(p.getUUID());
    }

    public static void cancel(ServerPlayer p, String reason) {
        TradeSession s = get(p);
        if (s != null) {
            s.cancel(reason);
        } else {
            p.sendSystemMessage(Component.literal("No active trade to cancel."));
        }
    }

    public static final class TradeSession {
        private final UUID a;
        private final UUID b;
        private final SimpleContainer aOffer = new SimpleContainer(6);
        private final SimpleContainer bOffer = new SimpleContainer(6);
        private long aCurrency;
        private long bCurrency;
        private boolean aReady;
        private boolean bReady;
        private boolean aConfirm;
        private boolean bConfirm;

        public TradeSession(ServerPlayer a, ServerPlayer b) {
            this.a = a.getUUID();
            this.b = b.getUUID();
        }

        public boolean contains(net.minecraft.world.entity.player.Player p) {
            UUID u = p.getUUID();
            return u.equals(a) || u.equals(b);
        }

        public SimpleContainer getContainerFor(net.minecraft.world.entity.player.Player p, boolean own) {
            boolean isA = p.getUUID().equals(a);
            return own ? (isA ? aOffer : bOffer) : (isA ? bOffer : aOffer);
        }

        public void open() {
            MinecraftServer srv = server();
            if (srv == null) {
                return;
            }
            ServerPlayer sa = srv.getPlayerList().getPlayer(a);
            ServerPlayer sb = srv.getPlayerList().getPlayer(b);
            if (sa == null || sb == null) {
                return;
            }
            sa.openMenu(new SimpleMenuProvider((id, inv, pl) -> new TradeMenu(id, inv, this), Component.literal("Trade")));
            sb.openMenu(new SimpleMenuProvider((id, inv, pl) -> new TradeMenu(id, inv, this), Component.literal("Trade")));
            sa.sendSystemMessage(Component.literal("Trade opened with " + sb.getName().getString() + "."));
            sb.sendSystemMessage(Component.literal("Trade opened with " + sa.getName().getString() + "."));
        }

        public void onOfferChanged(net.minecraft.world.entity.player.Player p) {
            aReady = false;
            bReady = false;
            aConfirm = false;
            bConfirm = false;
        }

        public void setCurrency(ServerPlayer p, long amt) {
            if (amt < 0) amt = 0;
            if (p.getUUID().equals(a)) aCurrency = amt;
            else bCurrency = amt;
            onOfferChanged(p);
        }

        public void setReady(ServerPlayer p, boolean v) {
            if (p.getUUID().equals(a)) aReady = v;
            else bReady = v;
            if (!v) {
                aConfirm = false;
                bConfirm = false;
            }
        }

        public void setConfirm(ServerPlayer p, boolean v) {
            if (!(aReady && bReady)) return;
            if (p.getUUID().equals(a)) aConfirm = v;
            else bConfirm = v;
            if (aConfirm && bConfirm) finalizeTrade();
        }

        private void finalizeTrade() {
            MinecraftServer srv = server();
            if (srv == null) {
                cancel("Server unavailable");
                return;
            }
            ServerPlayer sa = srv.getPlayerList().getPlayer(a);
            ServerPlayer sb = srv.getPlayerList().getPlayer(b);
            if (sa == null || sb == null) {
                cancel("Player offline");
                return;
            }

            if (CurrencyService.getBalanceTrace(sa) < aCurrency || CurrencyService.getBalanceTrace(sb) < bCurrency) {
                cancel("Insufficient balance");
                return;
            }

            // Capacity for incoming currency/items.
            if (!canReceiveCurrency(sa, bCurrency) || !canReceiveCurrency(sb, aCurrency)) {
                cancel("Cannot receive offered currency");
                return;
            }
            if (!hasCapacity(sa, bOffer) || !hasCapacity(sb, aOffer)) {
                cancel("Not enough inventory space");
                return;
            }

            boolean withdrewA = CurrencyService.tryWithdraw(sa, aCurrency);
            boolean withdrewB = CurrencyService.tryWithdraw(sb, bCurrency);
            if (!withdrewA || !withdrewB) {
                if (withdrewA) CurrencyService.tryDeposit(sa, aCurrency);
                if (withdrewB) CurrencyService.tryDeposit(sb, bCurrency);
                cancel("Currency withdrawal failed");
                return;
            }

            boolean depositedA = CurrencyService.tryDeposit(sa, bCurrency);
            boolean depositedB = CurrencyService.tryDeposit(sb, aCurrency);
            if (!depositedA || !depositedB) {
                if (depositedA) CurrencyService.tryWithdraw(sa, bCurrency);
                else CurrencyService.tryDeposit(sa, aCurrency);

                if (depositedB) CurrencyService.tryWithdraw(sb, aCurrency);
                else CurrencyService.tryDeposit(sb, bCurrency);

                // ensure original balances are restored
                CurrencyService.tryDeposit(sa, aCurrency);
                CurrencyService.tryDeposit(sb, bCurrency);
                cancel("Currency transfer failed");
                return;
            }

            moveAll(aOffer, sb);
            moveAll(bOffer, sa);
            sa.sendSystemMessage(Component.literal("Trade completed."));
            sb.sendSystemMessage(Component.literal("Trade completed."));
            end();
        }

        private boolean canReceiveCurrency(ServerPlayer player, long incoming) {
            if (incoming < 0) return false;
            long balance = CurrencyService.getBalanceTrace(player);
            return Long.MAX_VALUE - balance >= incoming;
        }

        private boolean hasCapacity(ServerPlayer p, SimpleContainer src) {
            int empty = 0;
            int items = 0;
            for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
                if (p.getInventory().getItem(i).isEmpty()) empty++;
            }
            for (int i = 0; i < src.getContainerSize(); i++) {
                if (!src.getItem(i).isEmpty()) items++;
            }
            return empty >= items;
        }

        private void moveAll(SimpleContainer c, ServerPlayer to) {
            for (int i = 0; i < c.getContainerSize(); i++) {
                var s = c.removeItemNoUpdate(i);
                if (!s.isEmpty()) to.getInventory().placeItemBackInInventory(s);
            }
        }

        private MinecraftServer server() {
            return ServerLifecycleHooks.getCurrentServer();
        }

        public void cancel(String reason) {
            MinecraftServer srv = server();
            if (srv == null) {
                end();
                return;
            }
            ServerPlayer sa = srv.getPlayerList().getPlayer(a);
            ServerPlayer sb = srv.getPlayerList().getPlayer(b);
            if (sa != null) {
                moveAll(aOffer, sa);
                sa.closeContainer();
                sa.sendSystemMessage(Component.literal("Trade cancelled: " + reason));
            }
            if (sb != null) {
                moveAll(bOffer, sb);
                sb.closeContainer();
                sb.sendSystemMessage(Component.literal("Trade cancelled: " + reason));
            }
            end();
        }

        private void end() {
            sessions.remove(a);
            sessions.remove(b);
        }
    }
}
