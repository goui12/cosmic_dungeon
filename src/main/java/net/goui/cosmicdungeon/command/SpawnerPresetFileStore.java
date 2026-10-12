package net.goui.cosmicdungeon.command;

import com.google.gson.*;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import net.goui.cosmicdungeon.block.entity.*;
import net.goui.cosmicdungeon.spawner.editor.SpawnerEditorConfig;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

/** Version4 keeps exact NBT types via SNBT; old partial files remain partial overlays. */
public final class SpawnerPresetFileStore {
    public static final int FORMAT_VERSION=4,MAX_BYTES=262144;
    public static final String FILE_METADATA="CosmicPresetFileMetadata";
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
    private static final Set<String> FILE_KEYS=Set.of("format","formatVersion","entityTypeId","bossOneShot","spawnerMobCap","minSpawnDelay","maxSpawnDelay","spawnerPresetNbt","blockEntitySnbt");
    private static final Set<String> TRANSIENT=Set.of("id","x","y","z","Delay","BossHasSpawned","CosmicSpawnBlocked",FILE_METADATA);
    private SpawnerPresetFileStore(){}
    public static boolean validName(String name){
        return name!=null&&name.matches("[A-Za-z0-9_-]{1,64}")&&!name.matches("(?i)(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])");
    }
    public static Path presetDirectory(MinecraftServer server){return server.getServerDirectory().resolve("cosmicdungeon").resolve("spawner_presets");}
    public static Path presetPath(MinecraftServer server,String name){
        if(!validName(name))throw new IllegalArgumentException("Preset names need1–64 letters, digits, '_' or '-'; reserved device names are not allowed.");
        return presetDirectory(server).resolve(name+".json");
    }
    private static void directory(Path dir)throws IOException{
        if(Files.isSymbolicLink(dir)||Files.isSymbolicLink(dir.getParent()))throw new IOException("Linked preset directories are not supported.");
        Files.createDirectories(dir);
    }
    private static void regular(Path path)throws IOException{
        if(Files.isSymbolicLink(path)||!Files.isRegularFile(path,LinkOption.NOFOLLOW_LINKS))throw new IOException("Preset is missing or is not a regular file.");
        if(Files.size(path)>MAX_BYTES)throw new IOException("Preset exceeds256KiB.");
    }
    public record LoadedPreset(CompoundTag configuration,boolean complete,JsonObject metadata){
        public LoadedPreset {configuration=configuration.copy();metadata=metadata.deepCopy();}
        @Override public CompoundTag configuration(){return configuration.copy();}
        @Override public JsonObject metadata(){return metadata.deepCopy();}
        public CompoundTag applyTo(CompoundTag target){
            var out=target.copy();var source=configuration.copy();
            if(complete){
                for(String key:List.of("SpawnerPreset","SpawnData","SpawnPotentials"))if(!source.contains(key))out.remove(key);
            }else{
                // Legacy files are partial overlays: retain authored settings while keeping entity identity consistent on reload.
                if(!source.contains("SpawnerPreset")&&out.contains("SpawnerPreset")){
                    var preserved=out.getCompoundOrEmpty("SpawnerPreset").copy();
                    preserved.putString("entityType",source.getStringOr("SpawnerEntityId","minecraft:pig"));out.put("SpawnerPreset",preserved);
                }
                var spawn=out.getCompoundOrEmpty("SpawnData").copy();var entity=spawn.getCompoundOrEmpty("entity").copy();
                entity.putString("id",source.getStringOr("SpawnerEntityId","minecraft:pig"));spawn.put("entity",entity);out.put("SpawnData",spawn);
            }
            if(source.contains("SpawnerPreset"))source.put("SpawnerPreset",preserveUnknownPreset(source.getCompoundOrEmpty("SpawnerPreset"),target.getCompoundOrEmpty("SpawnerPreset")));
            // Import authored configuration, never block identity or live delay/one-shot progress.
            for(String key:source.keySet())if(!TRANSIENT.contains(key))out.put(key,source.get(key).copy());
            out.putBoolean("BossHasSpawned",false);out.putInt("Delay",out.getIntOr("MinSpawnDelay",200));
            out.remove(FILE_METADATA);if(!metadata.isEmpty())out.putString(FILE_METADATA,GSON.toJson(metadata));
            return out;
        }
    }
    private static CompoundTag preserveUnknownPreset(CompoundTag source,CompoundTag target){
        var out=source.copy();var known=new HashSet<>(Set.of("presetVersion","entityType","customName","illagerCaptainVariant","equipmentMask","dropMask","entityFlags","nameOverride","captainOverride","intrinsicDropsOverride","intrinsicDrops","intrinsicDropRules"));
        for(var slot:CosmicSpawnerPreset.Slot.values()){known.add("eq_"+slot.id);known.add("drop_"+slot.id);}
        for(String key:target.keySet())if(!known.contains(key)&&!out.contains(key))out.put(key,target.get(key).copy());
        var flags=out.getCompoundOrEmpty("entityFlags").copy();var old=target.getCompoundOrEmpty("entityFlags");
        for(String key:old.keySet())if(!Set.of("Silent","NoAI","Invulnerable","PersistenceRequired","CustomNameVisible").contains(key)&&!flags.contains(key))flags.put(key,old.get(key).copy());
        if(!flags.isEmpty())out.put("entityFlags",flags);return out;
    }
    public static JsonObject encode(CompoundTag saved,JsonObject previous){
        var root=previous==null?new JsonObject():previous.deepCopy();
        String extras=saved.getStringOr(FILE_METADATA,"");
        if(!extras.isBlank())JsonParser.parseString(extras).getAsJsonObject().entrySet().forEach(e->{if(!FILE_KEYS.contains(e.getKey()))root.add(e.getKey(),e.getValue().deepCopy());});
        var config=saved.copy();for(String key:TRANSIENT)config.remove(key);
        root.addProperty("format","cosmicdungeon_spawner_preset");root.addProperty("formatVersion",FORMAT_VERSION);
        root.addProperty("blockEntitySnbt",config.toString());
        root.addProperty("entityTypeId",saved.getStringOr("SpawnerEntityId","minecraft:pig"));
        root.addProperty("bossOneShot",saved.getBooleanOr("BossOneShot",false));root.addProperty("spawnerMobCap",saved.getIntOr("SpawnerMobCap",0));
        root.addProperty("minSpawnDelay",saved.getIntOr("MinSpawnDelay",200));root.addProperty("maxSpawnDelay",saved.getIntOr("MaxSpawnDelay",800));
        if(saved.contains("SpawnerPreset"))root.add("spawnerPresetNbt",CompoundTag.CODEC.encodeStart(JsonOps.INSTANCE,saved.getCompoundOrEmpty("SpawnerPreset")).getOrThrow());
        else root.remove("spawnerPresetNbt");
        return root;
    }
    public static LoadedPreset decode(JsonObject root){
        try{
            if(root.has("format")&&!root.get("format").getAsString().equals("cosmicdungeon_spawner_preset"))throw new IllegalArgumentException("Not a Cosmic Spawner preset.");
            int version=root.has("formatVersion")?root.get("formatVersion").getAsInt():1;
            if(version<1||version>FORMAT_VERSION)throw new IllegalArgumentException("Unsupported preset format version.");
            var config=new CompoundTag();boolean complete=version==4;
            if(complete&&!root.has("blockEntitySnbt"))throw new IllegalArgumentException("Format4 needs its complete configuration.");
            if(complete)config=TagParser.parseCompoundFully(root.get("blockEntitySnbt").getAsString());
            else{
                config.putString("SpawnerEntityId",root.has("entityTypeId")?root.get("entityTypeId").getAsString():"minecraft:pig");
                config.putBoolean("BossOneShot",root.has("bossOneShot")&&root.get("bossOneShot").getAsBoolean());
                config.putInt("SpawnerMobCap",root.has("spawnerMobCap")?root.get("spawnerMobCap").getAsInt():0);
                int min=root.has("minSpawnDelay")?root.get("minSpawnDelay").getAsInt():200;
                config.putInt("MinSpawnDelay",min);config.putInt("MaxSpawnDelay",root.has("maxSpawnDelay")?root.get("maxSpawnDelay").getAsInt():min);
                if(root.has("spawnerPresetNbt")&&!root.get("spawnerPresetNbt").isJsonNull())
                    config.put("SpawnerPreset",CompoundTag.CODEC.parse(JsonOps.INSTANCE,root.get("spawnerPresetNbt")).getOrThrow());
            }
            validate(config);
            var extras=root.deepCopy();for(String key:FILE_KEYS)extras.remove(key);
            return new LoadedPreset(config,complete,extras);
        }catch(IllegalArgumentException ex){throw ex;}
        catch(Exception ex){throw new IllegalArgumentException("Malformed preset: "+ex.getMessage(),ex);}
    }
    public static void validate(CompoundTag config){ validate(config,false); }
    /** Only an opening snapshot may contain the unconfigured block sentinel; files and Apply stay strict. */
    public static void validateEditorSnapshot(CompoundTag config){ validate(config,true); }
    private static void validate(CompoundTag config,boolean opening){
        String id=config.getStringOr("SpawnerEntityId","");var entity=ResourceLocation.tryParse(id);
        if(!(opening&&id.equals("none"))&&(entity==null||net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getOptional(entity).isEmpty()))
            throw new IllegalArgumentException("Unknown preset entity.");
        int min=config.getIntOr("MinSpawnDelay",200),max=config.getIntOr("MaxSpawnDelay",800),cap=config.getIntOr("SpawnerMobCap",0);
        if(min<1||max<min||max>32767||cap<0||cap>32767)throw new IllegalArgumentException("Invalid preset spawn limits.");
        if(config.getIntOr("CosmicSpawnerDataVersion",0)>CosmicSpawnerBlockEntity.CURRENT_DATA_VERSION||
                config.getCompoundOrEmpty("SpawnerPreset").getIntOr("presetVersion",0)>CosmicSpawnerPreset.PRESET_VERSION)
            throw new IllegalArgumentException("Newer preset configuration is read-only.");
        for(String key:List.of("SpawnCount","MaxNearbyEntities","RequiredPlayerRange","SpawnRange"))
            if(config.contains(key)&&(config.getIntOr(key,-1)<0||config.getIntOr(key,0)>32767))throw new IllegalArgumentException("Invalid native spawn limit: "+key);
        if(config.toString().length()>65536)throw new IllegalArgumentException("Preset configuration exceeds editor limit.");
    }
    public static LoadedPreset readPreset(MinecraftServer server,String name)throws IOException{
        Path path=presetPath(server,name);directory(path.getParent());regular(path);
        String json=Files.readString(path,StandardCharsets.UTF_8);if(json.length()>MAX_BYTES)throw new IOException("Preset exceeds limit.");
        var root=JsonParser.parseString(json).getAsJsonObject();var result=decode(root);
        int version=root.has("formatVersion")?root.get("formatVersion").getAsInt():1;
        if(version<FORMAT_VERSION)LogUtils.getLogger().info("Cosmic Spawner preset '{}' format{} read compatibly; explicit next save uses{}.",name,version,FORMAT_VERSION);
        return result;
    }
    public static void writePreset(MinecraftServer server,String name,CosmicSpawnerBlockEntity be)throws IOException{
        writePreset(server,name,be.saveWithoutMetadata(server.registryAccess()),true);
    }
    public static void writePreset(MinecraftServer server,String name,CompoundTag configuration,boolean overwrite)throws IOException{
        writeFile(presetPath(server,name),configuration,overwrite);
    }
    public static void writeFile(Path path,CompoundTag configuration,boolean overwrite)throws IOException{
        String filename=path.getFileName().toString();
        if(!filename.endsWith(".json")||!validName(filename.substring(0,filename.length()-5)))throw new IOException("Invalid preset filename.");
        directory(path.getParent());JsonObject previous=new JsonObject();
        if(Files.exists(path,LinkOption.NOFOLLOW_LINKS)){
            regular(path);if(!overwrite)throw new FileAlreadyExistsException("Preset exists; confirm Overwrite.");
            previous=JsonParser.parseString(Files.readString(path,StandardCharsets.UTF_8)).getAsJsonObject();decode(previous);
        }
        validate(configuration);byte[] bytes=GSON.toJson(encode(configuration,previous)).getBytes(StandardCharsets.UTF_8);
        if(bytes.length>MAX_BYTES)throw new IOException("Preset exceeds256KiB.");
        Path temp=Files.createTempFile(path.getParent(),".preset-",".tmp");
        try{
            try(var channel=java.nio.channels.FileChannel.open(temp,StandardOpenOption.WRITE,StandardOpenOption.TRUNCATE_EXISTING)){
                var buffer=java.nio.ByteBuffer.wrap(bytes);while(buffer.hasRemaining())channel.write(buffer);channel.force(true);
            }
            if(overwrite){try{Files.move(temp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
                catch(AtomicMoveNotSupportedException ex){throw new IOException("Atomic preset replacement unavailable; existing file retained.",ex);}}
            else Files.move(temp,path);
        }finally{Files.deleteIfExists(temp);}
    }
    public static boolean deletePreset(MinecraftServer server,String name)throws IOException{
        var path=presetPath(server,name);directory(path.getParent());
        if(!Files.exists(path,LinkOption.NOFOLLOW_LINKS))return false;regular(path);return Files.deleteIfExists(path);
    }
    public static List<String> listPresetNames(MinecraftServer server)throws IOException{
        Path dir=presetDirectory(server);directory(dir);
        try(Stream<Path> stream=Files.list(dir)){
            var names=stream.filter(p->!Files.isSymbolicLink(p)&&Files.isRegularFile(p,LinkOption.NOFOLLOW_LINKS))
                    .map(p->p.getFileName().toString()).filter(n->n.endsWith(".json")).map(n->n.substring(0,n.length()-5))
                    .filter(SpawnerPresetFileStore::validName).sorted(String.CASE_INSENSITIVE_ORDER).limit(257).toList();
            if(names.size()>256)throw new IOException("Library exceeds256 named presets; use commands for additional files.");
            return names;
        }
    }
    public static void ensureExamplePresets(MinecraftServer server)throws IOException{
        Path dir=presetDirectory(server);directory(dir);try(var files=Files.list(dir)){if(files.findAny().isPresent())return;}
        example(server,"Skeleton_Master","minecraft:skeleton",false,6,80,120);
        example(server,"Warden_Trial","minecraft:warden",true,1,300,420);
        example(server,"Pillager_Captain_Elite","minecraft:pillager",false,8,60,100);
    }
    private static void example(MinecraftServer server,String name,String entity,boolean boss,int cap,int min,int max)throws IOException{
        var tag=new CompoundTag();tag.putString("SpawnerEntityId",entity);tag.putBoolean("BossOneShot",boss);
        tag.putInt("SpawnerMobCap",cap);tag.putInt("MinSpawnDelay",min);tag.putInt("MaxSpawnDelay",max);
        // Examples are deliberately legacy partial overlays; no fabricated weighted/native settings.
        var json=new JsonObject();json.addProperty("format","cosmicdungeon_spawner_preset");json.addProperty("formatVersion",3);
        json.addProperty("entityTypeId",entity);json.addProperty("bossOneShot",boss);json.addProperty("spawnerMobCap",cap);
        json.addProperty("minSpawnDelay",min);json.addProperty("maxSpawnDelay",max);
        Files.writeString(presetPath(server,name),GSON.toJson(json),StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW);
    }
}
