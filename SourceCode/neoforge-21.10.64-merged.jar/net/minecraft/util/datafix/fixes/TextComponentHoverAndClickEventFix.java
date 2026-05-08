package net.minecraft.util.datafix.fixes;

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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.util.datafix.ExtraDataFixUtils;

public class TextComponentHoverAndClickEventFix extends DataFix {
    public TextComponentHoverAndClickEventFix(Schema outputSchema) {
        super(outputSchema, true);
    }

    @Override
    protected TypeRewriteRule makeRule() {
        Type<? extends Pair<String, ?>> type = (Type<? extends Pair<String, ?>>)this.getInputSchema()
            .getType(References.TEXT_COMPONENT)
            .findFieldType("hoverEvent");
        return this.createFixer(this.getInputSchema().getTypeRaw(References.TEXT_COMPONENT), this.getOutputSchema().getType(References.TEXT_COMPONENT), type);
    }

    private <C1, C2, H extends Pair<String, ?>> TypeRewriteRule createFixer(Type<C1> inputComponentType, Type<C2> outputComponentType, Type<H> hoverEventType) {
        Type<Pair<String, Either<Either<String, List<C1>>, Pair<Either<List<C1>, Unit>, Pair<Either<C1, Unit>, Pair<Either<H, Unit>, Dynamic<?>>>>>>> type = DSL.named(
            References.TEXT_COMPONENT.typeName(),
            DSL.or(
                DSL.or(DSL.string(), DSL.list(inputComponentType)),
                DSL.and(
                    DSL.optional(DSL.field("extra", DSL.list(inputComponentType))),
                    DSL.optional(DSL.field("separator", inputComponentType)),
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
            Type<?> type1 = ExtraDataFixUtils.patchSubType(type, type, outputComponentType);
            return this.fixTypeEverywhere(
                "TextComponentHoverAndClickEventFix",
                type,
                outputComponentType,
                p_394396_ -> p_394417_ -> {
                    boolean flag = p_394417_.getSecond().map(p_394057_ -> false, p_394065_ -> {
                        Pair<Either<H, Unit>, Dynamic<?>> pair = p_394065_.getSecond().getSecond();
                        boolean flag1 = pair.getFirst().left().isPresent();
                        boolean flag2 = pair.getSecond().get("clickEvent").result().isPresent();
                        return flag1 || flag2;
                    });
                    return (C2)(!flag
                        ? p_394417_
                        : Util.writeAndReadTypedOrThrow(
                                ExtraDataFixUtils.cast(type1, p_394417_, p_394396_), outputComponentType, TextComponentHoverAndClickEventFix::fixTextComponent
                            )
                            .getValue());
                }
            );
        }
    }

    private static Dynamic<?> fixTextComponent(Dynamic<?> data) {
        return data.renameAndFixField("hoverEvent", "hover_event", TextComponentHoverAndClickEventFix::fixHoverEvent)
            .renameAndFixField("clickEvent", "click_event", TextComponentHoverAndClickEventFix::fixClickEvent);
    }

    private static Dynamic<?> copyFields(Dynamic<?> newData, Dynamic<?> oldData, String... fields) {
        for (String s : fields) {
            newData = Dynamic.copyField(oldData, s, newData, s);
        }

        return newData;
    }

    private static Dynamic<?> fixHoverEvent(Dynamic<?> data) {
        String s = data.get("action").asString("");

        return switch (s) {
            case "show_text" -> data.renameField("contents", "value");
            case "show_item" -> {
                Dynamic<?> dynamic1 = data.get("contents").orElseEmptyMap();
                Optional<String> optional = dynamic1.asString().result();
                yield optional.isPresent()
                    ? data.renameField("contents", "id")
                    : copyFields(data.remove("contents"), dynamic1, "id", "count", "components");
            }
            case "show_entity" -> {
                Dynamic<?> dynamic = data.get("contents").orElseEmptyMap();
                yield copyFields(data.remove("contents"), dynamic, "id", "type", "name").renameField("id", "uuid").renameField("type", "id");
            }
            default -> data;
        };
    }

    @Nullable
    private static <T> Dynamic<T> fixClickEvent(Dynamic<T> data) {
        String s = data.get("action").asString("");
        String s1 = data.get("value").asString("");

        return switch (s) {
            case "open_url" -> !validateUri(s1) ? null : data.renameField("value", "url");
            case "open_file" -> data.renameField("value", "path");
            case "run_command", "suggest_command" -> !validateChat(s1) ? null : data.renameField("value", "command");
            case "change_page" -> {
                Integer integer = data.get("value").result().map(TextComponentHoverAndClickEventFix::parseOldPage).orElse(null);
                if (integer == null) {
                    yield null;
                } else {
                    int i = Math.max(integer, 1);
                    yield data.remove("value").set("page", data.createInt(i));
                }
            }
            default -> data;
        };
    }

    @Nullable
    private static Integer parseOldPage(Dynamic<?> data) {
        Optional<Number> optional = data.asNumber().result();
        if (optional.isPresent()) {
            return optional.get().intValue();
        } else {
            try {
                return Integer.parseInt(data.asString(""));
            } catch (Exception exception) {
                return null;
            }
        }
    }

    private static boolean validateUri(String p_uri) {
        try {
            URI uri = new URI(p_uri);
            String s = uri.getScheme();
            if (s == null) {
                return false;
            } else {
                String s1 = s.toLowerCase(Locale.ROOT);
                return "http".equals(s1) || "https".equals(s1);
            }
        } catch (URISyntaxException urisyntaxexception) {
            return false;
        }
    }

    private static boolean validateChat(String chat) {
        for (int i = 0; i < chat.length(); i++) {
            char c0 = chat.charAt(i);
            if (c0 == 167 || c0 < ' ' || c0 == 127) {
                return false;
            }
        }

        return true;
    }
}
