package net.minecraft.client.renderer.item.properties.select;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.SelectItemModel;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record ComponentContents<T>(DataComponentType<T> componentType) implements SelectItemModelProperty<T> {
    private static final SelectItemModelProperty.Type<? extends ComponentContents<?>, ?> TYPE = createType();

    private static <T> SelectItemModelProperty.Type<ComponentContents<T>, T> createType() {
        Codec<? extends DataComponentType<?>> codec = BuiltInRegistries.DATA_COMPONENT_TYPE
            .byNameCodec()
            .validate(p_398049_ -> p_398049_.isTransient() ? DataResult.error(() -> "Component can't be serialized") : DataResult.success(p_398049_));
        MapCodec<SelectItemModel.UnbakedSwitch<ComponentContents<T>, T>> mapcodec = ((Codec<DataComponentType<T>>)codec).dispatchMap(
            "component",
            p_397332_ -> p_397332_.property().componentType,
            p_397367_ -> SelectItemModelProperty.Type.createCasesFieldCodec(p_397367_.codecOrThrow())
                .xmap(
                    p_397778_ -> new SelectItemModel.UnbakedSwitch<>(
                        new ComponentContents<>((DataComponentType<T>)p_397367_), (List<SelectItemModel.SwitchCase<T>>)p_397778_
                    ),
                    SelectItemModel.UnbakedSwitch::cases
                )
        );
        return new SelectItemModelProperty.Type<>(mapcodec);
    }

    public static <T> SelectItemModelProperty.Type<ComponentContents<T>, T> castType() {
        return (SelectItemModelProperty.Type<ComponentContents<T>, T>)TYPE;
    }

    @Nullable
    @Override
    public T get(ItemStack p_397602_, @Nullable ClientLevel p_397414_, @Nullable LivingEntity p_397883_, int p_397032_, ItemDisplayContext p_397548_) {
        return p_397602_.get(this.componentType);
    }

    @Override
    public SelectItemModelProperty.Type<ComponentContents<T>, T> type() {
        return castType();
    }

    @Override
    public Codec<T> valueCodec() {
        return this.componentType.codecOrThrow();
    }
}
