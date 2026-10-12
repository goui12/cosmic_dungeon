package net.goui.cosmicdungeon.client.screen.skills;

import java.util.Objects;
import java.util.function.Consumer;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/** Class action bridge; legacy resource packets never restore retired controls or HUD bars. */
@EventBusSubscriber(modid="cosmicdungeon",value=Dist.CLIENT)
public final class ClassResourceClient {
    private static boolean installed;
    private ClassResourceClient(){}
    public static void receive(ClassResourceSnapshot snapshot){Objects.requireNonNull(snapshot);install();}
    public static void recycleAction(Consumer<ClassResourceSnapshot> callback){Objects.requireNonNull(callback);install();}
    public static void clear(){TheurgistClient.clear();BogatyrClient.clear();}
    private static void install(){
        if(installed)return;installed=true;
        for(String id:new String[]{"theurgist","bogatyr"})SkillsPanelClient.register(id,new SkillsPanelClient.Provider(){
            @Override public SkillsPanelModel view(LocalPlayer player){
                var base=SkillsPanelModel.initial(id);
                return id.equals("theurgist")?TheurgistClient.augment(base):BogatyrClient.augment(base);
            }
            @Override public void activate(LocalPlayer player,String action){
                if(!ClassData.getClassId(player).equals(id))return;
                if(id.equals("theurgist"))TheurgistClient.activate(action);
                else if(id.equals("bogatyr"))BogatyrClient.activate(action);
            }
        });
    }
    @SubscribeEvent public static void login(ClientPlayerNetworkEvent.LoggingIn event){clear();}
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event){clear();}
}
