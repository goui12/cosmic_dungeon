package net.minecraft.core.component.predicates;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.advancements.critereon.CollectionPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.advancements.critereon.SingleComponentItemPredicate;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.component.ItemAttributeModifiers;

public record AttributeModifiersPredicate(Optional<CollectionPredicate<ItemAttributeModifiers.Entry, AttributeModifiersPredicate.EntryPredicate>> modifiers)
    implements SingleComponentItemPredicate<ItemAttributeModifiers> {
    public static final Codec<AttributeModifiersPredicate> CODEC = RecordCodecBuilder.create(
        p_399875_ -> p_399875_.group(
                CollectionPredicate.<ItemAttributeModifiers.Entry, AttributeModifiersPredicate.EntryPredicate>codec(
                        AttributeModifiersPredicate.EntryPredicate.CODEC
                    )
                    .optionalFieldOf("modifiers")
                    .forGetter(AttributeModifiersPredicate::modifiers)
            )
            .apply(p_399875_, AttributeModifiersPredicate::new)
    );

    @Override
    public DataComponentType<ItemAttributeModifiers> componentType() {
        return DataComponents.ATTRIBUTE_MODIFIERS;
    }

    public boolean matches(ItemAttributeModifiers p_399563_) {
        return !this.modifiers.isPresent() || this.modifiers.get().test(p_399563_.modifiers());
    }

    @Override
    public boolean matches(net.minecraft.core.component.DataComponentGetter getter) {
        if (getter instanceof net.minecraft.world.item.ItemStack stack) {
            return this.matches(stack.getAttributeModifiers());
        }
        return SingleComponentItemPredicate.super.matches(getter);
    }

    public record EntryPredicate(
        Optional<HolderSet<Attribute>> attribute,
        Optional<ResourceLocation> id,
        MinMaxBounds.Doubles amount,
        Optional<AttributeModifier.Operation> operation,
        Optional<EquipmentSlotGroup> slot
    ) implements Predicate<ItemAttributeModifiers.Entry> {
        public static final Codec<AttributeModifiersPredicate.EntryPredicate> CODEC = RecordCodecBuilder.create(
            p_400067_ -> p_400067_.group(
                    RegistryCodecs.homogeneousList(Registries.ATTRIBUTE)
                        .optionalFieldOf("attribute")
                        .forGetter(AttributeModifiersPredicate.EntryPredicate::attribute),
                    ResourceLocation.CODEC.optionalFieldOf("id").forGetter(AttributeModifiersPredicate.EntryPredicate::id),
                    MinMaxBounds.Doubles.CODEC
                        .optionalFieldOf("amount", MinMaxBounds.Doubles.ANY)
                        .forGetter(AttributeModifiersPredicate.EntryPredicate::amount),
                    AttributeModifier.Operation.CODEC.optionalFieldOf("operation").forGetter(AttributeModifiersPredicate.EntryPredicate::operation),
                    EquipmentSlotGroup.CODEC.optionalFieldOf("slot").forGetter(AttributeModifiersPredicate.EntryPredicate::slot)
                )
                .apply(p_400067_, AttributeModifiersPredicate.EntryPredicate::new)
        );

        public boolean test(ItemAttributeModifiers.Entry p_399885_) {
            if (this.attribute.isPresent() && !this.attribute.get().contains(p_399885_.attribute())) {
                return false;
            } else if (this.id.isPresent() && !this.id.get().equals(p_399885_.modifier().id())) {
                return false;
            } else if (!this.amount.matches(p_399885_.modifier().amount())) {
                return false;
            } else {
                return this.operation.isPresent() && this.operation.get() != p_399885_.modifier().operation()
                    ? false
                    : !this.slot.isPresent() || this.slot.get() == p_399885_.slot();
            }
        }
    }
}
