package net.minecraft.client.data.models.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DelegatedModel implements ModelInstance {
    private final ResourceLocation parent;

    public DelegatedModel(ResourceLocation parent) {
        this.parent = parent;
    }

    public JsonElement get() {
        JsonObject jsonobject = new JsonObject();
        jsonobject.addProperty("parent", this.parent.toString());
        return jsonobject;
    }
}
