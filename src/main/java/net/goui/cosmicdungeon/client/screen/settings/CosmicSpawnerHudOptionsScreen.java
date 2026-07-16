package net.goui.cosmicdungeon.client.screen.settings;

import net.goui.cosmicdungeon.client.SpawnerHudClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class CosmicSpawnerHudOptionsScreen extends Screen {
    private static final int ROW_HEIGHT = 24;
    private static final int TOP = 52;
    private static final int BOTTOM_PADDING = 38;

    private final Screen parent;
    private int scroll;
    private int maxScroll;

    public CosmicSpawnerHudOptionsScreen(Screen parent) {
        super(Component.literal("Cosmic Spawner HUD"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        List<Row> rows = rows();

        int contentHeight = rows.size() * ROW_HEIGHT;
        int viewport = Math.max(1, this.height - TOP - BOTTOM_PADDING);

        this.maxScroll = Math.max(0, contentHeight - viewport);
        this.scroll = Math.max(0, Math.min(this.scroll, this.maxScroll));

        int y = TOP - this.scroll;

        for (Row row : rows) {
            int rowY = y;

            if (rowY > TOP - ROW_HEIGHT && rowY < this.height - BOTTOM_PADDING) {
                addRenderableWidget(Button.builder(row.label().get(), button -> {
                    row.advance().run();
                    button.setMessage(row.label().get());
                    SpawnerHudClientConfig.save();
                }).bounds(this.width / 2 - 155, rowY, 310, 20).build());
            }

            y += ROW_HEIGHT;
        }

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(this.width / 2 - 100, this.height - 28, 200, 20)
                .build());
    }

    private List<Row> rows() {
        return List.of(
                bool("Mob Type", SpawnerHudClientConfig.MOB_TYPE),
                bool("Mob Name", SpawnerHudClientConfig.MOB_NAME),
                bool("Coordinates", SpawnerHudClientConfig.COORDINATES),
                bool("Boss One-Shot", SpawnerHudClientConfig.BOSS_ONE_SHOT),
                bool("Boss Spawned", SpawnerHudClientConfig.BOSS_SPAWNED),
                bool("Cap", SpawnerHudClientConfig.CAP),
                bool("Delay", SpawnerHudClientConfig.DELAY),
                bool("Spawn Count", SpawnerHudClientConfig.SPAWN_COUNT),
                bool("Spawn Range", SpawnerHudClientConfig.SPAWN_RANGE),
                bool("Required Player Range", SpawnerHudClientConfig.REQUIRED_PLAYER_RANGE),
                bool("Max Nearby Entities", SpawnerHudClientConfig.MAX_NEARBY_ENTITIES),
                bool("Preset Present", SpawnerHudClientConfig.PRESET_PRESENT),
                bool("Equipment", SpawnerHudClientConfig.EQUIPMENT),
                intCycle("Max Width", SpawnerHudClientConfig.MAX_WIDTH, 160, 220, 280, 340),
                enumCycle("Equipment Mode", SpawnerHudClientConfig.EQUIPMENT_MODE, SpawnerHudClientConfig.EQUIPMENT_MODE_ICONS, SpawnerHudClientConfig.EQUIPMENT_MODE_TEXT)
        );
    }

    private static Row bool(String label, ModConfigSpec.BooleanValue value) {
        return new Row(() -> Component.literal(label + ": " + (value.get() ? "ON" : "OFF")), () -> value.set(!value.get()));
    }

    private static Row intCycle(String label, ModConfigSpec.IntValue value, int... values) {
        return new Row(() -> Component.literal(label + ": " + value.get() + " px"), () -> {
            int current = value.get();
            int next = values[0];
            for (int i = 0; i < values.length; i++) {
                if (values[i] == current) {
                    next = values[(i + 1) % values.length];
                    break;
                }
                if (current < values[i]) {
                    next = values[i];
                    break;
                }
            }
            value.set(next);
        });
    }

    private static Row enumCycle(String label, ModConfigSpec.ConfigValue<String> value, String... values) {
        return new Row(() -> Component.literal(label + ": " + value.get()), () -> value.set(values[0].equals(value.get()) ? values[1] : values[0]));
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, Component.literal("Cosmic Dungeon Settings"), this.width / 2, 14, 0xFFFFFFFF);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 32, 0xFFFFD98A);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.maxScroll <= 0 || scrollY == 0) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        int previous = this.scroll;
        int amount = scrollY < 0 ? ROW_HEIGHT : -ROW_HEIGHT;

        this.scroll = Math.max(0, Math.min(this.maxScroll, this.scroll + amount));

        if (this.scroll != previous) {
            this.rebuildWidgets();
            return true;
        }

        return false;
    }

    @Override
    public void onClose() {
        SpawnerHudClientConfig.save();
        Minecraft.getInstance().setScreen(parent);
    }

    private record Row(java.util.function.Supplier<Component> label, Runnable advance) {}
}
