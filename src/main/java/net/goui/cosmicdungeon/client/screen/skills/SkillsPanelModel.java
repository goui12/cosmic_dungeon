package net.goui.cosmicdungeon.client.screen.skills;

import java.util.List;
import java.util.Objects;
import net.goui.cosmicdungeon.playerclass.api.ClassKeys;

/** Display/action descriptions only. A future server-authorized provider supplies real resource snapshots. */
public record SkillsPanelModel(String classId,String title,String resource,String resourceTooltip,List<Action> actions) {
    public record Action(String id,String label,String tooltip,boolean enabled){
        public Action{Objects.requireNonNull(id);Objects.requireNonNull(label);Objects.requireNonNull(tooltip);}
    }
    public SkillsPanelModel {
        Objects.requireNonNull(classId);Objects.requireNonNull(title);Objects.requireNonNull(resource);
        Objects.requireNonNull(resourceTooltip);actions=List.copyOf(actions);
        if(actions.size()>128||actions.stream().map(Action::id).distinct().count()!=actions.size())
            throw new IllegalArgumentException("Invalid skill actions");
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
