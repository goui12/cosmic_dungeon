package net.goui.cosmicdungeon.datagen;
import com.google.gson.*;
import net.minecraft.data.*;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
public final class D1EffectAtlasProvider implements DataProvider {
    private final PackOutput output;
    public D1EffectAtlasProvider(PackOutput output){this.output=output;}
    @Override public String getName(){return "D1 custom ammunition effect sprites";}
    @Override public CompletableFuture<?> run(CachedOutput cache){
        var root=new JsonObject();var sources=new JsonArray();
        var aliases=Map.of("mending_sting","regeneration","verdant_jolt","regeneration","tree_viper","poison",
                "bushmaster","poison","fer_de_lance","poison","pestis","weakness","black_bubo","weakness",
                "vapours","slowness","melancholia","slowness","deathly_stupor","slowness");
        aliases.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry->{
            var source=new JsonObject();source.addProperty("type","minecraft:single");
            source.addProperty("resource","cosmicdungeon:mob_effect/"+entry.getKey());
            source.addProperty("sprite","cosmicdungeon:"+entry.getKey());sources.add(source);
        });
        root.add("sources",sources);
        return DataProvider.saveStable(cache,root,output.getOutputFolder(PackOutput.Target.RESOURCE_PACK)
                .resolve("minecraft/atlases/mob_effects.json"));
    }
}
