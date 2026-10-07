package net.goui.cosmicdungeon.client.screen.skills;

import com.google.gson.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Optional;
import net.goui.cosmicdungeon.playerclass.api.ClassKeys;

/** Small client-only layout file. Loaded once; atomic writes occur only for actual user changes. */
public final class SkillsPanelPreferences {
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
    private final Path path;private JsonObject root=new JsonObject();private boolean writable=true;
    public SkillsPanelPreferences(Path path){
        this.path=path;
        if(!Files.exists(path))return;
        try{
            if(Files.size(path)>65536)throw new IOException("Oversized skill layout");
            var parsed=JsonParser.parseString(Files.readString(path,StandardCharsets.UTF_8));
            if(!parsed.isJsonObject())throw new IOException("Invalid skill layout");
            root=parsed.getAsJsonObject();
            if(root.has("version")&&root.get("version").getAsInt()!=1)writable=false;
            if(root.has("layouts")&&!root.get("layouts").isJsonObject())writable=false;
        }catch(IOException|RuntimeException invalid){writable=false;}
    }
    public boolean writable(){return writable;}
    public Optional<SkillsPanelState.Placement> get(String classId){
        if(!ClassKeys.isKnown(classId)||!root.has("layouts")||!root.get("layouts").isJsonObject())return Optional.empty();
        var layouts=root.getAsJsonObject("layouts");
        if(!layouts.has(classId)||!layouts.get(classId).isJsonObject())return Optional.empty();
        var value=layouts.getAsJsonObject(classId);
        try{
            if(!value.has("x")||!value.has("y")||!value.has("minimized"))return Optional.empty();
            return Optional.of(new SkillsPanelState.Placement(value.get("x").getAsDouble(),
                    value.get("y").getAsDouble(),value.get("minimized").getAsBoolean()));
        }catch(RuntimeException invalid){return Optional.empty();}
    }
    public boolean put(String classId,SkillsPanelState.Placement placement)throws IOException{
        if(!writable||!ClassKeys.isKnown(classId)||get(classId).filter(placement::equals).isPresent())return false;
        var next=root.deepCopy();var layouts=layouts(next);
        var entry=layouts.has(classId)&&layouts.get(classId).isJsonObject()?layouts.getAsJsonObject(classId):new JsonObject();
        entry.addProperty("x",placement.x());entry.addProperty("y",placement.y());entry.addProperty("minimized",placement.minimized());
        layouts.add(classId,entry);write(next);return true;
    }
    public boolean reset()throws IOException{
        if(!writable){root=new JsonObject();return false;}
        if(!root.has("layouts")||!root.get("layouts").isJsonObject())return false;
        var next=root.deepCopy();boolean changed=false;
        for(var id:ClassKeys.ORDERED){
            var value=next.getAsJsonObject("layouts").get(id);
            if(value==null||!value.isJsonObject())continue;
            var entry=value.getAsJsonObject();
            for(var key:new String[]{"x","y","minimized"})if(entry.has(key)){entry.remove(key);changed=true;}
        }
        if(changed)write(next);return changed;
    }
    private static JsonObject layouts(JsonObject target){
        if(!target.has("layouts"))target.add("layouts",new JsonObject());
        return target.getAsJsonObject("layouts");
    }
    private void write(JsonObject next)throws IOException{
        next.addProperty("version",1);
        var parent=path.toAbsolutePath().getParent();Files.createDirectories(parent);
        var temp=Files.createTempFile(parent,"skills-layout-",".tmp");
        try{
            Files.writeString(temp,GSON.toJson(next)+"\n",StandardCharsets.UTF_8);
            try{Files.move(temp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException unsupported){Files.move(temp,path,StandardCopyOption.REPLACE_EXISTING);}
            root=next;
        }finally{Files.deleteIfExists(temp);}
    }
}
