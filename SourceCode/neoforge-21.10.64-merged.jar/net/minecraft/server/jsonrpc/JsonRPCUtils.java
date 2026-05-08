package net.minecraft.server.jsonrpc;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

public class JsonRPCUtils {
    public static final String JSON_RPC_VERSION = "2.0";
    public static final String OPEN_RPC_VERSION = "1.3.2";

    public static JsonObject createSuccessResult(JsonElement requestId, JsonElement result) {
        JsonObject jsonobject = new JsonObject();
        jsonobject.addProperty("jsonrpc", "2.0");
        jsonobject.add("id", requestId);
        jsonobject.add("result", result);
        return jsonobject;
    }

    public static JsonObject createRequest(@Nullable Integer requestId, ResourceLocation methodName, List<JsonElement> params) {
        JsonObject jsonobject = new JsonObject();
        jsonobject.addProperty("jsonrpc", "2.0");
        if (requestId != null) {
            jsonobject.addProperty("id", requestId);
        }

        jsonobject.addProperty("method", methodName.toString());
        if (!params.isEmpty()) {
            JsonArray jsonarray = new JsonArray(params.size());

            for (JsonElement jsonelement : params) {
                jsonarray.add(jsonelement);
            }

            jsonobject.add("params", jsonarray);
        }

        return jsonobject;
    }

    public static JsonObject createError(JsonElement requestId, String message, int code, @Nullable String data) {
        JsonObject jsonobject = new JsonObject();
        jsonobject.addProperty("jsonrpc", "2.0");
        jsonobject.add("id", requestId);
        JsonObject jsonobject1 = new JsonObject();
        jsonobject1.addProperty("code", code);
        jsonobject1.addProperty("message", message);
        if (data != null && !data.isBlank()) {
            jsonobject1.addProperty("data", data);
        }

        jsonobject.add("error", jsonobject1);
        return jsonobject;
    }

    @Nullable
    public static JsonElement getRequestId(JsonObject json) {
        return json.get("id");
    }

    @Nullable
    public static String getMethodName(JsonObject json) {
        return GsonHelper.getAsString(json, "method", null);
    }

    @Nullable
    public static JsonElement getParams(JsonObject json) {
        return json.get("params");
    }

    @Nullable
    public static JsonElement getResult(JsonObject json) {
        return json.get("result");
    }

    @Nullable
    public static JsonObject getError(JsonObject json) {
        return GsonHelper.getAsJsonObject(json, "error", null);
    }
}
