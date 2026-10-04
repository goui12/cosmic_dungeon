package net.goui.cosmicdungeon.npc.tamsin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.goui.cosmicdungeon.Config;
import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.auth.AccessPolicy;
import net.goui.cosmicdungeon.block.custom.*;
import net.goui.cosmicdungeon.block.entity.ClassSelectorBlockEntity;
import net.goui.cosmicdungeon.dungeon.*;
import net.goui.cosmicdungeon.menu.ClassSelectorMenu;
import net.goui.cosmicdungeon.network.*;
import net.goui.cosmicdungeon.playerclass.api.*;
import net.minecraft.commands.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import java.util.*;

/** Bounded pre-entry lobby. Source: Tamsin 2026-08-19; Q&A D24 prohibits group merges. */
@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID)
public final class D1PartyService {
    private static final D1PartyLobby LOBBY = new D1PartyLobby();
    private static final Set<UUID> VIEWERS = new HashSet<>();
    private static final Map<UUID, Long> LAST_INVITE = new HashMap<>();
    private static final Map<UUID, Long> LAST_HUD_ACTION = new HashMap<>();
    private D1PartyService() {}
    private static long now(MinecraftServer server) { return server.overworld().getGameTime(); }
    private static void message(ServerPlayer player, String text) {
        if (player != null) player.sendSystemMessage(Component.literal(text));
    }
    private static boolean available(ServerPlayer player) {
        return player != null && player.isAlive() && !player.isSpectator() && !AccessPolicy.isDeveloper(player)
                && player.connection.isAcceptingMessages()
                && DungeonRunRegistryData.get(player.level().getServer()).findRunForPlayer(player.getUUID()).isEmpty();
    }
    private static boolean onboarded(ServerPlayer player) {
        return available(player) && TamsinService.accepted(player)
                && ClassSelectorTeleportUtil.isReadyEligibleClass(ClassData.getClassId(player));
    }
    private static ClassSelectorBlockEntity selector(MinecraftServer server, D1PartyLobby.Anchor anchor) {
        var level = ClassSelectorTeleportUtil.resolveLevel(server, anchor.dimension());
        var binding = TamsinData.get(server).binding(anchor.npc());
        if (level == null || binding == null || !binding.dimension().equals(anchor.dimension())
                || binding.selector() != anchor.selector()) return null;
        var npc = level.getEntity(anchor.npc());
        var pos = BlockPos.of(anchor.selector());
        if (npc == null || !npc.isAlive() || !level.hasChunkAt(pos)) return null;
        return level.getBlockEntity(pos) instanceof ClassSelectorBlockEntity block ? block : null;
    }
    private static boolean nearby(ServerPlayer player, D1PartyLobby.Anchor anchor) {
        if (!available(player) || !player.level().dimension().location().toString().equals(anchor.dimension())
                || selector(player.level().getServer(), anchor) == null) return false;
        var npc = player.level().getEntity(anchor.npc());
        double range = Config.SELECTOR_RANGE.get();
        return player.distanceToSqr(Vec3.atCenterOf(BlockPos.of(anchor.selector()))) <= range * range
                && player.distanceToSqr(npc) <= range * range;
    }
    private static D1PartyLobby.Anchor anchor(ServerPlayer player, ClassSelectorMenu menu) {
        if (menu.tamsinNpc() != null)
            return new D1PartyLobby.Anchor(menu.tamsinNpc(), menu.tamsinBinding().dimension(), menu.tamsinBinding().selector());
        var party = LOBBY.party(player.getUUID());
        // An existing member may view their group at the matching selector, never another block.
        var source = ClassSelectorTeleportUtil.pendingSource(player);
        return party != null && source != null && source.dimId().equals(party.anchor().dimension())
                && source.posLong() == party.anchor().selector() ? party.anchor() : null;
    }
    public static void show(ServerPlayer player, boolean force) {
        if (!(player.containerMenu instanceof ClassSelectorMenu menu) || !menu.stillValid(player)
                || menu.tamsinNpc() == null || menu.stage() != TamsinFlow.Stage.READY || !onboarded(player)) return;
        VIEWERS.add(player.getUUID());
        var p = LOBBY.party(player.getUUID());
        var anchor = anchor(player, menu);
        var block = anchor == null ? null : selector(player.level().getServer(), anchor);
        int capacity = p == null ? (block == null ? 6 : block.getMaxPlayers()) : p.capacity();
        var members = p == null ? new ArrayList<PartyPayloads.Member>() : new ArrayList<>(p.members().stream().map(id -> {
            var member = player.level().getServer().getPlayerList().getPlayer(id);
            return new PartyPayloads.Member(member == null ? "Offline" : member.getGameProfile().name(),
                    member == null ? "none" : ClassData.getClassId(member), p.ready().contains(id), p.leader().equals(id));
        }).toList());
        if(p!=null)p.contracts().forEach(hire->members.add(new PartyPayloads.Member(hire.name(),hire.classId(),true,false)));
        var i = LOBBY.invitation(player.getUUID());
        var sender = i == null ? null : player.level().getServer().getPlayerList().getPlayer(i.inviter());
        boolean leader = p != null && p.leader().equals(player.getUUID());
        boolean canSolo = block != null && nearby(player, anchor)
                && LOBBY.canStartSolo(player.getUUID(), capacity);
        String phase = p == null ? (canSolo ? "SOLO_AVAILABLE" : "UNGROUPED") : p.phase().name();
        boolean canInvite = block != null && capacity >= 2 && nearby(player, anchor) && p != null
                && p.anchor().equals(anchor) && p.occupied() < capacity
                && p.phase() != D1PartyLobby.Phase.QUEUED && p.phase() != D1PartyLobby.Phase.PREPARING;
        int countdown = p == null || p.countdownEnd() < 0 ? -1 : (int)Math.max(0, (p.countdownEnd() - now(player.level().getServer()) + 19) / 20);
        var view = new PartyPayloads.View(menu.containerId,
                new PartyPayloads.State(LOBBY.revision(player.getUUID()), phase,
                        leader, capacity, LOBBY.queuePosition(p), countdown), members,
                new PartyPayloads.Invite(i == null ? "" : i.token(), sender == null ? "" : sender.getGameProfile().name(),
                        i != null && i.accepted(), canInvite), recruitment(player, menu, p, anchor), p == null ? "HARD" : p.difficulty().name(),
                new PartyPayloads.Hire(p==null?"":p.hires().getOrDefault(player.getUUID(),""),p!=null&&p.hires().containsKey(player.getUUID())?(int)p.hireFee(player.getUUID()):net.goui.cosmicdungeon.mercenary.MercenaryConfig.HIRE_TRACE.get()));
        if (force || !view.equals(menu.lastPartyView)) {
            menu.lastPartyView = view; ModNetwork.sendTo(player, view);
        }
    }
    private static PartyPayloads.Recruitment recruitment(ServerPlayer player, ClassSelectorMenu menu,
            D1PartyLobby.Party party, D1PartyLobby.Anchor anchor) {
        if (party == null) return new PartyPayloads.Recruitment("", LOBBY.looking(player.getUUID()), 0, 1, List.of());
        var server = player.level().getServer();
        var candidates = LOBBY.lookingAt(anchor).stream().map(server.getPlayerList()::getPlayer)
                .filter(D1PartyService::onboarded).filter(p -> LOBBY.party(p.getUUID()) == null)
                .sorted(Comparator.comparing(p -> p.getGameProfile().name(), String.CASE_INSENSITIVE_ORDER)).toList();
        int pages = Math.max(1, (candidates.size() + 3) / 4);
        menu.recruitmentPage = Math.clamp(menu.recruitmentPage, 0, pages - 1);
        var rows = candidates.stream().skip((long) menu.recruitmentPage * 4).limit(4)
                .map(p -> new PartyPayloads.Member(p.getGameProfile().name(), ClassData.getClassId(p), false, false)).toList();
        return new PartyPayloads.Recruitment(party.name(), false, menu.recruitmentPage, pages, rows);
    }
    private static String rosterProblem(MinecraftServer server, D1PartyLobby.Party p, boolean compareClasses) {
        var block = selector(server, p.anchor());
        if (block == null) return "Tamsin or the group selector is unavailable.";
        if (!D1PartyRules.fits(p.occupied(), p.capacity()))
            return "The group no longer fits this selector's party limits.";
        for (UUID id : p.members()) {
            var member = server.getPlayerList().getPlayer(id);
            if (!onboarded(member) || !inStartingDimension(member, p.anchor())) return "Every member must be eligible and in the starting dimension.";
            if (compareClasses && !Objects.equals(p.classes().get(id), ClassData.getClassId(member)))
                return "A member changed class. Begin a new ready check.";
        }
        return null;
    }
    private static boolean inStartingDimension(ServerPlayer player, D1PartyLobby.Anchor anchor) {
        return player != null && anchor != null
                && D1PartyRules.sameStartingDimension(player.level().dimension().location().toString(), anchor.dimension());
    }
    private static String readiness(ServerPlayer player, long revision, String action) {
        var p = LOBBY.party(player.getUUID());
        if (p == null) return "Join a group first.";
        String problem = rosterProblem(player.level().getServer(), p, true);
        if (problem != null) { LOBBY.cancel(p); return problem; }
        return switch (action) {
            case "ready" -> LOBBY.ready(player.getUUID(), revision);
            case "unready" -> LOBBY.unready(player.getUUID(), revision);
            default -> LOBBY.queue(player.getUUID(), revision);
        };
    }
    private static void hudAction(ServerPlayer player, PartyPayloads.Action request) {
        if (!available(player) || player.containerMenu != player.inventoryMenu
                || !Set.of("ready", "unready", "leave", "accept", "decline").contains(request.action())) return;
        var server = player.level().getServer();
        long tick = now(server);
        if (tick - LAST_HUD_ACTION.getOrDefault(player.getUUID(), -1000000L) < Config.PARTY_ACTION_TICKS.get()) return;
        LAST_HUD_ACTION.put(player.getUUID(), tick);
        String error = null;
        if (!LOBBY.current(player.getUUID(), request.revision())) error = "Group changed; review the updated roster.";
        else switch (request.action()) {
            case "ready", "unready" -> error = readiness(player, request.revision(), request.action());
            case "leave" -> notifyRemoved(server, player.getUUID(), "Group changed; readiness cleared.");
            case "accept" -> {
                error = LOBBY.accept(player.getUUID(), request.target(), tick);
                if (error == null) {
                    resumeInvitation(player);
                    if (LOBBY.invitation(player.getUUID()) != null)
                        message(player, "Invitation accepted. Finish agreement/class selection at Tamsin, then return to the starting dimension.");
                }
            }
            case "decline" -> LOBBY.decline(player.getUUID(), request.target());
        }
        if (error != null) message(player, error);
        syncViewers(server);
    }
    public static void action(ServerPlayer player, PartyPayloads.Action request) {
        if (request.containerId() == -1) { hudAction(player, request); return; }
        if (!(player.containerMenu instanceof ClassSelectorMenu menu) || menu.containerId != request.containerId()
                || menu.tamsinNpc() == null || !menu.stillValid(player) || !onboarded(player)) return;
        if (request.action().equals("group")) {
            if (!TamsinFlow.canReady(true, menu.stage())) return;
            menu.setStage(TamsinFlow.Stage.READY); ClassNet.sendSelectorDataTo(player); return;
        }
        if (menu.stage() != TamsinFlow.Stage.READY) return;
        var server = player.level().getServer();
        if (now(server) - menu.lastPartyActionTick < Config.PARTY_ACTION_TICKS.get()) { show(player, true); return; }
        menu.lastPartyActionTick = now(server);
        if (!LOBBY.current(player.getUUID(), request.revision())) {
            message(player, "Group changed; review the updated roster."); show(player, true); return;
        }
        var p = LOBBY.party(player.getUUID());
        var anchor = anchor(player, menu);
        if (p != null && !Set.of("leave", "unready", "decline", "accept").contains(request.action())
                && !p.anchor().equals(anchor)) {
            message(player, "Use the Tamsin bound to your group's selector."); show(player, true); return;
        }
        String error = null;
        switch (request.action()) {
            case "create" -> {
                if (anchor == null || !nearby(player, anchor)) { error = "Speak with Tamsin to create a group."; break; }
                error = LOBBY.create(player.getUUID(), anchor, request.revision(), request.target(), selector(server, anchor).getMaxPlayers());
            }
            case "hire", "release_hire" -> {
                if(anchor==null||!nearby(player,anchor)){error="Speak with Tamsin to change a hire.";break;}
                error=LOBBY.hire(player.getUUID(),request.revision(),request.action().equals("release_hire")?"":request.target(),net.goui.cosmicdungeon.mercenary.MercenaryConfig.HIRE_TRACE.get());
            }
            case "difficulty" -> error = LOBBY.difficulty(player.getUUID(), request.revision(), request.target());
            case "capacity" -> {
                try { error = LOBBY.capacity(player.getUUID(), request.revision(), Integer.parseInt(request.target())); }
                catch (NumberFormatException invalid) { error = "Choose a maximum of 1-6 members."; }
            }
            case "lfg" -> {
                if (anchor == null || !nearby(player, anchor)) { error = "Speak with Tamsin to advertise your class."; break; }
                error = LOBBY.advertise(player.getUUID(), anchor, !LOBBY.looking(player.getUUID()));
            }
            case "lfg_prev", "lfg_next" -> {
                if (p == null) { error = "Join a group before recruiting."; break; }
                menu.recruitmentPage += request.action().equals("lfg_next") ? 1 : -1;
            }
            case "class" -> {
                if (p != null && p.phase() == D1PartyLobby.Phase.PREPARING) break;
                withdraw(player);
                menu.setStage(TamsinFlow.Stage.SELECTOR); ClassNet.sendSelectorDataTo(player); return;
            }
            case "solo" -> {
                if (anchor == null || !nearby(player, anchor)) {
                    error = "Speak with Tamsin to start a solo party."; break;
                }
                error = LOBBY.startSolo(player.getUUID(), anchor, request.revision(),
                        selector(server, anchor).getMaxPlayers());
            }
            case "invite" -> {
                if (anchor == null || !nearby(player, anchor)) { error = "Speak with Tamsin to form a group."; break; }
                long last = LAST_INVITE.getOrDefault(player.getUUID(), -1000000L);
                if (now(server) - last < Config.PARTY_INVITE_COOLDOWN_TICKS.get()) { error = "Please wait before inviting again."; break; }
                var target = server.getPlayerList().getPlayerByName(request.target());
                if (!available(target)) { error = "Choose an online, eligible dungeoneer."; break; }
                error = LOBBY.invite(player.getUUID(), target.getUUID(), anchor, now(server),
                        Config.PARTY_INVITATION_SECONDS.get() * 20, 6);
                if (error == null) {
                    LAST_INVITE.put(player.getUUID(), now(server));
                    var invitation = LOBBY.invitation(target.getUUID());
                    message(player, "Invitation sent to " + target.getGameProfile().name() + ".");
                    target.sendSystemMessage(Component.literal(player.getGameProfile().name() + " invites you to " + p.name() + ". ")
                            .append(Component.literal("[Accept]").withStyle(Style.EMPTY.withUnderlined(true)
                                    .withClickEvent(new ClickEvent.RunCommand("/d1 party accept " + invitation.token()))))
                            .append(Component.literal(" "))
                            .append(Component.literal("[Decline]").withStyle(Style.EMPTY.withUnderlined(true)
                                    .withClickEvent(new ClickEvent.RunCommand("/d1 party decline " + invitation.token())))));
                }
            }
            case "accept" -> {
                error = LOBBY.accept(player.getUUID(), request.target(), now(server));
                if (error == null) resumeInvitation(player);
            }
            case "decline" -> LOBBY.decline(player.getUUID(), request.target());
            case "leave" -> notifyRemoved(server, player.getUUID(), "Group changed; readiness and queue submission cleared.");
            case "unready" -> error = readiness(player, request.revision(), "unready");
            case "begin" -> {
                if (p == null) { error = "Invite players before starting a ready check."; break; }
                error = rosterProblem(server, p, false);
                if (error == null) {
                    Map<UUID, String> classes = new LinkedHashMap<>();
                    p.members().forEach(id -> classes.put(id, ClassData.getClassId(server.getPlayerList().getPlayer(id))));
                    error = LOBBY.begin(player.getUUID(), request.revision(), classes);
                }
            }
            case "ready", "queue" -> {
                error = readiness(player, request.revision(), request.action());
                if (D1PartyRules.closeAfterReady(p, player.getUUID(), request.action(), error)) player.closeContainer();
            }
            default -> error = "Unknown group action.";
        }
        if (error != null) message(player, error);
        show(player, true); syncViewers(server);
    }
    public static void resumeInvitation(ServerPlayer player) {
        var i = LOBBY.invitation(player.getUUID());
        if (i == null || !i.accepted()) return;
        var server = player.level().getServer();
        var inviter = server.getPlayerList().getPlayer(i.inviter());
        var block = selector(server, i.anchor());
        if (!available(inviter) || block == null) { LOBBY.decline(player.getUUID(), i.token()); return; }
        boolean eligible = onboarded(player) && inStartingDimension(player, i.anchor())
                && onboarded(inviter) && inStartingDimension(inviter, i.anchor());
        boolean wasGrouped = LOBBY.party(player.getUUID()) != null;
        String error = LOBBY.joinAccepted(player.getUUID(), now(server), 6, eligible);
        if (error != null) message(player, error);
        else if (!wasGrouped && LOBBY.party(player.getUUID()) != null) {
            var party = LOBBY.party(player.getUUID());
            notify(server, party, player.getGameProfile().name() + " joined. All ready confirmations cleared.");
        }
    }
    public static void withdraw(ServerPlayer player) {
        var p = LOBBY.party(player.getUUID());
        if (p != null && p.phase() != D1PartyLobby.Phase.PREPARING) {
            LOBBY.cancel(p); notify(player.level().getServer(), p, "Ready check and queue cancelled."); syncViewers(player.level().getServer());
        }
    }
    private static void notify(MinecraftServer server, D1PartyLobby.Party p, String text) {
        p.members().forEach(id -> message(server.getPlayerList().getPlayer(id), text));
    }
    private static void notifyRemoved(MinecraftServer server, UUID player, String text) {
        LOBBY.remove(player).forEach(id -> message(server.getPlayerList().getPlayer(id), text));
    }
    private static void syncViewers(MinecraftServer server) {
        D1PartyHudService.sync(server, LOBBY);
        for (UUID id : List.copyOf(VIEWERS)) {
            var player = server.getPlayerList().getPlayer(id);
            if (player == null || !(player.containerMenu instanceof ClassSelectorMenu menu)
                    || menu.stage() != TamsinFlow.Stage.READY || !menu.stillValid(player)) VIEWERS.remove(id);
            else show(player, false);
        }
    }
    public static void tick(MinecraftServer server) {
        long now = now(server);
        if (now % Config.PARTY_POLL_TICKS.get() != 0) return;
        LOBBY.expire(now);
        for (var id : LOBBY.lookingPlayers()) {
            if (!onboarded(server.getPlayerList().getPlayer(id)))
                LOBBY.advertise(id, null, false);
        }
        for (var i : LOBBY.invitations()) {
            var target = server.getPlayerList().getPlayer(i.target());
            if (!available(target) || !available(server.getPlayerList().getPlayer(i.inviter()))) LOBBY.decline(i.target(), i.token());
            else if (i.accepted()) resumeInvitation(target);
        }
        for (var p : LOBBY.parties()) {
            for (UUID id : p.members()) {
                if (!available(server.getPlayerList().getPlayer(id))) notifyRemoved(server, id, "A member left; ready confirmations cleared.");
            }
            if (LOBBY.party(p.leader()) != p) continue;
            if (p.phase() != D1PartyLobby.Phase.ASSEMBLY && p.phase() != D1PartyLobby.Phase.PREPARING) {
                String problem = rosterProblem(server, p, true);
                if (problem != null) { LOBBY.cancel(p); notify(server, p, problem); }
            }
        }
        var queued = LOBBY.queued();
        if (!queued.isEmpty()) {
            var p = queued.getFirst();
            if (DungeonRunRegistryData.get(server).firstAvailableSlot().isEmpty()) LOBBY.waitForSlot(p);
            else {
                LOBBY.startCountdown(p, now, Config.READY_COUNTDOWN_SECONDS.get() * 20);
                if (LOBBY.prepare(p, now)) {
                    boolean success = false;
                    try {
                        success = ClassSelectorEntryService.enter(server, p.anchor(), p.members(), p.classes(),
                                p.contracts(),
                                net.goui.cosmicdungeon.dungeon.DungeonDifficultyConfig.snapshot(p.difficulty()),
                                () -> LOBBY.party(p.leader()) == p && rosterProblem(server, p, true) == null);
                    } catch (RuntimeException failure) {
                        com.mojang.logging.LogUtils.getLogger().error("D1 party entry failed; cancelling preparation", failure);
                        var leader = server.getPlayerList().getPlayer(p.leader());
                        if (leader != null) DungeonLifecycleService.abortActiveRunForPlayer(leader);
                    } finally {
                        if (success) LOBBY.complete(p);
                        else { LOBBY.cancel(p); notify(server, p, "Entry stopped. Review the group and begin a new ready check."); }
                    }
                }
            }
        }
        syncViewers(server);
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            notifyRemoved(player.level().getServer(), player.getUUID(), "A member disconnected; group preparation cancelled.");
            VIEWERS.remove(player.getUUID()); LAST_INVITE.remove(player.getUUID()); LAST_HUD_ACTION.remove(player.getUUID());
            D1PartyHudService.forget(player.getUUID()); syncViewers(player.level().getServer());
        }
    }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) withdraw(player);
    }
    public static void clear() { LOBBY.clear(); VIEWERS.clear(); LAST_INVITE.clear(); LAST_HUD_ACTION.clear(); D1PartyHudService.clear(); }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) D1PartyHudService.track(player.getUUID());
    }
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = Commands.literal("party");
        for (String action : List.of("accept", "decline")) {
            root.then(Commands.literal(action).then(Commands.argument("token", StringArgumentType.word()).executes(ctx -> {
                var player = ctx.getSource().getPlayerOrException();
                if (!available(player)) return 0;
                String token = StringArgumentType.getString(ctx, "token");
                if (action.equals("decline")) LOBBY.decline(player.getUUID(), token);
                else {
                    String error = LOBBY.accept(player.getUUID(), token, now(player.level().getServer()));
                    if (error != null) { message(player, error); return 0; }
                    resumeInvitation(player);
                    if (LOBBY.invitation(player.getUUID()) != null)
                        message(player, "Invitation accepted. Finish Tamsin's agreement and class selection near your group.");
                }
                syncViewers(player.level().getServer()); return 1;
            })));
        }
        root.then(Commands.literal("leave").executes(ctx -> {
            var player = ctx.getSource().getPlayerOrException();
            notifyRemoved(player.level().getServer(), player.getUUID(), "Group changed; ready confirmations cleared.");
            syncViewers(player.level().getServer()); return 1;
        }));
        dispatcher.register(Commands.literal("d1").then(root));
    }
}
