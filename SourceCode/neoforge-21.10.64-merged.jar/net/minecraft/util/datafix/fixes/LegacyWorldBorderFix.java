package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class LegacyWorldBorderFix extends DataFix {
    public LegacyWorldBorderFix(Schema outputSchema) {
        super(outputSchema, false);
    }

    @Override
    protected TypeRewriteRule makeRule() {
        return this.fixTypeEverywhereTyped(
            "LegacyWorldBorderFix",
            this.getInputSchema().getType(References.LEVEL),
            p_445784_ -> p_445784_.update(
                DSL.remainderFinder(),
                p_445808_ -> {
                    Dynamic<?> dynamic = p_445808_.emptyMap()
                        .set("center_x", p_445808_.createDouble(p_445808_.get("BorderCenterX").asDouble(0.0)))
                        .set("center_z", p_445808_.createDouble(p_445808_.get("BorderCenterZ").asDouble(0.0)))
                        .set("size", p_445808_.createDouble(p_445808_.get("BorderSize").asDouble(5.999997E7F)))
                        .set("lerp_time", p_445808_.createLong(p_445808_.get("BorderSizeLerpTime").asLong(0L)))
                        .set("lerp_target", p_445808_.createDouble(p_445808_.get("BorderSizeLerpTarget").asDouble(0.0)))
                        .set("safe_zone", p_445808_.createDouble(p_445808_.get("BorderSafeZone").asDouble(5.0)))
                        .set("damage_per_block", p_445808_.createDouble(p_445808_.get("BorderDamagePerBlock").asDouble(0.2)))
                        .set("warning_blocks", p_445808_.createInt(p_445808_.get("BorderWarningBlocks").asInt(5)))
                        .set("warning_time", p_445808_.createInt(p_445808_.get("BorderWarningTime").asInt(15)));
                    p_445808_ = p_445808_.remove("BorderCenterX")
                        .remove("BorderCenterZ")
                        .remove("BorderSize")
                        .remove("BorderSizeLerpTime")
                        .remove("BorderSizeLerpTarget")
                        .remove("BorderSafeZone")
                        .remove("BorderDamagePerBlock")
                        .remove("BorderWarningBlocks")
                        .remove("BorderWarningTime");
                    return p_445808_.set("world_border", dynamic);
                }
            )
        );
    }
}
