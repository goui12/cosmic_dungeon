package net.goui.cosmicdungeon.client.screen.skills;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import net.goui.cosmicdungeon.network.BogatyrPayloads;
import net.goui.cosmicdungeon.network.BogatyrPayloads.View;
import net.goui.cosmicdungeon.playerclass.bogatyr.WolfMode;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/** Server-authoritative mode choices and inventory-backed Feed Pack; retired care quotes remain inert. */
public final class BogatyrClient {
    public static final String PATROL="bogatyr_patrol",STAY="bogatyr_stay",FEED_PACK="bogatyr_feed_pack";
    private static final List<WolfMode> MODES=List.of(WolfMode.PATROL,WolfMode.STAY);
    private static final List<String> LABELS=List.of("Patrol","Stay");
    private static final List<String> DETAILS=List.of(
            "Patrol\nYour wolves follow you and defend you and themselves when attacked. Returning from Stay forgets earlier enemies.",
            "Stay\nYour wolves return and sit within 5 blocks of your current position. They hold those positions even when you move and ignore attacks until you select Patrol.");
    private static final String FEED_DETAILS="Feed Pack\nUse up to 10 food items from your inventory to heal injured wolves first, then breed healthy adults."
            +"\nUses bones and rotten flesh first, then raw meat, then remaining meat from lowest to highest player food value."
            +"\nBones work as food only through Feed Pack. Wolves in Stay may move to breed, then return to their saved positions after breeding or failing to find a mate.";
    private static View view=View.empty(0);
    private static Consumer<BogatyrPayloads.Action> actionSender;
    private static Consumer<BogatyrPayloads.ModeAction> modeSender;
    private static boolean pending;
    private BogatyrClient(){}
    public static void actions(Consumer<BogatyrPayloads.Action> callback){actionSender=Objects.requireNonNull(callback);}
    public static void modeActions(Consumer<BogatyrPayloads.ModeAction> callback){modeSender=Objects.requireNonNull(callback);}
    public static String modeId(WolfMode mode){
        return switch(Objects.requireNonNull(mode)){
            case PATROL->PATROL;
            case STAY->STAY;
            default->"bogatyr_retired_mode_"+mode.ordinal();
        };
    }
    public static void clear(){view=View.empty(0);pending=false;}
    public static void accept(View value){
        Objects.requireNonNull(value);
        if(value.revision()<=view.revision())return;
        view=value;pending=false;
    }
    public static boolean pending(){return pending;}
    private static boolean ready(){return !pending&&view.run()>0;}
    public static SkillsPanelModel augment(SkillsPanelModel base){
        if(!base.classId().equals("bogatyr"))return base;
        var actions=new ArrayList<SkillsPanelModel.Action>(3);
        for(int i=0;i<MODES.size();i++){
            boolean enabled=modeSender!=null&&ready()&&view.modesEnabled();
            boolean selected=view.run()>0&&view.mode()==MODES.get(i);
            var tooltip=Component.literal(DETAILS.get(i)).withStyle(ChatFormatting.YELLOW);
            if(selected)tooltip.append(Component.literal("\nSelected mode.").withStyle(ChatFormatting.AQUA));
            if(!enabled)tooltip.append(Component.literal("\n"+(pending?"Waiting for the server."
                    :"Wolf modes are currently unavailable.")).withStyle(ChatFormatting.GRAY));
            actions.add(new SkillsPanelModel.Action(modeId(MODES.get(i)),LABELS.get(i),tooltip,enabled,selected));
        }
        boolean feedEnabled=actionSender!=null&&ready()&&view.feedInputs()>0;
        var feedTooltip=Component.literal(FEED_DETAILS).withStyle(ChatFormatting.YELLOW);
        if(feedEnabled)feedTooltip.append(Component.literal("\nReady to use: "+view.feedInputs()+" item"
                +(view.feedInputs()==1?"":"s")+".").withStyle(ChatFormatting.GREEN));
        else feedTooltip.append(Component.literal("\n"+(pending?"Waiting for the server."
                :view.run()==0?"Feed Pack is unavailable outside an active dungeon."
                :"No eligible wolves and food are ready to use.")).withStyle(ChatFormatting.GRAY));
        actions.add(new SkillsPanelModel.Action(FEED_PACK,"Feed Pack",feedTooltip,feedEnabled));
        String resource=view.run()>0?"Loaded wolves: "+view.loaded():"Waiting for wolfpack state";
        String tooltip=view.run()>0?"Living, loaded wolves you own in this dungeon: "+view.loaded()
                :"Wolfpack state is unavailable outside an active dungeon.";
        return new SkillsPanelModel(base.classId(),"Wolfpack",resource,tooltip,actions);
    }
    public static boolean activate(String actionId){
        if(!ready())return false;
        if(FEED_PACK.equals(actionId)){
            if(actionSender==null||view.feedInputs()==0)return false;
            var action=new BogatyrPayloads.Action(view.run(),view.revision(),BogatyrPayloads.Kind.FEED_PACK);pending=true;
            try{actionSender.accept(action);return true;}catch(RuntimeException failure){pending=false;throw failure;}
        }
        if(modeSender==null||!view.modesEnabled())return false;
        for(var mode:MODES)if(modeId(mode).equals(actionId)){
            if(view.mode()==mode)return false;
            var action=new BogatyrPayloads.ModeAction(view.run(),view.revision(),mode);pending=true;
            try{modeSender.accept(action);return true;}catch(RuntimeException failure){pending=false;throw failure;}
        }
        return false;
    }
}
