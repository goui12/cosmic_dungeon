package net.goui.cosmicdungeon.client.screen.skills;

import java.util.*;
import java.util.function.Consumer;
import net.goui.cosmicdungeon.network.TheurgistPayloads;
import net.goui.cosmicdungeon.network.TheurgistPayloads.*;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/** Client capabilities only. Each action captures the exact displayed run, generation and death/offer token. */
public final class TheurgistClient {
    public static final String CRAFT="theurgist_craft",EPIC="theurgist_epic",RESURRECT="theurgist_resurrect:";
    private static View view=View.empty(0);
    private static Consumer<TheurgistPayloads.Action> sender;
    private static boolean pending;
    private TheurgistClient(){}
    public static void actions(Consumer<TheurgistPayloads.Action> callback){sender=Objects.requireNonNull(callback);}
    public static void clear(){view=View.empty(0);pending=false;}
    public static void receive(View value){
        Objects.requireNonNull(value);pending=false; // Rejected actions also receive an unchanged authoritative acknowledgement.
        if(value.run()==view.run()&&value.revision()<view.revision())return;
        view=value;
    }
    public static boolean pending(){return pending;}
    private static boolean ready(){return sender!=null&&!pending&&view.run()>0;}
    public static Offer offer(){return view.run()>0&&!view.alive()?view.offer():null;}
    public static boolean canRespond(){return ready()&&offer()!=null;}
    public static String actionId(Target target){return RESURRECT+target.player()+":"+target.death();}
    private static Component cost(String text,int amount){
        return Component.literal(text+"\nCost: "+amount+" ").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal("Brewing Supplies").withStyle(ChatFormatting.BLUE,ChatFormatting.BOLD));
    }
    private static Component availability(Component text,boolean enabled,String unavailable){
        if(enabled)return text;
        return text.copy().append(Component.literal("\n"+(pending?"Waiting for the server.":unavailable)).withStyle(ChatFormatting.GRAY));
    }
    public static SkillsPanelModel augment(SkillsPanelModel base){
        if(!base.classId().equals("theurgist"))return base;
        boolean available=ready()&&view.theurgist()&&view.alive();
        var actions=new ArrayList<SkillsPanelModel.Action>(base.actions());
        boolean normal=available&&view.normal(),epic=available&&view.epic();
        actions.add(new SkillsPanelModel.Action(CRAFT,"Craft a potion [20]",
                availability(cost("Create one random positive tier I splash potion. No bottle, fuel or brewing stand is needed.",20),
                        normal,"Requires a living Theurgist, 20 Brewing Supplies and inventory space."),normal));
        actions.add(new SkillsPanelModel.Action(EPIC,"Craft an epic potion [40]",
                availability(cost("Create one random tier II splash potion: swiftness, healing, regeneration or strength.",40),
                        epic,"Requires a living Theurgist, 40 Brewing Supplies and inventory space."),epic));
        if(view.theurgist()&&view.alive()&&!view.targets().isEmpty()){
            for(var target:view.targets()){
                boolean enabled=available&&target.available();
                actions.add(new SkillsPanelModel.Action(actionId(target),"Resurrect "+target.name()+" [120]",
                        availability(cost("Offer resurrection to "+target.name()+" at their latest death position in this dungeon. "
                                +"You pay only if they accept and resurrection succeeds. No cooldown.",120),
                                enabled,"Requires 120 Brewing Supplies and an eligible dead teammate."),enabled));
            }
        }else actions.add(new SkillsPanelModel.Action("theurgist_resurrect_none","Resurrect...",
                Component.literal("No teammate currently needs resurrection."),false));
        return new SkillsPanelModel(base.classId(),base.title(),base.resource(),base.resourceTooltip(),actions,base.resourceSnapshot());
    }
    private static boolean send(Kind kind,UUID target,UUID token){
        if(!ready())return false;
        var action=new TheurgistPayloads.Action(view.run(),view.revision(),kind,target,token);pending=true;
        try{sender.accept(action);return true;}catch(RuntimeException failure){pending=false;throw failure;}
    }
    public static boolean activate(String actionId){
        if(!ready()||!view.theurgist()||!view.alive())return false;
        if(CRAFT.equals(actionId))return view.normal()&&send(Kind.CRAFT,TheurgistPayloads.NONE,TheurgistPayloads.NONE);
        if(EPIC.equals(actionId))return view.epic()&&send(Kind.EPIC,TheurgistPayloads.NONE,TheurgistPayloads.NONE);
        for(var target:view.targets())if(actionId(target).equals(actionId))
            return target.available()&&send(Kind.OFFER,target.player(),target.death());
        return false;
    }
    public static boolean acceptOffer(){var offer=offer();return offer!=null&&send(Kind.ACCEPT,offer.caster(),offer.id());}
    public static boolean declineOffer(){var offer=offer();return offer!=null&&send(Kind.DECLINE,offer.caster(),offer.id());}
    public static Component offerTooltip(Offer offer){
        return Component.literal("Resurrection from "+offer.name()+". You return to your latest death position. "
                +offer.name()+" pays 120 Brewing Supplies only if resurrection succeeds.");
    }
}
