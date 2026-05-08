package net.minecraft.client.renderer.item.properties.select;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.PrimitiveCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record ItemBlockState(String property) implements SelectItemModelProperty<String> {
    public static final PrimitiveCodec<String> VALUE_CODEC = Codec.STRING;
    public static final SelectItemModelProperty.Type<ItemBlockState, String> TYPE = SelectItemModelProperty.Type.create(
        RecordCodecBuilder.mapCodec(
            p_387614_ -> p_387614_.group(Codec.STRING.fieldOf("block_state_property").forGetter(ItemBlockState::property))
                .apply(p_387614_, ItemBlockState::new)
        ),
        VALUE_CODEC
    );

    @Nullable
    public String get(ItemStack p_387708_, @Nullable ClientLevel p_388625_, @Nullable LivingEntity p_388880_, int p_388899_, ItemDisplayContext p_388088_) {
        BlockItemStateProperties blockitemstateproperties = p_387708_.get(DataComponents.BLOCK_STATE);
        return blockitemstateproperties == null ? null : blockitemstateproperties.properties().get(this.property);
    }

    @Override
    public SelectItemModelProperty.Type<ItemBlockState, String> type() {
        return TYPE;
    }

    @Override
    public Codec<String> valueCodec() {
        return VALUE_CODEC;
    }
}
