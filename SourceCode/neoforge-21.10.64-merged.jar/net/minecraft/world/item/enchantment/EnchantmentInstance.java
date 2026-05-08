package net.minecraft.world.item.enchantment;

import net.minecraft.core.Holder;

/**
 * Defines an immutable instance of an enchantment and its level.
 * @param enchantment The enchantment being represented.
 * @param level The level of the enchantment.
 */
public record EnchantmentInstance(Holder<Enchantment> enchantment, int level) {
    public int weight() {
        return this.enchantment().value().getWeight();
    }
}
