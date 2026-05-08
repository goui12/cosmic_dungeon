package net.minecraft.client.data.models.model;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import net.minecraft.client.color.item.Constant;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.renderer.item.BlockModelWrapper;
import net.minecraft.client.renderer.item.CompositeModel;
import net.minecraft.client.renderer.item.ConditionalItemModel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.RangeSelectItemModel;
import net.minecraft.client.renderer.item.SelectItemModel;
import net.minecraft.client.renderer.item.SpecialModelWrapper;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.client.renderer.item.properties.conditional.HasComponent;
import net.minecraft.client.renderer.item.properties.conditional.IsUsingItem;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.client.renderer.item.properties.select.ContextDimension;
import net.minecraft.client.renderer.item.properties.select.ItemBlockState;
import net.minecraft.client.renderer.item.properties.select.LocalTime;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ItemModelUtils {
    public static ItemModel.Unbaked plainModel(ResourceLocation model) {
        return new BlockModelWrapper.Unbaked(model, List.of());
    }

    public static ItemModel.Unbaked tintedModel(ResourceLocation model, ItemTintSource... tintSources) {
        return new BlockModelWrapper.Unbaked(model, List.of(tintSources));
    }

    public static ItemTintSource constantTint(int value) {
        return new Constant(value);
    }

    public static ItemModel.Unbaked composite(ItemModel.Unbaked... models) {
        return new CompositeModel.Unbaked(List.of(models));
    }

    public static ItemModel.Unbaked specialModel(ResourceLocation base, SpecialModelRenderer.Unbaked specialModel) {
        return new SpecialModelWrapper.Unbaked(base, specialModel);
    }

    public static RangeSelectItemModel.Entry override(ItemModel.Unbaked model, float threshold) {
        return new RangeSelectItemModel.Entry(threshold, model);
    }

    public static ItemModel.Unbaked rangeSelect(RangeSelectItemModelProperty property, ItemModel.Unbaked fallback, RangeSelectItemModel.Entry... entries) {
        return new RangeSelectItemModel.Unbaked(property, 1.0F, List.of(entries), Optional.of(fallback));
    }

    public static ItemModel.Unbaked rangeSelect(
        RangeSelectItemModelProperty property, float scale, ItemModel.Unbaked fallback, RangeSelectItemModel.Entry... entries
    ) {
        return new RangeSelectItemModel.Unbaked(property, scale, List.of(entries), Optional.of(fallback));
    }

    public static ItemModel.Unbaked rangeSelect(RangeSelectItemModelProperty property, ItemModel.Unbaked fallback, List<RangeSelectItemModel.Entry> entries) {
        return new RangeSelectItemModel.Unbaked(property, 1.0F, entries, Optional.of(fallback));
    }

    public static ItemModel.Unbaked rangeSelect(RangeSelectItemModelProperty property, List<RangeSelectItemModel.Entry> entries) {
        return new RangeSelectItemModel.Unbaked(property, 1.0F, entries, Optional.empty());
    }

    public static ItemModel.Unbaked rangeSelect(RangeSelectItemModelProperty property, float scale, List<RangeSelectItemModel.Entry> entries) {
        return new RangeSelectItemModel.Unbaked(property, scale, entries, Optional.empty());
    }

    public static ItemModel.Unbaked conditional(ConditionalItemModelProperty property, ItemModel.Unbaked onTrue, ItemModel.Unbaked onFalse) {
        return new ConditionalItemModel.Unbaked(property, onTrue, onFalse);
    }

    public static <T> SelectItemModel.SwitchCase<T> when(T value, ItemModel.Unbaked model) {
        return new SelectItemModel.SwitchCase<>(List.of(value), model);
    }

    public static <T> SelectItemModel.SwitchCase<T> when(List<T> values, ItemModel.Unbaked model) {
        return new SelectItemModel.SwitchCase<>(values, model);
    }

    @SafeVarargs
    public static <T> ItemModel.Unbaked select(SelectItemModelProperty<T> property, ItemModel.Unbaked fallback, SelectItemModel.SwitchCase<T>... cases) {
        return select(property, fallback, List.of(cases));
    }

    public static <T> ItemModel.Unbaked select(SelectItemModelProperty<T> property, ItemModel.Unbaked fallback, List<SelectItemModel.SwitchCase<T>> cases) {
        return new SelectItemModel.Unbaked(new SelectItemModel.UnbakedSwitch<>(property, cases), Optional.of(fallback));
    }

    @SafeVarargs
    public static <T> ItemModel.Unbaked select(SelectItemModelProperty<T> property, SelectItemModel.SwitchCase<T>... cases) {
        return select(property, List.of(cases));
    }

    public static <T> ItemModel.Unbaked select(SelectItemModelProperty<T> property, List<SelectItemModel.SwitchCase<T>> cases) {
        return new SelectItemModel.Unbaked(new SelectItemModel.UnbakedSwitch<>(property, cases), Optional.empty());
    }

    public static ConditionalItemModelProperty isUsingItem() {
        return new IsUsingItem();
    }

    public static ConditionalItemModelProperty hasComponent(DataComponentType<?> componentType) {
        return new HasComponent(componentType, false);
    }

    public static ItemModel.Unbaked inOverworld(ItemModel.Unbaked inOverworldModel, ItemModel.Unbaked notInOverworldModel) {
        return select(new ContextDimension(), notInOverworldModel, when(Level.OVERWORLD, inOverworldModel));
    }

    public static <T extends Comparable<T>> ItemModel.Unbaked selectBlockItemProperty(
        Property<T> property, ItemModel.Unbaked fallback, Map<T, ItemModel.Unbaked> modelMap
    ) {
        List<SelectItemModel.SwitchCase<String>> list = modelMap.entrySet().stream().sorted(Entry.comparingByKey()).map(p_388831_ -> {
            String s = property.getName(p_388831_.getKey());
            return new SelectItemModel.SwitchCase<>(List.of(s), p_388831_.getValue());
        }).toList();
        return select(new ItemBlockState(property.getName()), fallback, list);
    }

    public static ItemModel.Unbaked isXmas(ItemModel.Unbaked xmasModel, ItemModel.Unbaked normalModel) {
        return select(LocalTime.create("MM-dd", "", Optional.empty()), normalModel, List.of(when(List.of("12-24", "12-25", "12-26"), xmasModel)));
    }
}
