package net.goui.cosmicdungeon.npc.tamsin;

import com.mojang.authlib.GameProfile;
import java.util.*;
import net.goui.cosmicdungeon.economy.CurrencyService;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.goui.cosmicdungeon.trade.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.item.*;

/** Native CI menus/custody; temporary in-memory connections never authenticate or launch a client. */
public final class PartyTradeGameTests {
    private PartyTradeGameTests() {}
    private static final class Fixture implements AutoCloseable {
        final GameTestHelper helper; final ServerLevel level; final D1PartyLobby lobby;
        final List<ServerPlayer> actors = new ArrayList<>();
        final List<io.netty.channel.embedded.EmbeddedChannel> channels = new ArrayList<>();
        final Map<UUID,ServerPlayer> players; final List<ServerPlayer> online;
        final ServerPlayer leader, member, outsider;
        final D1PartyLobby.Party party;
        @SuppressWarnings("unchecked") Fixture(GameTestHelper helper) {
            this.helper=helper;level=helper.getLevel();var server=level.getServer();
            try {
                var lf=D1PartyService.class.getDeclaredField("LOBBY");lf.setAccessible(true);lobby=(D1PartyLobby)lf.get(null);
                var pf=PlayerList.class.getDeclaredField("playersByUUID");pf.setAccessible(true);players=(Map<UUID,ServerPlayer>)pf.get(server.getPlayerList());
                var of=PlayerList.class.getDeclaredField("players");of.setAccessible(true);online=(List<ServerPlayer>)of.get(server.getPlayerList());
            } catch(ReflectiveOperationException e) { throw new IllegalStateException(e); }
            leader=player("TradeLeader");member=player("TradeMember");outsider=player("TradeOutsider");
            var anchor=new D1PartyLobby.Anchor(UUID.randomUUID(),level.dimension().location().toString(),0);
            ok(lobby.create(leader.getUUID(),anchor,0,"Trade test",6));
            ok(lobby.invite(leader.getUUID(),member.getUUID(),anchor,0,100,6));
            ok(lobby.accept(member.getUUID(),lobby.invitation(member.getUUID()).token(),1));
            ok(lobby.joinAccepted(member.getUUID(),2,6,true));
            party=lobby.party(leader.getUUID());
            ok(lobby.begin(leader.getUUID(),party.revision(),Map.of(leader.getUUID(),"bogatyr",member.getUUID(),"theurgist")));
            ok(lobby.ready(leader.getUUID(),party.revision()));ok(lobby.ready(member.getUUID(),party.revision()));
        }
        ServerPlayer player(String name) {
            var server=level.getServer();var profile=new GameProfile(UUID.randomUUID(),name);
            var p=new ServerPlayer(server,level,profile,ClientInformation.createDefault());
            var channel=new io.netty.channel.embedded.EmbeddedChannel();channels.add(channel);
            var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND) {
                @Override public io.netty.channel.Channel channel(){return channel;}
                @Override public boolean isConnected(){return true;}
            };
            net.neoforged.neoforge.network.registration.ChannelAttributes.setConnectionType(connection,net.neoforged.neoforge.network.connection.ConnectionType.NEOFORGE);
            net.neoforged.neoforge.network.registration.ChannelAttributes.setPayloadSetup(connection,net.neoforged.neoforge.network.registration.NetworkPayloadSetup.empty());
            p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(server,connection,p,
                    net.minecraft.server.network.CommonListenerCookie.createInitial(profile,false)) {
                @Override public void send(net.minecraft.network.protocol.Packet<?> packet){}
                @Override public void send(net.minecraft.network.protocol.Packet<?> packet,io.netty.channel.ChannelFutureListener listener){}
            };
            p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            check(!p.hasInfiniteMaterials(),"Trade fixtures use finite survival inventories");
            p.setHealth(20);p.setPos(helper.absoluteVec(new net.minecraft.world.phys.Vec3(1,3,1)));
            var root=new CompoundTag();root.putString(ClassData.KEY_CLASS_ID,"theurgist");p.getPersistentData().put(ClassData.ROOT_TAG,root);
            actors.add(p);players.put(p.getUUID(),p);online.add(p);CurrencyService.setBalanceTrace(p,100);
            return p;
        }
        TradeSessionData.TradeSession open() {
            TradeSessionData.invite(member,outsider);
            check(TradeSessionData.acceptInvite(outsider,member),"Ready members can open a native trade");
            return TradeSessionData.get(member);
        }
        void offer(TradeSessionData.TradeSession session,ServerPlayer player,Item item,int amount) {
            player.getInventory().setItem(0,new ItemStack(item,amount));
            check(net.goui.cosmicdungeon.economy.pricing.ItemTransferRules.tradeEligible(player.getInventory().getItem(0)),
                    "Fixture item must satisfy the unchanged trade catalog");
            ((TradeMenu)player.containerMenu).quickMoveStack(player,TradeMenu.HOTBAR_START);
            check(player.getInventory().getItem(0).isEmpty()
                    &&session.getContainerFor(player,true).getItem(0).getCount()==amount,"Native shift-click places the actual offered stack");
        }
        void readyUnchanged(long revision) {
            check(party.phase()==D1PartyLobby.Phase.READY_CHECK&&party.revision()==revision
                    &&party.ready().equals(Set.of(leader.getUUID(),member.getUUID())),"Trade operations preserve every ready confirmation and revision");
        }
        void ok(String error){check(error==null,error==null?"success":error);}
        void check(boolean value,String message){helper.assertTrue(value,Component.literal(message));}
        String start(ServerPlayer actor,long revision){return D1PartyTrades.start(level.getServer(),lobby,actor.getUUID(),revision);}
        @Override public void close() {
            lobby.complete(party);
            for(var p:actors){TradeSessionData.handleLogout(p);CurrencyService.clear(p);players.remove(p.getUUID());online.remove(p);p.discard();}
            channels.forEach(io.netty.channel.embedded.EmbeddedChannel::finishAndReleaseAll);
        }
    }
    public static void start(GameTestHelper helper) {
        try(var f=new Fixture(helper)) {
            long revision=f.party.revision();var session=f.open();f.readyUnchanged(revision);
            f.offer(session,f.member,Items.APPLE,3);f.offer(session,f.outsider,Items.BREAD,2);
            f.member.containerMenu.setCarried(new ItemStack(Items.GOLD_INGOT,5));
            session.setCurrency(f.member,25);session.setReady(f.member,true);session.setReady(f.outsider,true);
            session.setConfirm(f.member,true);f.readyUnchanged(revision);
            f.check(f.start(f.member,revision)!=null&&f.start(f.leader,revision-1)!=null,"Unauthorized and stale Start are rejected");
            f.check(TradeSessionData.isBusy(f.member),"Rejected Start leaves the unfinished trade intact");
            f.ok(f.start(f.leader,revision));
            f.check(f.party.phase()==D1PartyLobby.Phase.QUEUED&&f.party.ready().size()==2,"Start queues the same ready group");
            f.check(!TradeSessionData.isBusy(f.member)&&!TradeSessionData.isBusy(f.outsider)
                    &&f.member.containerMenu==f.member.inventoryMenu&&f.outsider.containerMenu==f.outsider.inventoryMenu,"Start closes both menus, including outsider");
            f.check(f.member.getInventory().countItem(Items.APPLE)==3&&f.member.getInventory().countItem(Items.GOLD_INGOT)==5
                    &&f.outsider.getInventory().countItem(Items.BREAD)==2,"Offers and cursor return to their original owners once");
            f.check(CurrencyService.getBalanceTrace(f.member)==100&&CurrencyService.getBalanceTrace(f.outsider)==100,"Unfinished payment is never charged");
            session.setConfirm(f.outsider,true);session.cancel("duplicate");
            f.check(f.member.getInventory().countItem(Items.APPLE)==3&&CurrencyService.getBalanceTrace(f.member)==100,"Stale confirm/cancel cannot transfer or duplicate");
            TradeSessionData.invite(f.outsider,f.member);
            f.check(!TradeSessionData.acceptInvite(f.member,f.outsider)&&!TradeSessionData.isBusy(f.member),"Queued participant cannot reopen a trade");
            f.lobby.startCountdown(f.party,0,1);f.check(f.lobby.prepare(f.party,1),"Fixture enters preparation");
            f.check(!D1PartyService.tradingAllowed(f.member),"Preparing participant cannot trade");
            f.lobby.complete(f.party);f.check(D1PartyService.tradingAllowed(f.member),"Entry completion restores ordinary trading");
            helper.succeed();
        }
    }
    public static void completed(GameTestHelper helper) {
        try(var f=new Fixture(helper)) {
            long revision=f.party.revision();var session=f.open();
            f.offer(session,f.member,Items.APPLE,3);f.offer(session,f.outsider,Items.BREAD,2);
            session.setCurrency(f.member,25);session.setReady(f.member,true);session.setReady(f.outsider,true);
            session.setConfirm(f.member,true);session.setConfirm(f.outsider,true);
            f.check(!TradeSessionData.isBusy(f.member),"Both confirmations complete the native trade");
            f.readyUnchanged(revision);
            f.check(f.outsider.getInventory().countItem(Items.APPLE)==3&&f.member.getInventory().countItem(Items.BREAD)==2
                    &&CurrencyService.getBalanceTrace(f.member)==75&&CurrencyService.getBalanceTrace(f.outsider)==125,"Completed items and Trace change owners exactly once; actual="+f.outsider.getInventory().countItem(Items.APPLE)+"/"
                    +f.member.getInventory().countItem(Items.BREAD)+"/"+CurrencyService.getBalanceTrace(f.member)+"/"+CurrencyService.getBalanceTrace(f.outsider));
            f.ok(f.start(f.leader,revision));
            f.check(f.outsider.getInventory().countItem(Items.APPLE)==3&&CurrencyService.getBalanceTrace(f.member)==75,"Adventure start never reverses a completed trade");
            helper.succeed();
        }
    }
    public static void fullInventory(GameTestHelper helper) {
        try(var f=new Fixture(helper)) {
            var session=f.open();f.offer(session,f.member,Items.APPLE,3);
            for(int i=0;i<36;i++)f.member.getInventory().setItem(i,new ItemStack(Items.STONE,64));
            f.ok(f.start(f.leader,f.party.revision()));
            f.check(f.member.getInventory().countItem(Items.STONE)==2304&&f.member.getInventory().countItem(Items.APPLE)==0,"Full inventory is never overwritten");
            f.member.getInventory().setItem(0,ItemStack.EMPTY);TradeCustody.claim(f.member);TradeCustody.claim(f.member);
            f.check(f.member.getInventory().countItem(Items.APPLE)==3,"Overflow stays recoverable and is claimed once; actual="+f.member.getInventory().countItem(Items.APPLE)
                    +", pending="+net.goui.cosmicdungeon.item.identity.ProtectedItemRecovery.pendingHere(f.member));
            helper.succeed();
        }
    }
}
