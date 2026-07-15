// file: src/main/java/net/goui/cosmicdungeon/menu/ModMenus.java
package net.goui.cosmicdungeon.menu;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.playerclass.api.ExtraInventoryMenu;
import net.goui.cosmicdungeon.trade.TradeMenu;
import net.goui.cosmicdungeon.playerclass.dragoon.repair.DragoonRepairMenu;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModMenus {
    private ModMenus() {}

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(BuiltInRegistries.MENU, CosmicDungeonMod.MOD_ID);

    /** Metalmancer inventory (player inv + 3 extra slots) */
    public static final Supplier<MenuType<ExtraInventoryMenu>> METALMANCER_INVENTORY =
            MENUS.register("metalmancer_inventory",
                    () -> new MenuType<>(ExtraInventoryMenu::new, FeatureFlags.DEFAULT_FLAGS));

    /** Class selector screen/menu */
    public static final Supplier<MenuType<ClassSelectorMenu>> CLASS_SELECTOR =
            MENUS.register("class_selector",
                    () -> new MenuType<>(ClassSelectorMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final Supplier<MenuType<VendorMenu>> VENDOR =
            MENUS.register("vendor",
                    () -> new MenuType<>(VendorMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final Supplier<MenuType<TradeMenu>> TRADE =
            MENUS.register("trade",
                    () -> new MenuType<>((id, inv) -> {
                        net.goui.cosmicdungeon.trade.TradeSessionData.TradeSession session = null;
                        if (inv.player instanceof net.minecraft.server.level.ServerPlayer sp) {
                            session = net.goui.cosmicdungeon.trade.TradeSessionData.get(sp);
                        }
                        return new TradeMenu(id, inv, session);
                    }, FeatureFlags.DEFAULT_FLAGS));

    public static final Supplier<MenuType<DragoonRepairMenu>> DRAGOON_REPAIR =
            MENUS.register("dragoon_repair",
                    () -> new MenuType<>((id, inv) -> {
                        net.goui.cosmicdungeon.playerclass.dragoon.repair.DragoonRepairSessionData.RepairSession session = null;
                        if (inv.player instanceof net.minecraft.server.level.ServerPlayer sp) {
                            session = net.goui.cosmicdungeon.playerclass.dragoon.repair.DragoonRepairSessionData.get(sp);
                        }
                        return new DragoonRepairMenu(id, inv, session);
                    }, FeatureFlags.DEFAULT_FLAGS));

    public static void register(IEventBus modBus) {
        MENUS.register(modBus);
    }
}
