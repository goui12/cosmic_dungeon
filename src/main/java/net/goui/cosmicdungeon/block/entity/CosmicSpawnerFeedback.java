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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.goui.cosmicdungeon.ammunition.AmmoChoices;
import net.goui.cosmicdungeon.auth.AccessPolicy;
import net.neoforged.bus.api.EventPriority;
import net.minecraft.util.TriState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Require 20 uninterrupted native breaking ticks; one title per hold, reset on release/retarget/tool change. */
@EventBusSubscriber(modid="cosmicdungeon")
public final class CosmicSpawnerFeedback {
    private record Hold(BlockPos pos,ResourceLocation dimension,int slot,ItemStack tool,int started,boolean warned){}
    private static final Map<ServerPlayer,Hold> HOLDS=new WeakHashMap<>();
    private record Authored(net.minecraft.world.InteractionHand hand,ItemStack stack,int tick){}
    private static final Map<ServerPlayer,Authored> AUTHORED=new WeakHashMap<>();
    public static void copied(ServerPlayer player,net.minecraft.world.InteractionHand hand,ItemStack stack){
        AUTHORED.put(player,new Authored(hand,stack.copy(),player.tickCount));
    }
    /** The client predicts sneak-bypass and may send a second item-use packet after the block click. */
    @SubscribeEvent public static void itemFallback(PlayerInteractEvent.RightClickItem event){
        if(!(event.getEntity() instanceof ServerPlayer player))return;
        var authored=AUTHORED.remove(player);
        if(authored==null||authored.hand!=event.getHand()||!player.isShiftKeyDown()
                ||!AmmoChoices.same(authored.stack,event.getItemStack()))return;
        long elapsed=(long)player.tickCount-authored.tick;
        if(elapsed<0||elapsed>4)return;
        event.setCancellationResult(net.minecraft.world.InteractionResult.CONSUME);
        event.setCanceled(true);
    }
    private CosmicSpawnerFeedback(){}
    @SubscribeEvent public static void action(PlayerInteractEvent.LeftClickBlock event){
        if(event.getEntity() instanceof ServerPlayer p)HOLDS.remove(p);
    }
    /** Sneaking normally bypasses blocks. Opt in only for the authorized authoring gesture. */
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void rightClick(PlayerInteractEvent.RightClickBlock event){
        if(!(event.getEntity() instanceof ServerPlayer p))return;
        AUTHORED.remove(p);
        if(event.getUseBlock()==TriState.FALSE||!p.isShiftKeyDown()||!AccessPolicy.isDeveloper(p)||event.getItemStack().isEmpty()
                ||!p.level().hasChunkAt(event.getPos())
                ||!(p.level().getBlockEntity(event.getPos()) instanceof CosmicSpawnerBlockEntity))return;
        event.setUseBlock(TriState.TRUE);
    }
    @SubscribeEvent public static void onTick(PlayerTickEvent.Post event){
        if(event.getEntity() instanceof ServerPlayer p)tick(p);
    }
    public static void tick(ServerPlayer p){
        var access=(SpawnerBreakAccess)p.gameMode;var pos=access.cosmicdungeon$breakPos();
        if(!access.cosmicdungeon$breaking()||!p.isAlive()||p.hasInfiniteMaterials()||p.isSpectator()||
                p.isUsingItem()||AmmoChoices.weapon(p.getMainHandItem())||p.getMainHandItem().is(ItemTags.PICKAXES)||!p.canInteractWithBlock(pos,0)||
                !p.level().hasChunkAt(pos)||!(p.level().getBlockEntity(pos) instanceof CosmicSpawnerBlockEntity)){
            HOLDS.remove(p);return;
        }
        var hold=HOLDS.get(p);
        var dimension=p.level().dimension().location();
        int slot=p.getInventory().getSelectedSlot();
        if(hold==null||!hold.pos.equals(pos)||!hold.dimension.equals(dimension)||hold.slot!=slot
                ||!AmmoChoices.same(hold.tool,p.getMainHandItem())||p.tickCount<hold.started){
            HOLDS.put(p,new Hold(pos.immutable(),dimension,slot,p.getMainHandItem().copy(),p.tickCount,false));return;
        }
        if(!hold.warned&&(long)p.tickCount-hold.started>=20){
            HOLDS.put(p,new Hold(hold.pos,hold.dimension,hold.slot,hold.tool,hold.started,true));
            p.connection.send(new ClientboundSetTitlesAnimationPacket(3,30,7));
            p.connection.send(new ClientboundSetTitleTextPacket(
                    Component.literal("You need a pickaxe to break that!").withStyle(ChatFormatting.YELLOW)));
        }
    }
}
