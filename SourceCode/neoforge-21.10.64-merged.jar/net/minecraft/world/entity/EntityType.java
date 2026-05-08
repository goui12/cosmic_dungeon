package net.minecraft.world.entity;

import com.google.common.collect.ImmutableSet;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.DependantName;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cod;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Dolphin;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.HappyGhast;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.animal.Panda;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.PolarBear;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.Salmon;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.animal.TropicalFish;
import net.minecraft.world.entity.animal.Turtle;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.animal.armadillo.Armadillo;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.coppergolem.CopperGolem;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.frog.Tadpole;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.horse.Donkey;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.animal.horse.Mule;
import net.minecraft.world.entity.animal.horse.SkeletonHorse;
import net.minecraft.world.entity.animal.horse.TraderLlama;
import net.minecraft.world.entity.animal.horse.ZombieHorse;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.GlowItemFrame;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Bogged;
import net.minecraft.world.entity.monster.CaveSpider;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.monster.Evoker;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Giant;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Illusioner;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Stray;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.monster.breeze.Breeze;
import net.minecraft.world.entity.monster.creaking.Creaking;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.DragonFireball;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.entity.projectile.EyeOfEnder;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.projectile.LlamaSpit;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.SpectralArrow;
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.entity.projectile.ThrownExperienceBottle;
import net.minecraft.world.entity.projectile.ThrownLingeringPotion;
import net.minecraft.world.entity.projectile.ThrownSplashPotion;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.entity.projectile.windcharge.BreezeWindCharge;
import net.minecraft.world.entity.projectile.windcharge.WindCharge;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.ChestBoat;
import net.minecraft.world.entity.vehicle.ChestRaft;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.entity.vehicle.MinecartChest;
import net.minecraft.world.entity.vehicle.MinecartCommandBlock;
import net.minecraft.world.entity.vehicle.MinecartFurnace;
import net.minecraft.world.entity.vehicle.MinecartHopper;
import net.minecraft.world.entity.vehicle.MinecartSpawner;
import net.minecraft.world.entity.vehicle.MinecartTNT;
import net.minecraft.world.entity.vehicle.Raft;
import net.minecraft.world.flag.FeatureElement;
import net.minecraft.world.flag.FeatureFlag;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.slf4j.Logger;

public class EntityType<T extends Entity> implements FeatureElement, EntityTypeTest<Entity, T> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Holder.Reference<EntityType<?>> builtInRegistryHolder = BuiltInRegistries.ENTITY_TYPE.createIntrusiveHolder(this);
    public static final Codec<EntityType<?>> CODEC = BuiltInRegistries.ENTITY_TYPE.byNameCodec();
    public static final StreamCodec<RegistryFriendlyByteBuf, EntityType<?>> STREAM_CODEC = ByteBufCodecs.registry(Registries.ENTITY_TYPE);
    private static final float MAGIC_HORSE_WIDTH = 1.3964844F;
    private static final int DISPLAY_TRACKING_RANGE = 10;
    public static final EntityType<Boat> ACACIA_BOAT = register(
        "acacia_boat",
        EntityType.Builder.of(boatFactory(() -> Items.ACACIA_BOAT), MobCategory.MISC)
            .noLootTable()
            .sized(1.375F, 0.5625F)
            .eyeHeight(0.5625F)
            .clientTrackingRange(10)
    );
    public static final EntityType<ChestBoat> ACACIA_CHEST_BOAT = register(
        "acacia_chest_boat",
        EntityType.Builder.of(chestBoatFactory(() -> Items.ACACIA_CHEST_BOAT), MobCategory.MISC)
            .noLootTable()
            .sized(1.375F, 0.5625F)
            .eyeHeight(0.5625F)
            .clientTrackingRange(10)
    );
    public static final EntityType<Allay> ALLAY = register(
        "allay",
        EntityType.Builder.of(Allay::new, MobCategory.CREATURE)
            .sized(0.35F, 0.6F)
            .eyeHeight(0.36F)
            .ridingOffset(0.04F)
            .clientTrackingRange(8)
            .updateInterval(2)
    );
    public static final EntityType<AreaEffectCloud> AREA_EFFECT_CLOUD = register(
        "area_effect_cloud",
        EntityType.Builder.<AreaEffectCloud>of(AreaEffectCloud::new, MobCategory.MISC)
            .noLootTable()
            .fireImmune()
            .sized(6.0F, 0.5F)
            .clientTrackingRange(10)
            .updateInterval(Integer.MAX_VALUE)
    );
    public static final EntityType<Armadillo> ARMADILLO = register(
        "armadillo", EntityType.Builder.of(Armadillo::new, MobCategory.CREATURE).sized(0.7F, 0.65F).eyeHeight(0.26F).clientTrackingRange(10)
    );
    public static final EntityType<ArmorStand> ARMOR_STAND = register(
        "armor_stand", EntityType.Builder.<ArmorStand>of(ArmorStand::new, MobCategory.MISC).sized(0.5F, 1.975F).eyeHeight(1.7775F).clientTrackingRange(10)
    );
    public static final EntityType<Arrow> ARROW = register(
        "arrow",
        EntityType.Builder.<Arrow>of(Arrow::new, MobCategory.MISC).noLootTable().sized(0.5F, 0.5F).eyeHeight(0.13F).clientTrackingRange(4).updateInterval(20)
    );
    public static final EntityType<Axolotl> AXOLOTL = register(
        "axolotl", EntityType.Builder.of(Axolotl::new, MobCategory.AXOLOTLS).sized(0.75F, 0.42F).eyeHeight(0.2751F).clientTrackingRange(10)
    );
    public static final EntityType<ChestRaft> BAMBOO_CHEST_RAFT = register(
        "bamboo_chest_raft",
        EntityType.Builder.of(chestRaftFactory(() -> Items.BAMBOO_CHEST_RAFT), MobCategory.MISC)
            .noLootTable()
            .sized(1.375F, 0.5625F)
            .eyeHeight(0.5625F)
            .clientTrackingRange(10)
    );
    public static final EntityType<Raft> BAMBOO_RAFT = register(
        "bamboo_raft",
        EntityType.Builder.of(raftFactory(() -> Items.BAMBOO_RAFT), MobCategory.MISC)
            .noLootTable()
            .sized(1.375F, 0.5625F)
            .eyeHeight(0.5625F)
            .clientTrackingRange(10)
    );
    public static final EntityType<Bat> BAT = register(
        "bat", EntityType.Builder.of(Bat::new, MobCategory.AMBIENT).sized(0.5F, 0.9F).eyeHeight(0.45F).clientTrackingRange(5)
    );
    public static final EntityType<Bee> BEE = register(
        "bee", EntityType.Builder.of(Bee::new, MobCategory.CREATURE).sized(0.7F, 0.6F).eyeHeight(0.3F).clientTrackingRange(8)
    );
    public static final EntityType<Boat> BIRCH_BOAT = register(
        "birch_boat",
        EntityType.Builder.of(boatFactory(() -> Items.BIRCH_BOAT), MobCategory.MISC)
            .noLootTable()
            .sized(1.375F, 0.5625F)
            .eyeHeight(0.5625F)
            .clientTrackingRange(10)
    );
    public static final EntityType<ChestBoat> BIRCH_CHEST_BOAT = register(
        "birch_chest_boat",
        EntityType.Builder.of(chestBoatFactory(() -> Items.BIRCH_CHEST_BOAT), MobCategory.MISC)
            .noLootTable()
            .sized(1.375F, 0.5625F)
            .eyeHeight(0.5625F)
            .clientTrackingRange(10)
    );
    public static final EntityType<Blaze> BLAZE = register(
        "blaze", EntityType.Builder.of(Blaze::new, MobCategory.MONSTER).fireImmune().sized(0.6F, 1.8F).clientTrackingRange(8).notInPeaceful()
    );
    public static final EntityType<Display.BlockDisplay> BLOCK_DISPLAY = register(
        "block_display",
        EntityType.Builder.of(Display.BlockDisplay::new, MobCategory.MISC).noLootTable().sized(0.0F, 0.0F).clientTrackingRange(10).updateInterval(1)
    );
    public static final EntityType<Bogged> BOGGED = register(
        "bogged",
        EntityType.Builder.of(Bogged::new, MobCategory.MONSTER).sized(0.6F, 1.99F).eyeHeight(1.74F).ridingOffset(-0.7F).clientTrackingRange(8).notInPeaceful()
    );
    public static final EntityType<Breeze> BREEZE = register(
        "breeze", EntityType.Builder.of(Breeze::new, MobCategory.MONSTER).sized(0.6F, 1.77F).eyeHeight(1.3452F).clientTrackingRange(10).notInPeaceful()
    );
    public static final EntityType<BreezeWindCharge> BREEZE_WIND_CHARGE = register(
        "breeze_wind_charge",
        EntityType.Builder.<BreezeWindCharge>of(BreezeWindCharge::new, MobCategory.MISC)
            .noLootTable()
            .sized(0.3125F, 0.3125F)
            .eyeHeight(0.0F)
            .clientTrackingRange(4)
            .updateInterval(10)
    );
    public static final EntityType<Camel> CAMEL = register(
        "camel", EntityType.Builder.of(Camel::new, MobCategory.CREATURE).sized(1.7F, 2.375F).eyeHeight(2.275F).clientTrackingRange(10)
    );
    public static final EntityType<Cat> CAT = register(
        "cat", EntityType.Builder.of(Cat::new, MobCategory.CREATURE).sized(0.6F, 0.7F).eyeHeight(0.35F).passengerAttachments(0.5125F).clientTrackingRange(8)
    );
    public static final EntityType<CaveSpider> CAVE_SPIDER = register(
        "cave_spider", EntityType.Builder.of(CaveSpider::new, MobCategory.MONSTER).sized(0.7F, 0.5F).eyeHeight(0.45F).clientTrackingRange(8).notInPeaceful()
    );
    public static final EntityType<Boat> CHERRY_BOAT = register(
        "cherry_boat",
        EntityType.Builder.of(boatFactory(() -> Items.CHERRY_BOAT), MobCategory.MISC)
            .noLootTable()
            .sized(1.375F, 0.5625F)
            .eyeHeight(0.5625F)
            .clientTrackingRange(10)
    );
    public static final EntityType<ChestBoat> CHERRY_CHEST_BOAT = register(
        "cherry_chest_boat",
        EntityType.Builder.of(chestBoatFactory(() -> Items.CHERRY_CHEST_BOAT), MobCategory.MISC)
            .noLootTable()
            .sized(1.375F, 0.5625F)
            .eyeHeight(0.5625F)
            .clientTrackingRange(10)
    );
    public static final EntityType<MinecartChest> CHEST_MINECART = register(
        "chest_minecart",
        EntityType.Builder.of(MinecartChest::new, MobCategory.MISC).noLootTable().sized(0.98F, 0.7F).passengerAttachments(0.1875F).clientTrackingRange(8)
    );
    public static final EntityType<Chicken> CHICKEN = register(
        "chicken",
        EntityType.Builder.of(Chicken::new, MobCategory.CREATURE)
            .sized(0.4F, 0.7F)
            .eyeHeight(0.644F)
            .passengerAttachments(new Vec3(0.0, 0.7, -0.1))
            .clientTrackingRange(10)
    );
    public static final EntityType<Cod> COD = register(
        "cod", EntityType.Builder.of(Cod::new, MobCategory.WATER_AMBIENT).sized(0.5F, 0.3F).eyeHeight(0.195F).clientTrackingRange(4)
    );
    public static final EntityType<CopperGolem> COPPER_GOLEM = register(
        "copper_golem", EntityType.Builder.of(CopperGolem::new, MobCategory.MISC).sized(0.49F, 0.98F).eyeHeight(0.8125F).clientTrackingRange(10)
    );
    public static final EntityType<MinecartCommandBlock> COMMAND_BLOCK_MINECART = register(
        "command_block_minecart",
        EntityType.Builder.of(MinecartCommandBlock::new, MobCategory.MISC)
            .noLootTable()
            .sized(0.98F, 0.7F)
            .passengerAttachments(0.1875F)
            .clientTrackingRange(8)
    );
    public static final EntityType<Cow> COW = register(
        "cow", EntityType.Builder.of(Cow::new, MobCategory.CREATURE).sized(0.9F, 1.4F).eyeHeight(1.3F).passengerAttachments(1.36875F).clientTrackingRange(10)
    );
    public static final EntityType<Creaking> CREAKING = register(
        "creaking", EntityType.Builder.of(Creaking::new, MobCategory.MONSTER).sized(0.9F, 2.7F).eyeHeight(2.3F).clientTrackingRange(8).notInPeaceful()
    );
    public static final EntityType<Creeper> CREEPER = register(
        "creeper", EntityType.Builder.of(Creeper::new, MobCategory.MONSTER).sized(0.6F, 1.7F).clientTrackingRange(8).notInPeaceful()
    );
    public static final EntityType<Boat> DARK_OAK_BOAT = register(
        "dark_oak_boat",
        EntityType.Builder.of(boatFactory(() -> Items.DARK_OAK_BOAT), MobCategory.MISC)
            .noLootTable()
            .sized(1.375F, 0.5625F)
            .eyeHeight(0.5625F)
            .clientTrackingRange(10)
    );
    public static final EntityType<ChestBoat> DARK_OAK_CHEST_BOAT = register(
        "dark_oak_chest_boat",
        EntityType.Builder.of(chestBoatFactory(() -> Items.DARK_OAK_CHEST_BOAT), MobCategory.MISC)
            .noLootTable()
            .sized(1.375F, 0.5625F)
            .eyeHeight(0.5625F)
            .clientTrackingRange(10)
    );
    public static final EntityType<Dolphin> DOLPHIN = register(
        "dolphin", EntityType.Builder.of(Dolphin::new, MobCategory.WATER_CREATURE).sized(0.9F, 0.6F).eyeHeight(0.3F)
    );
    public static final EntityType<Donkey> DONKEY = register(
        "donkey",
        EntityType.Builder.of(Donkey::new, MobCategory.CREATURE)
            .sized(1.3964844F, 1.5F)
            .eyeHeight(1.425F)
            .passengerAttachments(1.1125F)
            .clientTrackingRange(10)
    );
    public static final EntityType<DragonFireball> DRAGON_FIREBALL = register(
        "dragon_fireball",
        EntityType.Builder.<DragonFireball>of(DragonFireball::new, MobCategory.MISC).noLootTable().sized(1.0F, 1.0F).clientTrackingRange(4).updateInterval(10)
    );
    public static final EntityType<Drowned> DROWNED = register(
        "drowned",
        EntityType.Builder.of(Drowned::new, MobCategory.MONSTER)
            .sized(0.6F, 1.95F)
            .eyeHeight(1.74F)
            .passengerAttachments(2.0125F)
            .ridingOffset(-0.7F)
            .clientTrackingRange(8)
            .notInPeaceful()
    );
    public static final EntityType<ThrownEgg> EGG = register(
        "egg", EntityType.Builder.<ThrownEgg>of(ThrownEgg::new, MobCategory.MISC).noLootTable().sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10)
    );
    public static final EntityType<ElderGuardian> ELDER_GUARDIAN = register(
        "elder_guardian",
        EntityType.Builder.of(ElderGuardian::new, MobCategory.MONSTER)
            .sized(1.9975F, 1.9975F)
            .eyeHeight(0.99875F)
            .passengerAttachments(2.350625F)
            .clientTrackingRange(10)
            .notInPeaceful()
    );
    public static final EntityType<EnderMan> ENDERMAN = register(
        "enderman",
        EntityType.Builder.of(EnderMan::new, MobCategory.MONSTER)
            .sized(0.6F, 2.9F)
            .eyeHeight(2.55F)
            .passengerAttachments(2.80625F)
            .clientTrackingRange(8)
            .notInPeaceful()
    );
    public static final EntityType<Endermite> ENDERMITE = register(
        "endermite",
        EntityType.Builder.of(Endermite::new, MobCategory.MONSTER)
            .sized(0.4F, 0.3F)
            .eyeHeight(0.13F)
            .passengerAttachments(0.2375F)
            .clientTrackingRange(8)
            .notInPeaceful()
    );
    public static final EntityType<EnderDragon> ENDER_DRAGON = register(
        "ender_dragon",
        EntityType.Builder.of(EnderDragon::new, MobCategory.MONSTER).fireImmune().sized(16.0F, 8.0F).passengerAttachments(3.0F).clientTrackingRange(10)
    );
    public static final EntityType<ThrownEnderpearl> ENDER_PEARL = register(
        "ender_pearl",
        EntityType.Builder.<ThrownEnderpearl>of(ThrownEnderpearl::new, MobCategory.MISC)
            .noLootTable()
            .sized(0.25F, 0.25F)
            .clientTrackingRange(4)
            .updateInterval(10)
    );
    public static final EntityType<EndCrystal> END_CRYSTAL = register(
        "end_crystal",
        EntityType.Builder.<EndCrystal>of(EndCrystal::new, MobCategory.MISC)
            .noLootTable()
            .fireImmune()
            .sized(2.0F, 2.0F)
            .clientTrackingRange(16)
            .updateInterval(Integer.MAX_VALUE)
    );
    public static final EntityType<Evoker> EVOKER = register(
        "evoker",
        EntityType.Builder.of(Evoker::new, MobCategory.MONSTER)
            .sized(0.6F, 1.95F)
            .passengerAttachments(2.0F)
            .ridingOffset(-0.6F)
            .clientTrackingRange(8)
            .notInPeaceful()
    );
    public static final EntityType<EvokerFangs> EVOKER_FANGS = register(
        "evoker_fangs",
        EntityType.Builder.<EvokerFangs>of(EvokerFangs::new, MobCategory.MISC).noLootTable().sized(0.5F, 0.8F).clientTrackingRange(6).updateInterval(2)
    );
    public static final EntityType<ThrownExperienceBottle> EXPERIENCE_BOTTLE = register(
        "experience_bottle",
        EntityType.Builder.<ThrownExperienceBottle>of(ThrownExperienceBottle::new, MobCategory.MISC)
            .noLootTable()
            .sized(0.25F, 0.25F)
            .clientTrackingRange(4)
            .updateInterval(10)
    );
    public static final EntityType<ExperienceOrb> EXPERIENCE_ORB = register(
        "experience_orb",
        EntityType.Builder.<ExperienceOrb>of(ExperienceOrb::new, MobCategory.MISC).noLootTable().sized(0.5F, 0.5F).clientTrackingRange(6).updateInterval(20)
    );
    public static final EntityType<EyeOfEnder> EYE_OF_ENDER = register(
        "eye_of_ender",
        EntityType.Builder.<EyeOfEnder>of(EyeOfEnder::new, MobCategory.MISC).noLootTable().sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(4)
    );
    public static final EntityType<FallingBlockEntity> FALLING_BLOCK = register(
        "falling_block",
        EntityType.Builder.<FallingBlockEntity>of(FallingBlockEntity::new, MobCategory.MISC)
            .noLootTable()
            .sized(0.98F, 0.98F)
            .clientTrackingRange(10)
            .updateInterval(20)
    );
    public static final EntityType<LargeFireball> FIREBALL = register(
        "fireball",
        EntityType.Builder.<LargeFireball>of(LargeFireball::new, MobCategory.MISC).noLootTable().sized(1.0F, 1.0F).clientTrackingRange(4).updateInterval(10)
    );
    public static final EntityType<FireworkRocketEntity> FIREWORK_ROCKET = register(
        "firework_rocket",
        EntityType.Builder.<FireworkRocketEntity>of(FireworkRocketEntity::new, MobCategory.MISC)
            .noLootTable()
            .sized(0.25F, 0.25F)
            .clientTrackingRange(4)
            .updateInterval(10)
    );
    public static final EntityType<Fox> FOX = register(
        "fox",
        EntityType.Builder.of(Fox::new, MobCategory.CREATURE)
            .sized(0.6F, 0.7F)
            .eyeHeight(0.4F)
            .passengerAttachments(new Vec3(0.0, 0.6375, -0.25))
            .clientTrackingRange(8)
            .immuneTo(Blocks.SWEET_BERRY_BUSH)
    );
    public static final EntityType<Frog> FROG = register(
        "frog",
        EntityType.Builder.of(Frog::new, MobCategory.CREATURE).sized(0.5F, 0.5F).passengerAttachments(new Vec3(0.0, 0.375, -0.25)).clientTrackingRange(10)
    );
    public static final EntityType<MinecartFurnace> FURNACE_MINECART = register(
        "furnace_minecart",
        EntityType.Builder.of(MinecartFurnace::new, MobCategory.MISC).noLootTable().sized(0.98F, 0.7F).passengerAttachments(0.1875F).clientTrackingRange(8)
    );
    public static final EntityType<Ghast> GHAST = register(
        "ghast",
        EntityType.Builder.of(Ghast::new, MobCategory.MONSTER)
            .fireImmune()
            .sized(4.0F, 4.0F)
            .eyeHeight(2.6F)
            .passengerAttachments(4.0625F)
            .ridingOffset(0.5F)
            .clientTrackingRange(10)
            .notInPeaceful()
    );
    public static final EntityType<HappyGhast> HAPPY_GHAST = register(
        "happy_ghast",
        EntityType.Builder.of(HappyGhast::new, MobCategory.CREATURE)
            .sized(4.0F, 4.0F)
            .eyeHeight(2.6F)
            .passengerAttachments(new Vec3(0.0, 4.0, 1.7), new Vec3(-1.7, 4.0, 0.0), new Vec3(0.0, 4.0, -1.7), new Vec3(1.7, 4.0, 0.0))
            .ridingOffset(0.5F)
            .clientTrackingRange(10)
    );
    public static final EntityType<Giant> GIANT = register(
        "giant",
        EntityType.Builder.of(Giant::new, MobCategory.MONSTER)
            .sized(3.6F, 12.0F)
            .eyeHeight(10.44F)
            .ridingOffset(-3.75F)
            .clientTrackingRange(10)
            .notInPeaceful()
    );
    public static final EntityType<GlowItemFrame> GLOW_ITEM_FRAME = register(
        "glow_item_frame",
        EntityType.Builder.<GlowItemFrame>of(GlowItemFrame::new, MobCategory.MISC)
            .noLootTable()
            .sized(0.5F, 0.5F)
            .eyeHeight(0.0F)
            .clientTrackingRange(10)
            .updateInterval(Integer.MAX_VALUE)
    );
    public static final EntityType<GlowSquid> GLOW_SQUID = register(
        "glow_squid", EntityType.Builder.of(GlowSquid::new, MobCategory.UNDERGROUND_WATER_CREATURE).sized(0.8F, 0.8F).eyeHeight(0.4F).clientTrackingRange(10)
    );
    public static final EntityType<Goat> GOAT = register(
        "goat", EntityType.Builder.of(Goat::new, MobCategory.CREATURE).sized(0.9F, 1.3F).passengerAttachments(1.1125F).clientTrackingRange(10)
    );
    public static final EntityType<Guardian> GUARDIAN = register(
        "guardian",
        EntityType.Builder.of(Guardian::new, MobCategory.MONSTER)
            .sized(0.85F, 0.85F)
            .eyeHeight(0.425F)
            .passengerAttachments(0.975F)
            .clientTrackingRange(8)
            .notInPeaceful()
    );
    public static final EntityType<Hoglin> HOGLIN = register(
        "hoglin", EntityType.Builder.of(Hoglin::new, MobCategory.MONSTER).sized(1.3964844F, 1.4F).passengerAttachments(1.49375F).clientTrackingRange(8)
    );
    public static final EntityType<MinecartHopper> HOPPER_MINECART = register(
        "hopper_minecart",
        EntityType.Builder.of(MinecartHopper::new, MobCategory.MISC).noLootTable().sized(0.98F, 0.7F).passengerAttachments(0.1875F).clientTrackingRange(8)
    );
    public static final EntityType<Horse> HORSE = register(
        "horse",
        EntityType.Builder.of(Horse::new, MobCategory.CREATURE).sized(1.3964844F, 1.6F).eyeHeight(1.52F).passengerAttachments(1.44375F).clientTrackingRange(10)
    );
    public static final EntityType<Husk> HUSK = register(
        "husk",
        EntityType.Builder.of(Husk::new, MobCategory.MONSTER)
            .sized(0.6F, 1.95F)
            .eyeHeight(1.74F)
            .passengerAttachments(2.075F)
            .ridingOffset(-0.7F)
            .clientTrackingRange(8)
            .notInPeaceful()
    );
    public static final EntityType<Illusioner> ILLUSIONER = register(
        "illusioner",
        EntityType.Builder.of(Illusioner::new, MobCategory.MONSTER)
            .sized(0.6F, 1.95F)
            .passengerAttachments(2.0F)
            .ridingOffset(-0.6F)
            .clientTrackingRange(8)
            .notInPeaceful()
    );
    public static final EntityType<Interaction> INTERACTION = register(
        "interaction", EntityType.Builder.of(Interaction::new, MobCategory.MISC).noLootTable().sized(0.0F, 0.0F).clientTrackingRange(10)
    );
    public static final EntityType<IronGolem> IRON_GOLEM = register(
        "iron_golem", EntityType.Builder.of(IronGolem::new, MobCategory.MISC).sized(1.4F, 2.7F).clientTrackingRange(10)
    );
    public static final EntityType<ItemEntity> ITEM = register(
        "item",
        EntityType.Builder.<ItemEntity>of(ItemEntity::new, MobCategory.MISC)
            .noLootTable()
            .sized(0.25F, 0.25F)
            .eyeHeight(0.2125F)
            .clientTrackingRange(6)
            .updateInterval(20)
    );
    public static final EntityType<Display.ItemDisplay> ITEM_DISPLAY = register(
        "item_display",
        EntityType.Builder.of(Display.ItemDisplay::new, MobCategory.MISC).noLootTable().sized(0.0F, 0.0F).clientTrackingRange(10).updateInterval(1)
    );
    public static final EntityType<ItemFrame> ITEM_FRAME = register(
        "item_frame",
        EntityType.Builder.<ItemFrame>of(ItemFrame::new, MobCategory.MISC)
            .noLootTable()
            .sized(0.5F, 0.5F)
            .eyeHeight(0.0F)
            .clientTrackingRange(10)
            .updateInterval(Integer.MAX_VALUE)
    );
    public static final EntityType<Boat> JUNGLE_BOAT = register(
        "jungle_boat",
        EntityType.Builder.of(boatFactory(() -> Items.JUNGLE_BOAT), MobCategory.MISC)
            .noLootTable()
            .sized(1.375F, 0.5625F)
            .eyeHeight(0.5625F)
            .clientTrackingRange(10)
    );
    public static final EntityType<ChestBoat> JUNGLE_CHEST_BOAT = register(
        "jungle_chest_boat",
        EntityType.Builder.of(chestBoatFactory(() -> Items.JUNGLE_CHEST_BOAT), MobCategory.MISC)
            .noLootTable()
            .sized(1.375F, 0.5625F)
            .eyeHeight(0.5625F)
            .clientTrackingRange(10)
    );
    public static final EntityType<LeashFenceKnotEntity> LEASH_KNOT = register(
        "leash_knot",
        EntityType.Builder.<LeashFenceKnotEntity>of(LeashFenceKnotEntity::new, MobCategory.MISC)
            .noLootTable()
            .noSave()
            .sized(0.375F, 0.5F)
            .eyeHeight(0.0625F)
            .clientTrackingRange(10)
            .updateInterval(Integer.MAX_VALUE)
    );
    public static final EntityType<LightningBolt> LIGHTNING_BOLT = register(
        "lightning_bolt",
        EntityType.Builder.of(LightningBolt::new, MobCategory.MISC)
            .noLootTable()
            .noSave()
            .sized(0.0F, 0.0F)
            .clientTrackingRange(16)
            .updateInterval(Integer.MAX_VALUE)
    );
    public static final EntityType<Llama> LLAMA = register(
        "llama",
        EntityType.Builder.of(Llama::new, MobCategory.CREATURE)
            .sized(0.9F, 1.87F)
            .eyeHeight(1.7765F)
            .passengerAttachments(new Vec3(0.0, 1.37, -0.3))
            .clientTrackingRange(10)
    );
    public static final EntityType<LlamaSpit> LLAMA_SPIT = register(
        "llama_spit",
        EntityType.Builder.<LlamaSpit>of(LlamaSpit::new, MobCategory.MISC).noLootTable().sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10)
    );
    public static final EntityType<MagmaCube> MAGMA_CUBE = register(
        "magma_cube",
        EntityType.Builder.of(MagmaCube::new, MobCategory.MONSTER)
            .fireImmune()
            .sized(0.52F, 0.52F)
            .eyeHeight(0.325F)
            .spawnDimensionsScale(4.0F)
            .clientTrackingRange(8)
            .notInPeaceful()
    );
    public static final EntityType<Boat> MANGROVE_BOAT = register(
        "mangrove_boat",
        EntityType.Builder.of(boatFactory(() -> Items.MANGROVE_BOAT), MobCategory.MISC)
            .noLootTable()
            .sized(1.375F, 0.5625F)
            .eyeHeight(0.5625F)
            .clientTrackingRange(10)
    );
    public static final EntityType<ChestBoat> MANGROVE_CHEST_BOAT = register(
        "mangrove_chest_boat",
        EntityType.Builder.of(chestBoatFactory(() -> Items.MANGROVE_CHEST_BOAT), MobCategory.MISC)
            .noLootTable()
            .sized(1.375F, 0.5625F)
            .eyeHeight(0.5625F)
            .clientTrackingRange(10)
    );
    public static final EntityType<Mannequin> MANNEQUIN = register(
        "mannequin",
        EntityType.Builder.of(Mannequin::create, MobCategory.MISC)
            .sized(0.6F, 1.8F)
            .eyeHeight(1.62F)
            .vehicleAttachment(Avatar.DEFAULT_VEHICLE_ATTACHMENT)
            .clientTrackingRange(32)
            .updateInterval(2)
    );
    public static final EntityType<Marker> MARKER = register(
        "marker", EntityType.Builder.of(Marker::new, MobCategory.MISC).noLootTable().sized(0.0F, 0.0F).clientTrackingRange(0)
    );
    public static final EntityType<Minecart> MINECART = register(
        "minecart",
        EntityType.Builder.of(Minecart::new, MobCategory.MISC).noLootTable().sized(0.98F, 0.7F).passengerAttachments(0.1875F).clientTrackingRange(8)
    );
    public static final EntityType<MushroomCow> MOOSHROOM = register(
        "mooshroom",
        EntityType.Builder.of(MushroomCow::new, MobCategory.CREATURE).sized(0.9F, 1.4F).eyeHeight(1.3F).passengerAttachments(1.36875F).clientTrackingRange(10)
    );
    public static final EntityType<Mule> MULE = register(
        "mule",
        EntityType.Builder.of(Mule::new, MobCategory.CREATURE).sized(1.3964844F, 1.6F).eyeHeight(1.52F).passengerAttachments(1.2125F).clientTrackingRange(8)
    );
    public static final EntityType<Boat> OAK_BOAT = register(
        "oak_boat",
        EntityType.Builder.of(boatFactory(() -> Items.OAK_BOAT), MobCategory.MISC)
            .noLootTable()
            .sized(1.375F, 0.5625F)
            .eyeHeight(0.5625F)
            .clientTrackingRange(10)
    );
    public static final EntityType<ChestBoat> OAK_CHEST_BOAT = register(
        "oak_chest_boat",
        EntityType.Builder.of(chestBoatFactory(() -> Items.OAK_CHEST_BOAT), MobCategory.MISC)
            .noLootTable()
            .sized(1.375F, 0.5625F)
            .eyeHeight(0.5625F)
            .clientTrackingRange(10)
    );
    public static final EntityType<Ocelot> OCELOT = register(
        "ocelot", EntityType.Builder.of(Ocelot::new, MobCategory.CREATURE).sized(0.6F, 0.7F).passengerAttachments(0.6375F).clientTrackingRange(10)
    );
    public static final EntityType<OminousItemSpawner> OMINOUS_ITEM_SPAWNER = register(
        "ominous_item_spawner", EntityType.Builder.of(OminousItemSpawner::new, MobCategory.MISC).noLootTable().sized(0.25F, 0.25F).clientTrackingRange(8)
    );
    public static final EntityType<Painting> PAINTING = register(
        "painting",
        EntityType.Builder.<Painting>of(Painting::new, MobCategory.MISC)
            .noLootTable()
            .sized(0.5F, 0.5F)
            .clientTrackingRange(10)
            .updateInterval(Integer.MAX_VALUE)
    );
    public static final EntityType<Boat> PALE_OAK_BOAT = register(
        "pale_oak_boat",
        EntityType.Builder.of(boatFactory(() -> Items.PALE_OAK_BOAT), MobCategory.MISC)
            .noLootTable()
            .sized(1.375F, 0.5625F)
            .eyeHeight(0.5625F)
            .clientTrackingRange(10)
    );
    public static final EntityType<ChestBoat> PALE_OAK_CHEST_BOAT = register(
        "pale_oak_chest_boat",
        EntityType.Builder.of(chestBoatFactory(() -> Items.PALE_OAK_CHEST_BOAT), MobCategory.MISC)
            .noLootTable()
            .sized(1.375F, 0.5625F)
            .eyeHeight(0.5625F)
            .clientTrackingRange(10)
    );
    public static final EntityType<Panda> PANDA = register(
        "panda", EntityType.Builder.of(Panda::new, MobCategory.CREATURE).sized(1.3F, 1.25F).clientTrackingRange(10)
    );
    public static final EntityType<Parrot> PARROT = register(
        "parrot",
        EntityType.Builder.of(Parrot::new, MobCategory.CREATURE).sized(0.5F, 0.9F).eyeHeight(0.54F).passengerAttachments(0.4625F).clientTrackingRange(8)
    );
    public static final EntityType<Phantom> PHANTOM = register(
        "phantom",
        EntityType.Builder.of(Phantom::new, MobCategory.MONSTER)
            .sized(0.9F, 0.5F)
            .eyeHeight(0.175F)
            .passengerAttachments(0.3375F)
            .ridingOffset(-0.125F)
            .clientTrackingRange(8)
            .notInPeaceful()
    );
    public static final EntityType<Pig> PIG = register(
        "pig", EntityType.Builder.of(Pig::new, MobCategory.CREATURE).sized(0.9F, 0.9F).passengerAttachments(0.86875F).clientTrackingRange(10)
    );
    public static final EntityType<Piglin> PIGLIN = register(
        "piglin",
        EntityType.Builder.of(Piglin::new, MobCategory.MONSTER)
            .sized(0.6F, 1.95F)
            .eyeHeight(1.79F)
            .passengerAttachments(2.0125F)
            .ridingOffset(-0.7F)
            .clientTrackingRange(8)
    );
    public static final EntityType<PiglinBrute> PIGLIN_BRUTE = register(
        "piglin_brute",
        EntityType.Builder.of(PiglinBrute::new, MobCategory.MONSTER)
            .sized(0.6F, 1.95F)
            .eyeHeight(1.79F)
            .passengerAttachments(2.0125F)
            .ridingOffset(-0.7F)
            .clientTrackingRange(8)
            .notInPeaceful()
    );
    public static final EntityType<Pillager> PILLAGER = register(
        "pillager",
        EntityType.Builder.of(Pillager::new, MobCategory.MONSTER)
            .canSpawnFarFromPlayer()
            .sized(0.6F, 1.95F)
            .passengerAttachments(2.0F)
            .ridingOffset(-0.6F)
            .clientTrackingRange(8)
            .notInPeaceful()
    );
    public static final EntityType<PolarBear> POLAR_BEAR = register(
        "polar_bear", EntityType.Builder.of(PolarBear::new, MobCategory.CREATURE).immuneTo(Blocks.POWDER_SNOW).sized(1.4F, 1.4F).clientTrackingRange(10)
    );
    public static final EntityType<ThrownSplashPotion> SPLASH_POTION = register(
        "splash_potion",
        EntityType.Builder.<ThrownSplashPotion>of(ThrownSplashPotion::new, MobCategory.MISC)
            .noLootTable()
            .sized(0.25F, 0.25F)
            .clientTrackingRange(4)
            .updateInterval(10)
    );
    public static final EntityType<ThrownLingeringPotion> LINGERING_POTION = register(
        "lingering_potion",
        EntityType.Builder.<ThrownLingeringPotion>of(ThrownLingeringPotion::new, MobCategory.MISC)
            .noLootTable()
            .sized(0.25F, 0.25F)
            .clientTrackingRange(4)
            .updateInterval(10)
    );
    public static final EntityType<Pufferfish> PUFFERFISH = register(
        "pufferfish", EntityType.Builder.of(Pufferfish::new, MobCategory.WATER_AMBIENT).sized(0.7F, 0.7F).eyeHeight(0.455F).clientTrackingRange(4)
    );
    public static final EntityType<Rabbit> RABBIT = register(
        "rabbit", EntityType.Builder.of(Rabbit::new, MobCategory.CREATURE).sized(0.4F, 0.5F).clientTrackingRange(8)
    );
    public static final EntityType<Ravager> RAVAGER = register(
        "ravager",
        EntityType.Builder.of(Ravager::new, MobCategory.MONSTER)
            .sized(1.95F, 2.2F)
            .passengerAttachments(new Vec3(0.0, 2.2625, -0.0625))
            .clientTrackingRange(10)
            .notInPeaceful()
    );
    public static final EntityType<Salmon> SALMON = register(
        "salmon", EntityType.Builder.of(Salmon::new, MobCategory.WATER_AMBIENT).sized(0.7F, 0.4F).eyeHeight(0.26F).clientTrackingRange(4)
    );
    public static final EntityType<Sheep> SHEEP = register(
        "sheep",
        EntityType.Builder.of(Sheep::new, MobCategory.CREATURE).sized(0.9F, 1.3F).eyeHeight(1.235F).passengerAttachments(1.2375F).clientTrackingRange(10)
    );
    public static final EntityType<Shulker> SHULKER = register(
        "shulker",
        EntityType.Builder.of(Shulker::new, MobCategory.MONSTER).fireImmune().canSpawnFarFromPlayer().sized(1.0F, 1.0F).eyeHeight(0.5F).clientTrackingRange(10)
    );
    public static final EntityType<ShulkerBullet> SHULKER_BULLET = register(
        "shulker_bullet",
        EntityType.Builder.<ShulkerBullet>of(ShulkerBullet::new, MobCategory.MISC).noLootTable().sized(0.3125F, 0.3125F).clientTrackingRange(8)
    );
    public static final EntityType<Silverfish> SILVERFISH = register(
        "silverfish",
        EntityType.Builder.of(Silverfish::new, MobCategory.MONSTER)
            .sized(0.4F, 0.3F)
            .eyeHeight(0.13F)
            .passengerAttachments(0.2375F)
            .clientTrackingRange(8)
            .notInPeaceful()
    );
    public static final EntityType<Skeleton> SKELETON = register(
        "skeleton",
        EntityType.Builder.of(Skeleton::new, MobCategory.MONSTER)
            .sized(0.6F, 1.99F)
            .eyeHeight(1.74F)
            .ridingOffset(-0.7F)
            .clientTrackingRange(8)
            .notInPeaceful()
    );
    public static final EntityType<SkeletonHorse> SKELETON_HORSE = register(
        "skeleton_horse",
        EntityType.Builder.of(SkeletonHorse::new, MobCategory.CREATURE)
            .sized(1.3964844F, 1.6F)
            .eyeHeight(1.52F)
            .passengerAttachments(1.31875F)
            .clientTrackingRange(10)
    );
    public static final EntityType<Slime> SLIME = register(
        "slime",
        EntityType.Builder.of(Slime::new, MobCategory.MONSTER)
            .sized(0.52F, 0.52F)
            .eyeHeight(0.325F)
            .spawnDimensionsScale(4.0F)
            .clientTrackingRange(10)
            .notInPeaceful()
    );
    public static final EntityType<SmallFireball> SMALL_FIREBALL = register(
        "small_fireball",
        EntityType.Builder.<SmallFireball>of(SmallFireball::new, MobCategory.MISC)
            .noLootTable()
            .sized(0.3125F, 0.3125F)
            .clientTrackingRange(4)
            .updateInterval(10)
    );
    public static final EntityType<Sniffer> SNIFFER = register(
        "sniffer",
        EntityType.Builder.of(Sniffer::new, MobCategory.CREATURE)
            .sized(1.9F, 1.75F)
            .eyeHeight(1.05F)
            .passengerAttachments(2.09375F)
            .nameTagOffset(2.05F)
            .clientTrackingRange(10)
    );
    public static final EntityType<Snowball> SNOWBALL = register(
        "snowball",
        EntityType.Builder.<Snowball>of(Snowball::new, MobCategory.MISC).noLootTable().sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10)
    );
    public static final EntityType<SnowGolem> SNOW_GOLEM = register(
        "snow_golem",
        EntityType.Builder.of(SnowGolem::new, MobCategory.MISC).immuneTo(Blocks.POWDER_SNOW).sized(0.7F, 1.9F).eyeHeight(1.7F).clientTrackingRange(8)
    );
    public static final EntityType<MinecartSpawner> SPAWNER_MINECART = register(
        "spawner_minecart",
        EntityType.Builder.of(MinecartSpawner::new, MobCategory.MISC).noLootTable().sized(0.98F, 0.7F).passengerAttachments(0.1875F).clientTrackingRange(8)
    );
    public static final EntityType<SpectralArrow> SPECTRAL_ARROW = register(
        "spectral_arrow",
        EntityType.Builder.<SpectralArrow>of(SpectralArrow::new, MobCategory.MISC)
            .noLootTable()
            .sized(0.5F, 0.5F)
            .eyeHeight(0.13F)
            .clientTrackingRange(4)
            .updateInterval(20)
    );
    public static final EntityType<Spider> SPIDER = register(
        "spider",
        EntityType.Builder.of(Spider::new, MobCategory.MONSTER)
            .sized(1.4F, 0.9F)
            .eyeHeight(0.65F)
            .passengerAttachments(0.765F)
            .clientTrackingRange(8)
            .notInPeaceful()
    );
    public static final EntityType<Boat> SPRUCE_BOAT = register(
        "spruce_boat",
        EntityType.Builder.of(boatFactory(() -> Items.SPRUCE_BOAT), MobCategory.MISC)
            .noLootTable()
            .sized(1.375F, 0.5625F)
            .eyeHeight(0.5625F)
            .clientTrackingRange(10)
    );
    public static final EntityType<ChestBoat> SPRUCE_CHEST_BOAT = register(
        "spruce_chest_boat",
        EntityType.Builder.of(chestBoatFactory(() -> Items.SPRUCE_CHEST_BOAT), MobCategory.MISC)
            .noLootTable()
            .sized(1.375F, 0.5625F)
            .eyeHeight(0.5625F)
            .clientTrackingRange(10)
    );
    public static final EntityType<Squid> SQUID = register(
        "squid", EntityType.Builder.of(Squid::new, MobCategory.WATER_CREATURE).sized(0.8F, 0.8F).eyeHeight(0.4F).clientTrackingRange(8)
    );
    public static final EntityType<Stray> STRAY = register(
        "stray",
        EntityType.Builder.of(Stray::new, MobCategory.MONSTER)
            .sized(0.6F, 1.99F)
            .eyeHeight(1.74F)
            .ridingOffset(-0.7F)
            .immuneTo(Blocks.POWDER_SNOW)
            .clientTrackingRange(8)
            .notInPeaceful()
    );
    public static final EntityType<Strider> STRIDER = register(
        "strider", EntityType.Builder.of(Strider::new, MobCategory.CREATURE).fireImmune().sized(0.9F, 1.7F).clientTrackingRange(10)
    );
    public static final EntityType<Tadpole> TADPOLE = register(
        "tadpole", EntityType.Builder.of(Tadpole::new, MobCategory.CREATURE).sized(0.4F, 0.3F).eyeHeight(0.19500001F).clientTrackingRange(10)
    );
    public static final EntityType<Display.TextDisplay> TEXT_DISPLAY = register(
        "text_display",
        EntityType.Builder.of(Display.TextDisplay::new, MobCategory.MISC).noLootTable().sized(0.0F, 0.0F).clientTrackingRange(10).updateInterval(1)
    );
    public static final EntityType<PrimedTnt> TNT = register(
        "tnt",
        EntityType.Builder.<PrimedTnt>of(PrimedTnt::new, MobCategory.MISC)
            .noLootTable()
            .fireImmune()
            .sized(0.98F, 0.98F)
            .eyeHeight(0.15F)
            .clientTrackingRange(10)
            .updateInterval(10)
    );
    public static final EntityType<MinecartTNT> TNT_MINECART = register(
        "tnt_minecart",
        EntityType.Builder.of(MinecartTNT::new, MobCategory.MISC).noLootTable().sized(0.98F, 0.7F).passengerAttachments(0.1875F).clientTrackingRange(8)
    );
    public static final EntityType<TraderLlama> TRADER_LLAMA = register(
        "trader_llama",
        EntityType.Builder.of(TraderLlama::new, MobCategory.CREATURE)
            .sized(0.9F, 1.87F)
            .eyeHeight(1.7765F)
            .passengerAttachments(new Vec3(0.0, 1.37, -0.3))
            .clientTrackingRange(10)
    );
    public static final EntityType<ThrownTrident> TRIDENT = register(
        "trident",
        EntityType.Builder.<ThrownTrident>of(ThrownTrident::new, MobCategory.MISC)
            .noLootTable()
            .sized(0.5F, 0.5F)
            .eyeHeight(0.13F)
            .clientTrackingRange(4)
            .updateInterval(20)
    );
    public static final EntityType<TropicalFish> TROPICAL_FISH = register(
        "tropical_fish", EntityType.Builder.of(TropicalFish::new, MobCategory.WATER_AMBIENT).sized(0.5F, 0.4F).eyeHeight(0.26F).clientTrackingRange(4)
    );
    public static final EntityType<Turtle> TURTLE = register(
        "turtle",
        EntityType.Builder.of(Turtle::new, MobCategory.CREATURE).sized(1.2F, 0.4F).passengerAttachments(new Vec3(0.0, 0.55625, -0.25)).clientTrackingRange(10)
    );
    public static final EntityType<Vex> VEX = register(
        "vex",
        EntityType.Builder.of(Vex::new, MobCategory.MONSTER)
            .fireImmune()
            .sized(0.4F, 0.8F)
            .eyeHeight(0.51875F)
            .passengerAttachments(0.7375F)
            .ridingOffset(0.04F)
            .clientTrackingRange(8)
            .notInPeaceful()
    );
    public static final EntityType<Villager> VILLAGER = register(
        "villager", EntityType.Builder.<Villager>of(Villager::new, MobCategory.MISC).sized(0.6F, 1.95F).eyeHeight(1.62F).clientTrackingRange(10)
    );
    public static final EntityType<Vindicator> VINDICATOR = register(
        "vindicator",
        EntityType.Builder.of(Vindicator::new, MobCategory.MONSTER)
            .sized(0.6F, 1.95F)
            .passengerAttachments(2.0F)
            .ridingOffset(-0.6F)
            .clientTrackingRange(8)
            .notInPeaceful()
    );
    public static final EntityType<WanderingTrader> WANDERING_TRADER = register(
        "wandering_trader", EntityType.Builder.of(WanderingTrader::new, MobCategory.CREATURE).sized(0.6F, 1.95F).eyeHeight(1.62F).clientTrackingRange(10)
    );
    public static final EntityType<Warden> WARDEN = register(
        "warden",
        EntityType.Builder.of(Warden::new, MobCategory.MONSTER)
            .sized(0.9F, 2.9F)
            .passengerAttachments(3.15F)
            .attach(EntityAttachment.WARDEN_CHEST, 0.0F, 1.6F, 0.0F)
            .clientTrackingRange(16)
            .fireImmune()
            .notInPeaceful()
    );
    public static final EntityType<WindCharge> WIND_CHARGE = register(
        "wind_charge",
        EntityType.Builder.<WindCharge>of(WindCharge::new, MobCategory.MISC)
            .noLootTable()
            .sized(0.3125F, 0.3125F)
            .eyeHeight(0.0F)
            .clientTrackingRange(4)
            .updateInterval(10)
    );
    public static final EntityType<Witch> WITCH = register(
        "witch",
        EntityType.Builder.of(Witch::new, MobCategory.MONSTER)
            .sized(0.6F, 1.95F)
            .eyeHeight(1.62F)
            .passengerAttachments(2.2625F)
            .clientTrackingRange(8)
            .notInPeaceful()
    );
    public static final EntityType<WitherBoss> WITHER = register(
        "wither",
        EntityType.Builder.of(WitherBoss::new, MobCategory.MONSTER)
            .fireImmune()
            .immuneTo(Blocks.WITHER_ROSE)
            .sized(0.9F, 3.5F)
            .clientTrackingRange(10)
            .notInPeaceful()
    );
    public static final EntityType<WitherSkeleton> WITHER_SKELETON = register(
        "wither_skeleton",
        EntityType.Builder.of(WitherSkeleton::new, MobCategory.MONSTER)
            .fireImmune()
            .immuneTo(Blocks.WITHER_ROSE)
            .sized(0.7F, 2.4F)
            .eyeHeight(2.1F)
            .ridingOffset(-0.875F)
            .clientTrackingRange(8)
            .notInPeaceful()
    );
    public static final EntityType<WitherSkull> WITHER_SKULL = register(
        "wither_skull",
        EntityType.Builder.<WitherSkull>of(WitherSkull::new, MobCategory.MISC).noLootTable().sized(0.3125F, 0.3125F).clientTrackingRange(4).updateInterval(10)
    );
    public static final EntityType<Wolf> WOLF = register(
        "wolf",
        EntityType.Builder.of(Wolf::new, MobCategory.CREATURE)
            .sized(0.6F, 0.85F)
            .eyeHeight(0.68F)
            .passengerAttachments(new Vec3(0.0, 0.81875, -0.0625))
            .clientTrackingRange(10)
    );
    public static final EntityType<Zoglin> ZOGLIN = register(
        "zoglin",
        EntityType.Builder.of(Zoglin::new, MobCategory.MONSTER)
            .fireImmune()
            .sized(1.3964844F, 1.4F)
            .passengerAttachments(1.49375F)
            .clientTrackingRange(8)
            .notInPeaceful()
    );
    public static final EntityType<Zombie> ZOMBIE = register(
        "zombie",
        EntityType.Builder.<Zombie>of(Zombie::new, MobCategory.MONSTER)
            .sized(0.6F, 1.95F)
            .eyeHeight(1.74F)
            .passengerAttachments(2.0125F)
            .ridingOffset(-0.7F)
            .clientTrackingRange(8)
            .notInPeaceful()
    );
    public static final EntityType<ZombieHorse> ZOMBIE_HORSE = register(
        "zombie_horse",
        EntityType.Builder.of(ZombieHorse::new, MobCategory.CREATURE)
            .sized(1.3964844F, 1.6F)
            .eyeHeight(1.52F)
            .passengerAttachments(1.31875F)
            .clientTrackingRange(10)
    );
    public static final EntityType<ZombieVillager> ZOMBIE_VILLAGER = register(
        "zombie_villager",
        EntityType.Builder.of(ZombieVillager::new, MobCategory.MONSTER)
            .sized(0.6F, 1.95F)
            .passengerAttachments(2.125F)
            .ridingOffset(-0.7F)
            .eyeHeight(1.74F)
            .clientTrackingRange(8)
            .notInPeaceful()
    );
    public static final EntityType<ZombifiedPiglin> ZOMBIFIED_PIGLIN = register(
        "zombified_piglin",
        EntityType.Builder.of(ZombifiedPiglin::new, MobCategory.MONSTER)
            .fireImmune()
            .sized(0.6F, 1.95F)
            .eyeHeight(1.79F)
            .passengerAttachments(2.0F)
            .ridingOffset(-0.7F)
            .clientTrackingRange(8)
            .notInPeaceful()
    );
    public static final EntityType<Player> PLAYER = register(
        "player",
        EntityType.Builder.<Player>createNothing(MobCategory.MISC)
            .noSave()
            .noSummon()
            .sized(0.6F, 1.8F)
            .eyeHeight(1.62F)
            .vehicleAttachment(Avatar.DEFAULT_VEHICLE_ATTACHMENT)
            .clientTrackingRange(32)
            .updateInterval(2)
    );
    public static final EntityType<FishingHook> FISHING_BOBBER = register(
        "fishing_bobber",
        EntityType.Builder.<FishingHook>of(FishingHook::new, MobCategory.MISC)
            .noLootTable()
            .noSave()
            .noSummon()
            .sized(0.25F, 0.25F)
            .clientTrackingRange(4)
            .updateInterval(5)
    );
    private static final Set<EntityType<?>> OP_ONLY_CUSTOM_DATA = Set.of(FALLING_BLOCK, COMMAND_BLOCK_MINECART, SPAWNER_MINECART);
    private final EntityType.EntityFactory<T> factory;
    private final MobCategory category;
    private final ImmutableSet<Block> immuneTo;
    private final boolean serialize;
    private final boolean summon;
    private final boolean fireImmune;
    private final boolean canSpawnFarFromPlayer;
    private final int clientTrackingRange;
    private final int updateInterval;
    private final String descriptionId;
    @Nullable
    private Component description;
    private final Optional<ResourceKey<LootTable>> lootTable;
    private final EntityDimensions dimensions;
    private final float spawnDimensionsScale;
    private final FeatureFlagSet requiredFeatures;
    private final boolean allowedInPeaceful;

    private final java.util.function.Predicate<EntityType<?>> trackDeltasSupplier;
    private final java.util.function.ToIntFunction<EntityType<?>> trackingRangeSupplier;
    private final java.util.function.ToIntFunction<EntityType<?>> updateIntervalSupplier;
    private final boolean onlyOpCanSetNbt;

    private static <T extends Entity> EntityType<T> register(ResourceKey<EntityType<?>> key, EntityType.Builder<T> builder) {
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
    }

    private static ResourceKey<EntityType<?>> vanillaEntityId(String name) {
        return ResourceKey.create(Registries.ENTITY_TYPE, ResourceLocation.withDefaultNamespace(name));
    }

    private static <T extends Entity> EntityType<T> register(String key, EntityType.Builder<T> builder) {
        return register(vanillaEntityId(key), builder);
    }

    public static ResourceLocation getKey(EntityType<?> entityType) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
    }

    /**
     * Tries to get the entity type associated by the key.
     */
    public static Optional<EntityType<?>> byString(String key) {
        return BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.tryParse(key));
    }

    public EntityType(
        EntityType.EntityFactory<T> factory,
        MobCategory category,
        boolean serialize,
        boolean summon,
        boolean fireImmune,
        boolean canSpawnFarFromPlayer,
        ImmutableSet<Block> immuneTo,
        EntityDimensions dimensions,
        float spawnDimensionsScale,
        int clientTrackingRange,
        int updateInterval,
        String descriptionId,
        Optional<ResourceKey<LootTable>> lootTable,
        FeatureFlagSet requiredFeatures,
        boolean allowdInPeaceful
    ) {
        this(factory, category, serialize, summon, fireImmune, canSpawnFarFromPlayer, immuneTo, dimensions, spawnDimensionsScale, clientTrackingRange, updateInterval, descriptionId, lootTable, requiredFeatures, allowdInPeaceful, EntityType::defaultTrackDeltasSupplier, EntityType::defaultTrackingRangeSupplier, EntityType::defaultUpdateIntervalSupplier, false);
    }

    public EntityType(
            EntityType.EntityFactory<T> factory,
            MobCategory category,
            boolean serialize,
            boolean summon,
            boolean fireImmune,
            boolean canSpawnFarFromPlayer,
            ImmutableSet<Block> immuneTo,
            EntityDimensions dimensions,
            float spawnDimensionsScale,
            int clientTrackingRange,
            int updateInterval,
            String descriptionId,
            Optional<ResourceKey<LootTable>> lootTable,
            FeatureFlagSet requiredFeatures,
            boolean allowdInPeaceful,
            final java.util.function.Predicate<EntityType<?>> trackDeltasSupplier,
            final java.util.function.ToIntFunction<EntityType<?>> trackingRangeSupplier,
            final java.util.function.ToIntFunction<EntityType<?>> updateIntervalSupplier,
            boolean onlyOpCanSetNbt
    ) {
        this.factory = factory;
        this.category = category;
        this.canSpawnFarFromPlayer = canSpawnFarFromPlayer;
        this.serialize = serialize;
        this.summon = summon;
        this.fireImmune = fireImmune;
        this.immuneTo = immuneTo;
        this.dimensions = dimensions;
        this.spawnDimensionsScale = spawnDimensionsScale;
        this.clientTrackingRange = clientTrackingRange;
        this.updateInterval = updateInterval;
        this.descriptionId = descriptionId;
        this.lootTable = lootTable;
        this.requiredFeatures = requiredFeatures;
        this.allowedInPeaceful = allowdInPeaceful;
        this.trackDeltasSupplier = trackDeltasSupplier;
        this.trackingRangeSupplier = trackingRangeSupplier;
        this.updateIntervalSupplier = updateIntervalSupplier;
        this.onlyOpCanSetNbt = onlyOpCanSetNbt;
    }

    @Nullable
    public T spawn(
        ServerLevel level,
        @Nullable ItemStack spawnedFrom,
        @Nullable LivingEntity owner,
        BlockPos pos,
        EntitySpawnReason reason,
        boolean shouldOffsetY,
        boolean shouldOffsetYMore
    ) {
        Consumer<T> consumer;
        if (spawnedFrom != null) {
            consumer = createDefaultStackConfig(level, spawnedFrom, owner);
        } else {
            consumer = p_263563_ -> {};
        }

        return this.spawn(level, consumer, pos, reason, shouldOffsetY, shouldOffsetYMore);
    }

    public static <T extends Entity> Consumer<T> createDefaultStackConfig(Level level, ItemStack stack, @Nullable LivingEntity owner) {
        return appendDefaultStackConfig(p_262561_ -> {}, level, stack, owner);
    }

    public static <T extends Entity> Consumer<T> appendDefaultStackConfig(
        Consumer<T> consumer, Level level, ItemStack stack, @Nullable LivingEntity owner
    ) {
        return appendCustomEntityStackConfig(appendComponentsConfig(consumer, stack), level, stack, owner);
    }

    public static <T extends Entity> Consumer<T> appendComponentsConfig(Consumer<T> consumer, ItemStack stack) {
        return consumer.andThen(p_396670_ -> p_396670_.applyComponentsFromItemStack(stack));
    }

    public static <T extends Entity> Consumer<T> appendCustomEntityStackConfig(
        Consumer<T> consumer, Level level, ItemStack stack, @Nullable LivingEntity owner
    ) {
        TypedEntityData<EntityType<?>> typedentitydata = stack.get(DataComponents.ENTITY_DATA);
        return typedentitydata != null ? consumer.andThen(p_432524_ -> updateCustomEntityTag(level, owner, p_432524_, typedentitydata)) : consumer;
    }

    @Nullable
    public T spawn(ServerLevel level, BlockPos pos, EntitySpawnReason reason) {
        return this.spawn(level, null, pos, reason, false, false);
    }

    @Nullable
    public T spawn(
        ServerLevel level, @Nullable Consumer<T> consumer, BlockPos pos, EntitySpawnReason reason, boolean shouldOffsetY, boolean shouldOffsetYMore
    ) {
        T t = this.create(level, consumer, pos, reason, shouldOffsetY, shouldOffsetYMore);
        if (t != null) {
            level.addFreshEntityWithPassengers(t);
            if (t instanceof Mob mob) {
                mob.playAmbientSound();
            }
        }

        return t;
    }

    @Nullable
    public T create(
        ServerLevel level, @Nullable Consumer<T> consumer, BlockPos pos, EntitySpawnReason reason, boolean shouldOffsetY, boolean shouldOffsetYMore
    ) {
        T t = this.create(level, reason);
        if (t == null) {
            return null;
        } else {
            double d0;
            if (shouldOffsetY) {
                t.setPos(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
                d0 = getYOffset(level, pos, shouldOffsetYMore, t.getBoundingBox());
            } else {
                d0 = 0.0;
            }

            t.snapTo(pos.getX() + 0.5, pos.getY() + d0, pos.getZ() + 0.5, Mth.wrapDegrees(level.random.nextFloat() * 360.0F), 0.0F);
            if (t instanceof Mob mob) {
                mob.yHeadRot = mob.getYRot();
                mob.yBodyRot = mob.getYRot();
                mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), reason, null);
                if (mob.isSpawnCancelled()) {
                    // Neo: Discard mob, spawn was cancelled
                    mob.discard();
                    // return null, mob was killed, context should be lost
                    // fixes llamas for wandering trader spawning if wandering trader was cancelled
                    return null;
                }
            }

            if (consumer != null) {
                consumer.accept(t);
            }

            return t;
        }
    }

    protected static double getYOffset(LevelReader level, BlockPos pos, boolean shouldOffsetYMore, AABB box) {
        AABB aabb = new AABB(pos);
        if (shouldOffsetYMore) {
            aabb = aabb.expandTowards(0.0, -1.0, 0.0);
        }

        Iterable<VoxelShape> iterable = level.getCollisions(null, aabb);
        return 1.0 + Shapes.collide(Direction.Axis.Y, box, iterable, shouldOffsetYMore ? -2.0 : -1.0);
    }

    public static void updateCustomEntityTag(
        Level level, @Nullable LivingEntity owner, @Nullable Entity entity, TypedEntityData<EntityType<?>> data
    ) {
        MinecraftServer minecraftserver = level.getServer();
        if (minecraftserver != null && entity != null) {
            if (entity.getType() == data.type()) {
                if (level.isClientSide()
                    || !entity.getType().onlyOpCanSetNbt()
                    || owner instanceof Player player && minecraftserver.getPlayerList().isOp(player.nameAndId())) {
                    data.loadInto(entity);
                }
            }
        }
    }

    public boolean canSerialize() {
        return this.serialize;
    }

    public boolean canSummon() {
        return this.summon;
    }

    public boolean fireImmune() {
        return this.fireImmune;
    }

    public boolean canSpawnFarFromPlayer() {
        return this.canSpawnFarFromPlayer;
    }

    public MobCategory getCategory() {
        return this.category;
    }

    public String getDescriptionId() {
        return this.descriptionId;
    }

    public Component getDescription() {
        if (this.description == null) {
            this.description = Component.translatable(this.getDescriptionId());
        }

        return this.description;
    }

    @Override
    public String toString() {
        return this.getDescriptionId();
    }

    public String toShortString() {
        int i = this.getDescriptionId().lastIndexOf(46);
        return i == -1 ? this.getDescriptionId() : this.getDescriptionId().substring(i + 1);
    }

    public Optional<ResourceKey<LootTable>> getDefaultLootTable() {
        return this.lootTable;
    }

    public float getWidth() {
        return this.dimensions.width();
    }

    public float getHeight() {
        return this.dimensions.height();
    }

    @Override
    public FeatureFlagSet requiredFeatures() {
        return this.requiredFeatures;
    }

    @Nullable
    public T create(Level level, EntitySpawnReason spawnReason) {
        return !this.isEnabled(level.enabledFeatures()) ? null : this.factory.create(this, level);
    }

    public static Optional<Entity> create(ValueInput input, Level level, EntitySpawnReason spawnReason) {
        return Util.ifElse(
            by(input).map(p_359692_ -> p_359692_.create(level, spawnReason)),
            p_421348_ -> p_421348_.load(input),
            () -> LOGGER.warn("Skipping Entity with id {}", input.getStringOr("id", "[invalid]"))
        );
    }

    public static Optional<Entity> create(EntityType<?> entityType, ValueInput input, Level level, EntitySpawnReason spawnReason) {
        Optional<Entity> optional = Optional.ofNullable(entityType.create(level, spawnReason));
        optional.ifPresent(p_432510_ -> p_432510_.load(input));
        return optional;
    }

    public AABB getSpawnAABB(double x, double y, double z) {
        float f = this.spawnDimensionsScale * this.getWidth() / 2.0F;
        float f1 = this.spawnDimensionsScale * this.getHeight();
        return new AABB(x - f, y, z - f, x + f, y + f1, z + f);
    }

    public boolean isBlockDangerous(BlockState state) {
        if (this.immuneTo.contains(state.getBlock())) {
            return false;
        } else {
            return !this.fireImmune && NodeEvaluator.isBurningBlock(state)
                ? true
                : state.is(Blocks.WITHER_ROSE) || state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.CACTUS) || state.is(Blocks.POWDER_SNOW);
        }
    }

    public EntityDimensions getDimensions() {
        return this.dimensions;
    }

    public static Optional<EntityType<?>> by(ValueInput input) {
        return input.read("id", CODEC);
    }

    @Nullable
    public static Entity loadEntityRecursive(CompoundTag entityTag, Level level, EntitySpawnReason spawnReason, Function<Entity, Entity> adapter) {
        Entity entity;
        try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(LOGGER)) {
            entity = loadEntityRecursive(
                TagValueInput.create(problemreporter$scopedcollector, level.registryAccess(), entityTag), level, spawnReason, adapter
            );
        }

        return entity;
    }

    @Nullable
    public static Entity loadEntityRecursive(
        EntityType<?> entityType, CompoundTag tag, Level level, EntitySpawnReason spawnReason, Function<Entity, Entity> adapter
    ) {
        Entity entity;
        try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(LOGGER)) {
            entity = loadEntityRecursive(
                entityType, TagValueInput.create(problemreporter$scopedcollector, level.registryAccess(), tag), level, spawnReason, adapter
            );
        }

        return entity;
    }

    @Nullable
    public static Entity loadEntityRecursive(ValueInput input, Level level, EntitySpawnReason spawnReason, Function<Entity, Entity> adapter) {
        return loadStaticEntity(input, level, spawnReason)
            .map(adapter)
            .map(p_432515_ -> loadPassengersRecursive(p_432515_, input, level, spawnReason, adapter))
            .orElse(null);
    }

    @Nullable
    public static Entity loadEntityRecursive(
        EntityType<?> entityType, ValueInput valueInput, Level level, EntitySpawnReason spawnReason, Function<Entity, Entity> adapter
    ) {
        return loadStaticEntity(entityType, valueInput, level, spawnReason)
            .map(adapter)
            .map(p_432520_ -> loadPassengersRecursive(p_432520_, valueInput, level, spawnReason, adapter))
            .orElse(null);
    }

    private static Entity loadPassengersRecursive(
        Entity entityType, ValueInput valueInput, Level level, EntitySpawnReason spawnReason, Function<Entity, Entity> adapter
    ) {
        for (ValueInput valueinput : valueInput.childrenListOrEmpty("Passengers")) {
            Entity entity = loadEntityRecursive(valueinput, level, spawnReason, adapter);
            if (entity != null) {
                entity.startRiding(entityType, true, false);
            }
        }

        return entityType;
    }

    public static Stream<Entity> loadEntitiesRecursive(ValueInput.ValueInputList input, Level level, EntitySpawnReason spawnReason) {
        return input.stream().mapMulti((p_421351_, p_421352_) -> loadEntityRecursive(p_421351_, level, spawnReason, p_404267_ -> {
            p_421352_.accept(p_404267_);
            return p_404267_;
        }));
    }

    private static Optional<Entity> loadStaticEntity(ValueInput input, Level level, EntitySpawnReason spawnReason) {
        try {
            return create(input, level, spawnReason);
        } catch (RuntimeException runtimeexception) {
            LOGGER.warn("Exception loading entity: ", (Throwable)runtimeexception);
            return Optional.empty();
        }
    }

    private static Optional<Entity> loadStaticEntity(EntityType<?> entityType, ValueInput valueInput, Level level, EntitySpawnReason spawnReason) {
        try {
            return create(entityType, valueInput, level, spawnReason);
        } catch (RuntimeException runtimeexception) {
            LOGGER.warn("Exception loading entity: ", (Throwable)runtimeexception);
            return Optional.empty();
        }
    }

    public int clientTrackingRange() {
        return trackingRangeSupplier.applyAsInt(this);
    }
    private int defaultTrackingRangeSupplier() {
        return this.clientTrackingRange;
    }

    public int updateInterval() {
        return updateIntervalSupplier.applyAsInt(this);
    }
    private int defaultUpdateIntervalSupplier() {
        return this.updateInterval;
    }

    public boolean trackDeltas() {
        return this.trackDeltasSupplier.test(this);
    }
    private boolean defaultTrackDeltasSupplier() {
        return this != PLAYER
            && this != LLAMA_SPIT
            && this != WITHER
            && this != BAT
            && this != ITEM_FRAME
            && this != GLOW_ITEM_FRAME
            && this != LEASH_KNOT
            && this != PAINTING
            && this != END_CRYSTAL
            && this != EVOKER_FANGS;
    }

    /**
     * Checks if this entity type is contained in the tag
     */
    public boolean is(TagKey<EntityType<?>> tag) {
        return this.builtInRegistryHolder.is(tag);
    }

    public boolean is(HolderSet<EntityType<?>> entityType) {
        return entityType.contains(this.builtInRegistryHolder);
    }

    @Nullable
    public T tryCast(Entity entity) {
        return (T)(entity.getType() == this ? entity : null);
    }

    @Override
    public Class<? extends Entity> getBaseClass() {
        return Entity.class;
    }

    @Deprecated
    public Holder.Reference<EntityType<?>> builtInRegistryHolder() {
        return this.builtInRegistryHolder;
    }

    public boolean isAllowedInPeaceful() {
        return this.allowedInPeaceful;
    }

    private static EntityType.EntityFactory<Boat> boatFactory(Supplier<Item> boatItemGetter) {
        return (p_375558_, p_375559_) -> new Boat(p_375558_, p_375559_, boatItemGetter);
    }

    private static EntityType.EntityFactory<ChestBoat> chestBoatFactory(Supplier<Item> boatItemGetter) {
        return (p_375555_, p_375556_) -> new ChestBoat(p_375555_, p_375556_, boatItemGetter);
    }

    private static EntityType.EntityFactory<Raft> raftFactory(Supplier<Item> raftItemGetter) {
        return (p_375564_, p_375565_) -> new Raft(p_375564_, p_375565_, raftItemGetter);
    }

    private static EntityType.EntityFactory<ChestRaft> chestRaftFactory(Supplier<Item> raftItemGetter) {
        return (p_375561_, p_375562_) -> new ChestRaft(p_375561_, p_375562_, raftItemGetter);
    }

    public boolean onlyOpCanSetNbt() {
        if (onlyOpCanSetNbt) return true;
        return OP_ONLY_CUSTOM_DATA.contains(this);
    }

    public Stream<TagKey<EntityType<?>>> getTags() {return this.builtInRegistryHolder().tags();}

    public static class Builder<T extends Entity> {
        private final EntityType.EntityFactory<T> factory;
        private final MobCategory category;
        private ImmutableSet<Block> immuneTo = ImmutableSet.of();
        private boolean serialize = true;
        private boolean summon = true;
        private boolean fireImmune;
        private boolean canSpawnFarFromPlayer;
        private int clientTrackingRange = 5;
        private int updateInterval = 3;
        private EntityDimensions dimensions = EntityDimensions.scalable(0.6F, 1.8F);
        private float spawnDimensionsScale = 1.0F;
        private EntityAttachments.Builder attachments = EntityAttachments.builder();
        private FeatureFlagSet requiredFeatures = FeatureFlags.VANILLA_SET;
        private DependantName<EntityType<?>, Optional<ResourceKey<LootTable>>> lootTable = p_367917_ -> Optional.of(
            ResourceKey.create(Registries.LOOT_TABLE, p_367917_.location().withPrefix("entities/"))
        );
        private final DependantName<EntityType<?>, String> descriptionId = p_367918_ -> Util.makeDescriptionId("entity", p_367918_.location());
        private boolean allowedInPeaceful = true;
        private java.util.function.Predicate<EntityType<?>> velocityUpdateSupplier = EntityType::defaultTrackDeltasSupplier;
        private java.util.function.ToIntFunction<EntityType<?>> trackingRangeSupplier = EntityType::defaultTrackingRangeSupplier;
        private java.util.function.ToIntFunction<EntityType<?>> updateIntervalSupplier = EntityType::defaultUpdateIntervalSupplier;
        private boolean onlyOpCanSetNbt = false;

        private Builder(EntityType.EntityFactory<T> factory, MobCategory category) {
            this.factory = factory;
            this.category = category;
            this.canSpawnFarFromPlayer = category == MobCategory.CREATURE || category == MobCategory.MISC;
        }

        public static <T extends Entity> EntityType.Builder<T> of(EntityType.EntityFactory<T> factory, MobCategory category) {
            return new EntityType.Builder<>(factory, category);
        }

        public static <T extends Entity> EntityType.Builder<T> createNothing(MobCategory category) {
            return new EntityType.Builder<>((p_20708_, p_20709_) -> null, category);
        }

        public EntityType.Builder<T> sized(float width, float height) {
            this.dimensions = EntityDimensions.scalable(width, height);
            return this;
        }

        public EntityType.Builder<T> spawnDimensionsScale(float spawnDimensionsScale) {
            this.spawnDimensionsScale = spawnDimensionsScale;
            return this;
        }

        public EntityType.Builder<T> eyeHeight(float eyeHeight) {
            this.dimensions = this.dimensions.withEyeHeight(eyeHeight);
            return this;
        }

        public EntityType.Builder<T> passengerAttachments(float... attachPoints) {
            for (float f : attachPoints) {
                this.attachments = this.attachments.attach(EntityAttachment.PASSENGER, 0.0F, f, 0.0F);
            }

            return this;
        }

        public EntityType.Builder<T> passengerAttachments(Vec3... attachPoints) {
            for (Vec3 vec3 : attachPoints) {
                this.attachments = this.attachments.attach(EntityAttachment.PASSENGER, vec3);
            }

            return this;
        }

        public EntityType.Builder<T> vehicleAttachment(Vec3 attachPoint) {
            return this.attach(EntityAttachment.VEHICLE, attachPoint);
        }

        public EntityType.Builder<T> ridingOffset(float ridingOffset) {
            return this.attach(EntityAttachment.VEHICLE, 0.0F, -ridingOffset, 0.0F);
        }

        public EntityType.Builder<T> nameTagOffset(float nameTagOffset) {
            return this.attach(EntityAttachment.NAME_TAG, 0.0F, nameTagOffset, 0.0F);
        }

        public EntityType.Builder<T> attach(EntityAttachment attachment, float x, float y, float z) {
            this.attachments = this.attachments.attach(attachment, x, y, z);
            return this;
        }

        public EntityType.Builder<T> attach(EntityAttachment attachment, Vec3 pos) {
            this.attachments = this.attachments.attach(attachment, pos);
            return this;
        }

        public EntityType.Builder<T> noSummon() {
            this.summon = false;
            return this;
        }

        public EntityType.Builder<T> noSave() {
            this.serialize = false;
            return this;
        }

        public EntityType.Builder<T> fireImmune() {
            this.fireImmune = true;
            return this;
        }

        public EntityType.Builder<T> immuneTo(Block... blocks) {
            this.immuneTo = ImmutableSet.copyOf(blocks);
            return this;
        }

        public EntityType.Builder<T> canSpawnFarFromPlayer() {
            this.canSpawnFarFromPlayer = true;
            return this;
        }

        public EntityType.Builder<T> clientTrackingRange(int clientTrackingRange) {
            this.clientTrackingRange = clientTrackingRange;
            return this;
        }

        public EntityType.Builder<T> updateInterval(int updateInterval) {
            this.updateInterval = updateInterval;
            return this;
        }

        public EntityType.Builder<T> requiredFeatures(FeatureFlag... requiredFeatures) {
            this.requiredFeatures = FeatureFlags.REGISTRY.subset(requiredFeatures);
            return this;
        }

        public EntityType.Builder<T> noLootTable() {
            this.lootTable = DependantName.fixed(Optional.empty());
            return this;
        }

        public EntityType.Builder<T> notInPeaceful() {
            this.allowedInPeaceful = false;
            return this;
        }

        public EntityType.Builder<T> setUpdateInterval(int interval) {
            this.updateIntervalSupplier = t->interval;
            return this;
        }

        public EntityType.Builder<T> setTrackingRange(int range) {
            this.trackingRangeSupplier = t->range;
            return this;
        }

        public EntityType.Builder<T> setShouldReceiveVelocityUpdates(boolean value) {
            this.velocityUpdateSupplier = t->value;
            return this;
        }

        public EntityType.Builder<T> setOnlyOpCanSetNbt(boolean onlyOpCanSetNbt) {
            this.onlyOpCanSetNbt = onlyOpCanSetNbt;
            return this;
        }

        public EntityType<T> build(ResourceKey<EntityType<?>> entityType) {
            if (this.serialize) {
                Util.fetchChoiceType(References.ENTITY_TREE, entityType.location().toString());
            }

            return new EntityType<>(
                this.factory,
                this.category,
                this.serialize,
                this.summon,
                this.fireImmune,
                this.canSpawnFarFromPlayer,
                this.immuneTo,
                this.dimensions.withAttachments(this.attachments),
                this.spawnDimensionsScale,
                this.clientTrackingRange,
                this.updateInterval,
                this.descriptionId.get(entityType),
                this.lootTable.get(entityType),
                this.requiredFeatures,
                this.allowedInPeaceful,
                this.velocityUpdateSupplier,
                this.trackingRangeSupplier,
                this.updateIntervalSupplier,
                this.onlyOpCanSetNbt
            );
        }
    }

    @FunctionalInterface
    public interface EntityFactory<T extends Entity> {
        @Nullable
        T create(EntityType<T> entityType, Level level);
    }
}
