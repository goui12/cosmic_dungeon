package net.minecraft.util.debug;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.game.DebugEntityNameGenerator;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringUtil;
import net.minecraft.world.Container;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.gossip.GossipType;
import net.minecraft.world.entity.ai.memory.ExpirableValue;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.schedule.Activity;

public record DebugBrainDump(
    String name,
    String profession,
    int xp,
    float health,
    float maxHealth,
    String inventory,
    boolean wantsGolem,
    int angerLevel,
    List<String> activities,
    List<String> behaviors,
    List<String> memories,
    List<String> gossips,
    Set<BlockPos> pois,
    Set<BlockPos> potentialPois
) {
    public static final StreamCodec<FriendlyByteBuf, DebugBrainDump> STREAM_CODEC = StreamCodec.of(
        (p_449610_, p_449173_) -> p_449173_.write(p_449610_), DebugBrainDump::new
    );

    public DebugBrainDump(FriendlyByteBuf p_449299_) {
        this(
            p_449299_.readUtf(),
            p_449299_.readUtf(),
            p_449299_.readInt(),
            p_449299_.readFloat(),
            p_449299_.readFloat(),
            p_449299_.readUtf(),
            p_449299_.readBoolean(),
            p_449299_.readInt(),
            p_449299_.readList(FriendlyByteBuf::readUtf),
            p_449299_.readList(FriendlyByteBuf::readUtf),
            p_449299_.readList(FriendlyByteBuf::readUtf),
            p_449299_.readList(FriendlyByteBuf::readUtf),
            p_449299_.readCollection(HashSet::new, BlockPos.STREAM_CODEC),
            p_449299_.readCollection(HashSet::new, BlockPos.STREAM_CODEC)
        );
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeUtf(this.name);
        buffer.writeUtf(this.profession);
        buffer.writeInt(this.xp);
        buffer.writeFloat(this.health);
        buffer.writeFloat(this.maxHealth);
        buffer.writeUtf(this.inventory);
        buffer.writeBoolean(this.wantsGolem);
        buffer.writeInt(this.angerLevel);
        buffer.writeCollection(this.activities, FriendlyByteBuf::writeUtf);
        buffer.writeCollection(this.behaviors, FriendlyByteBuf::writeUtf);
        buffer.writeCollection(this.memories, FriendlyByteBuf::writeUtf);
        buffer.writeCollection(this.gossips, FriendlyByteBuf::writeUtf);
        buffer.writeCollection(this.pois, BlockPos.STREAM_CODEC);
        buffer.writeCollection(this.potentialPois, BlockPos.STREAM_CODEC);
    }

    public static DebugBrainDump takeBrainDump(ServerLevel level, LivingEntity entity) {
        String s = DebugEntityNameGenerator.getEntityName(entity);
        String s1;
        int i;
        if (entity instanceof Villager villager) {
            s1 = villager.getVillagerData().profession().getRegisteredName();
            i = villager.getVillagerXp();
        } else {
            s1 = "";
            i = 0;
        }

        float f1 = entity.getHealth();
        float f = entity.getMaxHealth();
        Brain<?> brain = entity.getBrain();
        long j = entity.level().getGameTime();
        String s2;
        if (entity instanceof InventoryCarrier inventorycarrier) {
            Container container = inventorycarrier.getInventory();
            s2 = container.isEmpty() ? "" : container.toString();
        } else {
            s2 = "";
        }

        boolean flag = entity instanceof Villager villager2 && villager2.wantsToSpawnGolem(j);
        int k = entity instanceof Warden warden ? warden.getClientAngerLevel() : -1;
        List<String> list3 = brain.getActiveActivities().stream().map(Activity::getName).toList();
        List<String> list = brain.getRunningBehaviors().stream().map(BehaviorControl::debugString).toList();
        List<String> list1 = getMemoryDescriptions(level, entity, j)
            .map(p_449405_ -> StringUtil.truncateStringIfNecessary(p_449405_, 255, true))
            .toList();
        Set<BlockPos> set = getKnownBlockPositions(brain, MemoryModuleType.JOB_SITE, MemoryModuleType.HOME, MemoryModuleType.MEETING_POINT);
        Set<BlockPos> set1 = getKnownBlockPositions(brain, MemoryModuleType.POTENTIAL_JOB_SITE);
        List<String> list2 = entity instanceof Villager villager1 ? getVillagerGossips(villager1) : List.of();
        return new DebugBrainDump(s, s1, i, f1, f, s2, flag, k, list3, list, list1, list2, set, set1);
    }

    @SafeVarargs
    private static Set<BlockPos> getKnownBlockPositions(Brain<?> brain, MemoryModuleType<GlobalPos>... moduleTypes) {
        return Stream.of(moduleTypes)
            .filter(brain::hasMemoryValue)
            .map(brain::getMemory)
            .flatMap(Optional::stream)
            .map(GlobalPos::pos)
            .collect(Collectors.toSet());
    }

    private static List<String> getVillagerGossips(Villager villager) {
        List<String> list = new ArrayList<>();
        villager.getGossips().getGossipEntries().forEach((p_449878_, p_449727_) -> {
            String s = DebugEntityNameGenerator.getEntityName(p_449878_);
            p_449727_.forEach((p_449915_, p_449107_) -> list.add(s + ": " + p_449915_ + ": " + p_449107_));
        });
        return list;
    }

    private static Stream<String> getMemoryDescriptions(ServerLevel level, LivingEntity entity, long gameTime) {
        return entity.getBrain().getMemories().entrySet().stream().map(p_449300_ -> {
            MemoryModuleType<?> memorymoduletype = p_449300_.getKey();
            Optional<? extends ExpirableValue<?>> optional = p_449300_.getValue();
            return getMemoryDescription(level, gameTime, memorymoduletype, optional);
        }).sorted();
    }

    private static String getMemoryDescription(
        ServerLevel level, long gameTime, MemoryModuleType<?> moduleType, Optional<? extends ExpirableValue<?>> value
    ) {
        String s;
        if (value.isPresent()) {
            ExpirableValue<?> expirablevalue = (ExpirableValue<?>)value.get();
            Object object = expirablevalue.getValue();
            if (moduleType == MemoryModuleType.HEARD_BELL_TIME) {
                long i = gameTime - (Long)object;
                s = i + " ticks ago";
            } else if (expirablevalue.canExpire()) {
                s = getShortDescription(level, object) + " (ttl: " + expirablevalue.getTimeToLive() + ")";
            } else {
                s = getShortDescription(level, object);
            }
        } else {
            s = "-";
        }

        return BuiltInRegistries.MEMORY_MODULE_TYPE.getKey(moduleType).getPath() + ": " + s;
    }

    private static String getShortDescription(ServerLevel level, @Nullable Object object) {
        return switch (object) {
            case null -> "-";
            case UUID uuid -> getShortDescription(level, level.getEntity(uuid));
            case Entity entity -> DebugEntityNameGenerator.getEntityName(entity);
            case WalkTarget walktarget -> getShortDescription(level, walktarget.getTarget());
            case EntityTracker entitytracker -> getShortDescription(level, entitytracker.getEntity());
            case GlobalPos globalpos -> getShortDescription(level, globalpos.pos());
            case BlockPosTracker blockpostracker -> getShortDescription(level, blockpostracker.currentBlockPosition());
            case DamageSource damagesource -> {
                Entity entity1 = damagesource.getEntity();
                yield entity1 == null ? object.toString() : getShortDescription(level, entity1);
            }
            case Collection<?> collection -> "["
                + (String)collection.stream().map(p_449928_ -> getShortDescription(level, p_449928_)).collect(Collectors.joining(", "))
                + "]";
            default -> object.toString();
        };
    }

    public boolean hasPoi(BlockPos pos) {
        return this.pois.contains(pos);
    }

    public boolean hasPotentialPoi(BlockPos pos) {
        return this.potentialPois.contains(pos);
    }
}
