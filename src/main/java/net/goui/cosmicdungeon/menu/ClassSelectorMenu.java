package net.goui.cosmicdungeon.menu;

import net.goui.cosmicdungeon.block.custom.ClassSelectorTeleportUtil;
import net.goui.cosmicdungeon.npc.tamsin.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import java.util.UUID;

public class ClassSelectorMenu extends AbstractContainerMenu {
    private final UUID tamsinNpc;
    private final TamsinData.Binding binding;
    private TamsinFlow.Stage stage;
    public net.goui.cosmicdungeon.network.PartyPayloads.View lastPartyView;
    public long lastPartyActionTick = -1000000L;
    public long lastTaxActionTick = -1000000L;
    public TamsinTaxService.Quote taxQuote;
    public ClassSelectorMenu(int id, Inventory inventory) {
        this(id, inventory, null, null, TamsinFlow.Stage.SELECTOR);
    }
    public ClassSelectorMenu(int id, Inventory inventory, UUID npc, TamsinData.Binding binding, TamsinFlow.Stage stage) {
        super(ModMenus.CLASS_SELECTOR.get(), id);
        this.tamsinNpc = npc; this.binding = binding; this.stage = stage;
    }
    public UUID tamsinNpc() { return tamsinNpc; }
    public TamsinData.Binding tamsinBinding() { return binding; }
    public TamsinFlow.Stage stage() { return stage; }
    public void setStage(TamsinFlow.Stage stage) { this.stage = stage; }
    @Override public boolean stillValid(Player player) {
        return !(player instanceof ServerPlayer serverPlayer)
                || (ClassSelectorTeleportUtil.validSourceSession(serverPlayer)
                && TamsinService.validNpcSession(serverPlayer, this));
    }
    @Override public ItemStack quickMoveStack(Player player, int slotIndex) { return ItemStack.EMPTY; }
}
