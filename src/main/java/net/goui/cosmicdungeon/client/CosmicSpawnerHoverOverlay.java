// file: src/main/java/net/goui/cosmicdungeon/client/CosmicSpawnerHoverOverlay.java
package net.goui.cosmicdungeon.client;

import net.goui.cosmicdungeon.block.entity.CosmicSpawnerBlockEntity;
import net.goui.cosmicdungeon.block.entity.CosmicSpawnerPreset;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.util.ArrayList;
import java.util.List;

public final class CosmicSpawnerHoverOverlay {

    private static final int PADDING = 6;
    private static final int MARGIN = 12;
    private static final int LINE_HEIGHT = 10;
    private static final int ROW_GAP = 2;
    private static final int ICON_SIZE = 16;
    private static final int ICON_GAP = 4;
    private static final int ICON_LABEL_GAP = 2;
    private static final CosmicSpawnerPreset.Slot[] EQUIPMENT_ORDER = {
            CosmicSpawnerPreset.Slot.MAINHAND,
            CosmicSpawnerPreset.Slot.OFFHAND,
            CosmicSpawnerPreset.Slot.HEAD,
            CosmicSpawnerPreset.Slot.CHEST,
            CosmicSpawnerPreset.Slot.LEGS,
            CosmicSpawnerPreset.Slot.FEET
    };

    private CosmicSpawnerHoverOverlay() {}

    /**
     * Client-only toggle for whether the developer spawner summary HUD is rendered.
     * Default: HIDE. Server controls this via SpawnerLabelPayload after developer checks.
     */
    public static boolean isEnabled() {
        return SpawnerLabelState.isEnabled();
    }

    public static void setEnabled(boolean enabled) {
        SpawnerLabelState.setEnabled(enabled);
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post e) {
        if (!SpawnerLabelState.isEnabled()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.screen != null || mc.getDebugOverlay().showDebugScreen()) return;

        HitResult hit = mc.hitResult;
        if (!(hit instanceof BlockHitResult bhr)) return;
        if (!(mc.level.getBlockEntity(bhr.getBlockPos()) instanceof CosmicSpawnerBlockEntity be)) return;

        Font font = mc.font;
        int maxWidth = SpawnerHudClientConfig.MAX_WIDTH.get();
        int contentMaxWidth = Math.max(24, maxWidth - (PADDING * 2));
        List<Row> rows = buildRows(be, bhr, font, contentMaxWidth);
        if (rows.isEmpty()) return;

        int contentWidth = rows.stream().mapToInt(Row::width).max().orElse(0);
        int boxWidth = Math.min(maxWidth, contentWidth + (PADDING * 2));
        int contentHeight = rows.stream().mapToInt(Row::height).sum() + (ROW_GAP * Math.max(0, rows.size() - 1));
        int boxHeight = contentHeight + (PADDING * 2);

        SpawnerHudClientConfig.Anchor anchor = SpawnerHudClientConfig.anchor();
        int edgeOffsetX = MARGIN + SpawnerHudClientConfig.HORIZONTAL_OFFSET.get();
        int edgeOffsetY = MARGIN + SpawnerHudClientConfig.VERTICAL_OFFSET.get();
        int x = anchor.left()
                ? edgeOffsetX
                : mc.getWindow().getGuiScaledWidth() - boxWidth - edgeOffsetX;
        int y = anchor.top()
                ? edgeOffsetY
                : mc.getWindow().getGuiScaledHeight() - boxHeight - edgeOffsetY;

        GuiGraphics graphics = e.getGuiGraphics();
        graphics.fill(x, y, x + boxWidth, y + boxHeight, SpawnerHudClientConfig.backgroundColor());
        graphics.fill(x, y, x + boxWidth, y + 1, SpawnerHudClientConfig.borderColor());
        graphics.fill(x, y + boxHeight - 1, x + boxWidth, y + boxHeight, SpawnerHudClientConfig.borderColor());

        int rowY = y + PADDING;
        for (Row row : rows) {
            row.render(graphics, font, x + PADDING, rowY);
            rowY += row.height() + ROW_GAP;
        }
    }

    private static List<Row> buildRows(CosmicSpawnerBlockEntity be, BlockHitResult bhr, Font font, int contentMaxWidth) {
        List<Row> rows = new ArrayList<>();
        addTextRow(rows, font, contentMaxWidth, SpawnerHudClientConfig.MOB_TYPE.get(), "Mob Type: " + be.getSpawnerDisplayEntityId());
        addTextRow(rows, font, contentMaxWidth, SpawnerHudClientConfig.MOB_NAME.get(), "Mob Name: " + be.getSpawnerDisplayMobName());
        addTextRow(rows, font, contentMaxWidth, SpawnerHudClientConfig.COORDINATES.get(), "Pos: " + bhr.getBlockPos().toShortString());
        addTextRow(rows, font, contentMaxWidth, SpawnerHudClientConfig.BOSS_ONE_SHOT.get(), "Boss One-Shot: " + be.isBossOneShot());
        addTextRow(rows, font, contentMaxWidth, SpawnerHudClientConfig.BOSS_SPAWNED.get(), "Boss Spawned: " + be.hasBossSpawned());
        addTextRow(rows, font, contentMaxWidth, SpawnerHudClientConfig.CAP.get(), "Cap: " + formatCap(be.getSpawnerMobCap()));
        addTextRow(rows, font, contentMaxWidth, SpawnerHudClientConfig.DELAY.get(), "Delay: range " + be.getSpawnerMinSpawnDelay() + "-" + be.getSpawnerMaxSpawnDelay() + " ticks");
        addTextRow(rows, font, contentMaxWidth, SpawnerHudClientConfig.SPAWN_COUNT.get(), "Spawn Count: " + be.getSpawnerSpawnCount());
        addTextRow(rows, font, contentMaxWidth, SpawnerHudClientConfig.SPAWN_RANGE.get(), "Spawn Range: " + be.getSpawnerSpawnRange());
        addTextRow(rows, font, contentMaxWidth, SpawnerHudClientConfig.REQUIRED_PLAYER_RANGE.get(), "Player Range: " + be.getSpawnerRequiredPlayerRange());
        addTextRow(rows, font, contentMaxWidth, SpawnerHudClientConfig.MAX_NEARBY_ENTITIES.get(), "Nearby Cap: " + be.getSpawnerMaxNearbyEntities());
        addTextRow(rows, font, contentMaxWidth, SpawnerHudClientConfig.PRESET_PRESENT.get(), "Preset: " + (be.getSpawnerPreset() == null ? "no" : "yes"));
        if (SpawnerHudClientConfig.EQUIPMENT.get()) addEquipmentRows(rows, be.getSpawnerPreset(), font, contentMaxWidth);
        return rows;
    }

    private static void addTextRow(List<Row> rows, Font font, int contentMaxWidth, boolean enabled, String text) {
        if (!enabled) return;
        rows.add(new TextRow(font.split(Component.literal(text), contentMaxWidth), font));
    }

    private static void addEquipmentRows(List<Row> rows, CosmicSpawnerPreset preset, Font font, int contentMaxWidth) {
        List<EquipmentEntry> entries = equipmentEntries(preset);
        if (entries.isEmpty()) {
            addTextRow(rows, font, contentMaxWidth, true, "Equipment: none");
            return;
        }
        if (SpawnerHudClientConfig.EQUIPMENT_MODE_TEXT.equalsIgnoreCase(SpawnerHudClientConfig.EQUIPMENT_MODE.get())) {
            addTextRow(rows, font, contentMaxWidth, true, "Equipment: " + equipmentText(entries));
            return;
        }
        rows.add(new EquipmentIconRow(entries, font, contentMaxWidth));
    }

    private static List<EquipmentEntry> equipmentEntries(CosmicSpawnerPreset preset) {
        List<EquipmentEntry> entries = new ArrayList<>();
        if (preset == null) return entries;
        for (CosmicSpawnerPreset.Slot slot : EQUIPMENT_ORDER) {
            ItemStack stack = preset.getEquipment(slot);
            if (stack != null && !stack.isEmpty()) entries.add(new EquipmentEntry(slotLabel(slot), stack));
        }
        return entries;
    }

    private static String equipmentText(List<EquipmentEntry> entries) {
        List<String> parts = new ArrayList<>();
        for (EquipmentEntry entry : entries) {
            parts.add(entry.label() + " " + entry.stack().getHoverName().getString());
        }
        return String.join(", ", parts);
    }

    private static String slotLabel(CosmicSpawnerPreset.Slot slot) {
        return switch (slot) {
            case MAINHAND -> "MH";
            case OFFHAND -> "OH";
            case HEAD -> "Head";
            case CHEST -> "Chest";
            case LEGS -> "Legs";
            case FEET -> "Feet";
        };
    }

    private static String formatCap(int cap) {
        return cap <= 0 ? "uncapped" : Integer.toString(cap);
    }

    private interface Row {
        int width();
        int height();
        void render(GuiGraphics graphics, Font font, int x, int y);
    }

    private record TextRow(List<FormattedCharSequence> lines, Font font) implements Row {
        @Override public int width() { return lines.stream().mapToInt(font::width).max().orElse(0); }
        @Override public int height() { return lines.size() * LINE_HEIGHT; }
        @Override public void render(GuiGraphics graphics, Font font, int x, int y) {
            int lineY = y;
            for (FormattedCharSequence line : lines) {
                graphics.drawString(font, line, x, lineY, 0xFFFFFFFF, true);
                lineY += LINE_HEIGHT;
            }
        }
    }

    private record EquipmentEntry(String label, ItemStack stack) {}

    private static final class EquipmentIconRow implements Row {
        private final List<List<EquipmentEntry>> lines = new ArrayList<>();
        private final Font font;
        private final int width;

        private EquipmentIconRow(List<EquipmentEntry> entries, Font font, int contentMaxWidth) {
            this.font = font;
            List<EquipmentEntry> current = new ArrayList<>();
            int currentWidth = 0;
            int maxLineWidth = 0;
            for (EquipmentEntry entry : entries) {
                int entryWidth = entryWidth(entry);
                int nextWidth = current.isEmpty() ? entryWidth : currentWidth + ICON_GAP + entryWidth;
                if (!current.isEmpty() && nextWidth > contentMaxWidth) {
                    lines.add(current);
                    maxLineWidth = Math.max(maxLineWidth, currentWidth);
                    current = new ArrayList<>();
                    currentWidth = entryWidth;
                } else {
                    currentWidth = nextWidth;
                }
                current.add(entry);
            }
            if (!current.isEmpty()) {
                lines.add(current);
                maxLineWidth = Math.max(maxLineWidth, currentWidth);
            }
            this.width = maxLineWidth;
        }

        @Override public int width() { return width; }
        @Override public int height() { return lines.size() * ICON_SIZE + Math.max(0, lines.size() - 1) * ROW_GAP; }
        @Override public void render(GuiGraphics graphics, Font font, int x, int y) {
            int lineY = y;
            for (List<EquipmentEntry> line : lines) {
                int itemX = x;
                for (EquipmentEntry entry : line) {
                    graphics.drawString(font, entry.label(), itemX, lineY + 4, 0xFFFFFFFF, true);
                    itemX += font.width(entry.label()) + ICON_LABEL_GAP;
                    graphics.renderItem(entry.stack(), itemX, lineY);
                    itemX += ICON_SIZE + ICON_GAP;
                }
                lineY += ICON_SIZE + ROW_GAP;
            }
        }

        private int entryWidth(EquipmentEntry entry) {
            return font.width(entry.label()) + ICON_LABEL_GAP + ICON_SIZE;
        }
    }
}
