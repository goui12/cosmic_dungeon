package net.minecraft.client.data.models.blockstates;

import net.minecraft.client.renderer.block.model.BlockModelDefinition;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface BlockModelDefinitionGenerator {
    Block block();

    BlockModelDefinition create();
}
