package net.goui.cosmicdungeon.npc.tamsin;

import java.util.UUID;
import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.npc.NpcIdentityData;
import net.goui.cosmicdungeon.npc.NpcIdentityService;
import net.goui.cosmicdungeon.vendor.VendorAssignmentService;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Derived visual state only. UUID bindings remain the sole authority for Tamsin's role. */
@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID)
public final class TamsinAppearance {
    public static final TamsinAppearance INSTANCE = new TamsinAppearance();
    private final DeferredRegister<AttachmentType<?>> attachments =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, CosmicDungeonMod.MOD_ID);
    private final DeferredHolder<AttachmentType<?>, AttachmentType<Boolean>> appearance =
            attachments.register("tamsin_appearance",
                    () -> AttachmentType.builder(() -> false).sync(ByteBufCodecs.BOOL).build());

    private TamsinAppearance() {}

    public void register(IEventBus bus) {
        attachments.register(bus);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void joined(EntityJoinLevelEvent event) {
        if (!event.isCanceled()) INSTANCE.refresh(event.getEntity());
    }

    public boolean isApplied(IAttachmentHolder holder) {
        // Reads must not create default attachments on every ordinary rendered villager.
        return Boolean.TRUE.equals(holder.getExistingDataOrNull(appearance));
    }

    public void refresh(Entity entity) {
        if (!(entity instanceof Villager) || !(entity.level() instanceof ServerLevel level)) return;
        var data = TamsinData.get(level.getServer());
        String owner = NpcIdentityData.get(level.getServer()).owner(NpcIdentityService.TAMSIN);
        boolean selected = !entity.isRemoved()
                && !VendorAssignmentService.hasAssignedProfile(entity)
                && !entity.getPersistentData().contains("cosmicdungeon_d1_watson_run")
                && matchesBinding(data, entity.getUUID(), owner);
        apply(entity, selected);
    }

    /** Used after an explicit unbind; indexed UUID lookups never load a chunk. */
    public void refresh(MinecraftServer server, UUID npc) {
        for (var level : server.getAllLevels()) {
            Entity entity = level.getEntity(npc);
            if (entity != null) refresh(entity);
        }
    }

    boolean matchesBinding(TamsinData data, UUID npc, String owner) {
        return data.binding(npc) != null && (owner == null || owner.equals(npc.toString()));
    }

    void apply(IAttachmentHolder holder, boolean selected) {
        if (selected) {
            if (!isApplied(holder)) holder.setData(appearance, true);
        } else if (holder.hasData(appearance)) {
            holder.removeData(appearance);
        }
    }
}
