// LitColoredAmethystBlock.java
package net.goui.cosmicdungeon.block.amethyst;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class LitColoredAmethystBlock extends Block {

    // keep your existing no-args if you want
    public LitColoredAmethystBlock() {
        super(BlockBehaviour.Properties.of()
                .lightLevel(s -> 15));
        // IMPORTANT: do NOT call .dropsLike(...) or touch registry holders here
    }

    // NEW: properties-accepting ctor so we can set per-color props at registration time
    public LitColoredAmethystBlock(BlockBehaviour.Properties props) {
        super(props);
        // also: no registry access here
    }
}