package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.stream.IntStream;

public class WorldSpawnDataFix extends DataFix {
    public WorldSpawnDataFix(Schema outputSchema) {
        super(outputSchema, false);
    }

    @Override
    protected TypeRewriteRule makeRule() {
        return this.fixTypeEverywhereTyped(
            "WorldSpawnDataFix",
            this.getInputSchema().getType(References.LEVEL),
            p_451117_ -> p_451117_.update(
                DSL.remainderFinder(),
                p_451264_ -> {
                    int i = p_451264_.get("SpawnX").asInt(0);
                    int j = p_451264_.get("SpawnY").asInt(0);
                    int k = p_451264_.get("SpawnZ").asInt(0);
                    float f = p_451264_.get("SpawnAngle").asFloat(0.0F);
                    Dynamic<?> dynamic = p_451264_.emptyMap()
                        .set("dimension", p_451264_.createString("minecraft:overworld"))
                        .set("pos", p_451264_.createIntList(IntStream.of(i, j, k)))
                        .set("yaw", p_451264_.createFloat(f))
                        .set("pitch", p_451264_.createFloat(0.0F));
                    p_451264_ = p_451264_.remove("SpawnX").remove("SpawnY").remove("SpawnZ").remove("SpawnAngle");
                    return p_451264_.set("spawn", dynamic);
                }
            )
        );
    }
}
