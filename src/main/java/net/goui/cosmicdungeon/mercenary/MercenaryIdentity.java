package net.goui.cosmicdungeon.mercenary;

import java.util.List;
import java.util.UUID;

/** Stable presentation from the existing random hire UUID; no new save fields or lookups. */
public final class MercenaryIdentity {
    private MercenaryIdentity() {}
    // Keep this pool and its ordering stable so existing hires retain their names.
    static final List<String> NAMES = List.of(
        "Aldred", "Alfric", "Ambrose", "Barnaby", "Benedict",
        "Bennet", "Clement", "Crispin", "Digory", "Edmund",
        "Edward", "Edwin", "Ellis", "Emery", "Ephraim",
        "Francis", "Gabriel", "Gawen", "Geoffrey", "Giles",
        "Godfrey", "Gregory", "Hamond", "Henry", "Hewett",
        "Humphrey", "Isaac", "Jasper", "Jervis", "John",
        "Jonas", "Josiah", "Laurence", "Leonard", "Lionel",
        "Martin", "Miles", "Nathaniel", "Nicholas", "Oswald",
        "Peregrine", "Ralph", "Reuben", "Richard", "Robert",
        "Roger", "Rowland", "Samuel", "Silas", "Thomas",
        "Abigail", "Agnes", "Alice", "Alisoun", "Anne",
        "Audrey", "Avis", "Barbara", "Beatrice", "Bess",
        "Bridget", "Cecily", "Charity", "Constance", "Cordelia",
        "Deborah", "Dionisia", "Dorcas", "Dorothy", "Edith",
        "Eleanor", "Elinor", "Elizabeth", "Ellen", "Emme",
        "Esther", "Faith", "Frances", "Grace", "Hannah",
        "Hester", "Isabel", "Jane", "Joan", "Joyce",
        "Judith", "Katherine", "Lettice", "Mabel", "Margaret",
        "Margery", "Martha", "Mary", "Maud", "Mercy",
        "Prudence", "Rachel", "Rose", "Ruth", "Susanna"
    );

    public static String name(UUID hireId) {
        return NAMES.get(Math.floorMod(hireId.hashCode(), NAMES.size()));
    }
}
