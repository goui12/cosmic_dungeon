package net.minecraft.gametest.framework;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Either;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.LongStream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.commands.FillBiomeCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class GameTestHelper {
    public final GameTestInfo testInfo;
    private boolean finalCheckAdded;

    public GameTestHelper(GameTestInfo testInfo) {
        this.testInfo = testInfo;
    }

    public GameTestAssertException assertionException(Component message) {
        return new GameTestAssertException(message, this.testInfo.getTick());
    }

    public GameTestAssertException assertionException(String messageKey, Object... args) {
        return this.assertionException(Component.translatableEscape(messageKey, args));
    }

    public GameTestAssertPosException assertionException(BlockPos pos, Component message) {
        return new GameTestAssertPosException(message, this.absolutePos(pos), pos, this.testInfo.getTick());
    }

    public GameTestAssertPosException assertionException(BlockPos pos, String messageKey, Object... args) {
        return this.assertionException(pos, Component.translatableEscape(messageKey, args));
    }

    public ServerLevel getLevel() {
        return this.testInfo.getLevel();
    }

    public BlockState getBlockState(BlockPos pos) {
        return this.getLevel().getBlockState(this.absolutePos(pos));
    }

    public <T extends BlockEntity> T getBlockEntity(BlockPos pos, Class<T> clazz) {
        BlockEntity blockentity = this.getLevel().getBlockEntity(this.absolutePos(pos));
        if (blockentity == null) {
            throw this.assertionException(pos, "test.error.missing_block_entity");
        } else if (clazz.isInstance(blockentity)) {
            return clazz.cast(blockentity);
        } else {
            throw this.assertionException(pos, "test.error.wrong_block_entity", blockentity.getType().builtInRegistryHolder().getRegisteredName());
        }
    }

    public void killAllEntities() {
        this.killAllEntitiesOfClass(Entity.class);
    }

    public void killAllEntitiesOfClass(Class<? extends Entity> entityClass) {
        AABB aabb = this.getBounds();
        List<? extends Entity> list = this.getLevel().getEntitiesOfClass(entityClass, aabb.inflate(1.0), p_177131_ -> !(p_177131_ instanceof Player));
        list.forEach(p_375467_ -> p_375467_.kill(this.getLevel()));
    }

    public ItemEntity spawnItem(Item item, Vec3 pos) {
        ServerLevel serverlevel = this.getLevel();
        Vec3 vec3 = this.absoluteVec(pos);
        ItemEntity itementity = new ItemEntity(serverlevel, vec3.x, vec3.y, vec3.z, new ItemStack(item, 1));
        itementity.setDeltaMovement(0.0, 0.0, 0.0);
        serverlevel.addFreshEntity(itementity);
        return itementity;
    }

    public ItemEntity spawnItem(Item item, float x, float y, float z) {
        return this.spawnItem(item, new Vec3(x, y, z));
    }

    public ItemEntity spawnItem(Item item, BlockPos pos) {
        return this.spawnItem(item, pos.getX(), pos.getY(), pos.getZ());
    }

    public <E extends Entity> E spawn(EntityType<E> type, BlockPos pos) {
        return this.spawn(type, Vec3.atBottomCenterOf(pos));
    }

    public <E extends Entity> List<E> spawn(EntityType<E> type, BlockPos pos, int count) {
        return this.spawn(type, Vec3.atBottomCenterOf(pos), count);
    }

    public <E extends Entity> List<E> spawn(EntityType<E> type, Vec3 pos, int count) {
        List<E> list = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            list.add(this.spawn(type, pos));
        }

        return list;
    }

    public <E extends Entity> E spawn(EntityType<E> type, Vec3 pos) {
        ServerLevel serverlevel = this.getLevel();
        E e = type.create(serverlevel, EntitySpawnReason.STRUCTURE);
        if (e == null) {
            throw this.assertionException(BlockPos.containing(pos), "test.error.spawn_failure", type.builtInRegistryHolder().getRegisteredName());
        } else {
            if (e instanceof Mob mob) {
                mob.setPersistenceRequired();
            }

            Vec3 vec3 = this.absoluteVec(pos);
            float f = e.rotate(this.getTestRotation());
            e.snapTo(vec3.x, vec3.y, vec3.z, f, e.getXRot());
            e.setYBodyRot(f);
            e.setYHeadRot(f);
            serverlevel.addFreshEntity(e);
            return e;
        }
    }

    public void hurt(Entity entity, DamageSource damageSource, float amount) {
        entity.hurtServer(this.getLevel(), damageSource, amount);
    }

    public void kill(Entity entity) {
        entity.kill(this.getLevel());
    }

    public <E extends Entity> E findOneEntity(EntityType<E> type) {
        return this.findClosestEntity(type, 0, 0, 0, 2.147483647E9);
    }

    public <E extends Entity> E findClosestEntity(EntityType<E> type, int x, int y, int z, double radius) {
        List<E> list = this.findEntities(type, x, y, z, radius);
        if (list.isEmpty()) {
            throw this.assertionException("test.error.expected_entity_around", type.getDescription(), x, y, z);
        } else if (list.size() > 1) {
            throw this.assertionException("test.error.too_many_entities", type.toShortString(), x, y, z, list.size());
        } else {
            Vec3 vec3 = this.absoluteVec(new Vec3(x, y, z));
            list.sort((p_319453_, p_319454_) -> {
                double d0 = p_319453_.position().distanceTo(vec3);
                double d1 = p_319454_.position().distanceTo(vec3);
                return Double.compare(d0, d1);
            });
            return list.get(0);
        }
    }

    public <E extends Entity> List<E> findEntities(EntityType<E> type, int x, int y, int z, double radius) {
        return this.findEntities(type, Vec3.atBottomCenterOf(new BlockPos(x, y, z)), radius);
    }

    public <E extends Entity> List<E> findEntities(EntityType<E> type, Vec3 pos, double radius) {
        ServerLevel serverlevel = this.getLevel();
        Vec3 vec3 = this.absoluteVec(pos);
        AABB aabb = this.testInfo.getStructureBounds();
        AABB aabb1 = new AABB(vec3.add(-radius, -radius, -radius), vec3.add(radius, radius, radius));
        return serverlevel.getEntities(type, aabb, p_319451_ -> p_319451_.getBoundingBox().intersects(aabb1) && p_319451_.isAlive());
    }

    public <E extends Entity> E spawn(EntityType<E> type, int x, int y, int z) {
        return this.spawn(type, new BlockPos(x, y, z));
    }

    public <E extends Entity> E spawn(EntityType<E> type, float x, float y, float z) {
        return this.spawn(type, new Vec3(x, y, z));
    }

    public <E extends Mob> E spawnWithNoFreeWill(EntityType<E> type, BlockPos pos) {
        E e = (E)this.spawn(type, pos);
        e.removeFreeWill();
        return e;
    }

    public <E extends Mob> E spawnWithNoFreeWill(EntityType<E> type, int x, int y, int z) {
        return this.spawnWithNoFreeWill(type, new BlockPos(x, y, z));
    }

    public <E extends Mob> E spawnWithNoFreeWill(EntityType<E> type, Vec3 pos) {
        E e = (E)this.spawn(type, pos);
        e.removeFreeWill();
        return e;
    }

    public <E extends Mob> E spawnWithNoFreeWill(EntityType<E> type, float x, float y, float z) {
        return this.spawnWithNoFreeWill(type, new Vec3(x, y, z));
    }

    public void moveTo(Mob mob, float x, float y, float z) {
        Vec3 vec3 = this.absoluteVec(new Vec3(x, y, z));
        mob.snapTo(vec3.x, vec3.y, vec3.z, mob.getYRot(), mob.getXRot());
    }

    public GameTestSequence walkTo(Mob mob, BlockPos pos, float speed) {
        return this.startSequence().thenExecuteAfter(2, () -> {
            Path path = mob.getNavigation().createPath(this.absolutePos(pos), 0);
            mob.getNavigation().moveTo(path, (double)speed);
        });
    }

    public void pressButton(int x, int y, int z) {
        this.pressButton(new BlockPos(x, y, z));
    }

    public void pressButton(BlockPos pos) {
        this.assertBlockTag(BlockTags.BUTTONS, pos);
        BlockPos blockpos = this.absolutePos(pos);
        BlockState blockstate = this.getLevel().getBlockState(blockpos);
        ButtonBlock buttonblock = (ButtonBlock)blockstate.getBlock();
        buttonblock.press(blockstate, this.getLevel(), blockpos, null);
    }

    public void useBlock(BlockPos pos) {
        this.useBlock(pos, this.makeMockPlayer(GameType.CREATIVE));
    }

    public void useBlock(BlockPos pos, Player player) {
        BlockPos blockpos = this.absolutePos(pos);
        this.useBlock(pos, player, new BlockHitResult(Vec3.atCenterOf(blockpos), Direction.NORTH, blockpos, true));
    }

    public void useBlock(BlockPos pos, Player player, BlockHitResult result) {
        BlockPos blockpos = this.absolutePos(pos);
        BlockState blockstate = this.getLevel().getBlockState(blockpos);
        InteractionHand interactionhand = InteractionHand.MAIN_HAND;
        InteractionResult interactionresult = blockstate.useItemOn(
            player.getItemInHand(interactionhand), this.getLevel(), player, interactionhand, result
        );
        if (!interactionresult.consumesAction()) {
            if (!(interactionresult instanceof InteractionResult.TryEmptyHandInteraction)
                || !blockstate.useWithoutItem(this.getLevel(), player, result).consumesAction()) {
                UseOnContext useoncontext = new UseOnContext(player, interactionhand, result);
                player.getItemInHand(interactionhand).useOn(useoncontext);
            }
        }
    }

    public LivingEntity makeAboutToDrown(LivingEntity entity) {
        entity.setAirSupply(0);
        entity.setHealth(0.25F);
        return entity;
    }

    public LivingEntity withLowHealth(LivingEntity entity) {
        entity.setHealth(0.25F);
        return entity;
    }

    public Player makeMockPlayer(final GameType gameType) {
        return new Player(this.getLevel(), new GameProfile(UUID.randomUUID(), "test-mock-player")) {
            @Nonnull
            @Override
            public GameType gameMode() {
                return gameType;
            }

            @Override
            public boolean isClientAuthoritative() {
                return false;
            }
        };
    }

    @Deprecated(
        forRemoval = true
    )
    public ServerPlayer makeMockServerPlayerInLevel() {
        CommonListenerCookie commonlistenercookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "test-mock-player"), false);
        ServerPlayer serverplayer = new ServerPlayer(
            this.getLevel().getServer(), this.getLevel(), commonlistenercookie.gameProfile(), commonlistenercookie.clientInformation()
        ) {
            @Override
            public GameType gameMode() {
                return GameType.CREATIVE;
            }
        };
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        this.getLevel().getServer().getPlayerList().placeNewPlayer(connection, serverplayer, commonlistenercookie);
        return serverplayer;
    }

    public void pullLever(int x, int y, int z) {
        this.pullLever(new BlockPos(x, y, z));
    }

    public void pullLever(BlockPos pos) {
        this.assertBlockPresent(Blocks.LEVER, pos);
        BlockPos blockpos = this.absolutePos(pos);
        BlockState blockstate = this.getLevel().getBlockState(blockpos);
        LeverBlock leverblock = (LeverBlock)blockstate.getBlock();
        leverblock.pull(blockstate, this.getLevel(), blockpos, null);
    }

    public void pulseRedstone(BlockPos pos, long delay) {
        this.setBlock(pos, Blocks.REDSTONE_BLOCK);
        this.runAfterDelay(delay, () -> this.setBlock(pos, Blocks.AIR));
    }

    public void destroyBlock(BlockPos pos) {
        this.getLevel().destroyBlock(this.absolutePos(pos), false, null);
    }

    public void setBlock(int x, int y, int z, Block block) {
        this.setBlock(new BlockPos(x, y, z), block);
    }

    public void setBlock(int x, int y, int z, BlockState state) {
        this.setBlock(new BlockPos(x, y, z), state);
    }

    public void setBlock(BlockPos pos, Block block) {
        this.setBlock(pos, block.defaultBlockState());
    }

    public void setBlock(BlockPos pos, BlockState state) {
        this.getLevel().setBlock(this.absolutePos(pos), state, 3);
    }

    public void setBlock(BlockPos pos, Block block, Direction facing) {
        this.setBlock(pos, block.defaultBlockState(), facing);
    }

    public void setBlock(BlockPos pos, BlockState state, Direction facing) {
        BlockState blockstate = state;
        if (state.hasProperty(HorizontalDirectionalBlock.FACING)) {
            blockstate = state.setValue(HorizontalDirectionalBlock.FACING, facing);
        }

        if (state.hasProperty(BlockStateProperties.FACING)) {
            blockstate = state.setValue(BlockStateProperties.FACING, facing);
        }

        this.getLevel().setBlock(this.absolutePos(pos), blockstate, 3);
    }

    public void setNight() {
        this.setDayTime(13000);
    }

    public void setDayTime(int time) {
        this.getLevel().setDayTime(time);
    }

    public void assertBlockPresent(Block block, int x, int y, int z) {
        this.assertBlockPresent(block, new BlockPos(x, y, z));
    }

    public void assertBlockPresent(Block block, BlockPos pos) {
        BlockState blockstate = this.getBlockState(pos);
        this.assertBlock(
            pos,
            p_177216_ -> blockstate.is(block),
            p_396374_ -> Component.translatable("test.error.expected_block", block.getName(), p_396374_.getName())
        );
    }

    public void assertBlockNotPresent(Block block, int x, int y, int z) {
        this.assertBlockNotPresent(block, new BlockPos(x, y, z));
    }

    public void assertBlockNotPresent(Block block, BlockPos pos) {
        this.assertBlock(
            pos,
            p_177251_ -> !this.getBlockState(pos).is(block),
            p_396386_ -> Component.translatable("test.error.unexpected_block", block.getName())
        );
    }

    public void assertBlockTag(TagKey<Block> tag, BlockPos pos) {
        this.assertBlockState(
            pos,
            p_396384_ -> p_396384_.is(tag),
            p_414993_ -> Component.translatable("test.error.expected_block_tag", Component.translationArg(tag.location()), p_414993_.getBlock().getName())
        );
    }

    public void succeedWhenBlockPresent(Block block, int x, int y, int z) {
        this.succeedWhenBlockPresent(block, new BlockPos(x, y, z));
    }

    public void succeedWhenBlockPresent(Block block, BlockPos pos) {
        this.succeedWhen(() -> this.assertBlockPresent(block, pos));
    }

    public void assertBlock(BlockPos pos, Predicate<Block> predicate, Function<Block, Component> message) {
        this.assertBlockState(pos, p_177296_ -> predicate.test(p_177296_.getBlock()), p_396382_ -> message.apply(p_396382_.getBlock()));
    }

    public <T extends Comparable<T>> void assertBlockProperty(BlockPos pos, Property<T> property, T value) {
        BlockState blockstate = this.getBlockState(pos);
        boolean flag = blockstate.hasProperty(property);
        if (!flag) {
            throw this.assertionException(pos, "test.error.block_property_missing", property.getName(), value);
        } else if (!blockstate.<T>getValue(property).equals(value)) {
            throw this.assertionException(pos, "test.error.block_property_mismatch", property.getName(), value, blockstate.getValue(property));
        }
    }

    public <T extends Comparable<T>> void assertBlockProperty(BlockPos pos, Property<T> property, Predicate<T> predicate, Component message) {
        this.assertBlockState(pos, p_277264_ -> {
            if (!p_277264_.hasProperty(property)) {
                return false;
            } else {
                T t = p_277264_.getValue(property);
                return predicate.test(t);
            }
        }, p_397919_ -> message);
    }

    public void assertBlockState(BlockPos pos, BlockState state) {
        BlockState blockstate = this.getBlockState(pos);
        if (!blockstate.equals(state)) {
            throw this.assertionException(pos, "test.error.state_not_equal", state, blockstate);
        }
    }

    public void assertBlockState(BlockPos pos, Predicate<BlockState> predicate, Function<BlockState, Component> message) {
        BlockState blockstate = this.getBlockState(pos);
        if (!predicate.test(blockstate)) {
            throw this.assertionException(pos, message.apply(blockstate));
        }
    }

    public <T extends BlockEntity> void assertBlockEntityData(BlockPos pos, Class<T> blockEntityClass, Predicate<T> predicate, Supplier<Component> message) {
        T t = this.getBlockEntity(pos, blockEntityClass);
        if (!predicate.test(t)) {
            throw this.assertionException(pos, message.get());
        }
    }

    public void assertRedstoneSignal(BlockPos pos, Direction direction, IntPredicate signalStrengthPredicate, Supplier<Component> message) {
        BlockPos blockpos = this.absolutePos(pos);
        ServerLevel serverlevel = this.getLevel();
        BlockState blockstate = serverlevel.getBlockState(blockpos);
        int i = blockstate.getSignal(serverlevel, blockpos, direction);
        if (!signalStrengthPredicate.test(i)) {
            throw this.assertionException(pos, message.get());
        }
    }

    public void assertEntityPresent(EntityType<?> type) {
        if (!this.getLevel().hasEntities(type, this.getBounds(), Entity::isAlive)) {
            throw this.assertionException("test.error.expected_entity_in_test", type.getDescription());
        }
    }

    public void assertEntityPresent(EntityType<?> type, int x, int y, int z) {
        this.assertEntityPresent(type, new BlockPos(x, y, z));
    }

    public void assertEntityPresent(EntityType<?> type, BlockPos pos) {
        BlockPos blockpos = this.absolutePos(pos);
        if (!this.getLevel().hasEntities(type, new AABB(blockpos), Entity::isAlive)) {
            throw this.assertionException(pos, "test.error.expected_entity", type.getDescription());
        }
    }

    public void assertEntityPresent(EntityType<?> type, AABB box) {
        AABB aabb = this.absoluteAABB(box);
        if (!this.getLevel().hasEntities(type, aabb, Entity::isAlive)) {
            throw this.assertionException(BlockPos.containing(box.getCenter()), "test.error.expected_entity", type.getDescription());
        }
    }

    public void assertEntitiesPresent(EntityType<?> entityType, int count) {
        List<? extends Entity> list = this.getLevel().getEntities(entityType, this.getBounds(), Entity::isAlive);
        if (list.size() != count) {
            throw this.assertionException("test.error.expected_entity_count", count, entityType.getDescription(), list.size());
        }
    }

    public void assertEntitiesPresent(EntityType<?> entityType, BlockPos pos, int count, double radius) {
        BlockPos blockpos = this.absolutePos(pos);
        List<? extends Entity> list = this.getEntities((EntityType<? extends Entity>)entityType, pos, radius);
        if (list.size() != count) {
            throw this.assertionException(pos, "test.error.expected_entity_count", count, entityType.getDescription(), list.size());
        }
    }

    public void assertEntityPresent(EntityType<?> type, BlockPos pos, double expansionAmount) {
        List<? extends Entity> list = this.getEntities((EntityType<? extends Entity>)type, pos, expansionAmount);
        if (list.isEmpty()) {
            BlockPos blockpos = this.absolutePos(pos);
            throw this.assertionException(pos, "test.error.expected_entity", type.getDescription());
        }
    }

    public <T extends Entity> List<T> getEntities(EntityType<T> entityType, BlockPos pos, double radius) {
        BlockPos blockpos = this.absolutePos(pos);
        return this.getLevel().getEntities(entityType, new AABB(blockpos).inflate(radius), Entity::isAlive);
    }

    public <T extends Entity> List<T> getEntities(EntityType<T> entityType) {
        return this.getLevel().getEntities(entityType, this.getBounds(), Entity::isAlive);
    }

    public void assertEntityInstancePresent(Entity entity, int x, int y, int z) {
        this.assertEntityInstancePresent(entity, new BlockPos(x, y, z));
    }

    public void assertEntityInstancePresent(Entity entity, BlockPos pos) {
        BlockPos blockpos = this.absolutePos(pos);
        List<? extends Entity> list = this.getLevel().getEntities(entity.getType(), new AABB(blockpos), Entity::isAlive);
        list.stream()
            .filter(p_177139_ -> p_177139_ == entity)
            .findFirst()
            .orElseThrow(() -> this.assertionException(pos, "test.error.expected_entity", entity.getType().getDescription()));
    }

    public void assertItemEntityCountIs(Item item, BlockPos pos, double expansionAmount, int count) {
        BlockPos blockpos = this.absolutePos(pos);
        List<ItemEntity> list = this.getLevel().getEntities(EntityType.ITEM, new AABB(blockpos).inflate(expansionAmount), Entity::isAlive);
        int i = 0;

        for (ItemEntity itementity : list) {
            ItemStack itemstack = itementity.getItem();
            if (itemstack.is(item)) {
                i += itemstack.getCount();
            }
        }

        if (i != count) {
            throw this.assertionException(pos, "test.error.expected_items_count", count, item.getName(), i);
        }
    }

    public void assertItemEntityPresent(Item item, BlockPos pos, double expansionAmount) {
        BlockPos blockpos = this.absolutePos(pos);
        Predicate<ItemEntity> predicate = p_445281_ -> p_445281_.isAlive() && p_445281_.getItem().is(item);
        if (!this.getLevel().hasEntities(EntityType.ITEM, new AABB(blockpos).inflate(expansionAmount), predicate)) {
            throw this.assertionException(pos, "test.error.expected_item", item.getName());
        }
    }

    public void assertItemEntityNotPresent(Item item, BlockPos pos, double radius) {
        BlockPos blockpos = this.absolutePos(pos);
        Predicate<ItemEntity> predicate = p_445283_ -> p_445283_.isAlive() && p_445283_.getItem().is(item);
        if (this.getLevel().hasEntities(EntityType.ITEM, new AABB(blockpos).inflate(radius), predicate)) {
            throw this.assertionException(pos, "test.error.unexpected_item", item.getName());
        }
    }

    public void assertItemEntityPresent(Item item) {
        Predicate<ItemEntity> predicate = p_445287_ -> p_445287_.isAlive() && p_445287_.getItem().is(item);
        if (!this.getLevel().hasEntities(EntityType.ITEM, this.getBounds(), predicate)) {
            throw this.assertionException("test.error.expected_item", item.getName());
        }
    }

    public void assertItemEntityNotPresent(Item item) {
        Predicate<ItemEntity> predicate = p_445285_ -> p_445285_.isAlive() && p_445285_.getItem().is(item);
        if (this.getLevel().hasEntities(EntityType.ITEM, this.getBounds(), predicate)) {
            throw this.assertionException("test.error.unexpected_item", item.getName());
        }
    }

    public void assertEntityNotPresent(EntityType<?> type) {
        List<? extends Entity> list = this.getLevel().getEntities(type, this.getBounds(), Entity::isAlive);
        if (!list.isEmpty()) {
            throw this.assertionException(list.getFirst().blockPosition(), "test.error.unexpected_entity", type.getDescription());
        }
    }

    public void assertEntityNotPresent(EntityType<?> type, int x, int y, int z) {
        this.assertEntityNotPresent(type, new BlockPos(x, y, z));
    }

    public void assertEntityNotPresent(EntityType<?> type, BlockPos pos) {
        BlockPos blockpos = this.absolutePos(pos);
        if (this.getLevel().hasEntities(type, new AABB(blockpos), Entity::isAlive)) {
            throw this.assertionException(pos, "test.error.unexpected_entity", type.getDescription());
        }
    }

    public void assertEntityNotPresent(EntityType<?> type, AABB box) {
        AABB aabb = this.absoluteAABB(box);
        List<? extends Entity> list = this.getLevel().getEntities(type, aabb, Entity::isAlive);
        if (!list.isEmpty()) {
            throw this.assertionException(list.getFirst().blockPosition(), "test.error.unexpected_entity", type.getDescription());
        }
    }

    public void assertEntityTouching(EntityType<?> type, double x, double y, double z) {
        Vec3 vec3 = new Vec3(x, y, z);
        Vec3 vec31 = this.absoluteVec(vec3);
        Predicate<? super Entity> predicate = p_177346_ -> p_177346_.getBoundingBox().intersects(vec31, vec31);
        if (!this.getLevel().hasEntities(type, this.getBounds(), predicate)) {
            throw this.assertionException(
                "test.error.expected_entity_touching", type.getDescription(), vec31.x(), vec31.y(), vec31.z(), x, y, z
            );
        }
    }

    public void assertEntityNotTouching(EntityType<?> type, double x, double y, double z) {
        Vec3 vec3 = new Vec3(x, y, z);
        Vec3 vec31 = this.absoluteVec(vec3);
        Predicate<? super Entity> predicate = p_177231_ -> !p_177231_.getBoundingBox().intersects(vec31, vec31);
        if (!this.getLevel().hasEntities(type, this.getBounds(), predicate)) {
            throw this.assertionException(
                "test.error.expected_entity_not_touching", type.getDescription(), vec31.x(), vec31.y(), vec31.z(), x, y, z
            );
        }
    }

    public <E extends Entity, T> void assertEntityData(BlockPos pos, EntityType<E> type, Predicate<E> predicate) {
        BlockPos blockpos = this.absolutePos(pos);
        List<E> list = this.getLevel().getEntities(type, new AABB(blockpos), Entity::isAlive);
        if (list.isEmpty()) {
            throw this.assertionException(pos, "test.error.expected_entity", type.getDescription());
        } else {
            for (E e : list) {
                if (!predicate.test(e)) {
                    throw this.assertionException(e.blockPosition(), "test.error.expected_entity_data_predicate", e.getName());
                }
            }
        }
    }

    public <E extends Entity, T> void assertEntityData(BlockPos pos, EntityType<E> type, Function<? super E, T> entityDataGetter, @Nullable T testEntityData) {
        BlockPos blockpos = this.absolutePos(pos);
        List<E> list = this.getLevel().getEntities(type, new AABB(blockpos), Entity::isAlive);
        if (list.isEmpty()) {
            throw this.assertionException(pos, "test.error.expected_entity", type.getDescription());
        } else {
            for (E e : list) {
                T t = entityDataGetter.apply(e);
                if (!Objects.equals(t, testEntityData)) {
                    throw this.assertionException(pos, "test.error.expected_entity_data", testEntityData, t);
                }
            }
        }
    }

    public <E extends LivingEntity> void assertEntityIsHolding(BlockPos pos, EntityType<E> entityType, Item item) {
        BlockPos blockpos = this.absolutePos(pos);
        List<E> list = this.getLevel().getEntities(entityType, new AABB(blockpos), Entity::isAlive);
        if (list.isEmpty()) {
            throw this.assertionException(pos, "test.error.expected_entity", entityType.getDescription());
        } else {
            for (E e : list) {
                if (e.isHolding(item)) {
                    return;
                }
            }

            throw this.assertionException(pos, "test.error.expected_entity_holding", item.getName());
        }
    }

    public <E extends Entity & InventoryCarrier> void assertEntityInventoryContains(BlockPos pos, EntityType<E> entityType, Item item) {
        BlockPos blockpos = this.absolutePos(pos);
        List<E> list = this.getLevel().getEntities(entityType, new AABB(blockpos), p_263479_ -> p_263479_.isAlive());
        if (list.isEmpty()) {
            throw this.assertionException(pos, "test.error.expected_entity", entityType.getDescription());
        } else {
            for (E e : list) {
                if (e.getInventory().hasAnyMatching(p_263481_ -> p_263481_.is(item))) {
                    return;
                }
            }

            throw this.assertionException(pos, "test.error.expected_entity_having", item.getName());
        }
    }

    public void assertContainerEmpty(BlockPos pos) {
        BaseContainerBlockEntity basecontainerblockentity = this.getBlockEntity(pos, BaseContainerBlockEntity.class);
        if (!basecontainerblockentity.isEmpty()) {
            throw this.assertionException(pos, "test.error.expected_empty_container");
        }
    }

    public void assertContainerContainsSingle(BlockPos pos, Item item) {
        BaseContainerBlockEntity basecontainerblockentity = this.getBlockEntity(pos, BaseContainerBlockEntity.class);
        if (basecontainerblockentity.countItem(item) != 1) {
            throw this.assertionException(pos, "test.error.expected_container_contents_single", item.getName());
        }
    }

    public void assertContainerContains(BlockPos pos, Item item) {
        BaseContainerBlockEntity basecontainerblockentity = this.getBlockEntity(pos, BaseContainerBlockEntity.class);
        if (basecontainerblockentity.countItem(item) == 0) {
            throw this.assertionException(pos, "test.error.expected_container_contents", item.getName());
        }
    }

    public void assertSameBlockStates(BoundingBox boundingBox, BlockPos pos) {
        BlockPos.betweenClosedStream(boundingBox).forEach(p_177267_ -> {
            BlockPos blockpos = pos.offset(p_177267_.getX() - boundingBox.minX(), p_177267_.getY() - boundingBox.minY(), p_177267_.getZ() - boundingBox.minZ());
            this.assertSameBlockState(p_177267_, blockpos);
        });
    }

    public void assertSameBlockState(BlockPos testPos, BlockPos comparisonPos) {
        BlockState blockstate = this.getBlockState(testPos);
        BlockState blockstate1 = this.getBlockState(comparisonPos);
        if (blockstate != blockstate1) {
            throw this.assertionException(testPos, "test.error.state_not_equal", blockstate1, blockstate);
        }
    }

    public void assertAtTickTimeContainerContains(long tickTime, BlockPos pos, Item item) {
        this.runAtTickTime(tickTime, () -> this.assertContainerContainsSingle(pos, item));
    }

    public void assertAtTickTimeContainerEmpty(long tickTime, BlockPos pos) {
        this.runAtTickTime(tickTime, () -> this.assertContainerEmpty(pos));
    }

    public <E extends Entity, T> void succeedWhenEntityData(BlockPos pos, EntityType<E> type, Function<E, T> entityDataGetter, T testEntityData) {
        this.succeedWhen(() -> this.assertEntityData(pos, type, entityDataGetter, testEntityData));
    }

    public void assertEntityPosition(Entity entity, AABB boundingBox, Component message) {
        if (!boundingBox.contains(this.relativeVec(entity.position()))) {
            throw this.assertionException(message);
        }
    }

    public <E extends Entity> void assertEntityProperty(E entity, Predicate<E> predicate, Component message) {
        if (!predicate.test(entity)) {
            throw this.assertionException(entity.blockPosition(), "test.error.entity_property", entity.getName(), message);
        }
    }

    public <E extends Entity, T> void assertEntityProperty(E entity, Function<E, T> valueGetter, T expectedValue, Component message) {
        T t = valueGetter.apply(entity);
        if (!t.equals(expectedValue)) {
            throw this.assertionException(entity.blockPosition(), "test.error.entity_property_details", entity.getName(), message, t, expectedValue);
        }
    }

    public void assertLivingEntityHasMobEffect(LivingEntity entity, Holder<MobEffect> effect, int amplifier) {
        MobEffectInstance mobeffectinstance = entity.getEffect(effect);
        if (mobeffectinstance == null || mobeffectinstance.getAmplifier() != amplifier) {
            throw this.assertionException("test.error.expected_entity_effect", entity.getName(), PotionContents.getPotionDescription(effect, amplifier));
        }
    }

    public void succeedWhenEntityPresent(EntityType<?> type, int x, int y, int z) {
        this.succeedWhenEntityPresent(type, new BlockPos(x, y, z));
    }

    public void succeedWhenEntityPresent(EntityType<?> type, BlockPos pos) {
        this.succeedWhen(() -> this.assertEntityPresent(type, pos));
    }

    public void succeedWhenEntityNotPresent(EntityType<?> type, int x, int y, int z) {
        this.succeedWhenEntityNotPresent(type, new BlockPos(x, y, z));
    }

    public void succeedWhenEntityNotPresent(EntityType<?> type, BlockPos pos) {
        this.succeedWhen(() -> this.assertEntityNotPresent(type, pos));
    }

    public void succeed() {
        this.testInfo.succeed();
    }

    private void ensureSingleFinalCheck() {
        if (this.finalCheckAdded) {
            throw new IllegalStateException("This test already has final clause");
        } else {
            this.finalCheckAdded = true;
        }
    }

    public void succeedIf(Runnable criterion) {
        this.ensureSingleFinalCheck();
        this.testInfo.createSequence().thenWaitUntil(0L, criterion).thenSucceed();
    }

    public void succeedWhen(Runnable criterion) {
        this.ensureSingleFinalCheck();
        this.testInfo.createSequence().thenWaitUntil(criterion).thenSucceed();
    }

    public void succeedOnTickWhen(int tick, Runnable criterion) {
        this.ensureSingleFinalCheck();
        this.testInfo.createSequence().thenWaitUntil(tick, criterion).thenSucceed();
    }

    public void runAtTickTime(long tickTime, Runnable task) {
        this.testInfo.setRunAtTickTime(tickTime, task);
    }

    public void runAfterDelay(long delay, Runnable task) {
        this.runAtTickTime(this.testInfo.getTick() + delay, task);
    }

    public void randomTick(BlockPos pos) {
        BlockPos blockpos = this.absolutePos(pos);
        ServerLevel serverlevel = this.getLevel();
        serverlevel.getBlockState(blockpos).randomTick(serverlevel, blockpos, serverlevel.random);
    }

    public void tickBlock(BlockPos pos) {
        BlockPos blockpos = this.absolutePos(pos);
        ServerLevel serverlevel = this.getLevel();
        serverlevel.getBlockState(blockpos).tick(serverlevel, blockpos, serverlevel.random);
    }

    public void tickPrecipitation(BlockPos pos) {
        BlockPos blockpos = this.absolutePos(pos);
        ServerLevel serverlevel = this.getLevel();
        serverlevel.tickPrecipitation(blockpos);
    }

    public void tickPrecipitation() {
        AABB aabb = this.getRelativeBounds();
        int i = (int)Math.floor(aabb.maxX);
        int j = (int)Math.floor(aabb.maxZ);
        int k = (int)Math.floor(aabb.maxY);

        for (int l = (int)Math.floor(aabb.minX); l < i; l++) {
            for (int i1 = (int)Math.floor(aabb.minZ); i1 < j; i1++) {
                this.tickPrecipitation(new BlockPos(l, k, i1));
            }
        }
    }

    public int getHeight(Heightmap.Types heightmapType, int x, int z) {
        BlockPos blockpos = this.absolutePos(new BlockPos(x, 0, z));
        return this.relativePos(this.getLevel().getHeightmapPos(heightmapType, blockpos)).getY();
    }

    public void fail(Component message, BlockPos pos) {
        throw this.assertionException(pos, message);
    }

    public void fail(Component message, Entity entity) {
        throw this.assertionException(entity.blockPosition(), message);
    }

    public void fail(Component message) {
        throw this.assertionException(message);
    }

    public void fail(String message) {
        throw this.assertionException(Component.literal(message));
    }

    public void failIf(Runnable criterion) {
        this.testInfo.createSequence().thenWaitUntil(criterion).thenFail(() -> this.assertionException("test.error.fail"));
    }

    public void failIfEver(Runnable criterion) {
        LongStream.range(this.testInfo.getTick(), this.testInfo.getTimeoutTicks())
            .forEach(p_177365_ -> this.testInfo.setRunAtTickTime(p_177365_, criterion::run));
    }

    public GameTestSequence startSequence() {
        return this.testInfo.createSequence();
    }

    public BlockPos absolutePos(BlockPos pos) {
        BlockPos blockpos = this.testInfo.getTestOrigin();
        BlockPos blockpos1 = blockpos.offset(pos);
        return StructureTemplate.transform(blockpos1, Mirror.NONE, this.testInfo.getRotation(), blockpos);
    }

    public BlockPos relativePos(BlockPos pos) {
        BlockPos blockpos = this.testInfo.getTestOrigin();
        Rotation rotation = this.testInfo.getRotation().getRotated(Rotation.CLOCKWISE_180);
        BlockPos blockpos1 = StructureTemplate.transform(pos, Mirror.NONE, rotation, blockpos);
        return blockpos1.subtract(blockpos);
    }

    public AABB absoluteAABB(AABB aabb) {
        Vec3 vec3 = this.absoluteVec(aabb.getMinPosition());
        Vec3 vec31 = this.absoluteVec(aabb.getMaxPosition());
        return new AABB(vec3, vec31);
    }

    public AABB relativeAABB(AABB aabb) {
        Vec3 vec3 = this.relativeVec(aabb.getMinPosition());
        Vec3 vec31 = this.relativeVec(aabb.getMaxPosition());
        return new AABB(vec3, vec31);
    }

    public Vec3 absoluteVec(Vec3 relativeVec3) {
        Vec3 vec3 = Vec3.atLowerCornerOf(this.testInfo.getTestOrigin());
        return StructureTemplate.transform(vec3.add(relativeVec3), Mirror.NONE, this.testInfo.getRotation(), this.testInfo.getTestOrigin());
    }

    public Vec3 relativeVec(Vec3 absoluteVec3) {
        Vec3 vec3 = Vec3.atLowerCornerOf(this.testInfo.getTestOrigin());
        return StructureTemplate.transform(absoluteVec3.subtract(vec3), Mirror.NONE, this.testInfo.getRotation(), this.testInfo.getTestOrigin());
    }

    public Rotation getTestRotation() {
        return this.testInfo.getRotation();
    }

    public Direction getTestDirection() {
        return this.testInfo.getRotation().rotate(Direction.SOUTH);
    }

    public void assertTrue(boolean condition, Component message) {
        if (!condition) {
            throw this.assertionException(message);
        }
    }

    public <N> void assertValueEqual(N expected, N actual, Component name) {
        if (!expected.equals(actual)) {
            throw this.assertionException("test.error.value_not_equal", name, expected, actual);
        }
    }

    public void assertFalse(boolean condition, Component message) {
        this.assertTrue(!condition, message);
    }

    public long getTick() {
        return this.testInfo.getTick();
    }

    public AABB getBounds() {
        return this.testInfo.getStructureBounds();
    }

    private AABB getRelativeBounds() {
        AABB aabb = this.testInfo.getStructureBounds();
        Rotation rotation = this.testInfo.getRotation();
        switch (rotation) {
            case COUNTERCLOCKWISE_90:
            case CLOCKWISE_90:
                return new AABB(0.0, 0.0, 0.0, aabb.getZsize(), aabb.getYsize(), aabb.getXsize());
            default:
                return new AABB(0.0, 0.0, 0.0, aabb.getXsize(), aabb.getYsize(), aabb.getZsize());
        }
    }

    public void forEveryBlockInStructure(Consumer<BlockPos> consumer) {
        AABB aabb = this.getRelativeBounds().contract(1.0, 1.0, 1.0);
        BlockPos.MutableBlockPos.betweenClosedStream(aabb).forEach(consumer);
    }

    public void onEachTick(Runnable task) {
        LongStream.range(this.testInfo.getTick(), this.testInfo.getTimeoutTicks())
            .forEach(p_177283_ -> this.testInfo.setRunAtTickTime(p_177283_, task::run));
    }

    public void placeAt(Player player, ItemStack stack, BlockPos pos, Direction direction) {
        BlockPos blockpos = this.absolutePos(pos.relative(direction));
        BlockHitResult blockhitresult = new BlockHitResult(Vec3.atCenterOf(blockpos), direction, blockpos, false);
        UseOnContext useoncontext = new UseOnContext(player, InteractionHand.MAIN_HAND, blockhitresult);
        stack.useOn(useoncontext);
    }

    public void setBiome(ResourceKey<Biome> biome) {
        AABB aabb = this.getBounds();
        BlockPos blockpos = BlockPos.containing(aabb.minX, aabb.minY, aabb.minZ);
        BlockPos blockpos1 = BlockPos.containing(aabb.maxX, aabb.maxY, aabb.maxZ);
        Either<Integer, CommandSyntaxException> either = FillBiomeCommand.fill(
            this.getLevel(), blockpos, blockpos1, this.getLevel().registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(biome)
        );
        if (either.right().isPresent()) {
            throw this.assertionException("test.error.set_biome");
        }
    }
}
