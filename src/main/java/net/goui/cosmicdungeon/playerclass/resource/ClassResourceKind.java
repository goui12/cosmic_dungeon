package net.goui.cosmicdungeon.playerclass.resource;

import java.util.Optional;
import net.goui.cosmicdungeon.util.ModTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/** Shared resource identities; eligibility always comes from the bound item tags. */
public enum ClassResourceKind {
    BREWING_SUPPLIES("brewing_supplies","theurgist","Brewing Supplies",ModTags.Items.BREWING_SUPPLIES),
    KIBBLE("kibble","bogatyr","Kibble",ModTags.Items.KIBBLE);
    public static final int CAP=600;
    private final String id,classId,title;
    private final TagKey<Item> tag;
    ClassResourceKind(String id,String classId,String title,TagKey<Item> tag){
        this.id=id;this.classId=classId;this.title=title;this.tag=tag;
    }
    public String id(){return id;}
    public String classId(){return classId;}
    public String title(){return title;}
    public TagKey<Item> tag(){return tag;}
    public static Optional<ClassResourceKind> byId(String id){
        for(var value:values())if(value.id.equals(id))return Optional.of(value);return Optional.empty();
    }
    public static Optional<ClassResourceKind> forClass(String id){
        for(var value:values())if(value.classId.equals(id))return Optional.of(value);return Optional.empty();
    }
}
