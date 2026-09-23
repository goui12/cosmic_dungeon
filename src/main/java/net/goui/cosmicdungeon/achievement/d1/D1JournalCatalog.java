package net.goui.cosmicdungeon.achievement.d1;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Player-facing approved text, not names used as item identity. */
public final class D1JournalCatalog {
    public record Entry(String id, String title, List<String> paragraphs, String digest) {}
    private static final Map<String, Entry> ENTRIES = load();
    private D1JournalCatalog() {}
    private static Map<String, Entry> load() {
        try (var stream = D1JournalCatalog.class.getResourceAsStream("/data/cosmicdungeon/lore/d1_journals.json")) {
            if (stream == null) throw new IllegalStateException("Missing D1 journal text");
            var object = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            var result = new LinkedHashMap<String, Entry>();
            for (int number = 1; number <= 3; number++) {
                var row = object.getAsJsonObject(Integer.toString(number));
                var paragraphs = new ArrayList<String>();
                row.getAsJsonArray("paragraphs").forEach(v -> paragraphs.add(v.getAsString()));
                var entry = new Entry("journal_" + number, row.get("title").getAsString(),
                        List.copyOf(paragraphs), row.get("sha256").getAsString());
                if (!hash(String.join(" ", paragraphs)).equals(entry.digest())) throw new IllegalStateException("Journal text checksum mismatch");
                result.put(entry.id(), entry);
            }
            return Map.copyOf(result);
        } catch (IOException failure) { throw new IllegalStateException("Cannot load D1 journal text", failure); }
    }
    public static Entry get(String id) { return id == null ? null : ENTRIES.get(id); }
    public static String normalize(String text) { return text.replaceAll("[\\s\\p{Z}]+", " ").trim(); }
    public static String hash(String text) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(normalize(text).getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
    public static boolean validMarker(int schema, String id, String edition) {
        var entry = get(id);
        return schema == 1 && entry != null && entry.digest().equals(edition);
    }
    public static boolean contentMatches(String id, String raw, String filtered) {
        var entry = get(id);
        return entry != null && hash(raw).equals(entry.digest()) && hash(filtered).equals(entry.digest());
    }
    public static List<String> pages(Entry entry) {
        var result = new ArrayList<String>(); var page = new StringBuilder();
        // Conservative vanilla-book layout. This is a text pagination limit, not a gameplay modifier.
        for (String word : normalize(String.join(" ", entry.paragraphs())).split(" ")) {
            if (!page.isEmpty() && page.length() + 1 + word.length() > 180) {
                result.add(page.toString()); page.setLength(0);
            }
            if (!page.isEmpty()) page.append(' ');
            page.append(word);
        }
        if (!page.isEmpty()) result.add(page.toString());
        return List.copyOf(result);
    }
}
