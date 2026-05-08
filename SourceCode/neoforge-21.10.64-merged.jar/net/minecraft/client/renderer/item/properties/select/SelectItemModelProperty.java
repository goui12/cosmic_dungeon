package net.minecraft.client.renderer.item.properties.select;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.SelectItemModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface SelectItemModelProperty<T> {
    @Nullable
    T get(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed, ItemDisplayContext displayContext);

    Codec<T> valueCodec();

    SelectItemModelProperty.Type<? extends SelectItemModelProperty<T>, T> type();

    @OnlyIn(Dist.CLIENT)
    public record Type<P extends SelectItemModelProperty<T>, T>(MapCodec<SelectItemModel.UnbakedSwitch<P, T>> switchCodec) {
        public static <P extends SelectItemModelProperty<T>, T> SelectItemModelProperty.Type<P, T> create(MapCodec<P> mapCodec, Codec<T> codec) {
            MapCodec<SelectItemModel.UnbakedSwitch<P, T>> mapcodec = RecordCodecBuilder.mapCodec(
                p_396319_ -> p_396319_.group(
                        mapCodec.forGetter(SelectItemModel.UnbakedSwitch::property),
                        createCasesFieldCodec(codec).forGetter(SelectItemModel.UnbakedSwitch::cases)
                    )
                    .apply(p_396319_, SelectItemModel.UnbakedSwitch::new)
            );
            return new SelectItemModelProperty.Type<>(mapcodec);
        }

        public static <T> MapCodec<List<SelectItemModel.SwitchCase<T>>> createCasesFieldCodec(Codec<T> codec) {
            return SelectItemModel.SwitchCase.codec(codec).listOf().validate(SelectItemModelProperty.Type::validateCases).fieldOf("cases");
        }

        private static <T> DataResult<List<SelectItemModel.SwitchCase<T>>> validateCases(List<SelectItemModel.SwitchCase<T>> cases) {
            if (cases.isEmpty()) {
                return DataResult.error(() -> "Empty case list");
            } else {
                Multiset<T> multiset = HashMultiset.create();

                for (SelectItemModel.SwitchCase<T> switchcase : cases) {
                    multiset.addAll(switchcase.values());
                }

                return multiset.size() != multiset.entrySet().size()
                    ? DataResult.error(
                        () -> "Duplicate case conditions: "
                            + multiset.entrySet()
                                .stream()
                                .filter(p_388701_ -> p_388701_.getCount() > 1)
                                .map(p_387521_ -> p_387521_.getElement().toString())
                                .collect(Collectors.joining(", "))
                    )
                    : DataResult.success(cases);
            }
        }
    }
}
