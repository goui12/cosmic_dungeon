package net.goui.cosmicdungeon.client.screen.skills;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import net.goui.cosmicdungeon.network.BogatyrPayloads;
import net.goui.cosmicdungeon.network.BogatyrPayloads.Kind;
import net.goui.cosmicdungeon.network.BogatyrPayloads.Quote;
import net.goui.cosmicdungeon.network.BogatyrPayloads.View;
import net.goui.cosmicdungeon.playerclass.bogatyr.WolfMode;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/** Displays the current server quote; commands never infer eligibility from visible nearby entities. */
public final class BogatyrClient {
    public static final String BREED="bogatyr_breed",SUMMON="bogatyr_summon",
            REGROUP="bogatyr_regroup",HEAL="bogatyr_heal";
    private static final List<String> IDS=List.of(BREED,SUMMON,REGROUP,HEAL);
    private static final List<String> LABELS=List.of("Breed","Summon","Regroup","Heal");
    private static final List<String> DETAILS=List.of(
            "Put eligible adult wolves into love mode. Costs 5 Kibble per wolf; an affordable part of your pack, even one wolf, can be selected. Wolves mate naturally.",
            "Summon one tamed wolf beside you. Costs 30 Kibble only when safe placement succeeds.",
            "Bring all living, loaded wolves you own in this dungeon to safe positions beside you. Costs 1 Kibble per wolf and requires enough for the entire affected pack. Unloaded wolves are not retrieved.",
            "Fully heal injured, living, loaded wolves you own, starting with the lowest health. Costs 5 Kibble per wolf; heal as many as you can afford.");
    private static final List<String> MODES=List.of("Defensive","Stand Ground","Aggressive",
            "Strategic","Search and Rescue","Danger Close");
    private static final List<WolfMode> CORE_MODES=List.of(WolfMode.DEFENSIVE,WolfMode.STAND_GROUND,WolfMode.AGGRESSIVE);
    private static final List<String> MODE_DETAILS=List.of(
            "Protect you and react when you or your wolves are attacked. Switching to this mode stands your wolves up.",
            "Sit immediately, stop moving and stop attacking. Your wolves remain seated even when attacked.",
            "Attack hostile creatures nearest you first, then work outward. Switching to this mode stands your wolves up.");
    private static View view=empty();
    private static Consumer<BogatyrPayloads.Action> sender;
    private static Consumer<BogatyrPayloads.ModeAction> modeSender;
    private static boolean pending;
    private BogatyrClient(){}
    private static View empty(){return new View(0,0,0,List.of(
            new Quote(0,0,false),new Quote(0,0,false),new Quote(0,0,false),new Quote(0,0,false)));}
    public static void actions(Consumer<BogatyrPayloads.Action> callback){sender=Objects.requireNonNull(callback);}
    public static void modeActions(Consumer<BogatyrPayloads.ModeAction> callback){modeSender=Objects.requireNonNull(callback);}
    public static String modeId(WolfMode mode){return "bogatyr_mode_"+CORE_MODES.indexOf(Objects.requireNonNull(mode));}
    public static void clear(){view=empty();pending=false;}
    public static void accept(View value){
        Objects.requireNonNull(value);
        if(value.revision()<=view.revision())return;
        view=value;pending=false;
    }
    public static boolean pending(){return pending;}
    private static boolean ready(){return !pending&&view.run()>0;}
    public static SkillsPanelModel augment(SkillsPanelModel base){
        if(!base.classId().equals("bogatyr"))return base;
        var actions=new ArrayList<SkillsPanelModel.Action>();
        for(var action:base.actions())if(!action.id().equals("guide"))actions.add(action);
        for(var kind:Kind.values()){
            int index=kind.ordinal();var quote=view.quotes().get(index);
            boolean enabled=sender!=null&&ready()&&quote.enabled()&&quote.count()>0;
            var tooltip=Component.literal(DETAILS.get(index)+"\nSelected wolves: "+quote.count()
                    +"\nLoaded wolves in this dungeon: "+view.loaded()+"\nTotal cost: "+quote.cost()+" ")
                    .withStyle(ChatFormatting.YELLOW)
                    .append(Component.literal("Kibble").withStyle(ChatFormatting.BLUE,ChatFormatting.BOLD));
            if(kind==Kind.BREED&&view.run()>0&&view.mode()==WolfMode.STAND_GROUND)
                tooltip.append(Component.literal("\nSwitch to Defensive or Aggressive before breeding.").withStyle(ChatFormatting.YELLOW));
            if(!enabled)tooltip.append(Component.literal("\n"+(pending?"Waiting for the server."
                    :"No eligible, affordable action is currently available.")).withStyle(ChatFormatting.GRAY));
            actions.add(new SkillsPanelModel.Action(IDS.get(index),
                    LABELS.get(index)+" "+quote.count()+" ["+quote.cost()+"]",tooltip,enabled));
        }
        for(int i=0;i<MODES.size();i++){
            if(i>=CORE_MODES.size()){
                actions.add(new SkillsPanelModel.Action("bogatyr_mode_"+i,MODES.get(i),"This wolf mode is not available.",false));
                continue;
            }
            boolean enabled=modeSender!=null&&ready()&&view.modesEnabled();
            boolean selected=view.run()>0&&view.mode()==CORE_MODES.get(i);
            var tooltip=Component.literal(MODE_DETAILS.get(i)+"\nCost: 0 ").withStyle(ChatFormatting.YELLOW)
                    .append(Component.literal("Kibble").withStyle(ChatFormatting.BLUE,ChatFormatting.BOLD));
            if(selected)tooltip.append(Component.literal("\nSelected mode.").withStyle(ChatFormatting.AQUA));
            if(!enabled)tooltip.append(Component.literal("\n"+(pending?"Waiting for the server."
                    :"Wolf modes are currently unavailable.")).withStyle(ChatFormatting.GRAY));
            actions.add(new SkillsPanelModel.Action(modeId(CORE_MODES.get(i)),MODES.get(i),tooltip,enabled,selected));
        }
        return new SkillsPanelModel(base.classId(),"Wolfpack",base.resource(),base.resourceTooltip(),
                actions,base.resourceSnapshot());
    }
    public static boolean activate(String actionId){
        if(!ready())return false;
        for(var mode:CORE_MODES)if(modeId(mode).equals(actionId)){
            if(modeSender==null||!view.modesEnabled())return false;
            var action=new BogatyrPayloads.ModeAction(view.run(),view.revision(),mode);pending=true;
            try{modeSender.accept(action);return true;}catch(RuntimeException failure){pending=false;throw failure;}
        }
        int index=IDS.indexOf(actionId);
        if(index<0||sender==null)return false;
        var quote=view.quotes().get(index);
        if(!quote.enabled()||quote.count()<=0)return false;
        var action=new BogatyrPayloads.Action(view.run(),view.revision(),Kind.values()[index]);
        pending=true;
        try{sender.accept(action);return true;}catch(RuntimeException failure){pending=false;throw failure;}
    }
}
