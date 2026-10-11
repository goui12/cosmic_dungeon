package net.goui.cosmicdungeon.item.identity;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.goui.cosmicdungeon.auth.AccessPolicy;
import net.goui.cosmicdungeon.component.ModDataComponents;
import net.goui.cosmicdungeon.economy.DeathCurrencyRecord;
import net.goui.cosmicdungeon.item.ModItems;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.goui.cosmicdungeon.playerclass.api.ClassItemUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.TriState;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;

/** Native CI-only drop, pickup, persistence and automation boundaries; no authored world data. */
public final class OwnershipDropGameTests {
    private OwnershipDropGameTests() {}

    private static final class Fixture implements AutoCloseable {
        final GameTestHelper helper;
        final ServerLevel level;
        final Vec3 origin;
        final List<ItemEntity> items = new ArrayList<>();
        final List<ItemEntity> tossed = new ArrayList<>();
        final List<ItemEntity> deathItems = new ArrayList<>();
        final List<io.netty.channel.embedded.EmbeddedChannel> channels = new ArrayList<>();
        final ServerPlayer owner, other;
        final Consumer<ItemTossEvent> onToss;
        final Consumer<LivingDropsEvent> onDeath;

        Fixture(GameTestHelper helper) {
            this.helper = helper;
            level = helper.getLevel();
            var anchor = helper.absolutePos(BlockPos.ZERO);
            var chunk = new ChunkPos(anchor);
            check(level.areEntitiesActuallyLoadedAndTicking(chunk), "Native fixture chunk is accessible");
            origin = new Vec3(chunk.getMinBlockX() + 8.5, anchor.getY() + 14.5, chunk.getMinBlockZ() + 8.5);
            owner = player("DropOwner");
            other = player("DropOther");
            onToss = event -> {
                if (event.getPlayer() == owner || event.getPlayer() == other) {
                    tossed.add(event.getEntity());
                    items.add(event.getEntity());
                }
            };
            onDeath = event -> {
                if (event.getEntity() == owner || event.getEntity() == other) {
                    deathItems.addAll(event.getDrops());
                    items.addAll(event.getDrops());
                }
            };
            NeoForge.EVENT_BUS.addListener(onToss);
            NeoForge.EVENT_BUS.addListener(onDeath);
        }

        ServerPlayer player(String name) {
            var profile = new GameProfile(UUID.randomUUID(), name);
            var player = new ServerPlayer(level.getServer(), level, profile, ClientInformation.createDefault());
            var channel = new io.netty.channel.embedded.EmbeddedChannel();
            channels.add(channel);
            var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND) {
                @Override public io.netty.channel.Channel channel() { return channel; }
            };
            player.connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(), connection, player,
                    net.minecraft.server.network.CommonListenerCookie.createInitial(profile, false)) {
                @Override public void send(Packet<?> packet) {}
                @Override public void send(Packet<?> packet, io.netty.channel.ChannelFutureListener listener) {}
            };
            player.setGameMode(GameType.SURVIVAL);
            player.setHealth(20);
            player.setPos(origin);
            var classTag = new CompoundTag();
            classTag.putString(ClassData.KEY_CLASS_ID, "theurgist");
            player.getPersistentData().put(ClassData.ROOT_TAG, classTag);
            check(!player.isCreative() && !player.isSpectator() && !AccessPolicy.isDeveloper(player),
                    "Fixture exercises dungeoneer survival, without developer bypass");
            return player;
        }

        ItemEntity toss(ItemStack stack) {
            var item = owner.drop(stack.copy(), false);
            check(item != null && tossed.contains(item), "Native toss event produced the real dropped entity");
            owned(item, owner.getUUID());
            return item;
        }
        void owned(ItemEntity item, UUID ownerId) {
            check(ownerId.equals(item.getTarget()), "Dropped entity preserves its expected owner");
        }
        ItemEntity lastToss(ItemStack expected) {
            check(!tossed.isEmpty(), "Native drop emitted an item");
            var item = tossed.getLast();
            owned(item, owner.getUUID());
            check(ItemStack.matches(expected, item.getItem()), "Drop preserves exact count and authored components");
            return item;
        }
        void check(boolean value, String message) { helper.assertTrue(value, Component.literal(message)); }

        @Override public void close() {
            NeoForge.EVENT_BUS.unregister(onToss);
            NeoForge.EVENT_BUS.unregister(onDeath);
            items.forEach(ItemEntity::discard);
            owner.discard();
            other.discard();
            channels.forEach(io.netty.channel.embedded.EmbeddedChannel::finishAndReleaseAll);
        }
    }

    private static ItemStack legacyArrows(int count) {
        var stack = new ItemStack(Items.TIPPED_ARROW, count);
        stack.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.REGENERATION));
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Mending Sting"));
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString("authored_unknown", "preserve"));
        return stack;
    }
    private static ItemStack currencyToken() {
        var stack = new ItemStack(Items.PAPER);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString(DeathCurrencyRecord.MARKER, "fixture"));
        return stack;
    }

    public static void dropPaths(GameTestHelper helper) {
        try (var f = new Fixture(helper)) {
            var marked = legacyArrows(8);
            marked.set(ModDataComponents.D1_ABILITY.get(), "mending_sting");
            for (var template : List.of(new ItemStack(ModItems.MENDING_STING.get(), 8), legacyArrows(8), marked)) {
                f.owner.getInventory().setSelectedSlot(0);
                f.owner.getInventory().setItem(0, template.copy());
                int before = f.tossed.size();
                f.check(f.owner.drop(false), "Native Q drops one class arrow");
                f.check(f.tossed.size() == before + 1 && f.owner.getInventory().getItem(0).getCount() == 7,
                        "Single drop removes exactly one arrow");
                f.lastToss(template.copyWithCount(1));
                f.check(f.owner.drop(true), "Native control-Q drops remaining class ammunition");
                f.check(f.owner.getInventory().getItem(0).isEmpty(), "Full drop empties selected slot");
                f.lastToss(template.copyWithCount(7));

                f.owner.getInventory().setItem(0, template.copy());
                int slot = f.owner.inventoryMenu.findSlot(f.owner.getInventory(), 0).orElseThrow();
                f.owner.inventoryMenu.clicked(slot, 1, ClickType.THROW, f.owner);
                f.check(f.owner.getInventory().getItem(0).isEmpty(), "Inventory THROW releases ammunition");
                f.lastToss(template);

                f.owner.inventoryMenu.setCarried(template.copy());
                f.owner.inventoryMenu.clicked(-999, 0, ClickType.PICKUP, f.owner);
                f.check(f.owner.inventoryMenu.getCarried().isEmpty(), "Outside cursor drop releases ammunition");
                f.lastToss(template);
            }
            var token = currencyToken();
            f.owner.getInventory().setItem(0, token.copy());
            int before = f.tossed.size();
            f.check(!f.owner.drop(true), "Currency recovery token still rejects manual drop");
            f.check(f.tossed.size() == before && ItemStack.matches(token, f.owner.getInventory().getItem(0)),
                    "Rejected currency drop is lossless");
        }
        helper.succeed();
    }

    public static void ownerPickup(GameTestHelper helper) {
        try (var f = new Fixture(helper)) {
            for (var item : List.of(Items.ARROW, Items.POTION, Items.SPLASH_POTION, Items.NETHER_WART, Items.IRON_SWORD)) {
                var template = new ItemStack(item);
                var dropped = f.toss(template);
                dropped.setNoPickUpDelay();
                dropped.playerTouch(f.other);
                f.check(f.other.getInventory().isEmpty() && !dropped.isRemoved()
                        && ItemStack.matches(template, dropped.getItem()), "Other player cannot collect ordinary belongings");
                var hopper = new SimpleContainer(5);
                f.check(!HopperBlockEntity.addItem(hopper, dropped) && hopper.isEmpty(),
                        "Hopper cannot launder owner-only ground items");
                dropped.playerTouch(f.owner);
                f.check(dropped.isRemoved() && f.owner.getInventory().countItem(item) == 1,
                        "Original owner can recover each ordinary item");
                f.owner.getInventory().clearContent();
            }

            var dropped = f.toss(new ItemStack(Items.ARROW, 3));
            var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, f.level.registryAccess());
            dropped.saveWithoutId(output);
            var image = output.buildResult();
            dropped.discard();
            var restored = new ItemEntity(f.level, f.origin.x, f.origin.y, f.origin.z, new ItemStack(Items.ARROW));
            restored.load(TagValueInput.create(ProblemReporter.DISCARDING, f.level.registryAccess(), image));
            f.items.add(restored);
            f.owned(restored, f.owner.getUUID());
            f.check(restored.getItem().getCount() == 3, "Native save/load retains item count and owner");
            restored.makeFakeItem();
            restored.setNoPickUpDelay();
            f.check(restored.getAge() == restored.getItem().getEntityLifespan(f.level) - 1,
                    "Late-lifetime test is one tick before normal expiry");
            Consumer<ItemEntityPickupEvent.Pre> forcePickup = event -> {
                if (event.getItemEntity() == restored) event.setCanPickup(TriState.TRUE);
            };
            NeoForge.EVENT_BUS.addListener(forcePickup);
            try {
                restored.playerTouch(f.other);
                f.check(f.other.getInventory().isEmpty() && restored.getItem().getCount() == 3 && !restored.isRemoved(),
                        "Late-lifetime ownership cannot be bypassed by a TRUE pickup event");
                restored.playerTouch(f.owner);
                f.check(restored.isRemoved() && f.owner.getInventory().countItem(Items.ARROW) == 3,
                        "Owner still recovers the exact saved stack near expiry");
            } finally { NeoForge.EVENT_BUS.unregister(forcePickup); }

            var malformed = new ItemStack(Items.IRON_SWORD);
            CustomData.update(DataComponents.CUSTOM_DATA, malformed, tag -> {
                tag.putString(ClassItemOwnership.KEY, "unknown-future-owner");
                tag.putString("authored_unknown", "keep");
            });
            var invalid = f.owner.drop(malformed.copy(), false);
            f.check(invalid != null && ItemStack.matches(malformed, invalid.getItem()), "Malformed ownership is not rewritten on toss");
            invalid.setNoPickUpDelay();
            invalid.playerTouch(f.owner);
            invalid.playerTouch(f.other);
            f.check(!invalid.isRemoved() && ItemStack.matches(malformed, invalid.getItem()),
                    "Malformed existing ownership cannot be collected by either player");
        }
        helper.succeed();
    }

    public static void deathDrops(GameTestHelper helper) {
        try (var f = new Fixture(helper)) {
            f.check(!f.level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY), "Native survival death drops inventory");
            var oldGear = new ItemStack(Items.IRON_CHESTPLATE);
            CustomData.update(DataComponents.CUSTOM_DATA, oldGear, tag -> {
                tag.putBoolean("no_drop", true);
                tag.putBoolean("class_issued", true);
                tag.putString("authored_unknown", "preserve");
            });
            var attuned = new ItemStack(Items.DIAMOND_SWORD);
            ClassItemUtil.attune(attuned, "theurgist", 1, 3, 25);
            var templates = List.of(legacyArrows(9), new ItemStack(Items.SPLASH_POTION), new ItemStack(Items.NETHER_WART, 4),
                    oldGear, attuned);
            var expected = new ArrayList<ItemStack>();
            for (int i = 0; i < templates.size(); i++) {
                f.owner.getInventory().setItem(i, templates.get(i).copy());
                expected.add(f.owner.getInventory().getItem(i).copy());
            }
            var token = currencyToken();
            f.owner.getInventory().setItem(8, token.copy());
            f.owner.setHealth(0);
            f.owner.die(f.level.damageSources().generic());
            f.check(f.deathItems.size() == expected.size(), "Death emits each real belongings stack exactly once");
            for (int i = 0; i < expected.size(); i++) {
                var stack = expected.get(i);
                f.check(f.owner.getInventory().getItem(i).isEmpty(), "Dropped source slot clears");
                var matches = f.deathItems.stream().filter(item -> ItemStack.matches(stack, item.getItem())).toList();
                f.check(matches.size() == 1, "Death preserves exact quantity and every authored component");
                var item = matches.getFirst();
                f.owned(item, f.owner.getUUID());
                item.setNoPickUpDelay();
                item.playerTouch(f.other);
                f.check(!item.isRemoved() && ItemStack.matches(stack, item.getItem()),
                        "Another player cannot collect a death drop");
            }
            f.check(ItemStack.matches(token, f.owner.getInventory().getItem(8)), "Currency recovery token is retained on death");
            f.check(f.other.getInventory().isEmpty(), "Failed death-loot pickup creates no inventory items");
        }
        helper.succeed();
    }
}
