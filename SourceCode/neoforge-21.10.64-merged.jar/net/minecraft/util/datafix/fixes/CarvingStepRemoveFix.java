package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Optional;

public class CarvingStepRemoveFix extends DataFix {
    public CarvingStepRemoveFix(Schema outputSchema) {
        super(outputSchema, false);
    }

    @Override
    protected TypeRewriteRule makeRule() {
        return this.fixTypeEverywhereTyped("CarvingStepRemoveFix", this.getInputSchema().getType(References.CHUNK), CarvingStepRemoveFix::fixChunk);
    }

    private static Typed<?> fixChunk(Typed<?> chunkTag) {
        return chunkTag.update(DSL.remainderFinder(), p_362371_ -> {
            Dynamic<?> dynamic = p_362371_;
            Optional<? extends Dynamic<?>> optional = p_362371_.get("CarvingMasks").result();
            if (optional.isPresent()) {
                Optional<? extends Dynamic<?>> optional1 = optional.get().get("AIR").result();
                if (optional1.isPresent()) {
                    dynamic = p_362371_.set("carving_mask", (Dynamic<?>)optional1.get());
                }
            }

            return dynamic.remove("CarvingMasks");
        });
    }
}
