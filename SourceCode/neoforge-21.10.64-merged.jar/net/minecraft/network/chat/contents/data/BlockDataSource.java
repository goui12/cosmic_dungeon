package net.minecraft.network.chat.contents.data;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

public record BlockDataSource(String posPattern, @Nullable Coordinates compiledPos) implements DataSource {
    public static final MapCodec<BlockDataSource> MAP_CODEC = RecordCodecBuilder.mapCodec(
        p_443129_ -> p_443129_.group(Codec.STRING.fieldOf("block").forGetter(BlockDataSource::posPattern)).apply(p_443129_, BlockDataSource::new)
    );

    public BlockDataSource(String p_442777_) {
        this(p_442777_, compilePos(p_442777_));
    }

    @Nullable
    private static Coordinates compilePos(String posPattern) {
        try {
            return BlockPosArgument.blockPos().parse(new StringReader(posPattern));
        } catch (CommandSyntaxException commandsyntaxexception) {
            return null;
        }
    }

    @Override
    public Stream<CompoundTag> getData(CommandSourceStack source) {
        if (this.compiledPos != null) {
            ServerLevel serverlevel = source.getLevel();
            BlockPos blockpos = this.compiledPos.getBlockPos(source);
            if (serverlevel.isLoaded(blockpos)) {
                BlockEntity blockentity = serverlevel.getBlockEntity(blockpos);
                if (blockentity != null) {
                    return Stream.of(blockentity.saveWithFullMetadata(source.registryAccess()));
                }
            }
        }

        return Stream.empty();
    }

    @Override
    public MapCodec<BlockDataSource> codec() {
        return MAP_CODEC;
    }

    @Override
    public String toString() {
        return "block=" + this.posPattern;
    }

    @Override
    public boolean equals(Object other) {
        return this == other ? true : other instanceof BlockDataSource blockdatasource && this.posPattern.equals(blockdatasource.posPattern);
    }

    @Override
    public int hashCode() {
        return this.posPattern.hashCode();
    }
}
