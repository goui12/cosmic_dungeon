package net.goui.cosmicdungeon.playerclass.api;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.dungeon.DungeonRunRegistryData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID)
public final class ClassCloneEvents {
    private ClassCloneEvents() {}

    private static final String KEY_PENDING_SELECTOR = "pending_class_selector";
    private static final String KEY_RUN_TEMP = "run_temp";

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!(event.getOriginal() instanceof ServerPlayer oldSp)) return;
        if (!(event.getEntity() instanceof ServerPlayer newSp)) return;

        CompoundTag oldPd = oldSp.getPersistentData();
        CompoundTag oldRoot = oldPd.getCompoundOrEmpty(ClassData.ROOT_TAG);

        if (oldRoot.isEmpty()) {
            return;
        }

        CompoundTag newRoot = oldRoot.copy();

        boolean keepRunTemp = false;
        if (newSp.level() instanceof net.minecraft.server.level.ServerLevel sl) {
            keepRunTemp = DungeonRunRegistryData.get(sl.getServer()).findRunForPlayer(newSp.getUUID()).isPresent();
        }

        if (!keepRunTemp) {
            newRoot.remove(KEY_PENDING_SELECTOR);
            newRoot.remove(KEY_RUN_TEMP);
        }

        CompoundTag newPd = newSp.getPersistentData();
        newPd.put(ClassData.ROOT_TAG, newRoot);

        ClassNet.sendFullTo(newSp);
    }
}