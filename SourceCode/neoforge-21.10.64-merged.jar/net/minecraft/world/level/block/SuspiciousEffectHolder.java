package net.minecraft.world.level.block;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.component.SuspiciousStewEffects;
import net.minecraft.world.level.ItemLike;

public interface SuspiciousEffectHolder {
    SuspiciousStewEffects getSuspiciousEffects();

    static List<SuspiciousEffectHolder> getAllEffectHolders() {
        return BuiltInRegistries.ITEM.stream().map(SuspiciousEffectHolder::tryGet).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Nullable
    static SuspiciousEffectHolder tryGet(ItemLike item) {
        if (item.asItem() instanceof BlockItem blockitem && blockitem.getBlock() instanceof SuspiciousEffectHolder suspiciouseffectholder1) {
            return suspiciouseffectholder1;
        } else {
            return item.asItem() instanceof SuspiciousEffectHolder suspiciouseffectholder ? suspiciouseffectholder : null;
        }
    }
}
