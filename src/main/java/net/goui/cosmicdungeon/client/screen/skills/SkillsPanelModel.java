package net.goui.cosmicdungeon.client.screen.skills;

import java.util.List;
import java.util.Objects;
import net.goui.cosmicdungeon.playerclass.api.ClassKeys;
import net.minecraft.network.chat.Component;

/** Display/action descriptions only; rich text and optional authoritative resource presentation are reusable. */
public record SkillsPanelModel(String classId,String title,String resource,Component resourceTooltip,
                               List<Action> actions,ClassResourceSnapshot resourceSnapshot) {
    public record Action(String id,String label,Component tooltip,boolean enabled,boolean selected){
        public Action{Objects.requireNonNull(id);Objects.requireNonNull(label);tooltip=Objects.requireNonNull(tooltip).copy();}
        public Action(String id,String label,Component tooltip,boolean enabled){this(id,label,tooltip,enabled,false);}
        public Action(String id,String label,String tooltip,boolean enabled){this(id,label,Component.literal(tooltip),enabled,false);}
        public Action(String id,String label,String tooltip,boolean enabled,boolean selected){this(id,label,Component.literal(tooltip),enabled,selected);}
    }
    public SkillsPanelModel(String classId,String title,String resource,String tooltip,List<Action> actions){
        this(classId,title,resource,Component.literal(tooltip),actions,null);
    }
    public SkillsPanelModel(String classId,String title,String resource,Component tooltip,List<Action> actions){
        this(classId,title,resource,tooltip,actions,null);
    }
    public SkillsPanelModel {
        Objects.requireNonNull(classId);Objects.requireNonNull(title);Objects.requireNonNull(resource);
        resourceTooltip=Objects.requireNonNull(resourceTooltip).copy();actions=List.copyOf(actions);
        if(actions.size()>128||actions.stream().map(Action::id).distinct().count()!=actions.size())
            throw new IllegalArgumentException("Invalid skill actions");
        if(resourceSnapshot!=null&&!resourceSnapshot.matches(classId))
            throw new IllegalArgumentException("Resource snapshot belongs to another class");
    }
    public static SkillsPanelModel initial(String rawClass){
        String id=ClassKeys.clamp(rawClass);
        var actions=switch(id){
            case ClassKeys.CLASS_ID_BOGATYR->List.of(
                    new Action("guide","Wolf care","Read the Bogatyr guide. Use bones to tame eligible wolves and food to care for your companions.",true));
            case ClassKeys.CLASS_ID_THEURGIST->List.of(
                    new Action("guide","Brewing","Read the Theurgist guide. Use a brewing stand to make valid potions instantly.",true));
            default->List.<Action>of();
        };
        String title=ClassKeys.CLASS_ID_NONE.equals(id)?"Skills":Character.toUpperCase(id.charAt(0))+id.substring(1)+" Skills";
        return new SkillsPanelModel(id,title,"Resource: —","No resource balance available.",actions);
    }
}
