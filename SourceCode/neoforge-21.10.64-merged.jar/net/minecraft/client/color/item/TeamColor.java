package net.minecraft.client.color.item;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.ARGB;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.scores.Team;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record TeamColor(int defaultColor) implements ItemTintSource {
    public static final MapCodec<TeamColor> MAP_CODEC = RecordCodecBuilder.mapCodec(
        p_390473_ -> p_390473_.group(ExtraCodecs.RGB_COLOR_CODEC.fieldOf("default").forGetter(TeamColor::defaultColor)).apply(p_390473_, TeamColor::new)
    );

    @Override
    public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity) {
        if (entity != null) {
            Team team = entity.getTeam();
            if (team != null) {
                ChatFormatting chatformatting = team.getColor();
                if (chatformatting.getColor() != null) {
                    return ARGB.opaque(chatformatting.getColor());
                }
            }
        }

        return ARGB.opaque(this.defaultColor);
    }

    @Override
    public MapCodec<TeamColor> type() {
        return MAP_CODEC;
    }
}
