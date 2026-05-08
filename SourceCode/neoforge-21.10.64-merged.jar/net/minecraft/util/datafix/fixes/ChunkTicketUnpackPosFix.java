package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.stream.IntStream;

public class ChunkTicketUnpackPosFix extends DataFix {
    private static final long CHUNK_COORD_BITS = 32L;
    private static final long CHUNK_COORD_MASK = 4294967295L;

    public ChunkTicketUnpackPosFix(Schema outputSchema) {
        super(outputSchema, false);
    }

    @Override
    protected TypeRewriteRule makeRule() {
        return this.fixTypeEverywhereTyped(
            "ChunkTicketUnpackPosFix",
            this.getInputSchema().getType(References.SAVED_DATA_TICKETS),
            p_405583_ -> p_405583_.update(
                DSL.remainderFinder(),
                p_405182_ -> p_405182_.update(
                    "data",
                    p_405553_ -> p_405553_.update(
                        "tickets", p_405676_ -> p_405676_.createList(p_405676_.asStream().map(p_405691_ -> p_405691_.update("chunk_pos", p_405415_ -> {
                            long i = p_405415_.asLong(0L);
                            int j = (int)(i & 4294967295L);
                            int k = (int)(i >>> 32 & 4294967295L);
                            return p_405415_.createIntList(IntStream.of(j, k));
                        })))
                    )
                )
            )
        );
    }
}
