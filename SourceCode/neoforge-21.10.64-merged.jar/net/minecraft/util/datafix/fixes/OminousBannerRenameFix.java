package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import net.minecraft.Util;

public class OminousBannerRenameFix extends ItemStackTagFix {
    public OminousBannerRenameFix(Schema schema) {
        super(schema, "OminousBannerRenameFix", p_216698_ -> p_216698_.equals("minecraft:white_banner"));
    }

    private <T> Dynamic<T> fixItemStackTag(Dynamic<T> data) {
        return data.update(
            "display",
            p_392900_ -> p_392900_.update(
                "Name",
                p_392899_ -> {
                    Optional<String> optional = p_392899_.asString().result();
                    return optional.isPresent()
                        ? p_392899_.createString(
                            optional.get().replace("\"translate\":\"block.minecraft.illager_banner\"", "\"translate\":\"block.minecraft.ominous_banner\"")
                        )
                        : p_392899_;
                }
            )
        );
    }

    @Override
    protected Typed<?> fixItemStackTag(Typed<?> data) {
        return Util.writeAndReadTypedOrThrow(data, data.getType(), this::fixItemStackTag);
    }
}
