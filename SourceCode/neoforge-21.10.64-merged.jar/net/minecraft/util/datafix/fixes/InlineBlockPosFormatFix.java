package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.util.datafix.ExtraDataFixUtils;

public class InlineBlockPosFormatFix extends DataFix {
    public InlineBlockPosFormatFix(Schema outputSchema) {
        super(outputSchema, false);
    }

    @Override
    public TypeRewriteRule makeRule() {
        OpticFinder<?> opticfinder = this.entityFinder("minecraft:vex");
        OpticFinder<?> opticfinder1 = this.entityFinder("minecraft:phantom");
        OpticFinder<?> opticfinder2 = this.entityFinder("minecraft:turtle");
        List<OpticFinder<?>> list = List.of(
            this.entityFinder("minecraft:item_frame"),
            this.entityFinder("minecraft:glow_item_frame"),
            this.entityFinder("minecraft:painting"),
            this.entityFinder("minecraft:leash_knot")
        );
        return TypeRewriteRule.seq(
            this.fixTypeEverywhereTyped(
                "InlineBlockPosFormatFix - player",
                this.getInputSchema().getType(References.PLAYER),
                p_404865_ -> p_404865_.update(DSL.remainderFinder(), this::fixPlayer)
            ),
            this.fixTypeEverywhereTyped(
                "InlineBlockPosFormatFix - entity",
                this.getInputSchema().getType(References.ENTITY),
                p_404996_ -> {
                    p_404996_ = p_404996_.update(DSL.remainderFinder(), this::fixLivingEntity)
                        .updateTyped(opticfinder, p_405778_ -> p_405778_.update(DSL.remainderFinder(), this::fixVex))
                        .updateTyped(opticfinder1, p_405644_ -> p_405644_.update(DSL.remainderFinder(), this::fixPhantom))
                        .updateTyped(opticfinder2, p_405664_ -> p_405664_.update(DSL.remainderFinder(), this::fixTurtle));

                    for (OpticFinder<?> opticfinder3 : list) {
                        p_404996_ = p_404996_.updateTyped(opticfinder3, p_404887_ -> p_404887_.update(DSL.remainderFinder(), this::fixBlockAttached));
                    }

                    return p_404996_;
                }
            )
        );
    }

    private OpticFinder<?> entityFinder(String entityName) {
        return DSL.namedChoice(entityName, this.getInputSchema().getChoiceType(References.ENTITY, entityName));
    }

    private Dynamic<?> fixPlayer(Dynamic<?> data) {
        data = this.fixLivingEntity(data);
        Optional<Number> optional = data.get("SpawnX").asNumber().result();
        Optional<Number> optional1 = data.get("SpawnY").asNumber().result();
        Optional<Number> optional2 = data.get("SpawnZ").asNumber().result();
        if (optional.isPresent() && optional1.isPresent() && optional2.isPresent()) {
            Dynamic<?> dynamic = data.createMap(
                Map.of(
                    data.createString("pos"),
                    ExtraDataFixUtils.createBlockPos(data, optional.get().intValue(), optional1.get().intValue(), optional2.get().intValue())
                )
            );
            dynamic = Dynamic.copyField(data, "SpawnAngle", dynamic, "angle");
            dynamic = Dynamic.copyField(data, "SpawnDimension", dynamic, "dimension");
            dynamic = Dynamic.copyField(data, "SpawnForced", dynamic, "forced");
            data = data.remove("SpawnX").remove("SpawnY").remove("SpawnZ").remove("SpawnAngle").remove("SpawnDimension").remove("SpawnForced");
            data = data.set("respawn", dynamic);
        }

        Optional<? extends Dynamic<?>> optional3 = data.get("enteredNetherPosition").result();
        if (optional3.isPresent()) {
            data = data.remove("enteredNetherPosition")
                .set(
                    "entered_nether_pos",
                    data.createList(
                        Stream.of(
                            data.createDouble(optional3.get().get("x").asDouble(0.0)),
                            data.createDouble(optional3.get().get("y").asDouble(0.0)),
                            data.createDouble(optional3.get().get("z").asDouble(0.0))
                        )
                    )
                );
        }

        return data;
    }

    private Dynamic<?> fixLivingEntity(Dynamic<?> data) {
        return ExtraDataFixUtils.fixInlineBlockPos(data, "SleepingX", "SleepingY", "SleepingZ", "sleeping_pos");
    }

    private Dynamic<?> fixVex(Dynamic<?> data) {
        return ExtraDataFixUtils.fixInlineBlockPos(data.renameField("LifeTicks", "life_ticks"), "BoundX", "BoundY", "BoundZ", "bound_pos");
    }

    private Dynamic<?> fixPhantom(Dynamic<?> data) {
        return ExtraDataFixUtils.fixInlineBlockPos(data.renameField("Size", "size"), "AX", "AY", "AZ", "anchor_pos");
    }

    private Dynamic<?> fixTurtle(Dynamic<?> data) {
        data = data.remove("TravelPosX").remove("TravelPosY").remove("TravelPosZ");
        data = ExtraDataFixUtils.fixInlineBlockPos(data, "HomePosX", "HomePosY", "HomePosZ", "home_pos");
        return data.renameField("HasEgg", "has_egg");
    }

    private Dynamic<?> fixBlockAttached(Dynamic<?> data) {
        return ExtraDataFixUtils.fixInlineBlockPos(data, "TileX", "TileY", "TileZ", "block_pos");
    }
}
