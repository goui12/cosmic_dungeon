package net.goui.cosmicdungeon.client.screen.spawner;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.goui.cosmicdungeon.network.SpawnerEditorPayloads;
import net.goui.cosmicdungeon.block.entity.CosmicSpawnerPreset.Slot;
import net.goui.cosmicdungeon.spawner.editor.SpawnerEquipmentDraft;
import net.goui.cosmicdungeon.spawner.editor.SpawnerEquipmentRules;
import net.goui.cosmicdungeon.spawner.editor.SpawnerLootDraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Edits a detached server draft. Preview entities are never ticked or inserted into any level. */
public final class SpawnerEditorScreen extends Screen {
    private record Choice(String id, String label) {}
    private record Caption(String text, int x, int y) {}
    private record Nav(Button button, ItemStack stack, SpawnerEditorState.Tab tab) {}
    private final SpawnerEditorState state;
    private final BlockPos pos;
    private final List<Choice> catalog = new ArrayList<>();
    private final List<Caption> captions = new ArrayList<>();
    private final List<Nav> navigation = new ArrayList<>();
    private final List<Button> entityButtons = new ArrayList<>();
    private final long openedNanos = System.nanoTime();
    private SpawnerEditorLayout layout;
    private Button apply, previous, next;
    private ConfirmScreen confirmation;
    private Mob preview;
    private final List<ItemStack> equipment;
    private final ItemStack held;
    private final List<EquipmentButton> equipmentButtons = new ArrayList<>();
    private record EquipmentButton(Button button, Slot slot) {}
    private SpawnerEquipmentScreen equipmentEditor;
    private SpawnerEquipmentDraft previewEquipment, cachedEquipmentDraft;
    private List<ItemStack> cachedEquipment = List.of();
    private boolean previewCaptain;
    private String previewId = "", previewFailure = "";
    private int filteredCount;
    private SpawnerLootDraft.Snapshot lootSnapshot;
    private List<String> presetNames, shortcuts;
    private UUID libraryOperation;
    private Consumer<SpawnerEditorPayloads.LibraryView> libraryReply;

    public SpawnerEditorScreen(SpawnerEditorState state, BlockPos pos, List<ItemStack> equipment, ItemStack held) {
        this(state, pos, equipment, held, SpawnerLootDraft.Snapshot.empty(), List.of(), List.of("", "", "", "", ""));
    }
    public SpawnerEditorScreen(SpawnerEditorState state, BlockPos pos, List<ItemStack> equipment, ItemStack held,
            SpawnerLootDraft.Snapshot loot, List<String> presetNames, List<String> shortcuts) {
        super(Component.literal("Cosmic Spawner Editor"));
        this.lootSnapshot = loot; this.presetNames = List.copyOf(presetNames); this.shortcuts = List.copyOf(shortcuts);
        this.state = state; this.pos = pos.immutable();
        this.equipment = equipment.stream().map(ItemStack::copy).toList(); this.held = held.copy();
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            String id = BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
            if (type.canSummon() && (type.getCategory() != MobCategory.MISC
                    || SpawnEggItem.byId(type) != null || id.equals(state.entityId())))
                catalog.add(new Choice(id, type.getDescription().getString()));
        }
        catalog.sort(Comparator.comparing(Choice::label).thenComparing(Choice::id));
    }
    public UUID session() { return state.session(); }
    public SpawnerLootDraft.Snapshot lootSnapshot() { return lootSnapshot; }
    public SpawnerLootDraft lootDraft() { return state.loot(); }
    public void saveLoot(SpawnerLootDraft value) { state.loot(value); }
    public List<String> presetNames() { return presetNames; }
    public List<String> shortcuts() { return shortcuts; }
    public String entityId() { return state.entityId(); }
    public boolean dirty() { return state.dirty(); }
    public boolean libraryPending() { return libraryOperation != null; }
    public boolean library(SpawnerEditorPayloads.LibraryKind kind, String name, int shortcut,
            Consumer<SpawnerEditorPayloads.LibraryView> reply) {
        if (state.closed() || state.pending() || libraryPending()) return false;
        UUID operation = UUID.randomUUID();
        libraryOperation = operation; libraryReply = reply;
        try {
            if (SpawnerEditorClient.sendLibrary(new SpawnerEditorPayloads.LibraryRequest(session(), operation, kind, name,
                    shortcut, state.draft(), state.equipment(), state.loot()))) return true;
        } catch (RuntimeException invalid) { state.notice("The preset request could not be sent."); }
        libraryOperation = null; libraryReply = null; return false;
    }
    public void receiveLibrary(SpawnerEditorPayloads.LibraryView view) {
        if (!session().equals(view.session()) || !view.operation().equals(libraryOperation) || state.closed()) return;
        libraryOperation = null;
        presetNames = List.copyOf(view.names()); shortcuts = List.copyOf(view.shortcuts());
        if (view.success()) lootSnapshot = new SpawnerLootDraft.Snapshot(lootSnapshot.gear(), lootSnapshot.rules(),
                view.loot().defaults(), view.loot().note(), view.loot().entityId());
        var callback = libraryReply; libraryReply = null;
        if (callback != null) callback.accept(view);
        state.notice(view.message()); updateEnabled();
    }
    public void confirm(String title, String message, Runnable accepted, Screen returnTo) {
        confirmation = new ConfirmScreen(yes -> {
            if (state.closed()) return;
            minecraft.setScreen(returnTo); if (yes) accepted.run();
        }, Component.literal(title), Component.literal(message),
                Component.literal("Confirm"), Component.literal("Keep editing"));
        minecraft.setScreen(confirmation);
    }
    @Override protected void init() {
        layout = SpawnerEditorLayout.fit(width, height);
        captions.clear(); navigation.clear(); entityButtons.clear(); equipmentButtons.clear(); previous = null; next = null;
        nav(SpawnerEditorState.Tab.ENTITY, Items.ZOMBIE_SPAWN_EGG, "Entity and name", 0);
        nav(SpawnerEditorState.Tab.EQUIPMENT, Items.DIAMOND_CHESTPLATE, "Equipment and enchantments", 1);
        nav(SpawnerEditorState.Tab.SPAWNING, Items.SPAWNER, "Spawning and limits", 2);
        nav(SpawnerEditorState.Tab.FLAGS, Items.LEVER, "Entity flags", 3);
        nav(SpawnerEditorState.Tab.PREVIEW, Items.SPYGLASS, "Visual preview", 4);
        switch (state.tab()) {
            case ENTITY -> entityPage();
            case EQUIPMENT -> equipmentPage();
            case SPAWNING -> spawningPage();
            case FLAGS -> flagsPage();
            case PREVIEW -> { }
        }
        int buttonWidth = Math.min(92, Math.max(44, (layout.contentWidth() - 8) / 2));
        apply = addRenderableWidget(Button.builder(Component.literal("Apply"), b -> apply())
                .bounds(layout.x() + layout.width() - buttonWidth * 2 - 16, layout.footerY(), buttonWidth, 20).build());
        apply.setTooltip(Tooltip.create(Component.literal("Validate and apply this draft to the selected spawner. No changes are saved until Apply succeeds.")));
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose())
                .bounds(layout.x() + layout.width() - buttonWidth - 10, layout.footerY(), buttonWidth, 20).build())
                .setTooltip(Tooltip.create(Component.literal("Close without applying. Changed drafts require confirmation.")));
        addRenderableWidget(Button.builder(Component.literal("Drops / files"),
                b -> minecraft.setScreen(new SpawnerManagementScreen(this)))
                .bounds(layout.x() + 8, layout.footerY(), 82, 20).build())
                .setTooltip(Tooltip.create(Component.literal("Gear drop chances, default loot, configured rules and named preset files.")));
        updateEnabled();
    }
    private void nav(SpawnerEditorState.Tab tab, Item icon, String label, int index) {
        int step = Math.min(27, Math.max(18, (layout.footerY() - layout.contentY() - 8) / 5));
        Button button = addRenderableWidget(Button.builder(Component.empty(), b -> {
            state.tab(tab); rebuildWidgets();
        }).createNarration(supplier -> Component.literal(label)).bounds(layout.x() + 8, layout.y() + 32 + index * step, 26, Math.min(24, step - 1)).build());
        button.setTooltip(Tooltip.create(Component.literal(label)));
        navigation.add(new Nav(button, new ItemStack(icon), tab));
    }
    private void entityPage() {
        int x = layout.contentX(), y = layout.contentY(), w = layout.contentWidth();
        int listWidth = Math.max(58, (w - 12) * 3 / 5), rightX = x + listWidth + 10;
        int rightWidth = Math.max(36, w - listWidth - 10);
        EditBox search = field(x, y, listWidth, state.search(), "Search entity name or registry ID", 100, value -> {
            state.search(value); refreshChoices();
        });
        search.setHint(Component.literal("Search mobs..."));
        captions.add(new Caption("Custom name", rightX, y));
        field(rightX, y + 12, rightWidth, state.name(), "Custom name (blank uses the entity name)", 128, state::name);
        toggle("Captain", rightX, y + 37, rightWidth, state::captain, state::captain,
                "Pillager captain. Select the pillager entity to use this flag.");
        for (int i = 0; i < layout.entityRows(); i++) {
            final int row = i;
            Button choice = addRenderableWidget(Button.builder(Component.empty(), b -> {
                List<Choice> matches = matches();
                int index = state.page() * layout.entityRows() + row;
                if (index < matches.size()) {
                    state.entityId(matches.get(index).id()); refreshChoices();
                }
            }).bounds(x, y + 25 + i * 22, listWidth, 20).build());
            entityButtons.add(choice);
        }
        int pagerY = y + 25 + layout.entityRows() * 22;
        previous = addRenderableWidget(Button.builder(Component.literal("<"), b -> {
            state.page(state.page() - 1); refreshChoices();
        }).bounds(x, pagerY, 24, 18).build());
        next = addRenderableWidget(Button.builder(Component.literal(">"), b -> {
            state.page(state.page() + 1); refreshChoices();
        }).bounds(x + listWidth - 24, pagerY, 24, 18).build());
        refreshChoices();
    }
    private List<Choice> matches() {
        return catalog.stream().filter(c -> SpawnerEditorState.matches(state.search(), c.id(), c.label())).toList();
    }
    private void refreshChoices() {
        List<Choice> matches = matches(); filteredCount = matches.size();
        state.page(SpawnerEditorLayout.page(state.page(), matches.size(), layout.entityRows()));
        for (int row = 0; row < entityButtons.size(); row++) {
            Button button = entityButtons.get(row);
            int index = state.page() * layout.entityRows() + row;
            button.visible = index < matches.size();
            if (button.visible) {
                Choice choice = matches.get(index);
                button.setMessage(Component.literal(font.plainSubstrByWidth(
                        (choice.id().equals(state.entityId()) ? "> " : "") + choice.label(), button.getWidth() - 8)));
                button.setTooltip(Tooltip.create(Component.literal(choice.label() + "\n" + choice.id()
                        + "\nSelect for this draft. The live spawner is unchanged until Apply.")));
            }
        }
        if (previous != null) previous.active = !state.pending() && state.page() > 0;
        if (next != null) next.active = !state.pending() && (state.page() + 1) * layout.entityRows() < filteredCount;
    }
    public boolean closed() { return state.closed(); }
    public void saveEquipment(Slot slot, SpawnerEquipmentDraft.Edit edit) { state.equipment(slot, edit); }
    public static String slotName(Slot slot) {
        return switch (slot) {
            case MAINHAND -> "Main hand"; case OFFHAND -> "Off hand"; case HEAD -> "Head";
            case CHEST -> "Chest"; case LEGS -> "Legs"; case FEET -> "Feet";
        };
    }
    private ItemStack originalEquipment(Slot slot) {
        return equipment.size() == Slot.values().length ? equipment.get(slot.ordinal()).copy() : ItemStack.EMPTY;
    }
    public boolean captainHeadFallback(Slot slot) { return slot == Slot.HEAD && state.captain(); }
    private ItemStack draftEquipment(Slot slot) {
        if (!state.equipment().equals(cachedEquipmentDraft)) {
            var resolved = new ArrayList<ItemStack>();
            for (Slot current : Slot.values()) {
                try {
                    resolved.add(SpawnerEquipmentRules.resolve(originalEquipment(current), held,
                            state.equipment().get(current), minecraft.level.registryAccess()));
                } catch (IllegalArgumentException invalid) { resolved.add(originalEquipment(current)); }
            }
            cachedEquipment = List.copyOf(resolved); cachedEquipmentDraft = state.equipment();
        }
        return cachedEquipment.get(slot.ordinal());
    }
    private void equipmentPage() {
        int x = layout.contentX(), y = layout.contentY(), w = (layout.contentWidth() - 8) / 2;
        for (Slot slot : Slot.values()) {
            int column = slot.ordinal() % 2, row = slot.ordinal() / 2;
            String label = "     " + slotName(slot);
            Button button = addRenderableWidget(Button.builder(Component.literal(label), b -> {
                equipmentEditor = new SpawnerEquipmentScreen(this, slot, originalEquipment(slot), held, state.equipment().get(slot));
                minecraft.setScreen(equipmentEditor);
            }).bounds(x + column * (w + 8), y + row * 24, w, 20).build());
            equipmentButtons.add(new EquipmentButton(button, slot));
        }
    }
    private void spawningPage() {
        int x = layout.contentX(), y = layout.contentY(), w = (layout.contentWidth() - 10) / 2;
        toggle("Boss one-shot", x, y, w, state::boss, state::boss,
                "Boss spawners release one boss only. The normal population cap does not create additional bosses.");
        number("Living mob cap", x + w + 10, y, w, state.cap(), state::cap,
                "Maximum simultaneously living mobs from this spawner. Zero means uncapped.");
        number("Min delay (ticks)", x, y + 40, w, state.minDelay(), state::minDelay,
                "Lower bound of the spawn delay in ticks. Twenty ticks is one second.");
        number("Max delay (ticks)", x + w + 10, y + 40, w, state.maxDelay(), state::maxDelay,
                "Upper bound of the spawn delay in ticks; must be at least the minimum.");
    }
    private void flagsPage() {
        int x = layout.contentX(), y = layout.contentY(), w = (layout.contentWidth() - 10) / 2;
        toggle("Silent", x, y, w, state::silent, state::silent, "Suppress entity sounds.");
        toggle("No AI", x + w + 10, y, w, state::noAi, state::noAi, "Disable the spawned mob's AI.");
        toggle("Invulnerable", x, y + 26, w, state::invulnerable, state::invulnerable, "Make the spawned entity invulnerable.");
        toggle("Persistent", x + w + 10, y + 26, w, state::persistent, state::persistent, "Prevent natural despawning.");
        toggle("Always show name", x, y + 52, layout.contentWidth(), state::nameVisible, state::nameVisible,
                "Keep the custom name visible. Set a custom name on the Entity page.");
    }
    private EditBox field(int x, int y, int w, String value, String hint, int maxLength, Consumer<String> change) {
        EditBox field = new EditBox(font, x, y, w, 20, Component.literal(hint));
        field.setMaxLength(maxLength); field.setValue(value);
        field.setTooltip(Tooltip.create(Component.literal(hint)));
        field.setResponder(change);
        return addRenderableWidget(field);
    }
    private void number(String label, int x, int y, int w, String value, Consumer<String> change, String tip) {
        captions.add(new Caption(label, x, y));
        field(x, y + 12, w, value, tip, 10, change);
    }
    private void toggle(String label, int x, int y, int w, BooleanSupplier current, Consumer<Boolean> change, String tip) {
        Button button = addRenderableWidget(Button.builder(toggleText(label, current.getAsBoolean()), b -> {
            change.accept(!current.getAsBoolean()); b.setMessage(toggleText(label, current.getAsBoolean()));
        }).bounds(x, y, w, 20).build());
        button.setTooltip(Tooltip.create(Component.literal(label + "\n" + tip)));
    }
    private Component toggleText(String label, boolean selected) {
        return Component.literal(font.plainSubstrByWidth((selected ? "[x] " : "[ ] ") + label,
                Math.max(24, (layout.contentWidth() - 10) / 2 - 8)));
    }
    private void apply() {
        if (libraryPending() || !state.beginApply()) return;
        try {
            if (!SpawnerEditorClient.send(new SpawnerEditorPayloads.Request(session(),
                    SpawnerEditorPayloads.Kind.APPLY, state.draft(), state.equipment(), state.loot())))
                state.reject(session(), "The server connection is unavailable.");
        } catch (RuntimeException failed) {
            state.reject(session(), "The edit request could not be sent.");
        }
        updateEnabled();
    }
    public void rejected(String message) {
        state.reject(session(), message); updateEnabled();
    }
    public void finished() {
        state.close(session()); preview = null; libraryOperation = null; libraryReply = null;
        if (minecraft != null && (minecraft.screen == this || minecraft.screen == confirmation || minecraft.screen == equipmentEditor
                || (minecraft.screen instanceof SpawnerEditorChildScreen child && child.owner() == this))) minecraft.setScreen(null);
    }
    private void cancel() {
        if (state.pending() || state.closed()) return;
        try {
            SpawnerEditorClient.send(new SpawnerEditorPayloads.Request(session(), SpawnerEditorPayloads.Kind.CANCEL, state.draft()));
        } catch (RuntimeException disconnected) {
            // No Apply is outstanding. A disconnected server session expires without committing the draft.
        } finally { finished(); }
    }
    @Override public void onClose() {
        if (state.pending() || libraryPending()) { state.notice("Waiting for the server operation to finish."); return; }
        if (!state.dirty()) { cancel(); return; }
        confirmation = new ConfirmScreen(discard -> {
            if (discard) cancel(); else if (!state.closed()) minecraft.setScreen(this);
        }, Component.literal("Discard spawner changes?"),
                Component.literal("Your draft has unapplied changes. The live spawner has not been changed."),
                Component.literal("Discard"), Component.literal("Keep editing"));
        minecraft.setScreen(confirmation);
    }
    private void updateEnabled() {
        for (var child : children()) {
            if (child instanceof AbstractWidget widget) widget.active = !state.pending() && !libraryPending() && !state.closed();
            if (child instanceof EditBox field) field.setEditable(!state.pending() && !libraryPending() && !state.closed());
        }
        if (apply != null) apply.active = !state.pending() && !libraryPending() && !state.closed() && state.error().isEmpty();
        if (previous != null) previous.active &= state.page() > 0;
        if (next != null) next.active &= (state.page() + 1) * layout.entityRows() < filteredCount;
    }
    @Override public void tick() {
        if (minecraft.player == null || minecraft.level == null) { finished(); return; }
        updateEnabled();
    }
    private String section() {
        return switch (state.tab()) {
            case ENTITY -> "Entity and name"; case EQUIPMENT -> "Equipment and enchantments"; case SPAWNING -> "Spawning and limits";
            case FLAGS -> "Entity flags"; case PREVIEW -> "Visual preview";
        };
    }
    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(layout.x(), layout.y(), layout.x() + layout.width(), layout.y() + layout.height(), 0xF016202C);
        g.fill(layout.x(), layout.y(), layout.x() + layout.width(), layout.y() + 25, 0xFF293B50);
        g.drawString(font, font.plainSubstrByWidth("Spawner / " + section(), layout.width() - 18),
                layout.x() + 8, layout.y() + 8, 0xFFE4C98A, false);
        for (Caption caption : captions) g.drawString(font, caption.text(), caption.x(), caption.y(), 0xFFDDE5EB, false);
        if (state.tab() == SpawnerEditorState.Tab.PREVIEW) renderPreview(g);
        if (state.tab() == SpawnerEditorState.Tab.ENTITY) {
            int rightX = layout.contentX() + Math.max(58, (layout.contentWidth() - 12) * 3 / 5) + 10;
            int rightW = layout.contentX() + layout.contentWidth() - rightX;
            g.drawString(font, font.plainSubstrByWidth(state.entityId(), rightW),
                    rightX, layout.contentY() + 62, 0xFF90CAF9, false);
            if (filteredCount == 0) g.drawString(font, "No matching mobs", layout.contentX(), layout.contentY() + 29, 0xFFBBBBBB, false);
            else g.drawCenteredString(font, (state.page() + 1) + "/" + Math.max(1, (filteredCount + layout.entityRows() - 1) / layout.entityRows()),
                    layout.contentX() + (rightX - layout.contentX() - 10) / 2,
                    layout.contentY() + 30 + layout.entityRows() * 22, 0xFFBBBBBB);
        }
        String notice = !state.notice().isEmpty() ? state.notice() : state.error();
        if (!notice.isEmpty()) {
            var lines = font.split(Component.literal(notice), layout.contentWidth());
            for (int i = 0; i < Math.min(2, lines.size()); i++)
                g.drawString(font, lines.get(i), layout.contentX(), layout.footerY() - 22 + i * 10, 0xFFFFC680, false);
        } else g.drawString(font, "Draft" + (state.dirty() ? " *" : "") + " / " + pos.toShortString(),
                layout.contentX(), layout.footerY() - 15, 0xFF9CA9B6, false);
        super.render(g, mouseX, mouseY, partialTick);
        for (EquipmentButton entry : equipmentButtons) {
            ItemStack stack = draftEquipment(entry.slot());
            if (!stack.isEmpty()) g.renderItem(stack, entry.button().getX() + 3, entry.button().getY() + 2);
            if (entry.button().isMouseOver(mouseX, mouseY)
                    || (entry.button().isFocused() && minecraft.getLastInputType().isKeyboard())) {
                var lines = stack.isEmpty() ? new ArrayList<Component>() : new ArrayList<>(getTooltipFromItem(minecraft, stack));
                lines.add(Component.literal("Edit " + slotName(entry.slot()) + (stack.isEmpty() ? " (empty)." : ".")));
                lines.add(Component.literal("Select items, copy held components and edit enchantments."));
                lines.add(Component.literal("Save item returns here; Apply commits the spawner draft."));
                if (captainHeadFallback(entry.slot()))
                    lines.add(Component.literal("A Captain with an empty Head slot uses its ominous banner. Disable Captain on the Entity page to remove that fallback."));
                g.setTooltipForNextFrame(font, lines, stack.getTooltipImage(), stack, mouseX, mouseY);
            }
        }
        for (Nav nav : navigation) {
            // Icons retain explicit native narration, focus and foreground tooltip behavior.
            g.fill(nav.button().getX() + 3, nav.button().getY() + 1, nav.button().getX() + 23, nav.button().getY() + nav.button().getHeight() - 1,
                    nav.tab() == state.tab() ? 0xFF667F8E : 0xFF303C48);
            g.renderItem(nav.stack(), nav.button().getX() + 5, nav.button().getY() + Math.max(0, (nav.button().getHeight() - 16) / 2));
        }
    }
    private void renderPreview(GuiGraphics g) {
        if (!previewId.equals(state.entityId())) {
            previewId = state.entityId(); preview = null; previewCaptain = false; previewEquipment = null; previewFailure = "";
            try {
                var entity = EntityType.byString(previewId)
                        .map(type -> type.create(minecraft.level, EntitySpawnReason.COMMAND)).orElse(null);
                if (entity instanceof Mob mob) preview = mob;
                else previewFailure = "This entity has no mob preview.";
            } catch (RuntimeException unavailable) { previewFailure = "Preview unavailable for this entity."; }
        }
        int x = layout.contentX(), y = layout.contentY(), w = layout.contentWidth(), h = layout.contentHeight();
        g.drawCenteredString(font, "Preview only / never spawned", x + w / 2, y, 0xFF9CA9B6);
        if (preview != null) {
            preview.setCustomName(state.name().isEmpty() ? null : Component.literal(state.name()));
            preview.setCustomNameVisible(state.nameVisible());
            preview.setSilent(true); preview.setNoAi(true);
            if (!state.equipment().equals(previewEquipment) || previewCaptain != state.captain()) {
                previewEquipment = state.equipment(); previewCaptain = state.captain();
                for (Slot slot : Slot.values()) preview.setItemSlot(slot.equipmentSlot, draftEquipment(slot).copy());
                if (preview instanceof net.minecraft.world.entity.monster.Pillager && state.captain()
                        && draftEquipment(Slot.HEAD).isEmpty())
                    preview.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,
                            net.minecraft.world.entity.raid.Raid.getOminousBannerInstance(
                                    minecraft.level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BANNER_PATTERN)));
            }
            float scale = Math.max(1, Math.min((w - 12) / Math.max(1, preview.getBbWidth() * 2),
                    (h - 24) / Math.max(1, preview.getBbHeight())));
            float angle = (float) ((System.nanoTime() - openedNanos) / 1_000_000_000.0 * 0.45);
            try {
                InventoryScreen.renderEntityInInventory(g, x, y + 14, x + w, y + h, scale,
                        new Vector3f(0, preview.getBbHeight() / 2, 0),
                        new Quaternionf().rotationZ((float) Math.PI).rotateY(angle), null, preview);
            } catch (RuntimeException unavailable) { preview = null; previewFailure = "Preview renderer unavailable."; }
        }
        if (!previewFailure.isEmpty()) g.drawCenteredString(font, previewFailure, x + w / 2, y + 30, 0xFFFFC680);
    }
    @Override public boolean isPauseScreen() { return false; }
}
