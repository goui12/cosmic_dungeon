package net.minecraft.util.datafix.fixes;

import com.google.gson.JsonElement;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JavaOps;
import com.mojang.serialization.JsonOps;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.Util;
import net.minecraft.util.GsonHelper;

public class LegacyHoverEventFix extends DataFix {
    public LegacyHoverEventFix(Schema outputSchema) {
        super(outputSchema, false);
    }

    @Override
    protected TypeRewriteRule makeRule() {
        Type<? extends Pair<String, ?>> type = (Type<? extends Pair<String, ?>>)this.getInputSchema()
            .getType(References.TEXT_COMPONENT)
            .findFieldType("hoverEvent");
        return this.createFixer(this.getInputSchema().getTypeRaw(References.TEXT_COMPONENT), type);
    }

    private <C, H extends Pair<String, ?>> TypeRewriteRule createFixer(Type<C> componentType, Type<H> hoverEventType) {
        Type<Pair<String, Either<Either<String, List<C>>, Pair<Either<List<C>, Unit>, Pair<Either<C, Unit>, Pair<Either<H, Unit>, Dynamic<?>>>>>>> type = DSL.named(
            References.TEXT_COMPONENT.typeName(),
            DSL.or(
                DSL.or(DSL.string(), DSL.list(componentType)),
                DSL.and(
                    DSL.optional(DSL.field("extra", DSL.list(componentType))),
                    DSL.optional(DSL.field("separator", componentType)),
                    DSL.optional(DSL.field("hoverEvent", hoverEventType)),
                    DSL.remainderType()
                )
            )
        );
        if (!type.equals(this.getInputSchema().getType(References.TEXT_COMPONENT))) {
            throw new IllegalStateException(
                "Text component type did not match, expected " + type + " but got " + this.getInputSchema().getType(References.TEXT_COMPONENT)
            );
        } else {
            return this.fixTypeEverywhere(
                "LegacyHoverEventFix",
                type,
                p_394584_ -> p_394164_ -> p_394164_.mapSecond(
                    p_393535_ -> p_393535_.mapRight(p_394347_ -> p_394347_.mapSecond(p_393557_ -> p_393557_.mapSecond(p_394017_ -> {
                        Dynamic<?> dynamic = p_394017_.getSecond();
                        Optional<? extends Dynamic<?>> optional = dynamic.get("hoverEvent").result();
                        if (optional.isEmpty()) {
                            return p_394017_;
                        } else {
                            Optional<? extends Dynamic<?>> optional1 = optional.get().get("value").result();
                            if (optional1.isEmpty()) {
                                return p_394017_;
                            } else {
                                String s = p_394017_.getFirst().left().map(Pair::getFirst).orElse("");
                                H h = this.fixHoverEvent(hoverEventType, s, (Dynamic<?>)optional.get());
                                return p_394017_.mapFirst(p_393590_ -> Either.left(h));
                            }
                        }
                    })))
                )
            );
        }
    }

    private <H> H fixHoverEvent(Type<H> type, String action, Dynamic<?> data) {
        return "show_text".equals(action) ? fixShowTextHover(type, data) : createPlaceholderHover(type, data);
    }

    private static <H> H fixShowTextHover(Type<H> type, Dynamic<?> data) {
        Dynamic<?> dynamic = data.renameField("value", "contents");
        return Util.readTypedOrThrow(type, dynamic).getValue();
    }

    private static <H> H createPlaceholderHover(Type<H> type, Dynamic<?> data) {
        JsonElement jsonelement = data.convert(JsonOps.INSTANCE).getValue();
        Dynamic<?> dynamic = new Dynamic<>(
            JavaOps.INSTANCE,
            Map.of("action", "show_text", "contents", Map.<String, String>of("text", "Legacy hoverEvent: " + GsonHelper.toStableString(jsonelement)))
        );
        return Util.readTypedOrThrow(type, dynamic).getValue();
    }
}
