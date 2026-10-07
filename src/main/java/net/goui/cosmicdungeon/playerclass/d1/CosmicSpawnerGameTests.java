package net.goui.cosmicdungeon.playerclass.d1;

import com.mojang.authlib.GameProfile;
import java.util.*;
import java.util.function.Consumer;
import net.goui.cosmicdungeon.block.ModBlocks;
import net.goui.cosmicdungeon.block.entity.CosmicSpawnerBlockEntity;
import net.goui.cosmicdungeon.dungeon.*;
import net.goui.cosmicdungeon.dungeon.d1.D1RunData;
import net.goui.cosmicdungeon.entity.ModEntities;
import net.goui.cosmicdungeon.mercenary.*;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.level.*;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;

/** Real shared detonation, native loot/removal and event vetoes in isolated synchronous CI fixtures. */
public final class CosmicSpawnerGameTests {
    private CosmicSpawnerGameTests() {}
    private static final class Fixture implements AutoCloseable {
        final GameTestHelper helper; final ServerLevel level; final Vec3 origin; final long id;
        final ServerPlayer player; final MercenaryEntity merc;
        final Map<Long,DungeonRunRegistryData.RunRecord> runs; final Map<UUID,ServerPlayer> players;
        final Map<BlockPos,BlockState> blocks = new LinkedHashMap<>();
        final Map<BlockPos,CompoundTag> entities = new HashMap<>();
        final Set<UUID> preexistingItems = new HashSet<>();
        final List<Packet<?>> packets = new ArrayList<>();
        final io.netty.channel.embedded.EmbeddedChannel channel = new io.netty.channel.embedded.EmbeddedChannel();
        @SuppressWarnings("unchecked") Fixture(GameTestHelper helper,long id) {
            this.helper=helper;this.id=id;level=helper.getLevel();var server=level.getServer();
            var anchor=helper.absoluteVec(new Vec3(1.5,14.5,1.5));
            // Keep all +/-6 block offsets and native drops in the already accessible helper chunk.
            // Block presence alone does not make a neighboring chunk's entity sections queryable.
            var chunk=new net.minecraft.world.level.ChunkPos(BlockPos.containing(anchor));
            origin=new Vec3(chunk.getMinBlockX()+8.5,anchor.y,chunk.getMinBlockZ()+8.5);
            level.getEntitiesOfClass(ItemEntity.class,new net.minecraft.world.phys.AABB(origin,origin).inflate(8))
                    .forEach(item->preexistingItems.add(item.getUUID()));
            var profile=new GameProfile(UUID.randomUUID(),"SpawnerTest");
            player=new ServerPlayer(server,level,profile,ClientInformation.createDefault());
            var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND) {
                @Override public io.netty.channel.Channel channel(){return channel;}
            };
            player.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(server,connection,player,
                    net.minecraft.server.network.CommonListenerCookie.createInitial(profile,false)) {
                @Override public void send(Packet<?> packet){packets.add(packet);}
                @Override public void send(Packet<?> packet,io.netty.channel.ChannelFutureListener listener){packets.add(packet);}
            };
            player.setGameMode(GameType.SURVIVAL);player.setHealth(20);player.setPos(origin);
            var tag=new CompoundTag();tag.putString(ClassData.KEY_CLASS_ID,"pyroclast");
            player.getPersistentData().put(ClassData.ROOT_TAG,tag);
            var contract=new MercenaryContract(UUID.randomUUID(),player.getUUID(),"pyroclast",2,50);
            var run=new DungeonRunRegistryData.RunRecord(id,"dungeon_1","minecraft:overworld",0,
                    List.of(level.dimension().location().toString()),1,"ACTIVE","",0,
                    List.of(player.getUUID()),List.of(),List.of()).withMercenaries(List.of(contract));
            try {
                var rf=DungeonRunRegistryData.class.getDeclaredField("runsById");rf.setAccessible(true);
                runs=(Map<Long,DungeonRunRegistryData.RunRecord>)rf.get(DungeonRunRegistryData.get(server));
                var pf=PlayerList.class.getDeclaredField("playersByUUID");pf.setAccessible(true);
                players=(Map<UUID,ServerPlayer>)pf.get(server.getPlayerList());
            } catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
            check(!runs.containsKey(id)&&!players.containsKey(player.getUUID()),"Isolated fixture IDs");
            runs.put(id,run);players.put(player.getUUID(),player);
            merc=new MercenaryEntity(ModEntities.MERCENARY.get(),level);merc.initialize(id,contract);
            merc.setPos(origin);merc.setNoAi(true);
        }
        BlockPos place(int x,int y,int z,BlockState state) {
            var pos=BlockPos.containing(origin).offset(x,y,z);
            if(!blocks.containsKey(pos)){
                blocks.put(pos,level.getBlockState(pos));
                var be=level.getBlockEntity(pos);if(be!=null)entities.put(pos,be.saveWithFullMetadata(level.registryAccess()));
            }
            level.setBlockAndUpdate(pos,state);return pos;
        }
        BlockPos spawner(int x,int y,int z) {
            var pos=place(x,y,z,ModBlocks.COSMIC_MOB_SPAWNER.get().defaultBlockState());
            ((CosmicSpawnerBlockEntity)level.getBlockEntity(pos)).setSpawnerSpawnRange(17);return pos;
        }
        FireworkRocketEntity rocket(LivingEntity owner) {
            return new FireworkRocketEntity(level,new ItemStack(net.goui.cosmicdungeon.item.ModItems.CINDERBITE.get()),
                    owner,origin.x,origin.y,origin.z,true);
        }
        void check(boolean value,String message){helper.assertTrue(value,Component.literal(message));}
        boolean remains(BlockPos pos){return level.getBlockState(pos).is(ModBlocks.COSMIC_MOB_SPAWNER.get());}
        List<ItemEntity> newDrops(){
            return level.getEntitiesOfClass(ItemEntity.class,new net.minecraft.world.phys.AABB(origin,origin).inflate(8))
                    .stream().filter(item->!preexistingItems.contains(item.getUUID())).toList();
        }
        @Override public void close(){
            newDrops().forEach(ItemEntity::discard);
            blocks.forEach(level::setBlockAndUpdate);
            entities.forEach((pos,tag)->{var be=level.getBlockEntity(pos);if(be!=null)be.loadWithComponents(
                    net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,level.registryAccess(),tag));});
            players.remove(player.getUUID());runs.remove(id);D1RunData.get(level.getServer()).clearRun(id);
            merc.discard();channel.finishAndReleaseAll();
        }
    }
    public static void player(GameTestHelper helper){destruction(helper,false);}
    public static void mercenary(GameTestHelper helper){destruction(helper,true);}
    private static void destruction(GameTestHelper helper,boolean hired){
        try(var f=new Fixture(helper,Long.MAX_VALUE-(hired?132:131))){
            var exposed=f.spawner(-2,0,0);var screened=f.spawner(-4,0,0);
            var boundary=f.spawner(0,0,5);var outside=f.spawner(0,0,-6);
            var shielded=f.spawner(3,0,0);var wall=f.place(2,0,0,Blocks.STONE.defaultBlockState());
            var protectedPos=f.spawner(0,3,0);var vetoed=f.spawner(0,-3,0);
            var ordinary=f.place(1,0,0,Blocks.STONE.defaultBlockState());
            var authored=((CosmicSpawnerBlockEntity)f.level.getBlockEntity(protectedPos)).saveWithoutMetadata(f.level.registryAccess());
            var shot=f.rocket(hired?f.merc:f.player);
            Consumer<BlockEvent.BreakEvent> protection=e->{if(e.getLevel()==f.level&&e.getPos().equals(protectedPos))e.setCanceled(true);};
            Consumer<ExplosionEvent.Detonate> filter=e->{if(e.getExplosion().getDirectSourceEntity()==shot){
                e.getAffectedBlocks().remove(vetoed);e.getAffectedBlocks().add(ordinary);e.getAffectedBlocks().add(exposed);
            }};
            NeoForge.EVENT_BUS.addListener(protection);NeoForge.EVENT_BUS.addListener(filter);
            try {
                f.check(D1RocketAbilities.explode(shot,f.level),"Admitted Pyroclast detonation is handled");
                f.check(!f.remains(exposed)&&!f.remains(boundary),"Exposed spawners through inclusive five-block boundary break");
                for(var pos:List.of(screened,outside,shielded,protectedPos,vetoed))
                    f.check(f.remains(pos),"Walls, radius and protection vetoes preserve spawners");
                f.check(f.level.getBlockState(wall).is(Blocks.STONE)&&f.level.getBlockState(ordinary).is(Blocks.STONE),
                        "Even event-added ordinary terrain remains intact");
                f.check(authored.equals(((CosmicSpawnerBlockEntity)f.level.getBlockEntity(protectedPos)).saveWithoutMetadata(f.level.registryAccess())),
                        "Protected authored block-entity data remains byte-equivalent");
                int bottles=f.newDrops().stream().filter(i->i.getItem().is(Items.EXPERIENCE_BOTTLE))
                        .mapToInt(i->i.getItem().getCount()).sum();
                f.check(bottles>=6&&bottles<=10,"Two spawners use existing three-to-five XP bottle loot; observed "+bottles+" new bottles");
                f.spawner(-2,0,0);D1RocketAbilities.explode(shot,f.level);
                f.check(f.remains(exposed),"Repeated callback cannot destroy more blocks or duplicate drops");
                var canceled=f.rocket(hired?f.merc:f.player);
                Consumer<ExplosionEvent.Start> start=e->{if(e.getExplosion().getDirectSourceEntity()==canceled)e.setCanceled(true);};
                NeoForge.EVENT_BUS.addListener(start);
                try{D1RocketAbilities.explode(canceled,f.level);f.check(f.remains(exposed),"Explosion start veto is honored");}
                finally{NeoForge.EVENT_BUS.unregister(start);}
                var stale=f.rocket(hired?f.merc:f.player);D1RocketAbilities.capture(stale);f.runs.remove(f.id);
                D1RocketAbilities.explode(stale,f.level);f.check(f.remains(exposed),"Ended run cannot destroy authored spawners");
                helper.succeed();
            }finally{NeoForge.EVENT_BUS.unregister(protection);NeoForge.EVENT_BUS.unregister(filter);}
        }
    }
    public static void warning(GameTestHelper helper){
        try(var f=new Fixture(helper,Long.MAX_VALUE-133)){
            var pos=f.spawner(0,0,0);var state=f.level.getBlockState(pos);
            f.packets.clear();f.player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            state.attack(f.level,pos,f.player);state.attack(f.level,pos,f.player);
            f.check(f.packets.stream().filter(p->p instanceof ClientboundSetTitleTextPacket).count()==1,
                    "Repeated empty-hand attempts produce one throttled title");
            var title=(ClientboundSetTitleTextPacket)f.packets.stream().filter(p->p instanceof ClientboundSetTitleTextPacket).findFirst().orElseThrow();
            f.check(title.text().getString().equals("You need a pickaxe to break that!"),"Exact requested warning");
            f.player.tickCount+=40;f.player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.WOODEN_PICKAXE));
            state.attack(f.level,pos,f.player);
            f.check(f.packets.size()==2,"Any tagged pickaxe needs no warning");
            f.player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.STICK));
            state.attack(f.level,pos,f.player);f.check(f.packets.size()==4,"Wrong tool warns again after throttle expires");
            f.check(f.remains(pos),"Feedback does not alter the authored spawner");
            helper.succeed();
        }
    }
}
