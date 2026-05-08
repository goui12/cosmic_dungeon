package net.minecraft.util.datafix;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.util.Optional;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.LenientJsonParser;
import net.minecraft.util.StrictJsonParser;

public class LegacyComponentDataFixUtils {
    private static final String EMPTY_CONTENTS = createTextComponentJson("");

    public static <T> Dynamic<T> createPlainTextComponent(DynamicOps<T> ops, String data) {
        String s = createTextComponentJson(data);
        return new Dynamic<>(ops, ops.createString(s));
    }

    public static <T> Dynamic<T> createEmptyComponent(DynamicOps<T> ops) {
        return new Dynamic<>(ops, ops.createString(EMPTY_CONTENTS));
    }

    public static String createTextComponentJson(String json) {
        JsonObject jsonobject = new JsonObject();
        jsonobject.addProperty("text", json);
        return GsonHelper.toStableString(jsonobject);
    }

    public static String createTranslatableComponentJson(String json) {
        JsonObject jsonobject = new JsonObject();
        jsonobject.addProperty("translate", json);
        return GsonHelper.toStableString(jsonobject);
    }

    public static <T> Dynamic<T> createTranslatableComponent(DynamicOps<T> ops, String data) {
        String s = createTranslatableComponentJson(data);
        return new Dynamic<>(ops, ops.createString(s));
    }

    public static String rewriteFromLenient(String data) {
        if (!data.isEmpty() && !data.equals("null")) {
            char c0 = data.charAt(0);
            char c1 = data.charAt(data.length() - 1);
            if (c0 == '"' && c1 == '"' || c0 == '{' && c1 == '}' || c0 == '[' && c1 == ']') {
                try {
                    JsonElement jsonelement = LenientJsonParser.parse(data);
                    if (jsonelement.isJsonPrimitive()) {
                        return createTextComponentJson(jsonelement.getAsString());
                    }

                    return GsonHelper.toStableString(jsonelement);
                } catch (JsonParseException jsonparseexception) {
                }
            }

            return createTextComponentJson(data);
        } else {
            return EMPTY_CONTENTS;
        }
    }

    public static boolean isStrictlyValidJson(Dynamic<?> data) {
        return data.asString().result().filter(p_446806_ -> {
            try {
                StrictJsonParser.parse(p_446806_);
                return true;
            } catch (JsonParseException jsonparseexception) {
                return false;
            }
        }).isPresent();
    }

    public static Optional<String> extractTranslationString(String data) {
        try {
            JsonElement jsonelement = LenientJsonParser.parse(data);
            if (jsonelement.isJsonObject()) {
                JsonObject jsonobject = jsonelement.getAsJsonObject();
                JsonElement jsonelement1 = jsonobject.get("translate");
                if (jsonelement1 != null && jsonelement1.isJsonPrimitive()) {
                    return Optional.of(jsonelement1.getAsString());
                }
            }
        } catch (JsonParseException jsonparseexception) {
        }

        return Optional.empty();
    }
}
