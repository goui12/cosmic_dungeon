package net.goui.cosmicdungeon.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.goui.cosmicdungeon.ammunition.AmmoChoices;
import net.goui.cosmicdungeon.network.AmmoPayloads;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.minecraft.client.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.settings.*;
import net.neoforged.neoforge.event.entity.living.LivingGetProjectileEvent;
import org.lwjgl.glfw.GLFW;
import static net.goui.cosmicdungeon.client.SkillInputPolicy.*;

/** Physical presses only; no queued GUI input, render inventory scans or client stock authority. */
@EventBusSubscriber(modid="cosmicdungeon",value=Dist.CLIENT)
public final class AmmoKeybindClient {
    private static KeyMapping key;
    private static final Presses PRESSES=new Presses();
    private static AmmoPayloads.View view;
    private static Object owner;
    private AmmoKeybindClient(){}
    public static void registerKeyMappings(RegisterKeyMappingsEvent event){
        key=new KeyMapping("key.cosmicdungeon.cycle_ammunition",KeyConflictContext.IN_GAME,KeyModifier.NONE,
                InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_R,CosmicDungeonKeybindCategories.COSMIC_DUNGEON);
        event.register(key);
    }
    private static int hand(Minecraft mc){
        if(mc.player==null)return -1;
        if(AmmoChoices.weapon(mc.player.getMainHandItem()))return 0;
        return AmmoChoices.weapon(mc.player.getOffhandItem())?1:-1;
    }
    public static Component hint(){
        return key==null||key.isUnbound()?Component.translatable("key.cosmicdungeon.unbound"):key.getTranslatedKeyMessage();
    }
    private static Input input(InputConstants.Key key){
        return new Input(key.getType()==InputConstants.Type.MOUSE?Device.MOUSE:
                key.getType()==InputConstants.Type.SCANCODE?Device.SCANCODE:Device.KEY,key.getValue());
    }
    private static Modifier modifier(KeyModifier modifier){
        return switch(modifier){case SHIFT->Modifier.SHIFT;case CONTROL->Modifier.CONTROL;case ALT->Modifier.ALT;default->Modifier.NONE;};
    }
    private static boolean matches(KeyMapping mapping,Input input,Modifier modifier){
        return mapping!=null&&!mapping.isUnbound()&&modifier!=null
                &&input(mapping.getKey()).equals(input)&&modifier(mapping.getKeyModifier())==modifier;
    }
    private static boolean activate(Input input,int bits){
        var mc=Minecraft.getInstance();int hand=hand(mc);
        if(key==null||hand<0||mc.screen!=null||!SkillInputPolicy.allowed(new Context(mc.level!=null&&mc.getConnection()!=null,
                mc.player.isAlive(),mc.player.isSpectator(),mc.isWindowActive(),mc.getOverlay()!=null,true,
                !mc.player.containerMenu.getCarried().isEmpty(),false,mc.player.containerMenu==mc.player.inventoryMenu)))return false;
        var modifier=SkillInputPolicy.modifier(bits,InputConstants.MOD_CONTROL,input);
        if(!matches(key,input,modifier))return false;
        for(var other:mc.options.keyMappings)
            if(other!=key&&other.getKeyConflictContext().isActive()&&matches(other,input,modifier))return false;
        var weapon=hand==0?mc.player.getMainHandItem():mc.player.getOffhandItem();
        ClientPacketDistributor.sendToServer(new AmmoPayloads.Cycle(hand,mc.player.getInventory().getSelectedSlot(),weapon.copyWithCount(1)));
        return true;
    }
    @SubscribeEvent public static void key(InputEvent.Key event){
        var input=input(InputConstants.getKey(event.getKeyEvent()));
        if(event.getAction()==InputConstants.RELEASE){PRESSES.release(input);return;}
        if(event.getAction()==InputConstants.PRESS&&PRESSES.press(input))activate(input,event.getModifiers());
    }
    @SubscribeEvent public static void screenRelease(ScreenEvent.KeyReleased.Pre event){
        PRESSES.release(input(InputConstants.getKey(event.getKeyEvent())));
    }
    @SubscribeEvent public static void mouse(InputEvent.MouseButton.Pre event){
        var input=new Input(Device.MOUSE,event.getButton());
        if(event.getAction()==InputConstants.RELEASE){PRESSES.release(input);return;}
        if(event.getAction()==InputConstants.PRESS&&PRESSES.press(input)&&activate(input,event.getModifiers()))event.setCanceled(true);
    }
    public static void receive(AmmoPayloads.View incoming){
        var mc=Minecraft.getInstance();
        if(mc.player==null||mc.level==null||!mc.level.dimension().location().equals(incoming.dimension())
                ||!ClassData.getClassId(mc.player).equals(incoming.classId()))return;
        if(owner==mc.player&&view!=null&&incoming.revision()<=view.revision())return;
        owner=mc.player;view=incoming;
        if(incoming.notice()==0)return;
        var message=incoming.selected().isEmpty()?Component.literal("Out of ammunition. "):
                Component.literal(incoming.notice()==3?"Switched ammunition to ":"Ammunition: ").append(incoming.selected().getHoverName()).append(". ");
        if(!incoming.selected().isEmpty()&&weapon(mc,incoming.hand()).getItem() instanceof CrossbowItem)
            message.append("Selection applies to the next load. ");
        message.append("Change ammo: ").append(hint()).append(".");
        mc.player.displayClientMessage(message,true);
    }
    private static ItemStack weapon(Minecraft mc,int hand){return hand==0?mc.player.getMainHandItem():mc.player.getOffhandItem();}
    private static boolean current(Minecraft mc){
        return view!=null&&owner==mc.player&&mc.player!=null&&mc.player.isAlive()&&mc.level!=null
                &&mc.level.dimension().location().equals(view.dimension())&&ClassData.getClassId(mc.player).equals(view.classId());
    }
    @SubscribeEvent public static void projectile(LivingGetProjectileEvent event){
        var mc=Minecraft.getInstance();
        if(!current(mc)||event.getEntity()!=mc.player||event.getProjectileWeaponItemStack()!=weapon(mc,view.hand()))return;
        if(view.slot()<0)return;
        var actual=mc.player.getInventory().getItem(view.slot());
        if(AmmoChoices.same(actual,view.selected())&&!actual.isEmpty())event.setProjectileItemStack(actual);
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        if(key!=null)while(key.consumeClick()){}
        var mc=Minecraft.getInstance();
        if(!current(mc)){view=null;owner=mc.player;}
        if(mc.player==null||!mc.isWindowActive())PRESSES.clear();
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event){view=null;owner=null;PRESSES.clear();}
    @SubscribeEvent public static void render(RenderGuiEvent.Post event){
        var mc=Minecraft.getInstance();
        if(!current(mc)||mc.screen!=null||mc.options.hideGui||hand(mc)!=view.hand())return;
        var weapon=weapon(mc,view.hand());if(!AmmoChoices.weapon(weapon))return;
        int width=Math.min(200,mc.getWindow().getGuiScaledWidth()-12);
        int x=(mc.getWindow().getGuiScaledWidth()-width)/2,y=mc.getWindow().getGuiScaledHeight()-80;
        var graphics=event.getGuiGraphics();graphics.fill(x,y,x+width,y+34,0xC0202028);
        if(!view.selected().isEmpty())graphics.renderItem(view.selected(),x+4,y+8);
        String label=view.selected().isEmpty()?"Out of ammunition":view.selected().getHoverName().getString();
        graphics.drawString(mc.font,mc.font.plainSubstrByWidth(label,width-28),x+24,y+4,0xFFFFFFFF);
        var hint=Component.literal(weapon.getItem() instanceof CrossbowItem?"Next load | ":"Change ammo | ").append(hint());
        graphics.drawString(mc.font,mc.font.plainSubstrByWidth(hint.getString(),width-28),x+24,y+19,0xFFBBBBBB);
    }
}
