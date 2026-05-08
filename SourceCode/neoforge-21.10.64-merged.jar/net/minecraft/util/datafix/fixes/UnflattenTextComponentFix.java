package net.minecraft.util.datafix.fixes;

import com.google.gson.JsonElement;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.Util;
import net.minecraft.util.LenientJsonParser;
import org.slf4j.Logger;

public class UnflattenTextComponentFix extends DataFix {
    private static final Logger LOGGER = LogUtils.getLogger();

    public UnflattenTextComponentFix(Schema outputSchema) {
        super(outputSchema, true);
    }

    @Override
    protected TypeRewriteRule makeRule() {
        Type<Pair<String, String>> type = (Type<Pair<String, String>>)this.getInputSchema().getType(References.TEXT_COMPONENT);
        Type<?> type1 = this.getOutputSchema().getType(References.TEXT_COMPONENT);
        return this.createFixer(type, type1);
    }

    private <T> TypeRewriteRule createFixer(Type<Pair<String, String>> inputType, Type<T> outputType) {
        return this.fixTypeEverywhere(
            "UnflattenTextComponentFix",
            inputType,
            outputType,
            p_393997_ -> p_394593_ -> Util.readTypedOrThrow(outputType, unflattenJson(p_393997_, p_394593_.getSecond()), true).getValue()
        );
    }

    private static <T> Dynamic<T> unflattenJson(DynamicOps<T> ops, String json) {
        try {
            JsonElement jsonelement = LenientJsonParser.parse(json);
            if (!jsonelement.isJsonNull()) {
                return new Dynamic<>(ops, JsonOps.INSTANCE.convertTo(ops, jsonelement));
            }
        } catch (Exception exception) {
            LOGGER.error("Failed to unflatten text component json: {}", json, exception);
        }

        return new Dynamic<>(ops, ops.createString(json));
    }
}
