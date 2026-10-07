package net.goui.cosmicdungeon.client.screen.requests;

import java.util.Objects;
import java.util.function.Consumer;
import net.goui.cosmicdungeon.client.screen.skills.ClassResourceSnapshot;
import net.goui.cosmicdungeon.client.screen.skills.SkillsPanelClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

/** Client-only snapshot/action bridge; SkillsPanelClient owns the single coordinated inventory event route. */
public final class SupplyRequestsClient {
    private static final SupplyRequestsState STATE=new SupplyRequestsState();
    private static Consumer<SupplyRequestAction> sender;
    private static Screen screen;
    private SupplyRequestsClient(){}
    public static void receive(SupplyRequestsSnapshot snapshot){STATE.receive(Objects.requireNonNull(snapshot));}
    public static void actions(Consumer<SupplyRequestAction> callback){sender=Objects.requireNonNull(callback);STATE.connected(true);}
    public static void clear(){STATE.clear();screen=null;}
    public static boolean canRequest(){return STATE.canRequest();}
    public static boolean canRequest(ClassResourceSnapshot resource){
        return resource!=null&&resource.active()&&resource.alive()&&resource.amount()<resource.cap()
                &&resource.runId()==STATE.snapshot().runId()&&canRequest();
    }
    public static void request(){if(sender!=null)STATE.decide(SupplyRequestAction.Decision.REQUEST,null).ifPresent(sender);}
    public static void init(Screen candidate){refresh(candidate);}
    private static boolean refresh(Screen candidate){
        if(!SkillsPanelClient.inventory(candidate)||Minecraft.getInstance().player==null)return false;
        if(screen!=candidate){STATE.release();STATE.scroll().reset();screen=candidate;}
        STATE.resize(SkillsPanelClient.layout(candidate).requests());return true;
    }
    public static boolean ownsInput(Screen candidate,double x,double y,boolean carrying){
        return candidate==screen&&STATE.owns(x,y,carrying);
    }
    public static boolean captured(){return STATE.captured();}
    public static boolean press(Screen candidate,double x,double y,int button,boolean carrying){
        if(!refresh(candidate))return false;
        var result=STATE.press(x,y,button,carrying);
        if(result.action()!=null&&sender!=null)sender.accept(result.action());
        return result.consumed();
    }
    public static boolean drag(Screen candidate,double y,int button,boolean carrying){
        return screen==candidate&&STATE.drag(y,button,carrying);
    }
    public static boolean release(Screen candidate,int button){return screen==candidate&&STATE.release(button);}
    public static boolean wheel(Screen candidate,double x,double y,double delta,boolean carrying){
        return refresh(candidate)&&STATE.wheel(x,y,delta,carrying);
    }
    public static void tooltip(Screen candidate,GuiGraphics g,int x,int y,boolean carrying){
        if(refresh(candidate)&&!carrying)SupplyRequestsComponent.tooltip(g,Minecraft.getInstance().font,STATE,x,y);
    }
    public static void draw(Screen candidate,GuiGraphics g,int x,int y,boolean carrying){
        if(refresh(candidate))SupplyRequestsComponent.draw(g,Minecraft.getInstance().font,STATE,carrying?-1:x,carrying?-1:y);
    }
    public static void close(Screen candidate){if(screen==candidate){STATE.release();screen=null;}}
}
