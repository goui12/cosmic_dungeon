package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import java.util.HashMap;
import java.util.Map;

public class PlayerEquipmentFix extends DataFix {
    private static final Map<Integer, String> SLOT_TRANSLATIONS = Map.of(100, "feet", 101, "legs", 102, "chest", 103, "head", -106, "offhand");

    public PlayerEquipmentFix(Schema outputSchema) {
        super(outputSchema, true);
    }

    @Override
    protected TypeRewriteRule makeRule() {
        Type<?> type = this.getInputSchema().getTypeRaw(References.PLAYER);
        Type<?> type1 = this.getOutputSchema().getTypeRaw(References.PLAYER);
        return this.writeFixAndRead("Player Equipment Fix", type, type1, p_401794_ -> {
            Map<Dynamic<?>, Dynamic<?>> map = new HashMap<>();
            p_401794_ = p_401794_.update("Inventory", p_401803_ -> p_401803_.createList(p_401803_.asStream().filter(p_401881_ -> {
                int i = p_401881_.get("Slot").asInt(-1);
                String s = SLOT_TRANSLATIONS.get(i);
                if (s != null) {
                    map.put(p_401803_.createString(s), p_401881_.remove("Slot"));
                }

                return s == null;
            })));
            return p_401794_.set("equipment", p_401794_.createMap(map));
        });
    }
}
