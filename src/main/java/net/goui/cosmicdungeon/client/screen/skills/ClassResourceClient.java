package net.goui.cosmicdungeon.client.screen.skills;

import java.util.Objects;
import java.util.function.Consumer;
import net.goui.cosmicdungeon.client.screen.requests.SupplyRequestsClient;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;

/** Snapshot and action bridge. Networking stays in the existing client-only network integration. */
@EventBusSubscriber(modid="cosmicdungeon",value=Dist.CLIENT)
public final class ClassResourceClient {
    private static final ClassResourceState STATE=new ClassResourceState();
    private static Consumer<ClassResourceSnapshot> sender;
    private static boolean installed;
    private ClassResourceClient(){}
    public static void receive(ClassResourceSnapshot snapshot){install();STATE.receive(Objects.requireNonNull(snapshot));}
    public static void recycleAction(Consumer<ClassResourceSnapshot> callback){sender=Objects.requireNonNull(callback);install();}
    public static void clear(){STATE.clear();TheurgistClient.clear();}
    private static void install(){
        if(installed)return;installed=true;
        for(String id:new String[]{"theurgist","bogatyr"})SkillsPanelClient.register(id,new SkillsPanelClient.Provider(){
            @Override public SkillsPanelModel view(LocalPlayer player){
                var base=ClassResourcePresentation.panel(id,STATE.snapshot(),STATE.awaitingAction()||sender==null,
                        SupplyRequestsClient.canRequest(STATE.snapshot()));
                return id.equals("theurgist")?TheurgistClient.augment(base):base;
            }
            @Override public void activate(LocalPlayer player,String action){
                if(!ClassData.getClassId(player).equals(id))return;
                if(action.equals("request_supplies")&&SupplyRequestsClient.canRequest(STATE.snapshot()))SupplyRequestsClient.request();
                else if(sender!=null&&action.equals("recycle"))STATE.recycle(id).ifPresent(sender);
                else if(id.equals("theurgist"))TheurgistClient.activate(action);
            }
        });
    }
    @SubscribeEvent public static void login(ClientPlayerNetworkEvent.LoggingIn event){clear();}
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event){clear();}
    @SubscribeEvent public static void hud(RenderGuiEvent.Post event){
        var mc=Minecraft.getInstance();var snapshot=STATE.snapshot();
        if(mc.player==null||mc.screen!=null||mc.options.hideGui||mc.getDebugOverlay().showDebugScreen()
                ||mc.player.isSpectator()||snapshot==null||!snapshot.active()||!snapshot.matches(ClassData.getClassId(mc.player)))return;
        var g=event.getGuiGraphics();g.nextStratum();
        var box=ClassResourceBar.world(g.guiWidth(),g.guiHeight(),Math.max(mc.gui.leftHeight,mc.gui.rightHeight));
        ClassResourceBar.draw(g,mc.font,box,snapshot);
    }
}
