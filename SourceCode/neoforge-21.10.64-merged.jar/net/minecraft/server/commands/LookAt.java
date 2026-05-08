package net.minecraft.server.commands;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@FunctionalInterface
public interface LookAt {
    void perform(CommandSourceStack source, Entity entity);

    public record LookAtEntity(Entity entity, EntityAnchorArgument.Anchor anchor) implements LookAt {
        @Override
        public void perform(CommandSourceStack p_380255_, Entity p_379889_) {
            if (p_379889_ instanceof ServerPlayer serverplayer) {
                serverplayer.lookAt(p_380255_.getAnchor(), this.entity, this.anchor);
            } else {
                p_379889_.lookAt(p_380255_.getAnchor(), this.anchor.apply(this.entity));
            }
        }
    }

    public record LookAtPosition(Vec3 position) implements LookAt {
        @Override
        public void perform(CommandSourceStack p_379815_, Entity p_379917_) {
            p_379917_.lookAt(p_379815_.getAnchor(), this.position);
        }
    }
}
