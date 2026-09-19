package net.goui.cosmicdungeon.achievement.d1;

import net.goui.cosmicdungeon.achievement.*;
import net.goui.cosmicdungeon.dungeon.d1.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import java.util.stream.Collectors;

/** Canonical signed text AND a server-authored marker are required. Rename/pickup alone never credits reading. */
public final class D1JournalService {
    private static final String KEY = "cosmicdungeon_d1_journal";
    private D1JournalService() {}
    public static boolean marked(ItemStack stack) {
        var custom = stack.get(DataComponents.CUSTOM_DATA);
        return custom != null && custom.copyTag().contains(KEY);
    }
    public static boolean textMatches(ItemStack stack, String id) {
        var book = stack.get(DataComponents.WRITTEN_BOOK_CONTENT);
        if (!stack.is(Items.WRITTEN_BOOK) || book == null || book.pages().isEmpty()) return false;
        String raw = book.pages().stream().map(p -> p.raw().getString()).collect(Collectors.joining(" "));
        String filtered = book.pages().stream().map(p -> p.get(true).getString()).collect(Collectors.joining(" "));
        return D1JournalCatalog.contentMatches(id, raw, filtered);
    }
    public static String identity(ItemStack stack) {
        if (!marked(stack)) return null;
        var marker = stack.get(DataComponents.CUSTOM_DATA).copyTag().getCompoundOrEmpty(KEY);
        String id = marker.getStringOr("id", "");
        return D1JournalCatalog.validMarker(marker.getIntOr("schema", 0), id, marker.getStringOr("edition", ""))
                && textMatches(stack, id) ? id : null;
    }
    public static ItemStack markCopy(ItemStack original, String id) {
        var entry = D1JournalCatalog.get(id);
        if (entry == null || marked(original) || !textMatches(original, id))
            throw new IllegalArgumentException("Hold an unmarked signed book with the canonical journal text");
        var copy = original.copy();
        var custom = copy.get(DataComponents.CUSTOM_DATA);
        var tag = custom == null ? new CompoundTag() : custom.copyTag();
        var marker = new CompoundTag(); marker.putInt("schema", 1);
        marker.putString("id", id); marker.putString("edition", entry.digest());
        tag.put(KEY, marker); copy.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return copy;
    }
    public static ItemStack create(String id) {
        var entry = D1JournalCatalog.get(id);
        if (entry == null) throw new IllegalArgumentException("Unknown journal");
        var stack = new ItemStack(Items.WRITTEN_BOOK);
        var pages = D1JournalCatalog.pages(entry).stream().map(s -> Filterable.<Component>passThrough(Component.literal(s))).toList();
        stack.set(DataComponents.WRITTEN_BOOK_CONTENT,
                new WrittenBookContent(Filterable.passThrough(entry.title()), "Unknown", 0, pages, true));
        return markCopy(stack, id);
    }
    public static void opened(ServerPlayer player, ItemStack stack, String requiredId) {
        String id = identity(stack);
        if (id == null || requiredId != null && !requiredId.equals(id)) return;
        var run = D1Members.run(player.level()).orElse(null);
        if (run == null || !D1Members.inside(player, run)) return;
        var data = D1RunData.get(player.level().getServer());
        String key = "journals:" + player.getUUID();
        data.recordUnique(run.runId(), key, id);
        if (data.values(run.runId(), key).containsAll(java.util.List.of("journal_1", "journal_2", "journal_3"))) {
            data.creditPersonal(run.runId(), CosmicAchievementIds.LIBRARIAN_1.toString(), player.getUUID());
            CosmicAdvancementUtil.grant(player, CosmicAchievementIds.LIBRARIAN_1);
        }
    }
    // TODO(M79/M81, licensed TEST): mark only reviewed canonical legacy books using explicit
    // developer preview/apply. Do not replace authored chest contents, infer identity from titles,
    // or bulk-stamp unloaded books. Verify hand/lectern opening and all three per-run pages.
}
