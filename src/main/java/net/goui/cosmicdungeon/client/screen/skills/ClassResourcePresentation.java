package net.goui.cosmicdungeon.client.screen.skills;

/** Legacy resource snapshots cannot restore retired Kibble, Brewing Supplies or Requests controls. */
public final class ClassResourcePresentation {
    private ClassResourcePresentation(){}
    public static SkillsPanelModel panel(String classId,ClassResourceSnapshot snapshot,boolean awaitingAction){
        return panel(classId,snapshot,awaitingAction,false);
    }
    public static SkillsPanelModel panel(String classId,ClassResourceSnapshot snapshot,boolean awaitingAction,boolean canRequest){
        return SkillsPanelModel.initial(classId);
    }
}
