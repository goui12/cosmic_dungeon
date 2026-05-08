package net.minecraft.client.renderer.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import javax.annotation.Nullable;
import net.minecraft.util.RegistryContextSwapper;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record ClientItem(ItemModel.Unbaked model, ClientItem.Properties properties, @Nullable RegistryContextSwapper registrySwapper) {
    public static final Codec<ClientItem> CODEC = RecordCodecBuilder.create(
        p_390086_ -> p_390086_.group(
                ItemModels.CODEC.fieldOf("model").forGetter(ClientItem::model), ClientItem.Properties.MAP_CODEC.forGetter(ClientItem::properties)
            )
            .apply(p_390086_, ClientItem::new)
    );

    public ClientItem(ItemModel.Unbaked p_387005_, ClientItem.Properties p_390352_) {
        this(p_387005_, p_390352_, null);
    }

    public ClientItem withRegistrySwapper(RegistryContextSwapper registrySwapper) {
        return new ClientItem(this.model, this.properties, registrySwapper);
    }

    @OnlyIn(Dist.CLIENT)
    public record Properties(boolean handAnimationOnSwap, boolean oversizedInGui) {
        public static final ClientItem.Properties DEFAULT = new ClientItem.Properties(true, false);
        public static final MapCodec<ClientItem.Properties> MAP_CODEC = RecordCodecBuilder.mapCodec(
            p_428091_ -> p_428091_.group(
                    Codec.BOOL.optionalFieldOf("hand_animation_on_swap", true).forGetter(ClientItem.Properties::handAnimationOnSwap),
                    Codec.BOOL.optionalFieldOf("oversized_in_gui", false).forGetter(ClientItem.Properties::oversizedInGui)
                )
                .apply(p_428091_, ClientItem.Properties::new)
        );
    }
}
