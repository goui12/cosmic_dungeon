package net.minecraft.client.color.block;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.IdMapper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BlockColors {
    private static final int DEFAULT = -1;
    public static final int LILY_PAD_IN_WORLD = -14647248;
    public static final int LILY_PAD_DEFAULT = -9321636;
    // Neo: Use the block instance directly as non-Vanilla block ids are not constant
    private final java.util.Map<Block, BlockColor> blockColors = new java.util.IdentityHashMap<>();
    private final Map<Block, Set<Property<?>>> coloringStates = Maps.newHashMap();

    public static BlockColors createDefault() {
        BlockColors blockcolors = new BlockColors();
        blockcolors.register(
            (p_276233_, p_276234_, p_276235_, p_276236_) -> p_276234_ != null && p_276235_ != null
                ? BiomeColors.getAverageGrassColor(
                    p_276234_, p_276233_.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.UPPER ? p_276235_.below() : p_276235_
                )
                : GrassColor.getDefaultColor(),
            Blocks.LARGE_FERN,
            Blocks.TALL_GRASS
        );
        blockcolors.addColoringState(DoublePlantBlock.HALF, Blocks.LARGE_FERN, Blocks.TALL_GRASS);
        blockcolors.register(
            (p_276237_, p_276238_, p_276239_, p_276240_) -> p_276238_ != null && p_276239_ != null
                ? BiomeColors.getAverageGrassColor(p_276238_, p_276239_)
                : GrassColor.getDefaultColor(),
            Blocks.GRASS_BLOCK,
            Blocks.FERN,
            Blocks.SHORT_GRASS,
            Blocks.POTTED_FERN,
            Blocks.BUSH
        );
        blockcolors.register((p_276241_, p_276242_, p_276243_, p_276244_) -> {
            if (p_276244_ != 0) {
                return p_276242_ != null && p_276243_ != null ? BiomeColors.getAverageGrassColor(p_276242_, p_276243_) : GrassColor.getDefaultColor();
            } else {
                return -1;
            }
        }, Blocks.PINK_PETALS, Blocks.WILDFLOWERS);
        blockcolors.register((p_386206_, p_386207_, p_386208_, p_386209_) -> -10380959, Blocks.SPRUCE_LEAVES);
        blockcolors.register((p_386198_, p_386199_, p_386200_, p_386201_) -> -8345771, Blocks.BIRCH_LEAVES);
        blockcolors.register(
            (p_386202_, p_386203_, p_386204_, p_386205_) -> p_386203_ != null && p_386204_ != null
                ? BiomeColors.getAverageFoliageColor(p_386203_, p_386204_)
                : -12012264,
            Blocks.OAK_LEAVES,
            Blocks.JUNGLE_LEAVES,
            Blocks.ACACIA_LEAVES,
            Blocks.DARK_OAK_LEAVES,
            Blocks.VINE,
            Blocks.MANGROVE_LEAVES
        );
        blockcolors.register(
            (p_406181_, p_406182_, p_406183_, p_406184_) -> p_406182_ != null && p_406183_ != null
                ? BiomeColors.getAverageDryFoliageColor(p_406182_, p_406183_)
                : -10732494,
            Blocks.LEAF_LITTER
        );
        blockcolors.register(
            (p_92621_, p_92622_, p_92623_, p_92624_) -> p_92622_ != null && p_92623_ != null ? BiomeColors.getAverageWaterColor(p_92622_, p_92623_) : -1,
            Blocks.WATER,
            Blocks.BUBBLE_COLUMN,
            Blocks.WATER_CAULDRON
        );
        blockcolors.register(
            (p_92616_, p_92617_, p_92618_, p_92619_) -> RedStoneWireBlock.getColorForPower(p_92616_.getValue(RedStoneWireBlock.POWER)), Blocks.REDSTONE_WIRE
        );
        blockcolors.addColoringState(RedStoneWireBlock.POWER, Blocks.REDSTONE_WIRE);
        blockcolors.register(
            (p_92611_, p_92612_, p_92613_, p_92614_) -> p_92612_ != null && p_92613_ != null ? BiomeColors.getAverageGrassColor(p_92612_, p_92613_) : -1,
            Blocks.SUGAR_CANE
        );
        blockcolors.register((p_92606_, p_92607_, p_92608_, p_92609_) -> -2046180, Blocks.ATTACHED_MELON_STEM, Blocks.ATTACHED_PUMPKIN_STEM);
        blockcolors.register((p_359066_, p_359067_, p_359068_, p_359069_) -> {
            int i = p_359066_.getValue(StemBlock.AGE);
            return ARGB.color(i * 32, 255 - i * 8, i * 4);
        }, Blocks.MELON_STEM, Blocks.PUMPKIN_STEM);
        blockcolors.addColoringState(StemBlock.AGE, Blocks.MELON_STEM, Blocks.PUMPKIN_STEM);
        blockcolors.register((p_92596_, p_92597_, p_92598_, p_92599_) -> p_92597_ != null && p_92598_ != null ? -14647248 : -9321636, Blocks.LILY_PAD);
        net.neoforged.neoforge.client.ClientHooks.onBlockColorsInit(blockcolors);
        return blockcolors;
    }

    public int getColor(BlockState state, Level level, BlockPos pos) {
        BlockColor blockcolor = this.blockColors.get(state.getBlock());
        if (blockcolor != null) {
            return blockcolor.getColor(state, null, null, 0);
        } else {
            MapColor mapcolor = state.getMapColor(level, pos);
            return mapcolor != null ? mapcolor.col : -1;
        }
    }

    public int getColor(BlockState state, @Nullable BlockAndTintGetter level, @Nullable BlockPos pos, int tintIndex) {
        BlockColor blockcolor = this.blockColors.get(state.getBlock());
        return blockcolor == null ? -1 : blockcolor.getColor(state, level, pos, tintIndex);
    }

    /**
 * @deprecated Register via {@link
 *             net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Block
 *             }
 */
    @Deprecated
    public void register(BlockColor blockColor, Block... blocks) {
        for (Block block : blocks) {
            this.blockColors.put(block, blockColor);
        }
    }

    private void addColoringStates(Set<Property<?>> properties, Block... blocks) {
        for (Block block : blocks) {
            this.coloringStates.put(block, properties);
        }
    }

    private void addColoringState(Property<?> property, Block... blocks) {
        this.addColoringStates(ImmutableSet.of(property), blocks);
    }

    public Set<Property<?>> getColoringProperties(Block block) {
        return this.coloringStates.getOrDefault(block, ImmutableSet.of());
    }
}
