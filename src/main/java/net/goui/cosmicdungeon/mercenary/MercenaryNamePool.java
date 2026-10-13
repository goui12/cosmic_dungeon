package net.goui.cosmicdungeon.mercenary;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * New-hire names attested in 1400-1499. Region labels select documented cultural
 * samples, not continent-wide naming traditions. See FIXSET_F4_NAME_SOURCES_20261013.md.
 */
final class MercenaryNamePool {
    private MercenaryNamePool() {}

    record RegionPool(String region, String culture, List<String> male, List<String> female) {
        RegionPool {
            male = List.copyOf(male);
            female = List.copyOf(female);
        }
    }

    static final List<RegionPool> REGIONS = List.of(
        new RegionPool("English", "England",
            List.of("John", "Thomas", "William", "Henry", "Richard"),
            List.of("Agnes", "Alice", "Iohane", "Margery", "Elizabeth")),
        new RegionPool("French", "Paris",
            List.of("Jehan", "Guillaume", "Pierre", "Charles", "Jaques"),
            List.of("Jehanne", "Marie", "Collette", "Perrette", "Ysabeau")),
        new RegionPool("German-speaking", "Rottweil, Swabian/Alemannic",
            List.of("Hans", "Heinrich", "Conrad", "Berchtold", "Caspar"),
            List.of("Ursula", "Dorothea", "Adelhaid", "Magdalen", "Othilia")),
        new RegionPool("Italian", "Florence, Tuscany",
            List.of("Giovanni", "Antonio", "Piero", "Francesco", "Lorenzo"),
            List.of("Caterina", "Antonia", "Beatrice", "Giovanna", "Francesca")),
        new RegionPool("Asia", "Muromachi Japan, Ashikaga/Hino and imperial courts",
            List.of("Yoshinori", "Yoshikazu", "Yoshimasa", "Yoshihisa", "Yoshimi"),
            List.of("Yasuko", "Eishi", "Shigeko", "Tomiko", "Sukeko")),
        new RegionPool("Africa", "Mamluk Egypt, Cairo court",
            List.of("Faraj", "Shaykh", "Barsbay", "Inal", "Qaytbay"),
            List.of("Julban", "Mughal", "Zaynab", "Shakarbay", "Fatimah")),
        new RegionPool("Russia", "Muscovy, Pskov and Novgorod",
            List.of("Ivan", "Vasilii", "Dmitrii", "Iurii", "Andrei"),
            List.of("Mariia", "Stefanida", "Marfa", "Sofiia", "Ponaria")),
        new RegionPool("Mediterranean", "Catalonia, Crown of Aragon; Latin/Catalan records",
            List.of("Andreu", "Baltasar", "Gabriel", "Manuel", "Ramon"),
            List.of("Elionor", "Eulalia", "Blanquina", "Francina", "Isabel"))
    );

    // Exactly ten entries per region and five per gender: equal pool representation.
    static final List<String> NAMES = REGIONS.stream()
        .flatMap(region -> Stream.concat(region.male().stream(), region.female().stream()))
        .toList();

    static String name(UUID hireId) {
        return NAMES.get(Math.floorMod(hireId.hashCode(), NAMES.size()));
    }
}
