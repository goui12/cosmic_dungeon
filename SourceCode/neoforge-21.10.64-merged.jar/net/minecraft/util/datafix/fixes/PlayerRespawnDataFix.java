package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class PlayerRespawnDataFix extends DataFix {
    public PlayerRespawnDataFix(Schema outputSchema) {
        super(outputSchema, false);
    }

    @Override
    protected TypeRewriteRule makeRule() {
        return this.fixTypeEverywhereTyped(
            "PlayerRespawnDataFix",
            this.getInputSchema().getType(References.PLAYER),
            p_451009_ -> p_451009_.update(
                DSL.remainderFinder(),
                p_450976_ -> p_450976_.update(
                    "respawn",
                    p_451327_ -> p_451327_.set("dimension", p_451327_.createString(p_451327_.get("dimension").asString("minecraft:overworld")))
                        .set("yaw", p_451327_.createFloat(p_451327_.get("angle").asFloat(0.0F)))
                        .set("pitch", p_451327_.createFloat(0.0F))
                        .remove("angle")
                )
            )
        );
    }
}
