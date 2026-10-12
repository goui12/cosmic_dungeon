package net.goui.cosmicdungeon.client;

import net.goui.cosmicdungeon.block.entity.CosmicSpawnerBlockEntity;
import net.goui.cosmicdungeon.network.SpawnerEditorPayloads.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** One private Developer response cached for the current target, at most two requests/second. */
public final class SpawnerHoverClient {
    private static ClientLevel level;private static BlockPos target;
    private static CosmicSpawnerBlockEntity view;private static int clock,next,received;
    private SpawnerHoverClient(){}
    private static BlockPos looking(Minecraft mc){
        return mc.hitResult instanceof BlockHitResult hit&&hit.getType()==HitResult.Type.BLOCK&&
                mc.level.getBlockEntity(hit.getBlockPos()) instanceof CosmicSpawnerBlockEntity?hit.getBlockPos():null;
    }
    public static void clear(){level=null;target=null;view=null;clock=next=received=0;}
    private static void resetSession(){
        clear();SpawnerLabelState.setEnabled(false);
        net.goui.cosmicdungeon.client.screen.spawner.SpawnerEditorClient.clear();
    }
    @SubscribeEvent public static void login(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingIn event){resetSession();}
    @SubscribeEvent public static void logout(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event){resetSession();}
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        var mc=Minecraft.getInstance();
        if(!SpawnerLabelState.isEnabled()||mc.player==null||mc.level==null){clear();return;}
        if(level!=mc.level){clear();level=mc.level;}
        clock++;
        var pos=mc.screen==null?looking(mc):null;
        if(!java.util.Objects.equals(pos,target)){target=pos;view=null;}
        if(target!=null&&clock>=next){
            next=clock+10;ClientPacketDistributor.sendToServer(new HoverRequest(target));
        }
    }
    public static void receive(HoverView message){
        var mc=Minecraft.getInstance();
        if(!SpawnerLabelState.isEnabled()||level!=mc.level||target==null||!target.equals(message.pos())||
                !target.equals(looking(mc)))return;
        var state=mc.level.getBlockState(target);
        var be=new CosmicSpawnerBlockEntity(target,state);
        // Detached instance: never attached/ticked, so detailed data cannot replace the public world block entity.
        be.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,mc.level.registryAccess(),message.details()));
        view=be;received=clock;
    }
    public static CosmicSpawnerBlockEntity view(BlockPos pos){
        return target!=null&&target.equals(pos)&&clock-received<=25?view:null;
    }
}
