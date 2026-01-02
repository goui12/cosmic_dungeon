// file: net/goui/cosmicdungeon/playerclass/api/ClassCloneEvents.java
package net.goui.cosmicdungeon.playerclass.api;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID)
public final class ClassCloneEvents {
    private ClassCloneEvents() {}

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!(event.getOriginal() instanceof ServerPlayer oldSp)) return;
        if (!(event.getEntity() instanceof ServerPlayer newSp)) return;

        // Copy our cosmicdungeon root from old -> new
        CompoundTag oldPd   = oldSp.getPersistentData();
        CompoundTag oldRoot = oldPd.getCompoundOrEmpty(ClassData.ROOT_TAG);

        // If we never stored anything, nothing to do
        if (oldRoot.isEmpty()) {
            return;
        }

        // Clone the root as-is (we don't have temporary vs permanent right now)
        CompoundTag newRoot = oldRoot.copy();

        // Attach to the new player
        CompoundTag newPd = newSp.getPersistentData();
        newPd.put(ClassData.ROOT_TAG, newRoot);

        // Re-sync to client so HUD/overlays/inventory override kick back in
        ClassNet.sendFullTo(newSp);
    }
}
