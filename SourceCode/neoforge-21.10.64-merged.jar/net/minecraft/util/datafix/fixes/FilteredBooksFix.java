package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import net.minecraft.Util;

public class FilteredBooksFix extends ItemStackTagFix {
    public FilteredBooksFix(Schema outputSchema) {
        super(
            outputSchema,
            "Remove filtered text from books",
            p_216664_ -> p_216664_.equals("minecraft:writable_book") || p_216664_.equals("minecraft:written_book")
        );
    }

    @Override
    protected Typed<?> fixItemStackTag(Typed<?> data) {
        return Util.writeAndReadTypedOrThrow(data, data.getType(), p_392830_ -> p_392830_.remove("filtered_title").remove("filtered_pages"));
    }
}
