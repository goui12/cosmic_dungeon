package net.minecraft.world.entity.ai.attributes;

import com.google.common.collect.Multimap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;

public class AttributeMap {
    private final Map<Holder<Attribute>, AttributeInstance> attributes = new Object2ObjectOpenHashMap<>();
    private final Set<AttributeInstance> attributesToSync = new ObjectOpenHashSet<>();
    private final Set<AttributeInstance> attributesToUpdate = new ObjectOpenHashSet<>();
    private final AttributeSupplier supplier;

    public AttributeMap(AttributeSupplier supplier) {
        this.supplier = supplier;
    }

    private void onAttributeModified(AttributeInstance instance) {
        this.attributesToUpdate.add(instance);
        if (instance.getAttribute().value().isClientSyncable()) {
            this.attributesToSync.add(instance);
        }
    }

    public Set<AttributeInstance> getAttributesToSync() {
        return this.attributesToSync;
    }

    public Set<AttributeInstance> getAttributesToUpdate() {
        return this.attributesToUpdate;
    }

    public Collection<AttributeInstance> getSyncableAttributes() {
        return this.attributes.values().stream().filter(p_315935_ -> p_315935_.getAttribute().value().isClientSyncable()).collect(Collectors.toList());
    }

    @Nullable
    public AttributeInstance getInstance(Holder<Attribute> attribute) {
        return this.attributes.computeIfAbsent(attribute, p_315936_ -> this.supplier.createInstance(this::onAttributeModified, (Holder<Attribute>)p_315936_));
    }

    public boolean hasAttribute(Holder<Attribute> attribute) {
        return this.attributes.get(attribute) != null || this.supplier.hasAttribute(attribute);
    }

    public boolean hasModifier(Holder<Attribute> attribute, ResourceLocation id) {
        AttributeInstance attributeinstance = this.attributes.get(attribute);
        return attributeinstance != null ? attributeinstance.getModifier(id) != null : this.supplier.hasModifier(attribute, id);
    }

    public double getValue(Holder<Attribute> attribute) {
        AttributeInstance attributeinstance = this.attributes.get(attribute);
        return attributeinstance != null ? attributeinstance.getValue() : this.supplier.getValue(attribute);
    }

    public double getBaseValue(Holder<Attribute> attribute) {
        AttributeInstance attributeinstance = this.attributes.get(attribute);
        return attributeinstance != null ? attributeinstance.getBaseValue() : this.supplier.getBaseValue(attribute);
    }

    public double getModifierValue(Holder<Attribute> attribute, ResourceLocation id) {
        AttributeInstance attributeinstance = this.attributes.get(attribute);
        return attributeinstance != null ? attributeinstance.getModifier(id).amount() : this.supplier.getModifierValue(attribute, id);
    }

    public void addTransientAttributeModifiers(Multimap<Holder<Attribute>, AttributeModifier> modifiers) {
        modifiers.forEach((p_351795_, p_351796_) -> {
            AttributeInstance attributeinstance = this.getInstance((Holder<Attribute>)p_351795_);
            if (attributeinstance != null) {
                attributeinstance.removeModifier(p_351796_.id());
                attributeinstance.addTransientModifier(p_351796_);
            }
        });
    }

    public void removeAttributeModifiers(Multimap<Holder<Attribute>, AttributeModifier> modifiers) {
        modifiers.asMap().forEach((p_344297_, p_344298_) -> {
            AttributeInstance attributeinstance = this.attributes.get(p_344297_);
            if (attributeinstance != null) {
                p_344298_.forEach(p_351794_ -> attributeinstance.removeModifier(p_351794_.id()));
            }
        });
    }

    public void assignAllValues(AttributeMap map) {
        map.attributes.values().forEach(p_315934_ -> {
            AttributeInstance attributeinstance = this.getInstance(p_315934_.getAttribute());
            if (attributeinstance != null) {
                attributeinstance.replaceFrom(p_315934_);
            }
        });
    }

    public void assignBaseValues(AttributeMap map) {
        map.attributes.values().forEach(p_348165_ -> {
            AttributeInstance attributeinstance = this.getInstance(p_348165_.getAttribute());
            if (attributeinstance != null) {
                attributeinstance.setBaseValue(p_348165_.getBaseValue());
            }
        });
    }

    public void assignPermanentModifiers(AttributeMap map) {
        map.attributes.values().forEach(p_359721_ -> {
            AttributeInstance attributeinstance = this.getInstance(p_359721_.getAttribute());
            if (attributeinstance != null) {
                attributeinstance.addPermanentModifiers(p_359721_.getPermanentModifiers());
            }
        });
    }

    public boolean resetBaseValue(Holder<Attribute> attribute) {
        if (!this.supplier.hasAttribute(attribute)) {
            return false;
        } else {
            AttributeInstance attributeinstance = this.attributes.get(attribute);
            if (attributeinstance != null) {
                attributeinstance.setBaseValue(this.supplier.getBaseValue(attribute));
            }

            return true;
        }
    }

    public List<AttributeInstance.Packed> pack() {
        List<AttributeInstance.Packed> list = new ArrayList<>(this.attributes.values().size());

        for (AttributeInstance attributeinstance : this.attributes.values()) {
            list.add(attributeinstance.pack());
        }

        return list;
    }

    public void apply(List<AttributeInstance.Packed> attributes) {
        for (AttributeInstance.Packed attributeinstance$packed : attributes) {
            AttributeInstance attributeinstance = this.getInstance(attributeinstance$packed.attribute());
            if (attributeinstance != null) {
                attributeinstance.apply(attributeinstance$packed);
            }
        }
    }
}
