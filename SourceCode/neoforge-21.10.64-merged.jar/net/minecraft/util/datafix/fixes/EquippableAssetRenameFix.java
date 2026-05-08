package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;

public class EquippableAssetRenameFix extends DataFix {
    public EquippableAssetRenameFix(Schema outputSchema) {
        super(outputSchema, true);
    }

    @Override
    protected TypeRewriteRule makeRule() {
        Type<?> type = this.getInputSchema().getType(References.DATA_COMPONENTS);
        OpticFinder<?> opticfinder = type.findField("minecraft:equippable");
        return this.fixTypeEverywhereTyped(
            "equippable asset rename fix",
            type,
            p_387324_ -> p_387324_.updateTyped(
                opticfinder, p_387135_ -> p_387135_.update(DSL.remainderFinder(), p_387159_ -> p_387159_.renameField("model", "asset_id"))
            )
        );
    }
}
