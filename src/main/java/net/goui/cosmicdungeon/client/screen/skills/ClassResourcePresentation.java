package net.goui.cosmicdungeon.client.screen.skills;

import java.util.ArrayList;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/** Exact user-facing resource wording; no balances or enabled actions are inferred without a server snapshot. */
public final class ClassResourcePresentation {
    private ClassResourcePresentation(){}
    public static Component resourceTooltip(ClassResourceSnapshot snapshot){
        return Component.literal(snapshot.tooltipName()+" <"+snapshot.amount()+" / "+snapshot.cap()+">")
                .withStyle(ChatFormatting.BLUE,ChatFormatting.BOLD);
    }
    public static Component requestTooltip(String resourceName){
        return Component.literal("Click to request supplies from your group. If they accept, their supplies will be automatically recycled into your ")
                .withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(resourceName).withStyle(ChatFormatting.BLUE,ChatFormatting.BOLD))
                .append(Component.literal("\nNo supply requests available.").withStyle(ChatFormatting.GRAY).withStyle(style->style.withBold(false)));
    }
    public static Component recycleTooltip(String resourceName){
        return Component.literal("Click to convert all items into ").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(resourceName).withStyle(ChatFormatting.BLUE,ChatFormatting.BOLD));
    }
    public static SkillsPanelModel panel(String classId,ClassResourceSnapshot snapshot,boolean awaitingAction){
        var base=SkillsPanelModel.initial(classId);
        if(!classId.equals("theurgist")&&!classId.equals("bogatyr"))return base;
        boolean present=snapshot!=null&&snapshot.matches(classId);
        String name=classId.equals("theurgist")?"brewing supplies":"Kibble";
        var actions=new ArrayList<SkillsPanelModel.Action>();
        actions.add(new SkillsPanelModel.Action("request_supplies","Request Supplies",requestTooltip(name),false));
        actions.add(new SkillsPanelModel.Action("recycle","Recycle",recycleTooltip(name),
                present&&snapshot.canRecycle()&&!awaitingAction));
        actions.addAll(base.actions());
        return present?new SkillsPanelModel(classId,base.title(),snapshot.title()+": "+snapshot.count(),
                resourceTooltip(snapshot),actions,snapshot)
                :new SkillsPanelModel(classId,base.title(),base.resource(),base.resourceTooltip(),actions);
    }
}
