package net.goui.cosmicdungeon.playerclass.theurgist;

import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;

/** Exact native positive pools; authority, resource debit and physical delivery belong to the caller. */
public final class TheurgistPotionCatalog {
    private static final List<Holder<Potion>> NORMAL=List.of(Potions.NIGHT_VISION,Potions.INVISIBILITY,
            Potions.FIRE_RESISTANCE,Potions.SWIFTNESS,Potions.HEALING,Potions.REGENERATION,Potions.STRENGTH,Potions.LUCK);
    private static final List<Holder<Potion>> EPIC=List.of(Potions.STRONG_SWIFTNESS,Potions.STRONG_HEALING,
            Potions.STRONG_REGENERATION,Potions.STRONG_STRENGTH);
    private TheurgistPotionCatalog(){}
    public static int cost(boolean epic){return epic?40:20;}
    public static List<Holder<Potion>> pool(boolean epic){return epic?EPIC:NORMAL;}
    public static ItemStack create(boolean epic,RandomSource random){
        var pool=pool(epic);
        return PotionContents.createItemStack(Items.SPLASH_POTION,pool.get(random.nextInt(pool.size())));
    }
}
