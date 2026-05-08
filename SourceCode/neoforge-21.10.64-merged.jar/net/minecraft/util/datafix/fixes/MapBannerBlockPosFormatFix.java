package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.List.ListType;
import com.mojang.serialization.Dynamic;
import net.minecraft.util.datafix.ExtraDataFixUtils;

public class MapBannerBlockPosFormatFix extends DataFix {
    public MapBannerBlockPosFormatFix(Schema outputSchema) {
        super(outputSchema, false);
    }

    @Override
    protected TypeRewriteRule makeRule() {
        Type<?> type = this.getInputSchema().getType(References.SAVED_DATA_MAP_DATA);
        OpticFinder<?> opticfinder = type.findField("data");
        OpticFinder<?> opticfinder1 = opticfinder.type().findField("banners");
        OpticFinder<?> opticfinder2 = DSL.typeFinder(((ListType)opticfinder1.type()).getElement());
        return this.fixTypeEverywhereTyped(
            "MapBannerBlockPosFormatFix",
            type,
            p_392879_ -> p_392879_.updateTyped(
                opticfinder,
                p_392873_ -> p_392873_.updateTyped(
                    opticfinder1,
                    p_392875_ -> p_392875_.updateTyped(
                        opticfinder2,
                        p_325992_ -> p_325992_.update(DSL.remainderFinder(), p_326061_ -> p_326061_.update("Pos", ExtraDataFixUtils::fixBlockPos))
                    )
                )
            )
        );
    }
}
