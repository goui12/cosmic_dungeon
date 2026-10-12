package net.goui.cosmicdungeon.block.entity;

import java.util.Map;
import java.util.WeakHashMap;
import net.goui.cosmicdungeon.mixin.SpawnerBreakAccess;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Require 20 uninterrupted native breaking ticks; one title per hold, reset on release/retarget/tool change. */
@EventBusSubscriber(modid="cosmicdungeon")
public final class CosmicSpawnerFeedback {
    private record Hold(BlockPos pos,int started,boolean warned){}
    private static final Map<ServerPlayer,Hold> HOLDS=new WeakHashMap<>();
    private CosmicSpawnerFeedback(){}
    @SubscribeEvent public static void action(PlayerInteractEvent.LeftClickBlock event){
        if(event.getEntity() instanceof ServerPlayer p)HOLDS.remove(p);
    }
    @SubscribeEvent public static void onTick(PlayerTickEvent.Post event){
        if(event.getEntity() instanceof ServerPlayer p)tick(p);
    }
    public static void tick(ServerPlayer p){
        var access=(SpawnerBreakAccess)p.gameMode;var pos=access.cosmicdungeon$breakPos();
        if(!access.cosmicdungeon$breaking()||!p.isAlive()||p.hasInfiniteMaterials()||p.isSpectator()||
                p.getMainHandItem().is(ItemTags.PICKAXES)||!p.canInteractWithBlock(pos,0)||
                !p.level().hasChunkAt(pos)||!(p.level().getBlockEntity(pos) instanceof CosmicSpawnerBlockEntity)){
            HOLDS.remove(p);return;
        }
        var hold=HOLDS.get(p);
        if(hold==null||!hold.pos.equals(pos)||p.tickCount<hold.started){
            HOLDS.put(p,new Hold(pos.immutable(),p.tickCount,false));return;
        }
        if(!hold.warned&&(long)p.tickCount-hold.started>=20){
            HOLDS.put(p,new Hold(hold.pos,hold.started,true));
            p.connection.send(new ClientboundSetTitlesAnimationPacket(3,30,7));
            p.connection.send(new ClientboundSetTitleTextPacket(
                    Component.literal("You need a pickaxe to break that!").withStyle(ChatFormatting.YELLOW)));
        }
    }
}
