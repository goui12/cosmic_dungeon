package net.goui.cosmicdungeon.npc.tamsin;

import com.mojang.authlib.GameProfile;
import java.util.*;
import java.io.IOException;
import java.nio.file.*;
import net.goui.cosmicdungeon.economy.CurrencyService;
import net.goui.cosmicdungeon.economy.PlayerCurrencyData;
import net.goui.cosmicdungeon.component.ModDataComponents;
import net.goui.cosmicdungeon.dungeon.*;
import net.goui.cosmicdungeon.transaction.PlayerSaveProof;
import net.goui.cosmicdungeon.gametest.DurableRecoveryGameTestSupport;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.goui.cosmicdungeon.trade.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.*;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.*;
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
        final ServerPlayer leader; ServerPlayer member, outsider;
        final List<Path> files=new ArrayList<>();
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
            for(var id:party.members())ok(lobby.resourceSharing(id,party.revision(),party.id().toString(),false));
            ok(lobby.begin(leader.getUUID(),party.revision(),Map.of(leader.getUUID(),"bogatyr",member.getUUID(),"theurgist")));
            ok(lobby.ready(leader.getUUID(),party.revision()));ok(lobby.ready(member.getUUID(),party.revision()));
        }
        ServerPlayer player(String name) {
            var profile=new GameProfile(UUID.randomUUID(),name);var base=level.getServer().getWorldPath(LevelResource.PLAYER_DATA_DIR);
            for(String suffix:List.of(".dat",".dat_old")){
                var file=base.resolve(profile.id()+suffix);check(!Files.exists(file),"New trade fixture owner files");files.add(file);
            }
            return player(profile,true);
        }
        ServerPlayer player(GameProfile profile,boolean initializeBalance) {
            var server=level.getServer();
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
            actors.add(p);players.put(p.getUUID(),p);online.add(p);if(initializeBalance)CurrencyService.setBalanceTrace(p,100);
            return p;
        }
        CompoundTag disk(ServerPlayer player){
            try{return NbtIo.readCompressed(level.getServer().getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(player.getStringUUID()+".dat"),NbtAccounter.create(64L*1024*1024));}
            catch(IOException ex){throw new IllegalStateException(ex);}
        }
        ServerPlayer reload(ServerPlayer previous){
            var image=disk(previous);online.remove(previous);var next=player(previous.getGameProfile(),false);
            next.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),image));next.connection.player=next;
            if(previous==member)member=next;if(previous==outsider)outsider=next;
            check(next!=previous&&players.get(next.getUUID())==next&&PlayerSaveProof.matches(image,PlayerSaveProof.snapshot(next)),
                    "Native trade owner disk image replaces the original object without resetting balances");
            return next;
        }
        PlayerCurrencyData reloadAccounts(){return DurableRecoveryGameTestSupport.reload(level.getServer(),PlayerCurrencyData.class,PlayerCurrencyData.SAVE_ID);}
        void end(TradeSessionData.TradeSession session){
            try{var end=TradeSessionData.TradeSession.class.getDeclaredMethod("end");end.setAccessible(true);end.invoke(session);}
            catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}
            member.closeContainer();outsider.closeContainer();
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
            try{for(var file:files)Files.deleteIfExists(file);}catch(IOException ex){throw new IllegalStateException("Trade fixture owner cleanup failed",ex);}
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
    @SuppressWarnings("unchecked")
    public static void outsiderRecoveryHold(GameTestHelper helper) {
        try(var f=new Fixture(helper)) {
            var session=f.open();f.offer(session,f.member,Items.APPLE,3);f.offer(session,f.outsider,Items.BREAD,2);
            final Set<ServerPlayer> holds;
            try {
                var field=TradeCustody.class.getDeclaredField("HOLDS");field.setAccessible(true);
                holds=(Set<ServerPlayer>)field.get(null);
            } catch(ReflectiveOperationException error) { throw new IllegalStateException(error); }
            long revision=f.party.revision();
            // Simulate the durable recovery guard independently of a real client's disconnect transport.
            holds.add(f.outsider);
            try {
                f.check(f.start(f.leader,revision)!=null,"An outside trader's recovery hold blocks this Start");
                f.readyUnchanged(revision);
                f.check(!TradeSessionData.isBusy(f.member)&&!TradeSessionData.isBusy(f.outsider),
                        "Both unfinished trade sessions still close");
                f.check(!TradeCustody.held(f.member),"The party member is not the failed participant");
                for(int retry=0;retry<3;retry++) {
                    f.check(f.start(f.leader,revision)!=null,"Every retry retains the outside recovery dependency");
                    f.readyUnchanged(revision);
                }
                f.check(f.member.getInventory().countItem(Items.APPLE)==3,"Successful member refund remains intact");
            } finally { holds.remove(f.outsider); }
            f.players.remove(f.outsider.getUUID());
            try {
                f.check(f.start(f.leader,revision)!=null,"An offline peer does not bypass pending recovery");
                f.readyUnchanged(revision);
            } finally {f.players.put(f.outsider.getUUID(),f.outsider);}
            f.check(TradeCustody.beforeInventoryChange(f.outsider),"Outside recovery can resume");
            TradeCustody.claim(f.outsider);TradeCustody.claim(f.outsider);
            f.check(f.outsider.getInventory().countItem(Items.BREAD)==2,"Outside refund is preserved exactly once");
            var next=f.player("NextTrader");
            TradeSessionData.invite(f.outsider,next);
            f.check(TradeSessionData.acceptInvite(next,f.outsider),"A recovered outsider can open a new independent trade");
            f.ok(f.start(f.leader,revision));
            f.check(TradeSessionData.isBusy(f.outsider)&&TradeSessionData.isBusy(next),
                    "Resolving an old dependency does not cancel the outsider's later unrelated trade");
            f.check(f.member.getInventory().countItem(Items.APPLE)==3&&f.outsider.getInventory().countItem(Items.BREAD)==2,
                    "Retries and subsequent Start never duplicate either refund");
            helper.succeed();
        }
    }

    public static void ownershipAndBalance(GameTestHelper helper) {
        ordinaryTradeRecovery(helper);
        malformedTradeProofs(helper);
        chopTrades(helper);
        cookedChopTrades(helper);
        chopRegistryReplay(helper);
        try(var f=new Fixture(helper)) {
            CurrencyService.setBalanceTrace(f.member,200_000);
            CurrencyService.setBalanceTrace(f.outsider,200_000);
            var session=f.open();
            for(var denomination:net.goui.cosmicdungeon.economy.CurrencyDenomination.values()) {
                session.adjustCurrency(f.member,denomination.id(),10);
                f.check(TradeOfferBalance.remaining(CurrencyService.getAvailableTrace(f.member),10*denomination.traceValue())
                        ==200_000-10*denomination.traceValue(),"Every denomination subtracts exactly once from the offer preview");
                session.adjustCurrency(f.member,denomination.id(),-10);
            }
            var sword=new ItemStack(Items.DIAMOND_SWORD);
            sword.setDamageValue(17);
            net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA,sword,t->{
                t.putString(net.goui.cosmicdungeon.item.identity.ClassItemOwnership.KEY,f.member.getUUID().toString());
                t.putBoolean("no_trade",true);t.putString("future","untouched");
            });
            f.member.getInventory().setItem(0,sword);
            f.check(!net.goui.cosmicdungeon.item.identity.ItemMovementGuard.denyClick(f.member,TradeMenu.HOTBAR_START,0,
                    net.minecraft.world.inventory.ClickType.QUICK_MOVE),"Packet guard admits an owned ordinary sword to trade custody");
            ((TradeMenu)f.member.containerMenu).quickMoveStack(f.member,TradeMenu.HOTBAR_START);
            f.check(session.getContainerFor(f.member,true).getItem(0).is(Items.DIAMOND_SWORD),"Unpriced sword enters offer");
            var potion=net.goui.cosmicdungeon.crafting.PotionStacks.crafted(net.minecraft.world.item.alchemy.PotionContents.createItemStack(
                    Items.SPLASH_POTION,net.minecraft.world.item.alchemy.Potions.HEALING));
            potion.setCount(47);potion.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,Component.literal("Trade potion"));
            net.goui.cosmicdungeon.playerclass.api.ClassItemUtil.attune(potion,"theurgist",1,1,0);
            f.outsider.getInventory().setItem(0,potion.copy());
            f.check(!net.goui.cosmicdungeon.item.identity.ItemMovementGuard.denyClick(f.outsider,TradeMenu.HOTBAR_START,0,
                    net.minecraft.world.inventory.ClickType.QUICK_MOVE),"Attuned potion is permitted through actual click guard");
            ((TradeMenu)f.outsider.containerMenu).quickMoveStack(f.outsider,TradeMenu.HOTBAR_START);
            f.check(session.getContainerFor(f.outsider,true).getItem(0).is(Items.SPLASH_POTION),"Potion exemption reaches native offer");
            f.member.containerMenu.setCarried(new ItemStack(Items.GOLD_INGOT,3));
            session.setCurrency(f.member,5);session.setCurrency(f.outsider,10);
            session.setReady(f.member,true);session.setReady(f.outsider,true);
            session.setConfirm(f.member,true);session.setConfirm(f.outsider,true);
            f.check(!TradeSessionData.isBusy(f.member),"Native mixed trade commits");
            f.check(CurrencyService.getBalanceTrace(f.member)==200_005&&CurrencyService.getBalanceTrace(f.outsider)==199_995,
                    "Simultaneous currency offers settle once in opposite directions");
            ItemStack received=ItemStack.EMPTY;
            for(int i=0;i<36;i++)if(f.outsider.getInventory().getItem(i).is(Items.DIAMOND_SWORD))received=f.outsider.getInventory().getItem(i);
            f.check(!received.isEmpty()&&f.outsider.getUUID().equals(net.goui.cosmicdungeon.item.identity.ClassItemOwnership.owner(received))
                    &&received.getDamageValue()==17,"Committed sword retains authored damage and acquires recipient custody");
            f.check(f.member.getInventory().countItem(Items.SPLASH_POTION)==47&&f.member.getInventory().countItem(Items.GOLD_INGOT)==3,
                    "Potion stack transfers while original cursor remains with its owner");
            var expectedPotion=TradeItemTransfer.forRecipient(potion,f.outsider.getUUID(),f.member.getUUID());
            f.check(java.util.stream.IntStream.range(0,36).mapToObj(i->f.member.getInventory().getItem(i))
                    .anyMatch(item->item.getCount()==47&&ItemStack.isSameItemSameComponents(expectedPotion,item)),
                    "Native trade retains all potion components and only projects recipient ownership");
            session.cancel("stale");session.setConfirm(f.member,true);
            f.check(f.outsider.getInventory().countItem(Items.DIAMOND_SWORD)==1&&CurrencyService.getBalanceTrace(f.member)==200_005,
                    "Stale traffic cannot duplicate delivery/payment");
            var dropped=f.outsider.drop(received.copy(),false);
            f.check(dropped!=null&&f.outsider.getUUID().equals(dropped.getTarget()),"Recipient owns subsequent ground drop");
            if(dropped!=null)dropped.discard();
            helper.succeed();
        }
    }
    private static void chopTrades(GameTestHelper helper) {
        for(boolean alreadyOwned:new boolean[]{false,true})try(var f=new Fixture(helper)) {
            var registry=net.goui.cosmicdungeon.dungeon.ChopOwnershipData.get(f.level.getServer());
            var item=net.goui.cosmicdungeon.item.ModItems.RAW_FARROWS_CHOP.get();
            var offered=net.goui.cosmicdungeon.dungeon.ChopOwnershipService.preparePurchase(f.member,new ItemStack(item));
            net.goui.cosmicdungeon.dungeon.ChopOwnershipService.purchased(f.member,offered);
            var token=offered.get(net.goui.cosmicdungeon.component.ModDataComponents.CHOP_TOKEN.get());
            UUID otherToken=null;
            if(alreadyOwned) {
                var own=net.goui.cosmicdungeon.dungeon.ChopOwnershipService.preparePurchase(f.outsider,new ItemStack(item));
                net.goui.cosmicdungeon.dungeon.ChopOwnershipService.purchased(f.outsider,own);
                otherToken=own.get(net.goui.cosmicdungeon.component.ModDataComponents.CHOP_TOKEN.get());
                f.outsider.getInventory().setItem(0,own);
            }
            try {
                var session=f.open();f.member.getInventory().setItem(0,offered);
                ((TradeMenu)f.member.containerMenu).quickMoveStack(f.member,TradeMenu.HOTBAR_START);
                f.check(session.getContainerFor(f.member,true).getItem(0).is(item),"Personal Chop enters authorized trade escrow");
                session.setReady(f.member,true);session.setReady(f.outsider,true);
                session.setConfirm(f.member,true);session.setConfirm(f.outsider,true);
                if(alreadyOwned) {
                    f.check(f.member.getInventory().countItem(item)==1&&f.outsider.getInventory().countItem(item)==1,
                            "One-Chop capacity conflict cancels without losing either original");
                    f.check(registry.entry(f.member.getUUID()).token().equals(token.toString()),"Cancellation keeps donor registry entitlement");
                } else {
                    f.check(registry.entry(f.member.getUUID())==null&&registry.entry(f.outsider.getUUID()).token().equals(token.toString()),
                            "Committed trade moves exact registry token once");
                    var received=java.util.stream.IntStream.range(0,36).mapToObj(i->f.outsider.getInventory().getItem(i)).filter(s->s.is(item)).findFirst().orElseThrow();
                    f.check(net.goui.cosmicdungeon.dungeon.ChopOwnershipService.owned(f.outsider,received),"Recipient can use the transferred Chop");
                    f.check(TradeTransactions.reconcile(f.member)&&TradeTransactions.reconcile(f.outsider)
                            &&f.outsider.getInventory().countItem(item)==1,"Repeated recovery never repeats Chop delivery");
                }
            } finally {
                registry.release(f.member.getUUID(),token);registry.release(f.outsider.getUUID(),token);
                if(otherToken!=null)registry.release(f.outsider.getUUID(),otherToken);
                f.check(registry.flushVerified(),"Only fixture Chop registry entries retired");
            }
        }
    }
    /** Real native escrow objects remain owned by their original player throughout a cooked transfer. */
    @SuppressWarnings("unchecked")
    private static void cookedChopTrades(GameTestHelper helper) {
        // 0: success; 1: missing recipient escrow; 2: another owner's escrow; 3: own escrow not outside-active.
        for(int scenario=0;scenario<4;scenario++)try(var f=new Fixture(helper)) {
            long run=Long.MAX_VALUE-4320-scenario;var server=f.level.getServer();
            final Map<Long,DungeonRunRegistryData.RunRecord> runs;
            try {
                var field=DungeonRunRegistryData.class.getDeclaredField("runsById");field.setAccessible(true);
                runs=(Map<Long,DungeonRunRegistryData.RunRecord>)field.get(DungeonRunRegistryData.get(server));
            } catch(ReflectiveOperationException error) { throw new IllegalStateException(error); }
            f.check(!runs.containsKey(run),"Cooked trade fixture never overwrites an existing run");
            runs.put(run,new DungeonRunRegistryData.RunRecord(run,"dungeon_1",f.level.dimension().location().toString(),0,
                    List.of(f.level.dimension().location().toString()),1,"ACTIVE","",0,
                    List.of(f.member.getUUID(),f.outsider.getUUID()),List.of(),List.of()));
            var escrow=DungeonInventoryEscrowData.get(server);var registry=ChopOwnershipData.get(server);
            var item=net.goui.cosmicdungeon.item.ModItems.FARROWS_CHOP.get();
            var offered=ChopOwnershipService.preparePurchase(f.member,new ItemStack(item));
            ChopOwnershipService.purchased(f.member,offered);registry.bindRun(f.member.getUUID(),run);
            var token=offered.get(ModDataComponents.CHOP_TOKEN.get());
            var target=new DungeonReturnTarget(f.member.getUUID(),run,f.level.dimension().location().toString(),1.25,3,1.75,45,10);
            offered.set(ModDataComponents.DUNGEON_RETURN_TARGET.get(),target);
            offered.set(ModDataComponents.COORDINATES.get(),helper.absolutePos(new net.minecraft.core.BlockPos(1,3,1)));
            offered.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,Component.literal("Authored return Chop"));
            var donorItems=net.minecraft.core.NonNullList.withSize(f.member.getInventory().getContainerSize(),ItemStack.EMPTY);
            var recipientItems=net.minecraft.core.NonNullList.withSize(f.outsider.getInventory().getContainerSize(),ItemStack.EMPTY);
            donorItems.set(0,new ItemStack(Items.DIAMOND,11));recipientItems.set(0,new ItemStack(Items.EMERALD,7));
            var outside=ChopTravelRecovery.encode(f.member,net.minecraft.core.NonNullList.withSize(donorItems.size(),ItemStack.EMPTY));
            var donorEscrow=new DungeonInventoryEscrowData.Entry(run,f.member.getUUID(),ChopTravelRecovery.encode(f.member,donorItems),outside.copy(),true);
            var recipientEscrow=new DungeonInventoryEscrowData.Entry(run,f.outsider.getUUID(),ChopTravelRecovery.encode(f.outsider,recipientItems),outside.copy(),true);
            escrow.put(donorEscrow);escrow.put(recipientEscrow);
            try {
                f.check(escrow.flushVerified()&&registry.flushVerified(),"Distinct cooked custody fixtures are durably saved");
                var session=f.open();f.member.getInventory().setItem(0,offered.copy());
                ((TradeMenu)f.member.containerMenu).quickMoveStack(f.member,TradeMenu.HOTBAR_START);
                f.check(session.getContainerFor(f.member,true).getItem(0).is(item),"Cooked Chop reaches native offer slots");
                session.setReady(f.member,true);session.setReady(f.outsider,true);
                if(scenario!=0) {
                    escrow.remove(run,f.outsider.getUUID());
                    if(scenario==2)escrow.put(new DungeonInventoryEscrowData.Entry(run,f.leader.getUUID(),recipientEscrow.dungeonInventory().copy(),outside.copy(),true));
                    if(scenario==3)escrow.put(new DungeonInventoryEscrowData.Entry(run,f.outsider.getUUID(),recipientEscrow.dungeonInventory().copy(),outside.copy(),false));
                    f.check(escrow.flushVerified(),"Changed escrow authorization is saved before final confirmation");
                    boolean rejected=false;
                    try { ChopTradeCustody.prepare(f.member,f.outsider); }
                    catch(IllegalArgumentException expected) { rejected=true; }
                    f.check(rejected,"Cooked custody rejects missing, foreign, or non-outside recipient escrow");
                }
                var donorBefore=registry.image(f.member.getUUID());var recipientBefore=registry.image(f.outsider.getUUID());
                session.setConfirm(f.member,true);session.setConfirm(f.outsider,true);
                f.check(!TradeSessionData.isBusy(f.member)&&!TradeSessionData.isBusy(f.outsider),"Cooked attempt closes both native menus");
                if(scenario==0) {
                    var received=java.util.stream.IntStream.range(0,36).mapToObj(i->f.outsider.getInventory().getItem(i)).filter(s->s.is(item)).findFirst().orElseThrow();
                    var moved=received.get(ModDataComponents.DUNGEON_RETURN_TARGET.get());
                    f.check(ChopOwnershipService.owned(f.outsider,received)&&registry.entry(f.member.getUUID())==null,
                            "Cooked transfer moves registry entitlement and native item ownership together");
                    f.check(new DungeonReturnTarget(f.outsider.getUUID(),run,target.dimensionId(),target.x(),target.y(),target.z(),target.yaw(),target.pitch()).equals(moved)
                            &&Objects.equals(offered.get(ModDataComponents.COORDINATES.get()),received.get(ModDataComponents.COORDINATES.get()))
                            &&received.getHoverName().getString().equals("Authored return Chop"),"Recipient identity changes without losing return destination or authored metadata");
                    f.check(DungeonInventoryEscrowData.image(donorEscrow).equals(DungeonInventoryEscrowData.image(escrow.get(run,f.member.getUUID()).orElseThrow()))
                            &&DungeonInventoryEscrowData.image(recipientEscrow).equals(DungeonInventoryEscrowData.image(escrow.get(run,f.outsider.getUUID()).orElseThrow())),
                            "Trade never transfers, swaps, or rewrites either owner's stored dungeon inventory");
                    f.check(f.outsider.getInventory().countItem(Items.DIAMOND)==0&&f.outsider.getInventory().countItem(Items.EMERALD)==0,
                            "Trade grants only the Chop, never either escrow's contents");
                } else {
                    f.check(donorBefore.equals(registry.image(f.member.getUUID()))&&recipientBefore.equals(registry.image(f.outsider.getUUID())),
                            "Rejected cooked authorization leaves both registry images exact");
                    f.check(f.member.getInventory().countItem(item)==1&&f.outsider.getInventory().countItem(item)==0,
                            "Rejected cooked offer returns once to donor without transferring entitlement");
                }
                f.check(TradeTransactions.reconcile(f.member)&&TradeTransactions.reconcile(f.outsider)
                        &&f.member.getInventory().countItem(item)+f.outsider.getInventory().countItem(item)==1,
                        "Repeated cooked recovery preserves exactly one original Chop");
            } finally {
                TradeSessionData.handleLogout(f.member);TradeSessionData.handleLogout(f.outsider);
                for(var owner:List.of(f.member.getUUID(),f.outsider.getUUID(),f.leader.getUUID()))escrow.remove(run,owner);
                registry.release(f.member.getUUID(),token);registry.release(f.outsider.getUUID(),token);runs.remove(run);
                f.check(escrow.flushVerified()&&registry.flushVerified(),"Only cooked fixture escrow and registry rows retired");
            }
        }
    }
    /** Reconcile the real saved boundary after the registry pair moved but before its applied acknowledgement. */
    private static void chopRegistryReplay(GameTestHelper helper) {
        for(boolean donorFirst:new boolean[]{false,true})try(var f=new Fixture(helper)) {
            var server=f.level.getServer();var registry=ChopOwnershipData.get(server);var accounts=PlayerCurrencyData.get(server);
            var item=net.goui.cosmicdungeon.item.ModItems.RAW_FARROWS_CHOP.get();
            var offered=ChopOwnershipService.preparePurchase(f.member,new ItemStack(item));ChopOwnershipService.purchased(f.member,offered);
            var token=offered.get(ModDataComponents.CHOP_TOKEN.get());
            try {
                var session=f.open();f.member.getInventory().setItem(0,offered);
                ((TradeMenu)f.member.containerMenu).quickMoveStack(f.member,TradeMenu.HOTBAR_START);
                f.check(TradeTransactions.decide(f.member,f.outsider,session.id(),0,0),"Replay fixture reaches the real durable COMMITTED decision");
                var plan=accounts.tradePlan(session.id()).orElseThrow();var image=plan.chops();
                f.check(!image.getBooleanOr("applied",false)&&!plan.firstAck()&&!plan.secondAck(),"Neither registry nor owner acknowledgement was invented");
                f.check(registry.transferVerified(plan.first(),plan.second(),image.getCompoundOrEmpty("first_before"),image.getCompoundOrEmpty("second_before"),
                        image.getCompoundOrEmpty("first_after"),image.getCompoundOrEmpty("second_after")),"Exact registry pair saved before acknowledgement");
                accounts=f.reloadAccounts();registry=DurableRecoveryGameTestSupport.reload(server,ChopOwnershipData.class,"cosmicdungeon_chop_owners_v1");
                var saved=accounts.tradePlan(session.id()).orElseThrow();
                f.check(!saved.chops().getBooleanOr("applied",false)&&registry.entry(f.member.getUUID())==null
                        &&registry.entry(f.outsider.getUUID()).token().equals(token.toString()),"Disk account plan still unapplied while registry already contains exact after-images");
                f.check(TradeCustody.read(f.member).equals(saved.firstBefore())&&TradeCustody.read(f.outsider).equals(saved.secondBefore())
                        &&f.member.getInventory().countItem(item)==0&&f.outsider.getInventory().countItem(item)==0,"Both saved owner reservations precede any returned item");
                f.end(session);f.reload(f.member);f.reload(f.outsider);
                if(donorFirst)f.check(TradeTransactions.reconcile(f.member),"Donor-first Chop recovery saves its receipt before recipient return");
                f.check(TradeTransactions.reconcile(f.outsider),"Recipient recovery accepts already-moved registry without repeating its transition");
                accounts=f.reloadAccounts();registry=DurableRecoveryGameTestSupport.reload(server,ChopOwnershipData.class,"cosmicdungeon_chop_owners_v1");
                saved=savedTradeAccounts(server).tradePlan(session.id()).orElse(null);
                f.check(donorFirst?saved==null:saved!=null&&saved.chops().getBooleanOr("applied",false)&&saved.secondAck()&&!saved.firstAck(),
                        "Applied registry acknowledgement is durable before recipient can claim its queued item");
                TradeCustody.claim(f.outsider);
                var received=java.util.stream.IntStream.range(0,36).mapToObj(i->f.outsider.getInventory().getItem(i)).filter(s->s.is(item)).findFirst().orElseThrow();
                f.check(ChopOwnershipService.owned(f.outsider,received),"Recovered recipient owns the one physical Chop");
                // A settled recipient can legitimately consume its Raw Chop before the donor logs in.
                ChopOwnershipService.consumed(f.outsider,received);received.shrink(1);
                f.check(registry.flushVerified()&&PlayerSaveProof.save(f.outsider),"Recipient's later legitimate use has its own durable outcome");
                accounts=f.reloadAccounts();registry=DurableRecoveryGameTestSupport.reload(server,ChopOwnershipData.class,"cosmicdungeon_chop_owners_v1");f.reload(f.member);f.reload(f.outsider);
                f.check(TradeTransactions.reconcile(f.member)&&TradeTransactions.reconcile(f.outsider)
                        &&TradeTransactions.reconcile(f.member),"Late donor and repeated recovery use applied marker instead of resurrecting old registry state");
                f.check(registry.entry(f.member.getUUID())==null&&registry.entry(f.outsider.getUUID())==null
                        &&f.member.getInventory().countItem(item)==0&&f.outsider.getInventory().countItem(item)==0
                        &&accounts.tradePlan(session.id()).isEmpty(),"Both acknowledgements retire only this trade; consumed Chop is never recreated");
            } finally {
                TradeSessionData.handleLogout(f.member);TradeSessionData.handleLogout(f.outsider);
                registry.release(f.member.getUUID(),token);registry.release(f.outsider.getUUID(),token);
                f.check(registry.flushVerified(),"Replay fixture leaves no Chop registry entitlement");
            }
        }
    }
    private static void ordinaryTradeRecovery(GameTestHelper helper){
        for(boolean reverse:new boolean[]{false,true})try(var f=new Fixture(helper)){
            var session=f.open();f.offer(session,f.member,Items.APPLE,3);f.offer(session,f.outsider,Items.BREAD,2);
            f.member.containerMenu.setCarried(new ItemStack(Items.GOLD_INGOT,5));
            var root=f.member.getPersistentData().getCompoundOrEmpty(ClassData.ROOT_TAG).copy();
            root.putString("future_recovery_marker","preserved");
            root.put(TradeTransactions.RECEIPT,net.goui.cosmicdungeon.playerclass.dragoon.repair.RepairTransactions.receiptImage(
                    f.member.getUUID(),UUID.randomUUID(),net.goui.cosmicdungeon.economy.AccountTransfer.CANCELLED));
            f.member.getPersistentData().put(ClassData.ROOT_TAG,root);
            f.check(TradeTransactions.decide(f.member,f.outsider,session.id(),25,0),"Real ordinary trade decision commits before either item receipt");
            var server=f.level.getServer();var beforeOwnerAck=DurableRecoveryGameTestSupport.disk(server,PlayerCurrencyData.class,PlayerCurrencyData.SAVE_ID);
            f.end(session);var accounts=f.reloadAccounts();f.reload(f.member);f.reload(f.outsider);
            var first=reverse?f.outsider:f.member;
            f.check(TradeTransactions.reconcile(first),"Either native owner may settle the disk-loaded committed decision first");
            accounts=DurableRecoveryGameTestSupport.restoreCut(server,PlayerCurrencyData.class,PlayerCurrencyData.SAVE_ID,beforeOwnerAck);
            first=f.reload(first);
            f.check(TradeTransactions.reconcile(first),"Real owner receipt suppresses item projection when the world acknowledgement was interrupted");
            TradeCustody.claim(first);f.check(PlayerSaveProof.save(first),"Claimed owner result is natively saved before reverse recovery");
            accounts=f.reloadAccounts();f.reload(f.member);f.reload(f.outsider);
            for(var owner:reverse?List.of(f.member,f.outsider):List.of(f.outsider,f.member)){
                f.check(TradeTransactions.reconcile(owner),"Other owner and repeated native recovery settle once");
                TradeCustody.claim(owner);f.check(PlayerSaveProof.save(owner),"Native complete owner image saved");
            }
            accounts=f.reloadAccounts();f.reload(f.outsider);f.reload(f.member);
            f.check(TradeTransactions.reconcile(f.member)&&TradeTransactions.reconcile(f.outsider)&&accounts.tradePlan(session.id()).isEmpty(),
                    "Both receipts retire only this ordinary trade and survive repeated reverse reload");
            f.check(f.member.getInventory().countItem(Items.BREAD)==2&&f.outsider.getInventory().countItem(Items.APPLE)==3
                    &&f.member.getInventory().countItem(Items.GOLD_INGOT)==5&&f.outsider.getInventory().countItem(Items.GOLD_INGOT)==0
                    &&CurrencyService.getBalanceTrace(f.member)==75&&CurrencyService.getBalanceTrace(f.outsider)==125
                    &&f.member.getPersistentData().getCompoundOrEmpty(ClassData.ROOT_TAG).getStringOr("future_recovery_marker","").equals("preserved"),
                    "Item/cursor ownership, balanced payment, supported previous receipt and unrelated owner fields survive every cut exactly once");
        }
        for(boolean partial:new boolean[]{false,true})try(var f=new Fixture(helper)){
            var session=f.open();f.offer(session,f.member,Items.APPLE,3);f.offer(session,f.outsider,Items.BREAD,2);
            f.check(PlayerSaveProof.save(f.member)&&PlayerSaveProof.save(f.outsider),"Open custody and remaining inventories have native owner files");
            var accounts=PlayerCurrencyData.get(f.level.getServer());
            var terms=new net.goui.cosmicdungeon.economy.AccountTransfer.Terms(f.member.getUUID(),f.outsider.getUUID(),25,0,
                    "player_trade",TradeCustody.read(f.member).getLongOr("run",0),session.id().toString());
            accounts.reserve(session.id(),terms,System.currentTimeMillis());f.check(accounts.flushVerified(),"Account reservation precedes owner binding");
            if(partial){
                TradeCustody.write(f.member,TradeCustodyImages.bind(TradeCustody.read(f.member),session.id()));
                f.check(PlayerSaveProof.save(f.member),"Only the first owner transaction binding reached its native file");
            }
            f.end(session);accounts=f.reloadAccounts();f.reload(f.member);f.reload(f.outsider);
            for(var owner:List.of(f.outsider,f.member)){
                f.check(TradeTransactions.reconcile(owner),"Interrupted zero/partial trade preparation cancels through production recovery");
                TradeCustody.claim(owner);TradeCustody.claim(owner);
            }
            f.check(f.member.getInventory().countItem(Items.APPLE)==3&&f.outsider.getInventory().countItem(Items.BREAD)==2
                    &&CurrencyService.getBalanceTrace(f.member)==100&&CurrencyService.getBalanceTrace(f.outsider)==100,
                    "Uncommitted disk preparation returns original offers once without charging either owner");
            // With no owner binding, ordinary cancellation must still resolve the unrelated account reservation.
            if(accounts.transfer(session.id()).orElseThrow().reserved())accounts.cancelTransfer(session.id(),terms,System.currentTimeMillis(),"isolated fixture cancellation");
            f.check(accounts.flushVerified()&&CurrencyService.getAvailableTrace(f.member)==100&&CurrencyService.getAvailableTrace(f.outsider)==100,
                    "Startup-equivalent cancellation retires only isolated unfinished payment and restores all available balances");
        }
    }
    private static void malformedTradeProofs(GameTestHelper helper){
        for(String key:List.of(TradeCustody.KEY,TradeTransactions.RECEIPT))for(int shape=0;shape<(key.equals(TradeCustody.KEY)?4:3);shape++)try(var f=new Fixture(helper)){
            var session=f.open();f.offer(session,f.member,Items.APPLE,3);f.offer(session,f.outsider,Items.BREAD,2);
            var accounts=PlayerCurrencyData.get(f.level.getServer());
            var terms=new net.goui.cosmicdungeon.economy.AccountTransfer.Terms(f.member.getUUID(),f.outsider.getUUID(),25,0,
                    "player_trade",TradeCustody.read(f.member).getLongOr("run",0),session.id().toString());
            accounts.reserve(session.id(),terms,System.currentTimeMillis());f.check(accounts.flushVerified(),"Reserved account image precedes native owner bindings");
            for(var owner:List.of(f.member,f.outsider)){
                TradeCustody.write(owner,TradeCustodyImages.bind(TradeCustody.read(owner),session.id()));f.check(PlayerSaveProof.save(owner),"Paired native trade reservations saved");
            }
            accounts.prepareTrade(session.id(),new TradeCommitPlan(f.member.getUUID(),f.outsider.getUUID(),TradeCustody.read(f.member),TradeCustody.read(f.outsider),false,false));
            f.check(accounts.flushVerified(),"Reserved trade decision saved before malformed proof");f.end(session);
            try{
                var root=f.member.getPersistentData().getCompoundOrEmpty(ClassData.ROOT_TAG).copy();
                if(shape==0)root.putString(key,"preserve malformed trade proof");
                else if(shape==1)root.put(key,new ListTag());
                else{
                    var future=key.equals(TradeCustody.KEY)?TradeCustody.read(f.member):
                            net.goui.cosmicdungeon.playerclass.dragoon.repair.RepairTransactions.receiptImage(f.member.getUUID(),UUID.randomUUID(),net.goui.cosmicdungeon.economy.AccountTransfer.CANCELLED);
                    if(shape==2)future.putInt("schema",99);
                    else{
                        future.putString("session",UUID.randomUUID().toString());future.putString("transaction",UUID.randomUUID().toString());
                        TradeCustodyImages.validate(future,f.member.getUUID()); // Structurally valid, but foreign to the durable decision.
                    }
                    future.putString("unknown","preserve");root.put(key,future);
                }
                f.member.getPersistentData().put(ClassData.ROOT_TAG,root);f.check(PlayerSaveProof.save(f.member),"Unsupported trade proof retains native file");
                var disk=f.disk(f.member);var inventory=ChopTravelRecovery.saveInventory(f.member);accounts=f.reloadAccounts();
                var journal=DurableRecoveryGameTestSupport.image(PlayerCurrencyData.class,accounts);f.reload(f.member);f.reload(f.outsider);
                f.check(!TradeTransactions.reconcile(f.member)&&TradeCustody.held(f.member),"Malformed/future RESERVED owner proof holds before account cancellation");
                f.check(f.member.getPersistentData().getCompoundOrEmpty(ClassData.ROOT_TAG).equals(root)&&f.disk(f.member).equals(disk)
                        &&ChopTravelRecovery.saveInventory(f.member).equals(inventory)&&journal.equals(DurableRecoveryGameTestSupport.image(PlayerCurrencyData.class,accounts))
                        &&accounts.transfer(session.id()).orElseThrow().reserved(),"Held trade preserves original owner image and undecided account journal");
            }finally{
                accounts=PlayerCurrencyData.get(f.level.getServer());
                if(accounts.transfer(session.id()).orElseThrow().reserved())accounts.cancelTransfer(session.id(),terms,System.currentTimeMillis(),"isolated malformed fixture cleanup");
                if(accounts.tradePlan(session.id()).isPresent()){
                    accounts.acknowledgeTrade(session.id(),f.member.getUUID());accounts.acknowledgeTrade(session.id(),f.outsider.getUUID());
                }
                f.check(accounts.flushVerified(),"Only isolated held fixture account rows cleaned after preservation assertions");
            }
        }
    }
    private static PlayerCurrencyData savedTradeAccounts(net.minecraft.server.MinecraftServer server) {
        try {
            var read=PlayerCurrencyData.class.getDeclaredMethod("read",net.minecraft.server.MinecraftServer.class);
            read.setAccessible(true);return (PlayerCurrencyData)read.invoke(null,server);
        } catch(ReflectiveOperationException error) { throw new IllegalStateException(error); }
    }
    public static void cancelAndDisconnect(GameTestHelper helper) {
        for(boolean disconnect:new boolean[]{false,true})try(var f=new Fixture(helper)) {
            var session=f.open();
            var armor=new ItemStack(Items.IRON_CHESTPLATE);
            net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA,armor,
                    t->t.putString(net.goui.cosmicdungeon.item.identity.ClassItemOwnership.KEY,f.member.getUUID().toString()));
            f.member.getInventory().setItem(0,armor);
            ((TradeMenu)f.member.containerMenu).quickMoveStack(f.member,TradeMenu.HOTBAR_START);
            var potions=net.goui.cosmicdungeon.crafting.PotionStacks.crafted(net.minecraft.world.item.alchemy.PotionContents.createItemStack(
                    Items.SPLASH_POTION,net.minecraft.world.item.alchemy.Potions.HEALING));potions.setCount(31);
            f.member.getInventory().setItem(0,potions.copy());
            ((TradeMenu)f.member.containerMenu).quickMoveStack(f.member,TradeMenu.HOTBAR_START);
            session.setCurrency(f.member,5);session.setCurrency(f.outsider,10);
            if(disconnect)TradeSessionData.handleLogout(f.member);else session.cancel("requested");
            f.check(CurrencyService.getAvailableTrace(f.member)==100&&CurrencyService.getAvailableTrace(f.outsider)==100,
                    "Cancel/disconnect leaves both complete available balances");
            f.check(f.member.getInventory().countItem(Items.IRON_CHESTPLATE)==1&&f.outsider.getInventory().countItem(Items.IRON_CHESTPLATE)==0,
                    "Cancel/disconnect returns armor only to the original owner");
            session.cancel("duplicate");TradeCustody.claim(f.member);
            f.check(f.member.getInventory().countItem(Items.IRON_CHESTPLATE)==1,"Repeated recovery never duplicates");
            f.check(f.member.getInventory().countItem(Items.SPLASH_POTION)==31&&f.outsider.getInventory().countItem(Items.SPLASH_POTION)==0
                    &&java.util.stream.IntStream.range(0,36).mapToObj(i->f.member.getInventory().getItem(i))
                    .anyMatch(item->item.getCount()==31&&ItemStack.isSameItemSameComponents(potions,item)),
                    "Cancel/disconnect and repeated custody recovery return the exact crafted stack once");
        }
        helper.succeed();
    }
}
