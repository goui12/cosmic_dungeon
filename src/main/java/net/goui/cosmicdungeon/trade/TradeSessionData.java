package net.goui.cosmicdungeon.trade;

import net.goui.cosmicdungeon.achievement.TradeAchievementService;
import net.goui.cosmicdungeon.economy.CurrencyDenomination;
import net.goui.cosmicdungeon.economy.CurrencyService;
import net.goui.cosmicdungeon.network.ModNetwork;
import net.goui.cosmicdungeon.network.TradePayloads;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class TradeSessionData {
    private static final long INVITE_EXPIRATION_TICKS = 20L * 30L;
    private static final long REQUEST_COOLDOWN_TICKS = 20L * 3L;

    private static final Map<InviteKey, PendingInvite> invites = new HashMap<>();
    private static final Map<UUID, TradeSession> sessions = new HashMap<>();
    private static final Map<UUID, Long> lastInviteByRequester = new HashMap<>();

    private TradeSessionData() {}

    public static void invite(ServerPlayer from, ServerPlayer to) {
        if (from == null) return;
        if (to == null) {
            from.sendSystemMessage(Component.literal("That player is not online."));
            return;
        }

        cleanupExpiredInvites(from);

        if (from.getUUID().equals(to.getUUID())) {
            from.sendSystemMessage(Component.literal("You cannot trade with yourself."));
            return;
        }
        if (isBusy(from)) {
            from.sendSystemMessage(Component.literal("You are already in an active trade."));
            return;
        }
        if (isBusy(to)) {
            from.sendSystemMessage(Component.literal(to.getName().getString() + " is already in an active trade."));
            return;
        }
        if (!from.isAlive() || from.isSpectator()) {
            from.sendSystemMessage(Component.literal("You cannot send a trade request right now."));
            return;
        }
        if (!to.isAlive() || to.isSpectator()) {
            from.sendSystemMessage(Component.literal("Unable to trade with that player right now."));
            return;
        }

        long now = currentGameTime(from);
        long lastInvite = lastInviteByRequester.getOrDefault(from.getUUID(), Long.MIN_VALUE / 2L);
        long elapsed = now - lastInvite;
        if (elapsed >= 0L && elapsed < REQUEST_COOLDOWN_TICKS) {
            long remainingSeconds = Math.max(1L, (REQUEST_COOLDOWN_TICKS - elapsed + 19L) / 20L);
            from.sendSystemMessage(Component.literal("Please wait " + remainingSeconds + "s before sending another trade request."));
            return;
        }

        InviteKey key = new InviteKey(from.getUUID(), to.getUUID());
        invites.put(key, new PendingInvite(from.getUUID(), to.getUUID(), now, now));
        lastInviteByRequester.put(from.getUUID(), now);

        to.sendSystemMessage(inviteMessage(from.getName().getString()));
        from.sendSystemMessage(Component.literal("Trade invite sent to " + to.getName().getString() + "."));
    }

    public static boolean acceptInvite(ServerPlayer accepter, ServerPlayer inviter) {
        if (accepter == null) return false;
        if (inviter == null) {
            accepter.sendSystemMessage(Component.literal("That trade inviter is no longer online."));
            return false;
        }

        InviteKey key = new InviteKey(inviter.getUUID(), accepter.getUUID());
        PendingInvite invite = invites.get(key);
        if (invite == null) {
            accepter.sendSystemMessage(Component.literal("No pending trade invite from " + inviter.getName().getString() + "."));
            return false;
        }
        if (invite.isExpired(currentGameTime(accepter))) {
            invites.remove(key);
            accepter.sendSystemMessage(Component.literal("Trade invite from " + inviter.getName().getString() + " has expired."));
            return false;
        }
        if (isBusy(inviter)) {
            invites.remove(key);
            accepter.sendSystemMessage(Component.literal(inviter.getName().getString() + " is already in an active trade."));
            return false;
        }
        if (isBusy(accepter)) {
            accepter.sendSystemMessage(Component.literal("You are already in an active trade."));
            return false;
        }
        if (!inviter.isAlive() || inviter.isSpectator() || !accepter.isAlive() || accepter.isSpectator()) {
            invites.remove(key);
            accepter.sendSystemMessage(Component.literal("Unable to start that trade right now."));
            return false;
        }

        invites.remove(key);
        removePendingInvitesInvolving(inviter.getUUID());
        removePendingInvitesInvolving(accepter.getUUID());

        TradeSession s = new TradeSession(inviter, accepter);
        sessions.put(inviter.getUUID(), s);
        sessions.put(accepter.getUUID(), s);
        if (!s.open()) {
            s.cancel("Unable to open trade");
            return false;
        }
        return true;
    }

    public static boolean denyInvite(ServerPlayer receiver, ServerPlayer requester) {
        if (receiver == null) return false;
        if (requester == null) {
            receiver.sendSystemMessage(Component.literal("No pending trade invite from that player."));
            return false;
        }

        cleanupExpiredInvites(receiver);

        InviteKey key = new InviteKey(requester.getUUID(), receiver.getUUID());
        PendingInvite invite = invites.remove(key);
        if (invite == null || invite.isExpired(currentGameTime(receiver))) {
            receiver.sendSystemMessage(Component.literal("No pending trade invite from " + requester.getName().getString() + "."));
            return false;
        }

        receiver.sendSystemMessage(Component.literal("Denied trade request from " + requester.getName().getString() + "."));
        requester.sendSystemMessage(Component.literal(receiver.getName().getString() + " denied your trade request."));
        return true;
    }

    public static TradeSession get(ServerPlayer p) {
        return p == null ? null : sessions.get(p.getUUID());
    }

    public static boolean isBusy(ServerPlayer p) {
        return p != null && sessions.containsKey(p.getUUID());
    }

    public static void cancel(ServerPlayer p, String reason) {
        TradeSession s = get(p);
        if (s != null) {
            s.cancel(reason);
        } else if (p != null) {
            p.sendSystemMessage(Component.literal("No active trade to cancel."));
        }
    }

    public static void cleanupExpiredInvites(MinecraftServer server) {
        if (server == null) return;
        long now = server.overworld().getGameTime();
        invites.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
    }

    public static void cleanupExpiredInvites(ServerPlayer player) {
        if (player == null) return;
        cleanupExpiredInvites(player.level().getServer());
    }

    public static void handleLogout(ServerPlayer player) {
        if (player == null) return;
        removePendingInvitesInvolving(player.getUUID());
        TradeSession session = get(player);
        if (session != null) {
            session.cancelBecausePlayerLeft(player);
        }
    }

    private static void removePendingInvitesInvolving(UUID playerId) {
        invites.entrySet().removeIf(entry -> entry.getValue().involves(playerId));
    }

    private static long currentGameTime(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        return server == null ? 0L : server.overworld().getGameTime();
    }

    private static Component inviteMessage(String requesterName) {
        Component acceptButton = Component.literal("[Accept Trade]").withStyle(
                Style.EMPTY.withColor(ChatFormatting.GREEN)
                        .withClickEvent(new ClickEvent.RunCommand("/trade accept " + requesterName))
                        .withHoverEvent(new HoverEvent.ShowText(Component.literal("Accept trade request from " + requesterName)))
        );
        Component denyButton = Component.literal("[Deny Trade]").withStyle(
                Style.EMPTY.withColor(ChatFormatting.RED)
                        .withClickEvent(new ClickEvent.RunCommand("/trade deny " + requesterName))
                        .withHoverEvent(new HoverEvent.ShowText(Component.literal("Deny trade request from " + requesterName)))
        );
        return Component.literal(requesterName + " wants to trade. ").append(acceptButton).append(" ").append(denyButton);
    }

    private record InviteKey(UUID inviterId, UUID receiverId) {}

    private record PendingInvite(UUID inviterId, UUID receiverId, long createdGameTime, long lastUpdatedGameTime) {
        boolean isExpired(long now) {
            return now - createdGameTime >= INVITE_EXPIRATION_TICKS;
        }

        boolean involves(UUID playerId) {
            return inviterId.equals(playerId) || receiverId.equals(playerId);
        }
    }

    public static final class TradeSession {
        private final UUID sessionId = UUID.randomUUID();
        private final UUID a;
        private final UUID b;
        private final SimpleContainer aOffer = new SimpleContainer(TradeMenu.OFFER_SLOTS);
        private final SimpleContainer bOffer = new SimpleContainer(TradeMenu.OFFER_SLOTS);
        private boolean ended;
        private boolean finalizing;
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
            return !ended && (u.equals(a) || u.equals(b));
        }

        public boolean isValidFor(net.minecraft.world.entity.player.Player p) {
            if (p == null || ended || !contains(p)) return false;
            MinecraftServer srv = p.level().getServer();
            if (srv == null) srv = server();
            return srv != null
                    && srv.getPlayerList().getPlayer(a) != null
                    && srv.getPlayerList().getPlayer(b) != null;
        }

        public boolean canEditOffer(net.minecraft.world.entity.player.Player p) {
            if (!isValidFor(p)) return false;
            UUID u = p.getUUID();
            if (u.equals(a)) return !aReady && !aConfirm && !finalizing;
            if (u.equals(b)) return !bReady && !bConfirm && !finalizing;
            return false;
        }

        public boolean canCancelFromMenuClose(ServerPlayer p) {
            return p != null && !ended && !finalizing && sessions.get(p.getUUID()) == this && contains(p);
        }

        public void cancelFromMenuClose(ServerPlayer p) {
            if (!canCancelFromMenuClose(p)) return;
            UUID otherId = p.getUUID().equals(a) ? b : a;
            cancel("Menu closed");
            MinecraftServer srv = p.level().getServer();
            ServerPlayer other = srv == null ? null : srv.getPlayerList().getPlayer(otherId);
            if (other != null) {
                other.sendSystemMessage(Component.literal("Trade cancelled: other player closed the menu"));
            }
        }

        public SimpleContainer getContainerFor(net.minecraft.world.entity.player.Player p, boolean own) {
            boolean isA = p.getUUID().equals(a);
            return own ? (isA ? aOffer : bOffer) : (isA ? bOffer : aOffer);
        }

        public boolean open() {
            MinecraftServer srv = server();
            if (srv == null || ended) {
                return false;
            }
            ServerPlayer sa = srv.getPlayerList().getPlayer(a);
            ServerPlayer sb = srv.getPlayerList().getPlayer(b);
            if (sa == null || sb == null) {
                return false;
            }
            sa.openMenu(new SimpleMenuProvider((id, inv, pl) -> new TradeMenu(id, inv, this), Component.literal("Trading with: " + sb.getName().getString())));
            sb.openMenu(new SimpleMenuProvider((id, inv, pl) -> new TradeMenu(id, inv, this), Component.literal("Trading with: " + sa.getName().getString())));
            syncAll("");
            sa.sendSystemMessage(Component.literal("Trade opened with " + sb.getName().getString() + "."));
            sb.sendSystemMessage(Component.literal("Trade opened with " + sa.getName().getString() + "."));
            return true;
        }

        public void onOfferChanged(net.minecraft.world.entity.player.Player p) {
            if (ended || finalizing || p == null || !contains(p)) return;
            if (!canEditOffer(p)) {
                syncAll("Offer locked after accepting. Cancel to change it.");
                return;
            }
            boolean hadAcceptance = aReady || bReady || aConfirm || bConfirm;
            aReady = false;
            bReady = false;
            aConfirm = false;
            bConfirm = false;
            syncAll(hadAcceptance ? "Offer changed; acceptance reset." : "");
        }

        public void setCurrency(ServerPlayer p, long amt) {
            if (ended || finalizing || p == null || !contains(p)) return;
            if (!canEditOffer(p)) {
                syncAll("Currency offer locked after accepting. Cancel to change it.");
                return;
            }
            if (amt < 0L) amt = 0L;
            long balance = CurrencyService.getBalanceTrace(p);
            if (amt > balance) amt = balance;
            if (p.getUUID().equals(a)) {
                if (aCurrency == amt) return;
                aCurrency = amt;
            } else if (p.getUUID().equals(b)) {
                if (bCurrency == amt) return;
                bCurrency = amt;
            } else {
                return;
            }
            onOfferChanged(p);
        }

        public void adjustCurrency(ServerPlayer p, String denominationId, int deltaCount) {
            if (ended || p == null || deltaCount == 0) return;
            CurrencyDenomination denomination = CurrencyDenomination.fromId(denominationId);
            if (denomination == null) return;
            long current;
            if (p.getUUID().equals(a)) current = aCurrency;
            else if (p.getUUID().equals(b)) current = bCurrency;
            else return;

            long deltaTrace;
            try {
                deltaTrace = Math.multiplyExact((long) deltaCount, denomination.traceValue());
            } catch (ArithmeticException ex) {
                deltaTrace = deltaCount > 0 ? Long.MAX_VALUE : Long.MIN_VALUE;
            }

            long adjusted;
            try {
                adjusted = Math.addExact(current, deltaTrace);
            } catch (ArithmeticException ex) {
                adjusted = deltaTrace > 0L ? Long.MAX_VALUE : Long.MIN_VALUE;
            }
            setCurrency(p, adjusted);
        }

        public void setReady(ServerPlayer p, boolean v) {
            if (ended || finalizing || p == null || !contains(p)) return;
            if (p.getUUID().equals(a)) {
                aReady = v;
                aConfirm = false;
            } else if (p.getUUID().equals(b)) {
                bReady = v;
                bConfirm = false;
            } else {
                return;
            }
            if (!v) {
                aConfirm = false;
                bConfirm = false;
            }
            syncAll("");
        }

        public void setConfirm(ServerPlayer p, boolean v) {
            if (ended || finalizing || p == null || !contains(p) || !(aReady && bReady)) return;
            if (p.getUUID().equals(a)) {
                if (!aReady) return;
                aConfirm = v;
            } else if (p.getUUID().equals(b)) {
                if (!bReady) return;
                bConfirm = v;
            } else {
                return;
            }
            if (aConfirm && bConfirm) {
                finalizeTrade();
            } else {
                syncAll("");
            }
        }

        private void finalizeTrade() {
            if (finalizing || ended) return;
            finalizing = true;
            syncAll("Finalizing trade...");
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

            TradeFinalizationService.Result result = TradeFinalizationService.finalizeTrade(
                    participant(sa, aCurrency),
                    participant(sb, bCurrency),
                    offeredItems(aOffer),
                    offeredItems(bOffer)
            );
            if (result != TradeFinalizationService.Result.SUCCESS) {
                cancel(cancelReason(result));
                return;
            }

            syncAll("Trade completed.");
            sa.sendSystemMessage(Component.literal("Trade completed."));
            sb.sendSystemMessage(Component.literal("Trade completed."));
            TradeAchievementService.onSuccessfulTrade(sa, sb);
            end();
            closeMenus(sa, sb);
        }

        private TradeFinalizationService.TradeParticipant participant(ServerPlayer player, long offeredCurrencyTrace) {
            return new ServerBackedParticipant() {
                @Override
                public ServerPlayer player() {
                    return player;
                }

                @Override
                public long offeredCurrencyTrace() {
                    return offeredCurrencyTrace;
                }

                @Override
                public long balanceTrace() {
                    return CurrencyService.getBalanceTrace(player);
                }

                @Override
                public long capacityTrace() {
                    return CurrencyService.getCapacity(player);
                }

                @Override
                public boolean tryWithdraw(long traceAmount) {
                    return CurrencyService.tryWithdraw(player, traceAmount);
                }

                @Override
                public boolean tryDeposit(long traceAmount) {
                    return CurrencyService.tryDeposit(player, traceAmount);
                }

                @Override
                public void setBalanceTrace(long traceAmount) {
                    CurrencyService.setBalanceTrace(player, traceAmount);
                }

                @Override
                public boolean canReceiveItems(TradeFinalizationService.OfferedItems items) {
                    return items.canMoveInto(this);
                }

                @Override
                public void receiveItems(TradeFinalizationService.OfferedItems items) {
                    items.moveInto(this);
                }
            };
        }

        private TradeFinalizationService.OfferedItems offeredItems(SimpleContainer container) {
            return new TradeFinalizationService.OfferedItems() {
                @Override
                public boolean canMoveInto(TradeFinalizationService.TradeParticipant receiver) {
                    return receiverCanAcceptItems(receiver, container);
                }

                @Override
                public void moveInto(TradeFinalizationService.TradeParticipant receiver) {
                    receiverAcceptItems(receiver, container);
                }
            };
        }

        private boolean receiverCanAcceptItems(TradeFinalizationService.TradeParticipant receiver, SimpleContainer container) {
            if (receiver instanceof ServerBackedParticipant serverBacked) {
                return hasCapacityFor(serverBacked.player(), container);
            }
            return container.getContainerSize() == 0;
        }

        private void receiverAcceptItems(TradeFinalizationService.TradeParticipant receiver, SimpleContainer container) {
            if (receiver instanceof ServerBackedParticipant serverBacked) {
                moveAll(container, serverBacked.player());
            }
        }

        private String cancelReason(TradeFinalizationService.Result result) {
            return switch (result) {
                case INSUFFICIENT_BALANCE -> "Insufficient balance";
                case CANNOT_RECEIVE_CURRENCY -> "Cannot receive offered currency";
                case NOT_ENOUGH_INVENTORY_SPACE -> "Not enough inventory space";
                case CURRENCY_WITHDRAWAL_FAILED -> "Currency withdrawal failed";
                case CURRENCY_TRANSFER_FAILED -> "Currency transfer failed";
                case SUCCESS -> "Trade completed";
            };
        }

        private interface ServerBackedParticipant extends TradeFinalizationService.TradeParticipant {
            ServerPlayer player();
        }

        private void syncAll(String statusMessage) {
            MinecraftServer srv = server();
            if (srv == null || ended) return;
            ServerPlayer sa = srv.getPlayerList().getPlayer(a);
            ServerPlayer sb = srv.getPlayerList().getPlayer(b);
            if (sa != null && sb != null) {
                syncTo(sa, sb, aCurrency, bCurrency, aReady, bReady, aConfirm, bConfirm, statusMessage);
                syncTo(sb, sa, bCurrency, aCurrency, bReady, aReady, bConfirm, aConfirm, statusMessage);
            }
        }

        private void syncTo(
                ServerPlayer self,
                ServerPlayer other,
                long selfOfferedTrace,
                long otherOfferedTrace,
                boolean selfReady,
                boolean otherReady,
                boolean selfConfirmed,
                boolean otherConfirmed,
                String statusMessage
        ) {
            if (!(self.containerMenu instanceof TradeMenu)) return;
            ModNetwork.sendTo(self, new TradePayloads.S2C_TradeState(
                    self.containerMenu.containerId,
                    sessionId,
                    self.getName().getString(),
                    other.getName().getString(),
                    CurrencyService.getBalanceTrace(self),
                    CurrencyService.getBalanceTrace(other),
                    selfOfferedTrace,
                    otherOfferedTrace,
                    selfReady,
                    otherReady,
                    selfConfirmed,
                    otherConfirmed,
                    statusMessage == null ? "" : statusMessage
            ));
        }


        private boolean hasCapacityFor(ServerPlayer p, SimpleContainer src) {
            ItemStack[] simulated = new ItemStack[p.getInventory().getContainerSize()];
            for (int i = 0; i < simulated.length; i++) {
                simulated[i] = p.getInventory().getItem(i).copy();
            }

            for (int i = 0; i < src.getContainerSize(); i++) {
                ItemStack remaining = src.getItem(i).copy();
                if (remaining.isEmpty()) continue;

                for (int slot = 0; slot < simulated.length && !remaining.isEmpty(); slot++) {
                    ItemStack existing = simulated[slot];
                    if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, remaining)) {
                        int canMove = Math.min(remaining.getCount(), existing.getMaxStackSize() - existing.getCount());
                        if (canMove > 0) {
                            existing.grow(canMove);
                            remaining.shrink(canMove);
                        }
                    }
                }
                for (int slot = 0; slot < simulated.length && !remaining.isEmpty(); slot++) {
                    ItemStack existing = simulated[slot];
                    if (existing.isEmpty()) {
                        int canMove = Math.min(remaining.getCount(), remaining.getMaxStackSize());
                        simulated[slot] = remaining.copyWithCount(canMove);
                        remaining.shrink(canMove);
                    }
                }
                if (!remaining.isEmpty()) {
                    return false;
                }
            }
            return true;
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

        public void cancelBecausePlayerLeft(ServerPlayer departed) {
            if (ended) return;
            markEnded();
            UUID departedId = departed.getUUID();
            UUID remainingId = departedId.equals(a) ? b : a;
            MinecraftServer srv = departed.level().getServer();
            ServerPlayer remaining = srv == null ? null : srv.getPlayerList().getPlayer(remainingId);

            if (departedId.equals(a)) {
                moveAll(aOffer, departed);
                if (remaining != null) moveAll(bOffer, remaining);
            } else {
                moveAll(bOffer, departed);
                if (remaining != null) moveAll(aOffer, remaining);
            }

            departed.closeContainer();
            if (remaining != null) {
                remaining.closeContainer();
                remaining.sendSystemMessage(Component.literal("Trade cancelled: other player disconnected"));
            }
            clear(aOffer);
            clear(bOffer);
        }

        public void cancel(String reason) {
            if (ended) return;
            syncAll("Trade cancelled: " + reason);
            MinecraftServer srv = server();
            markEnded();
            if (srv == null) {
                clear(aOffer);
                clear(bOffer);
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
            clear(aOffer);
            clear(bOffer);
        }

        private void closeMenus(ServerPlayer sa, ServerPlayer sb) {
            sa.closeContainer();
            sb.closeContainer();
        }

        private void end() {
            if (ended) return;
            markEnded();
            clear(aOffer);
            clear(bOffer);
        }

        private void markEnded() {
            ended = true;
            sessions.remove(a, this);
            sessions.remove(b, this);
        }

        private void clear(SimpleContainer c) {
            for (int i = 0; i < c.getContainerSize(); i++) {
                c.setItem(i, ItemStack.EMPTY);
            }
        }
    }
}
