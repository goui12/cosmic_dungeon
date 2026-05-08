package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class ForcedChunkToTicketFix extends DataFix {
    public ForcedChunkToTicketFix(Schema outputSchema) {
        super(outputSchema, false);
    }

    @Override
    protected TypeRewriteRule makeRule() {
        return this.fixTypeEverywhereTyped(
            "ForcedChunkToTicketFix",
            this.getInputSchema().getType(References.SAVED_DATA_TICKETS),
            p_394265_ -> p_394265_.update(
                DSL.remainderFinder(),
                p_394249_ -> p_394249_.update(
                    "data",
                    p_393474_ -> p_393474_.renameAndFixField(
                        "Forced",
                        "tickets",
                        p_393711_ -> p_393711_.createList(
                            p_393711_.asLongStream()
                                .mapToObj(
                                    p_393760_ -> p_394249_.emptyMap()
                                        .set("type", p_394249_.createString("minecraft:forced"))
                                        .set("level", p_394249_.createInt(31))
                                        .set("ticks_left", p_394249_.createLong(0L))
                                        .set("chunk_pos", p_394249_.createLong(p_393760_))
                                )
                        )
                    )
                )
            )
        );
    }
}
