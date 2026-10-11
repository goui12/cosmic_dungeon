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
    public static View view(){return view;}
    public static boolean canOpenPicker(){return sender!=null&&view.run()>0&&view.theurgist()&&view.alive();}
    public static Component resurrectionTooltip(){
        return Component.literal("Resurrect: offer revival at a teammate's latest death position. No item or resource cost. "
                +"A successful resurrection starts your 3-minute cooldown. The teammate must accept within 60 seconds.\n"
                +net.goui.cosmicdungeon.client.screen.ResurrectionPresentation.status(view.resurrection()));
    }
    public static Component targetTooltip(Target target){
        return resurrectionTooltip().copy().append(Component.literal("\n"+target.name()+": "
                +(pending?"Waiting for the server.":target.available()?"Available for an offer.":"Not currently available for an offer.")));
    }
    private static boolean ready(){return sender!=null&&!pending&&view.run()>0;}
    public static Offer offer(){return view.run()>0&&!view.alive()?view.offer():null;}
    public static boolean canRespond(){return ready()&&offer()!=null;}
    public static String actionId(Target target){return RESURRECT+target.player()+":"+target.death();}
    private static Component availability(Component text,boolean enabled,String unavailable){
        if(enabled)return text;
        return text.copy().append(Component.literal("\n"+(pending?"Waiting for the server.":unavailable)).withStyle(ChatFormatting.GRAY));
    }
    public static SkillsPanelModel augment(SkillsPanelModel base){
        if(!base.classId().equals("theurgist"))return base;
        boolean available=ready()&&view.theurgist()&&view.alive();
        var actions=new ArrayList<SkillsPanelModel.Action>(base.actions());
        if(view.theurgist()&&view.alive()&&!view.targets().isEmpty()){
            for(var target:view.targets()){
                boolean enabled=available&&target.available();
                actions.add(new SkillsPanelModel.Action(actionId(target),"Resurrect "+target.name(),
                        availability(targetTooltip(target),enabled,"Requires an eligible dead teammate and a ready resurrection."),enabled));
            }
        }else actions.add(new SkillsPanelModel.Action("theurgist_resurrect_none","Resurrect...",
                resurrectionTooltip().copy().append(Component.literal("\nNo teammate currently needs resurrection.")),false));
        return new SkillsPanelModel(base.classId(),base.title(),base.resource(),base.resourceTooltip(),actions,base.resourceSnapshot());
    }
    private static boolean send(Kind kind,UUID target,UUID token){
        if(!ready())return false;
        var action=new TheurgistPayloads.Action(view.run(),view.revision(),kind,target,token);pending=true;
        try{sender.accept(action);return true;}catch(RuntimeException failure){pending=false;throw failure;}
    }
    public static boolean activate(String actionId){
        if(!ready()||!view.theurgist()||!view.alive())return false;
        if(CRAFT.equals(actionId)||EPIC.equals(actionId))return false; // Retired random crafting; use the inventory palette.
        for(var target:view.targets())if(actionId(target).equals(actionId))
            return target.available()&&send(Kind.OFFER,target.player(),target.death());
        return false;
    }
    public static boolean acceptOffer(){var offer=offer();return offer!=null&&send(Kind.ACCEPT,offer.caster(),offer.id());}
    public static boolean declineOffer(){var offer=offer();return offer!=null&&send(Kind.DECLINE,offer.caster(),offer.id());}
    public static Component offerTooltip(Offer offer){
        return Component.literal("Resurrection from "+offer.name()+". You return to your latest death position. "
                +"Accept or decline within 60 seconds. No item or resource cost. "+offer.name()+" starts a 3-minute cooldown only if resurrection succeeds.");
    }
}
