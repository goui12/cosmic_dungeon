package net.goui.cosmicdungeon.playerclass.api;

import net.goui.cosmicdungeon.menu.ModMenus;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

import javax.annotation.Nullable;
import java.util.Optional;

public class ExtraInventoryMenu extends AbstractCraftingMenu {
    private final Player player;

    // your extra 3 slots
    private final SimpleContainer extra = new SimpleContainer(3);

    // slot index layout (after we add them in this order):
    // 0                 -> result
    // 1..4              -> 2x2 crafting inputs
    // 5..40             -> player inventory (27) + hotbar (9)
    // 41..43            -> extra 3
    private static final int RESULT_SLOT      = 0;
    private static final int CRAFT_START      = 1;
    private static final int CRAFT_END_EXCL   = 5;   // 1..4
    private static final int PLAYER_START     = 5;
    private static final int PLAYER_END_EXCL  = 41;  // 5..40
    private static final int EXTRA_START      = 41;
    private static final int EXTRA_END_EXCL   = 44;  // 41..43

    // Player Inventory backing indices (vanilla):
    // 0..8   hotbar, 9..35 main, 36 feet, 37 legs, 38 chest, 39 head, 40 offhand
    private static final int INV_FEET    = 36;
    private static final int INV_LEGS    = 37;
    private static final int INV_CHEST   = 38;
    private static final int INV_HEAD    = 39;
    private static final int INV_OFFHAND = 40;

    // Container slot indexes (filled in the ctor when we add those slots)
    private int slotHead = -1, slotChest = -1, slotLegs = -1, slotFeet = -1, slotOffhand = -1;

    // Player main/hotbar split (within PLAYER_START..PLAYER_END_EXCL)
    private static final int PLAYER_MAIN_COUNT = 27;
    private static final int PLAYER_HOTBAR_COUNT = 9;

    public ExtraInventoryMenu(int id, Inventory inv) {
        // NOTE: AbstractCraftingMenu(type, containerId, gridW, gridH)
        super(ModMenus.METALMANCER_INVENTORY.get(), id, 2, 2);
        this.player = inv.player;

        loadExtraFromPersistentData();

        // --- result + 2x2 crafting grid (coords tuned for your PNG; nudge if needed)
        this.addResultSlot(this.player, /*x*/153, /*y*/27);
        this.addCraftingGridSlots(/*x*/98, /*y*/18);

        // --- player inventory + hotbar (vanilla helper)
        this.addStandardInventorySlots(inv, /*left*/8, /*top*/84);

        // --- extra 3 slots (to the right of hotbar)
        {
            int x = 115, y = 160;
            for (int i = 0; i < extra.getContainerSize(); i++) {
                this.addSlot(new Slot(extra, i, x + i * 18, y));
            }
        }

        // --- armor column (Head, Chest, Legs, Feet) — also capture container indices
        {
            int ax = 8;     // X for armor column (tweak to your art)
            int ay = 8;     // Y of the head slot
            int step = 18;  // vertical spacing

            ResourceLocation headIcon  = null;
            ResourceLocation chestIcon = null;
            ResourceLocation legsIcon  = null;
            ResourceLocation feetIcon  = null;

            slotHead  = this.addSlot(new ArmorLikeSlot(inv, this.player, EquipmentSlot.HEAD,  INV_HEAD,  ax, ay + step * 0, headIcon)).index;
            slotChest = this.addSlot(new ArmorLikeSlot(inv, this.player, EquipmentSlot.CHEST, INV_CHEST, ax, ay + step * 1, chestIcon)).index;
            slotLegs  = this.addSlot(new ArmorLikeSlot(inv, this.player, EquipmentSlot.LEGS,  INV_LEGS,  ax, ay + step * 2, legsIcon)).index;
            slotFeet  = this.addSlot(new ArmorLikeSlot(inv, this.player, EquipmentSlot.FEET,  INV_FEET,  ax, ay + step * 3, feetIcon)).index;
        }

        // --- offhand / shield slot — capture container index
        {
            int sx = 78; // X for offhand (tweak to your art)
            int sy = 62; // Y for offhand
            slotOffhand = this.addSlot(new Slot(inv, INV_OFFHAND, sx, sy)).index;
        }
    }

    /* ---------------- vanilla-style hooks ---------------- */

    @Override
    public void slotsChanged(Container changed) {
        super.slotsChanged(changed);
        // Compute result server-side using a local copy of CraftingMenu's helper
        if (changed == this.craftSlots && this.player.level() instanceof ServerLevel sl) {
            updateCraftingResult(this, sl, this.player, this.craftSlots, this.resultSlots, null);
        }
    }

    @Override
    public void removed(Player player) {
        super.removed(player);

        // return crafting inputs like vanilla
        if (!player.level().isClientSide()) {
            this.clearContainer(player, this.craftSlots);
        }

        // persist your 3 extra slots
        NonNullList<ItemStack> list = NonNullList.withSize(extra.getContainerSize(), ItemStack.EMPTY);
        for (int i = 0; i < extra.getContainerSize(); i++) {
            list.set(i, extra.getItem(i));
        }

        var out = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
        ContainerHelper.saveAllItems(out, list);
        CompoundTag extraTag = out.buildResult();

        CompoundTag root = player.getPersistentData().getCompoundOrEmpty(ClassData.ROOT_TAG).copy();
        root.put(ClassData.KEY_EXTRA, extraTag);
        player.getPersistentData().put(ClassData.ROOT_TAG, root);
    }

    @Override
    public boolean stillValid(Player p) { return true; }

    @Override
    public ItemStack quickMoveStack(Player p, int idx) {
        if (idx < 0 || idx >= this.slots.size()) return ItemStack.EMPTY;

        ItemStack ret = ItemStack.EMPTY;
        Slot s = this.slots.get(idx);
        if (s == null || !s.hasItem()) return ItemStack.EMPTY;

        ItemStack in = s.getItem();
        ret = in.copy();

        // Player main/hotbar ranges (in container-slot space)
        final int MAIN_START   = PLAYER_START;
        final int MAIN_END     = PLAYER_START + PLAYER_MAIN_COUNT;   // exclusive
        final int HOTBAR_START = MAIN_END;
        final int HOTBAR_END   = PLAYER_END_EXCL;                    // exclusive

        // Route based on where the click came from
        if (idx == RESULT_SLOT) {
            // result -> player main first, then hotbar
            if (!this.moveItemStackTo(in, MAIN_START, MAIN_END, false) &&
                    !this.moveItemStackTo(in, HOTBAR_START, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
            s.onQuickCraft(in, ret);

        } else if (idx >= PLAYER_START && idx < PLAYER_END_EXCL) {
            // From player inv/hotbar:

            // 1) Try offhand if item can equip there (shields, etc.)
            if (slotOffhand >= 0 && in.canEquip(EquipmentSlot.OFFHAND, this.player)) {
                if (this.moveItemStackTo(in, slotOffhand, slotOffhand + 1, false)) {
                    return finishQuickMove(s, p, in, ret);
                }
            }
            // 2) Try armor slots if item can equip there (H/C/L/F)
            if (slotHead >= 0 && in.canEquip(EquipmentSlot.HEAD, this.player)) {
                if (this.moveItemStackTo(in, slotHead, slotHead + 1, false)) {
                    return finishQuickMove(s, p, in, ret);
                }
            }
            if (slotChest >= 0 && in.canEquip(EquipmentSlot.CHEST, this.player)) {
                if (this.moveItemStackTo(in, slotChest, slotChest + 1, false)) {
                    return finishQuickMove(s, p, in, ret);
                }
            }
            if (slotLegs >= 0 && in.canEquip(EquipmentSlot.LEGS, this.player)) {
                if (this.moveItemStackTo(in, slotLegs, slotLegs + 1, false)) {
                    return finishQuickMove(s, p, in, ret);
                }
            }
            if (slotFeet >= 0 && in.canEquip(EquipmentSlot.FEET, this.player)) {
                if (this.moveItemStackTo(in, slotFeet, slotFeet + 1, false)) {
                    return finishQuickMove(s, p, in, ret);
                }
            }

            // 3) Not equippable: try extra slots
            if (this.moveItemStackTo(in, EXTRA_START, EXTRA_END_EXCL, false)) {
                return finishQuickMove(s, p, in, ret);
            }

            // 4) Vanilla-like swap: main <-> hotbar
            if (idx >= MAIN_START && idx < MAIN_END) {
                if (!this.moveItemStackTo(in, HOTBAR_START, HOTBAR_END, false)) return ItemStack.EMPTY;
            } else if (idx >= HOTBAR_START && idx < HOTBAR_END) {
                if (!this.moveItemStackTo(in, MAIN_START, MAIN_END, false)) return ItemStack.EMPTY;
            } else {
                return ItemStack.EMPTY;
            }

        } else if (idx >= CRAFT_START && idx < CRAFT_END_EXCL) {
            // From crafting input -> player
            if (!this.moveItemStackTo(in, PLAYER_START, PLAYER_END_EXCL, false)) return ItemStack.EMPTY;

        } else if (idx >= EXTRA_START && idx < EXTRA_END_EXCL) {
            // From extra slots -> player
            if (!this.moveItemStackTo(in, PLAYER_START, PLAYER_END_EXCL, false)) return ItemStack.EMPTY;

        } else if (idx == slotHead || idx == slotChest || idx == slotLegs || idx == slotFeet || idx == slotOffhand) {
            // From armor/offhand -> player
            if (!this.moveItemStackTo(in, PLAYER_START, PLAYER_END_EXCL, false)) return ItemStack.EMPTY;

        } else {
            // Fallback: send to player
            if (!this.moveItemStackTo(in, PLAYER_START, PLAYER_END_EXCL, false)) return ItemStack.EMPTY;
        }

        return finishQuickMove(s, p, in, ret);
    }

    private static ItemStack finishQuickMove(Slot s, Player p, ItemStack in, ItemStack ret) {
        if (in.isEmpty()) s.setByPlayer(ItemStack.EMPTY, ret);
        else s.setChanged();
        if (in.getCount() == ret.getCount()) return ItemStack.EMPTY;
        s.onTake(p, in);
        return ret;
    }

    @Override
    public Slot getResultSlot() { return this.slots.get(RESULT_SLOT); }

    @Override
    public java.util.List<Slot> getInputGridSlots() { return this.slots.subList(CRAFT_START, CRAFT_END_EXCL); }

    @Override
    public RecipeBookType getRecipeBookType() { return RecipeBookType.CRAFTING; }

    @Override
    protected Player owner() { return this.player; }

    /* ---------------- helpers ---------------- */

    private void loadExtraFromPersistentData() {
        CompoundTag root = player.getPersistentData().getCompoundOrEmpty(ClassData.ROOT_TAG);
        if (!root.contains(ClassData.KEY_EXTRA)) return;

        var input = TagValueInput.create(
                ProblemReporter.DISCARDING,
                player.level().registryAccess(),
                root.getCompound(ClassData.KEY_EXTRA).orElseGet(CompoundTag::new)
        );

        NonNullList<ItemStack> list = NonNullList.withSize(extra.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, list);
        for (int i = 0; i < list.size(); i++) {
            extra.setItem(i, list.get(i));
        }
    }

    /** Local copy of CraftingMenu.slotChangedCraftingGrid (vanilla), so we can call it outside the package. */
    private static void updateCraftingResult(
            AbstractContainerMenu menu,
            ServerLevel level,
            Player player,
            CraftingContainer craftSlots,
            ResultContainer resultSlots,
            @Nullable RecipeHolder<CraftingRecipe> recipeHint
    ) {
        CraftingInput input = craftSlots.asCraftInput();
        ServerPlayer sp = (ServerPlayer) player;

        ItemStack out = ItemStack.EMPTY;
        Optional<RecipeHolder<CraftingRecipe>> match =
                level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level, recipeHint);

        if (match.isPresent()) {
            RecipeHolder<CraftingRecipe> holder = match.get();
            CraftingRecipe recipe = holder.value();
            if (resultSlots.setRecipeUsed(sp, holder)) {
                ItemStack assembled = recipe.assemble(input, level.registryAccess());
                if (assembled.isItemEnabled(level.enabledFeatures())) {
                    out = assembled;
                }
            }
        }

        resultSlots.setItem(0, out);
        menu.setRemoteSlot(0, out);
        sp.connection.send(new ClientboundContainerSetSlotPacket(menu.containerId, menu.incrementStateId(), 0, out));
    }

    /** Server-side helper to open the menu. */
    public static void open(ServerPlayer sp) { sp.openMenu(new Provider()); }

    /** MenuProvider: empty title so no top-left "Inventory" label shows. */
    private static final class Provider implements MenuProvider {
        @Override public Component getDisplayName() { return Component.empty(); }
        @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
            return new ExtraInventoryMenu(id, inv);
        }
        @Override public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buf) {}
        @Override public boolean shouldTriggerClientSideContainerClosingOnOpen() { return true; }
    }

    /** Local copy of vanilla ArmorSlot behavior (vanilla class is package-private). */
    static final class ArmorLikeSlot extends Slot {
        private final LivingEntity owner;
        private final EquipmentSlot eqSlot;
        @Nullable private final ResourceLocation emptyIcon;

        ArmorLikeSlot(Container container, LivingEntity owner, EquipmentSlot eqSlot,
                      int slotIndex, int x, int y, @Nullable ResourceLocation emptyIcon) {
            super(container, slotIndex, x, y);
            this.owner = owner;
            this.eqSlot = eqSlot;
            this.emptyIcon = emptyIcon;
        }

        @Override public void setByPlayer(ItemStack newStack, ItemStack oldStack) {
            this.owner.onEquipItem(this.eqSlot, oldStack, newStack);
            super.setByPlayer(newStack, oldStack);
        }

        @Override public int getMaxStackSize() { return 1; }

        @Override public boolean mayPlace(ItemStack stack) {
            return stack.canEquip(this.eqSlot, this.owner);
        }

        @Override public boolean isActive() {
            try {
                return this.owner.canUseSlot(this.eqSlot);
            } catch (Throwable t) {
                return true;
            }
        }

        @Override public boolean mayPickup(Player player) {
            ItemStack cur = this.getItem();
            if (!cur.isEmpty() && !player.isCreative()
                    && EnchantmentHelper.has(cur, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE)) {
                return false;
            }
            return super.mayPickup(player);
        }

        @Override @Nullable public ResourceLocation getNoItemIcon() { return this.emptyIcon; }
    }

    // Give SatchelApi a safe way to reach the live extra slots while the menu is open.
    public net.minecraft.world.SimpleContainer getExtraContainer() {
        return this.extra;
    }

    /** Returns the open ExtraInventoryMenu for this player, or null if not open. */
    public static @org.jetbrains.annotations.Nullable ExtraInventoryMenu getOpen(ServerPlayer sp) {
        return (sp.containerMenu instanceof ExtraInventoryMenu m) ? m : null;
    }
}
