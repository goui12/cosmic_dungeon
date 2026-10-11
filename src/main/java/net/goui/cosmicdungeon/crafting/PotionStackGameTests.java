package net.goui.cosmicdungeon.crafting;

import com.mojang.authlib.GameProfile;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.goui.cosmicdungeon.transaction.PlayerSaveProof;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.*;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.ThrownSplashPotion;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;

/** Native menus, use, remainder, player saves and physical pickup retain bounded potion stacks. */
public final class PotionStackGameTests {
    private PotionStackGameTests() {}
    private static ItemStack splash(int count) {
        return PotionStacks.crafted(PotionContents.createItemStack(Items.SPLASH_POTION,Potions.HEALING)).copyWithCount(count);
    }
    private static ItemStack drink(int count) {
        return PotionStacks.crafted(PotionContents.createItemStack(Items.POTION,Potions.TURTLE_MASTER)).copyWithCount(count);
    }
    private static final class Fixture implements AutoCloseable {
        final GameTestHelper h;final ServerLevel level;final Vec3 origin;final GameProfile profile;
        final List<ServerPlayer> owned=new ArrayList<>();final List<io.netty.channel.embedded.EmbeddedChannel> channels=new ArrayList<>();
        final Set<UUID> initial=new HashSet<>();final List<Path> files;ServerPlayer p;
        Fixture(GameTestHelper h) {
            this.h=h;level=h.getLevel();origin=h.absoluteVec(new Vec3(.5,10.5,.5));
            profile=new GameProfile(UUID.randomUUID(),"PotionStackTest");var server=level.getServer();String id=profile.id().toString();
            files=List.of(server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(id+".dat"),
                    server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(id+".dat_old"),
                    server.getWorldPath(LevelResource.PLAYER_STATS_DIR).resolve(id+".json"),
                    server.getWorldPath(LevelResource.PLAYER_ADVANCEMENTS_DIR).resolve(id+".json"));
            check(files.stream().noneMatch(Files::exists),"Fixture player files must be new");
            p=create();for(var entity:nearby())initial.add(entity.getUUID());
        }
        ServerPlayer create() {
            var server=level.getServer();var channel=new io.netty.channel.embedded.EmbeddedChannel();channels.add(channel);
            var player=new ServerPlayer(server,level,profile,ClientInformation.createDefault());
            var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND) {
                @Override public io.netty.channel.Channel channel(){return channel;}
                @Override public boolean isConnected(){return true;}
            };
            net.neoforged.neoforge.network.registration.ChannelAttributes.setConnectionType(connection,
                    net.neoforged.neoforge.network.connection.ConnectionType.NEOFORGE);
            net.neoforged.neoforge.network.registration.ChannelAttributes.setPayloadSetup(connection,
                    net.neoforged.neoforge.network.registration.NetworkPayloadSetup.empty());
            player.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(server,connection,player,
                    net.minecraft.server.network.CommonListenerCookie.createInitial(profile,false)) {
                @Override public void send(Packet<?> packet){}
                @Override public void send(Packet<?> packet,io.netty.channel.ChannelFutureListener listener){}
                @Override public void resetPosition(){}
                @Override public void teleport(double x,double y,double z,float yaw,float pitch){}
            };
            var root=new CompoundTag();root.putString(ClassData.KEY_CLASS_ID,"theurgist");root.putString("future_stack_fixture","kept");
            player.getPersistentData().put(ClassData.ROOT_TAG,root);
            player.setGameMode(GameType.SURVIVAL);player.setHealth(20);player.setPos(origin);owned.add(player);
            return player;
        }
        List<Entity> nearby() {
            return level.getEntitiesOfClass(Entity.class,new net.minecraft.world.phys.AABB(origin,origin).inflate(8),
                    e->e instanceof ItemEntity||e instanceof ThrownSplashPotion);
        }
        List<ItemEntity> drops() {return nearby().stream().filter(e->e instanceof ItemEntity&&!initial.contains(e.getUUID()))
                .map(e->(ItemEntity)e).toList();}
        List<ThrownSplashPotion> projectiles() {return nearby().stream().filter(e->e instanceof ThrownSplashPotion potion&&potion.getOwner()==p)
                .map(e->(ThrownSplashPotion)e).toList();}
        void clear() {p.inventoryMenu.setCarried(ItemStack.EMPTY);p.getInventory().clearContent();}
        int count(Item item) {int count=0;for(int slot=0;slot<p.getInventory().getContainerSize();slot++)
            if(p.getInventory().getItem(slot).is(item))count+=p.getInventory().getItem(slot).getCount();return count;}
        void check(boolean condition,String message){h.assertTrue(condition,Component.literal(message));}
        void reload() {
            check(PlayerSaveProof.save(p),"Native inventory and component save succeeds");
            try {
                var image=NbtIo.readCompressed(files.getFirst(),NbtAccounter.create(64L*1024*1024));
                p=create();p.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),image));p.setPos(origin);
                check(PlayerSaveProof.matches(PlayerSaveProof.snapshot(p),image),"Native reload preserves exact inventory image");
            }catch(IOException error){throw new IllegalStateException(error);}
        }
        @Override public void close() {
            for(var entity:nearby())if(!initial.contains(entity.getUUID()))entity.discard();
            for(var player:owned)player.discard();
            try {
                for(String name:List.of("stats","advancements")) {
                    var field=PlayerList.class.getDeclaredField(name);field.setAccessible(true);
                    var map=(Map<?,?>)field.get(level.getServer().getPlayerList());
                    if(map.get(profile.id()) instanceof net.minecraft.server.PlayerAdvancements progress)progress.stopListening();
                    map.remove(profile.id());
                }
                for(var file:files)Files.deleteIfExists(file);
            }catch(ReflectiveOperationException|IOException error){throw new IllegalStateException("Potion stack fixture cleanup",error);}
            channels.forEach(io.netty.channel.embedded.EmbeddedChannel::finishAndReleaseAll);
        }
    }

    public static void menu(GameTestHelper h) {
        try(var f=new Fixture(h)) {
            var p=f.p;var inventory=p.getInventory();var menu=p.inventoryMenu;f.clear();
            inventory.setItem(0,splash(8));menu.clicked(36,1,ClickType.PICKUP,p);
            f.check(inventory.getItem(0).getCount()==4&&menu.getCarried().getCount()==4,"Right-click splits eight into two stacks of four");
            menu.clicked(37,0,ClickType.PICKUP,p);
            f.check(inventory.getItem(1).getCount()==4&&menu.getCarried().isEmpty(),"Left-click places all four with native components");
            menu.setCarried(splash(9));menu.clicked(-999,AbstractContainerMenu.getQuickcraftMask(0,0),ClickType.QUICK_CRAFT,p);
            for(int slot=38;slot<=40;slot++)menu.clicked(slot,AbstractContainerMenu.getQuickcraftMask(1,0),ClickType.QUICK_CRAFT,p);
            menu.clicked(-999,AbstractContainerMenu.getQuickcraftMask(2,0),ClickType.QUICK_CRAFT,p);
            f.check(menu.getCarried().isEmpty()&&inventory.getItem(2).getCount()==3&&inventory.getItem(3).getCount()==3
                    &&inventory.getItem(4).getCount()==3,"Native drag divides nine across three slots");
            menu.setCarried(splash(60));menu.clicked(37,0,ClickType.PICKUP,p);
            f.check(inventory.getItem(1).getCount()==64&&menu.getCarried().isEmpty(),"Native merge reaches the bounded sixty-four limit");
            menu.setCarried(splash(2));menu.clicked(37,0,ClickType.PICKUP,p);
            f.check(inventory.getItem(1).getCount()==64&&menu.getCarried().getCount()==2,"Full stack cannot absorb or discard overflow");
            menu.setCarried(ItemStack.EMPTY);menu.clicked(38,40,ClickType.SWAP,p);
            f.check(inventory.getItem(2).isEmpty()&&p.getOffhandItem().getCount()==3,"Offhand swap preserves stack");
            menu.clicked(39,7,ClickType.SWAP,p);
            f.check(inventory.getItem(3).isEmpty()&&inventory.getItem(7).getCount()==3,"Number-key hotbar swap preserves stack");

            f.clear();inventory.setItem(9,splash(12));menu.clicked(9,0,ClickType.QUICK_MOVE,p);
            f.check(inventory.getItem(9).isEmpty()&&f.count(Items.SPLASH_POTION)==12
                    &&java.util.stream.IntStream.range(0,9).anyMatch(i->inventory.getItem(i).getCount()==12),
                    "Native shift move transfers a stack to hotbar without normalizing to one");
            f.clear();menu.setCarried(splash(11));menu.removed(p);
            f.check(menu.getCarried().isEmpty()&&f.count(Items.SPLASH_POTION)==11,"Normal menu close returns carried stack once");
            for(int slot=0;slot<inventory.getContainerSize();slot++)if(inventory.getItem(slot).is(Items.SPLASH_POTION))
                f.check(inventory.getItem(slot).getMaxStackSize()==64,"Menu paths retain the native component");
        }
        h.succeed();
    }

    public static void consumption(GameTestHelper h) {
        try(var f=new Fixture(h)) {
            var p=f.p;f.clear();p.setItemInHand(InteractionHand.MAIN_HAND,drink(3));
            p.setItemInHand(InteractionHand.MAIN_HAND,p.getMainHandItem().finishUsingItem(f.level,p));
            f.check(p.getMainHandItem().getCount()==2&&p.getMainHandItem().getMaxStackSize()==64&&f.count(Items.GLASS_BOTTLE)==1,
                    "One drink consumes one potion and returns one empty bottle while preserving two");
            p.setItemInHand(InteractionHand.MAIN_HAND,p.getMainHandItem().finishUsingItem(f.level,p));
            f.check(p.getMainHandItem().getCount()==1&&f.count(Items.GLASS_BOTTLE)==2,"Second drink creates one additional bottle");
            p.setItemInHand(InteractionHand.MAIN_HAND,p.getMainHandItem().finishUsingItem(f.level,p));
            f.check(p.getMainHandItem().is(Items.GLASS_BOTTLE)&&f.count(Items.GLASS_BOTTLE)==3&&f.count(Items.POTION)==0,
                    "Last drink replaces the held item with exactly one bottle");

            f.clear();for(int i=0;i<36;i++)p.getInventory().setItem(i,new ItemStack(Items.DIAMOND,64));
            p.setItemInHand(InteractionHand.OFF_HAND,drink(2));int drops=f.drops().size();
            p.setItemInHand(InteractionHand.OFF_HAND,p.getOffhandItem().finishUsingItem(f.level,p));
            f.check(p.getOffhandItem().getCount()==1&&p.getOffhandItem().is(Items.POTION)&&f.count(Items.GLASS_BOTTLE)==0,
                    "Full inventory offhand drink preserves the remaining potion");
            var bottles=f.drops().stream().filter(e->e.getItem().is(Items.GLASS_BOTTLE)).toList();
            f.check(f.drops().size()==drops+1&&bottles.size()==1&&bottles.getFirst().getItem().getCount()==1,
                    "Full inventory drops exactly one physical remainder bottle");

            f.clear();p.setItemInHand(InteractionHand.MAIN_HAND,splash(3));
            p.getMainHandItem().use(f.level,p,InteractionHand.MAIN_HAND);
            f.check(p.getMainHandItem().getCount()==2&&f.projectiles().size()==1,"Main-hand throw consumes one and spawns one projectile");
            p.setItemInHand(InteractionHand.OFF_HAND,splash(2));p.getOffhandItem().use(f.level,p,InteractionHand.OFF_HAND);
            f.check(p.getOffhandItem().getCount()==1&&f.projectiles().size()==2,"Offhand throw consumes one and spawns one projectile");
            for(var projectile:f.projectiles())f.check(projectile.getItem().getCount()==1
                    &&ItemStack.isSameItemSameComponents(projectile.getItem(),splash(1)),
                    "Each projectile carries only one identical potion unit");
            f.check(f.count(Items.GLASS_BOTTLE)==0,"Splash throws never mint glass bottles");
        }
        h.succeed();
    }

    public static void custody(GameTestHelper h) {
        try(var f=new Fixture(h)) {
            f.clear();var marked=splash(64);marked.set(DataComponents.CUSTOM_NAME,Component.literal("Saved potion stack"));
            CustomData.update(DataComponents.CUSTOM_DATA,marked,t->t.putString("future_component","kept"));
            f.p.getInventory().setItem(0,marked);f.p.setItemInHand(InteractionHand.OFF_HAND,drink(12));
            var before=marked.copy();f.reload();
            f.check(ItemStack.matches(before,f.p.getInventory().getItem(0))&&f.p.getOffhandItem().getCount()==12
                    &&f.p.getOffhandItem().getMaxStackSize()==64,"Player file reload retains potion effects, count, cap, name and unknown data");
            f.check(f.p.getPersistentData().getCompoundOrEmpty(ClassData.ROOT_TAG).getStringOr("future_stack_fixture","").equals("kept"),
                    "Unrelated future saved data survives");

            f.clear();for(int i=0;i<36;i++)f.p.getInventory().setItem(i,new ItemStack(Items.DIAMOND,64));
            f.p.getInventory().setItem(0,splash(63));f.p.setItemInHand(InteractionHand.OFF_HAND,drink(2));
            var dropped=f.p.drop(splash(4),false);f.check(dropped!=null&&dropped.getItem().getCount()==4,"Manual drop keeps all four units");
            dropped.setNoPickUpDelay();dropped.setTarget(null);dropped.playerTouch(f.p);
            f.check(f.p.getInventory().getItem(0).getCount()==64&&!dropped.isRemoved()&&dropped.getItem().getCount()==3,
                    "Nearly full inventory picks up exactly one and leaves three physical units");
            dropped.playerTouch(f.p);
            f.check(dropped.getItem().getCount()==3&&f.p.getInventory().getItem(0).getCount()==64,"Repeated full-inventory pickup cannot duplicate or discard units");
            f.p.getInventory().setItem(1,ItemStack.EMPTY);dropped.playerTouch(f.p);
            f.check(dropped.isRemoved()&&f.p.getInventory().getItem(1).getCount()==3
                    &&ItemStack.isSameItemSameComponents(f.p.getInventory().getItem(1),splash(1)),
                    "Later room collects the original three units with all components");
            f.reload();f.check(f.p.getInventory().getItem(0).getCount()==64&&f.p.getInventory().getItem(1).getCount()==3,
                    "Physical pickup result survives another native save and reload");
        }
        h.succeed();
    }
}
