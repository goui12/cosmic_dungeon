package net.minecraft.util.datafix.fixes;

import com.google.common.collect.Streams;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.Util;
import net.minecraft.util.datafix.LegacyComponentDataFixUtils;

public class DropInvalidSignDataFix extends DataFix {
    private final String entityName;

    public DropInvalidSignDataFix(Schema outputSchema, String entityName) {
        super(outputSchema, false);
        this.entityName = entityName;
    }

    private <T> Dynamic<T> fix(Dynamic<T> data) {
        data = data.update("front_text", DropInvalidSignDataFix::fixText);
        data = data.update("back_text", DropInvalidSignDataFix::fixText);

        for (String s : BlockEntitySignDoubleSidedEditableTextFix.FIELDS_TO_DROP) {
            data = data.remove(s);
        }

        return data;
    }

    private static <T> Dynamic<T> fixText(Dynamic<T> textDynamic) {
        Optional<Stream<Dynamic<T>>> optional = textDynamic.get("filtered_messages").asStreamOpt().result();
        if (optional.isEmpty()) {
            return textDynamic;
        } else {
            Dynamic<T> dynamic = LegacyComponentDataFixUtils.createEmptyComponent(textDynamic.getOps());
            List<Dynamic<T>> list = textDynamic.get("messages").asStreamOpt().result().orElse(Stream.of()).toList();
            List<Dynamic<T>> list1 = Streams.mapWithIndex(optional.get(), (p_294909_, p_296017_) -> {
                Dynamic<T> dynamic1 = p_296017_ < list.size() ? list.get((int)p_296017_) : dynamic;
                return p_294909_.equals(dynamic) ? dynamic1 : p_294909_;
            }).toList();
            return list1.equals(list) ? textDynamic.remove("filtered_messages") : textDynamic.set("filtered_messages", textDynamic.createList(list1.stream()));
        }
    }

    @Override
    public TypeRewriteRule makeRule() {
        Type<?> type = this.getInputSchema().getType(References.BLOCK_ENTITY);
        Type<?> type1 = this.getInputSchema().getChoiceType(References.BLOCK_ENTITY, this.entityName);
        OpticFinder<?> opticfinder = DSL.namedChoice(this.entityName, type1);
        return this.fixTypeEverywhereTyped(
            "DropInvalidSignDataFix for " + this.entityName,
            type,
            p_392821_ -> p_392821_.updateTyped(
                opticfinder,
                type1,
                p_392818_ -> {
                    boolean flag = p_392818_.get(DSL.remainderFinder()).get("_filtered_correct").asBoolean(false);
                    return flag
                        ? p_392818_.update(DSL.remainderFinder(), p_392816_ -> p_392816_.remove("_filtered_correct"))
                        : Util.writeAndReadTypedOrThrow(p_392818_, type1, this::fix);
                }
            )
        );
    }
}
