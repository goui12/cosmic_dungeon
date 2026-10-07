package net.goui.cosmicdungeon.client.screen.skills;

import java.io.IOException;
import java.util.*;
import java.util.function.BiConsumer;
import net.goui.cosmicdungeon.playerclass.api.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.*;

/** Client-only adapter. The class ID comes from the existing server class sync; no gameplay packets are invented. */
@EventBusSubscriber(modid="cosmicdungeon",value=Dist.CLIENT)
public final class SkillsPanelClient {
    public interface Provider {
        SkillsPanelModel view(LocalPlayer player);
        /** Later providers must route gameplay actions through their existing server-authorized service. */
        default void activate(LocalPlayer player,String action){}
    }
    private static final Map<String,Provider> PROVIDERS=new HashMap<>();
    private static final Map<String,SkillsPanelModel> INITIAL=new HashMap<>();
    private static BiConsumer<Screen,String> help=(screen,id)->{};
    private static SkillsPanelPreferences preferences;
    private static Screen screen;private static String classId;private static SkillsPanelState state;
    private static SkillsPanelModel model;private static boolean warned;
    private SkillsPanelClient(){}
    public static void classHelp(BiConsumer<Screen,String> opener){help=Objects.requireNonNull(opener);}
    public static void register(String id,Provider provider){
        if(!ClassKeys.isKnown(id)||ClassKeys.CLASS_ID_NONE.equals(id))throw new IllegalArgumentException("Unknown class");
        PROVIDERS.put(id,Objects.requireNonNull(provider));
    }
    public static boolean inventory(Screen candidate){return candidate instanceof InventoryScreen||candidate instanceof CreativeModeInventoryScreen;}
    public static SharedInventoryLayout layout(Screen candidate){
        var inventory=(AbstractContainerScreen<?>)candidate;
        return SharedInventoryLayout.of(candidate.width,candidate.height,inventory.getGuiLeft(),inventory.getGuiTop(),
                inventory.getXSize(),inventory.getYSize());
    }
    private static SkillsPanelPreferences preferences(){
        if(preferences==null)preferences=new SkillsPanelPreferences(FMLPaths.CONFIGDIR.get().resolve("cosmicdungeon-skills-layout.json"));
        return preferences;
    }
    private static boolean refresh(Screen candidate){
        var mc=Minecraft.getInstance();
        if(!inventory(candidate)||mc.player==null)return false;
        String id=ClassKeys.clamp(ClassData.getClassId(mc.player));
        if(screen!=candidate||!id.equals(classId)){
            persist();if(state!=null)state.release();
            screen=candidate;classId=id;state=new SkillsPanelState(layout(candidate),preferences().get(id).orElse(null));
        }else state.resize(layout(candidate));
        var provider=PROVIDERS.get(id);
        model=provider==null?INITIAL.computeIfAbsent(id,SkillsPanelModel::initial):Objects.requireNonNull(provider.view(mc.player));
        if(!model.classId().equals(id))throw new IllegalStateException("Skills provider changed class identity");
        state.actions(model.actions().size());return true;
    }
    private static boolean carrying(){
        var player=Minecraft.getInstance().player;
        return player==null||!player.containerMenu.getCarried().isEmpty();
    }
    public static boolean ownsInput(Screen candidate,double x,double y){
        return candidate==screen&&state!=null&&!carrying()&&(state.captured()||state.geometry().panel().contains(x,y));
    }
    private static void warn(Exception error){
        if(!warned){com.mojang.logging.LogUtils.getLogger().warn("Could not save Skills panel layout; original file retained",error);warned=true;}
    }
    private static void persist(){
        if(state==null||!state.dirty())return;
        try{
            if(!preferences().writable())throw new IOException("Layout is unreadable or uses a newer format");
            preferences().put(classId,state.placement());
        }catch(IOException error){warn(error);}
        state.saved();
    }
    /** Settings action resets all known class placements; no player/world data is touched. */
    public static void resetLayout(){
        try{preferences().reset();}catch(IOException error){warn(error);}
        if(state!=null){state.release();state=new SkillsPanelState(layout(screen),null);}
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void init(ScreenEvent.Init.Post event){refresh(event.getScreen());}
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void tooltip(ScreenEvent.Render.Pre event){
        if(refresh(event.getScreen())&&!carrying())
            SkillsPanelComponent.tooltip(event.getGuiGraphics(),Minecraft.getInstance().font,model,state,event.getMouseX(),event.getMouseY());
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void render(ScreenEvent.Render.Post event){
        if(!refresh(event.getScreen()))return;
        SkillsPanelComponent.draw(event.getGuiGraphics(),Minecraft.getInstance().font,
                model,state,carrying()?-1:event.getMouseX(),carrying()?-1:event.getMouseY());
        // Native carried/touchscreen item rendering stays above the movable overlay.
        ((AbstractContainerScreen<?>)event.getScreen()).renderCarriedItem(event.getGuiGraphics(),event.getMouseX(),event.getMouseY());
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void press(ScreenEvent.MouseButtonPressed.Pre event){
        if(!refresh(event.getScreen()))return;
        var result=state.press(event.getMouseX(),event.getMouseY(),event.getMouseButtonEvent().button(),carrying());
        if(!result.consumed())return;
        event.setCanceled(true);
        if(result.changed())persist();
        if(result.help()){help.accept(screen,classId);return;}
        if(result.action()>=0){
            var action=model.actions().get(result.action());
            if(!action.enabled())return;
            if(action.id().equals("guide"))help.accept(screen,classId);
            else{
                var provider=PROVIDERS.get(classId);var player=Minecraft.getInstance().player;
                if(provider!=null&&player!=null)provider.activate(player,action.id());
            }
        }
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void drag(ScreenEvent.MouseDragged.Pre event){
        if(screen==event.getScreen()&&state!=null&&state.drag(event.getMouseX(),event.getMouseY(),event.getMouseButton(),carrying()))
            event.setCanceled(true);
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void release(ScreenEvent.MouseButtonReleased.Pre event){
        if(screen==event.getScreen()&&state!=null){
            boolean owned=state.release(event.getButton());persist();if(owned&&!carrying())event.setCanceled(true);
        }
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void scroll(ScreenEvent.MouseScrolled.Pre event){
        if(refresh(event.getScreen())&&state.wheel(event.getMouseX(),event.getMouseY(),event.getScrollDeltaY(),carrying()))
            event.setCanceled(true);
    }
    @SubscribeEvent public static void close(ScreenEvent.Closing event){
        if(screen==event.getScreen()){persist();if(state!=null)state.release();screen=null;classId=null;state=null;model=null;}
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event){
        persist();if(state!=null)state.release();screen=null;classId=null;state=null;model=null;
    }
}
