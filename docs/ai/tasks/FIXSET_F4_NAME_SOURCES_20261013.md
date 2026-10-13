# F4 mercenary name source card — 2026-10-13

## Decision and compatibility contract

The new-hire pool has exactly **80 distinct display strings**: eight requested region labels, ten entries per label, five male and five female. Every entry has an attested bearer or dated record within **1400–1499**, and every display string contains at most nine ASCII letters. These are historical given-name samples, not claims that the bearers were mercenaries or that each name was exclusive to its region.

`MercenaryIdentity.name(UUID)` and its ordered 100-name legacy pool remain unchanged. `newName(UUID)` selects from the separate pool below. Callers must use the new selector only when creating a new hire and persist the selected display string. A save without the new field must continue to resolve through the legacy method; adding this pool alone must never rename existing hires. Contract codecs, persistence, and hire integration belong to the F4 root controller.

The pool is immutable and selection is O(1) after one static flattening of 80 entries. Ten slots per region and five per sex provide equal representation in the pool; random hiring does not promise quotas in any small party. UUID hash selection is deterministic and uses floorMod for negative hashes. No runtime downloads, cultural inference from the player, tick work, registries, or network schema are involved.

## Actual cultural scope and complete pool

Broad labels identify specific documented samples, not a single homogeneous continental culture. Each row is a separate source tradition; there is no duplicate display string or reused regional list.

| Requested label | Actual sample | Male (5) | Female (5) |
| --- | --- | --- | --- |
| English | England, primarily Middle English wills | John, Thomas, William, Henry, Richard | Agnes, Alice, Iohane, Margery, Elizabeth |
| French | Paris, French tax rolls | Jehan, Guillaume, Pierre, Charles, Jaques | Jehanne, Marie, Collette, Perrette, Ysabeau |
| German-speaking | Rottweil, Swabian/Alemannic records | Hans, Heinrich, Conrad, Berchtold, Caspar | Ursula, Dorothea, Adelhaid, Magdalen, Othilia |
| Italian | Florence, Tuscany, Catasto census | Giovanni, Antonio, Piero, Francesco, Lorenzo | Caterina, Antonia, Beatrice, Giovanna, Francesca |
| Asia | Muromachi Japan, Ashikaga/Hino and imperial courts | Yoshinori, Yoshikazu, Yoshimasa, Yoshihisa, Yoshimi | Yasuko, Eishi, Shigeko, Tomiko, Sukeko |
| Africa | Mamluk Egypt, Cairo court | Faraj, Shaykh, Barsbay, Inal, Qaytbay | Julban, Mughal, Zaynab, Shakarbay, Fatimah |
| Russia | Muscovy, Pskov and Novgorod | Ivan, Vasilii, Dmitrii, Iurii, Andrei | Mariia, Stefanida, Marfa, Sofiia, Ponaria |
| Mediterranean | Catalonia, Crown of Aragon; Latin/Catalan records | Andreu, Baltasar, Gabriel, Manuel, Ramon | Elionor, Eulalia, Blanquina, Francina, Isabel |

## English evidence

Source: scholarly [Dictionary of Medieval Names from European Sources](https://dmnes.org/), edited by Sara L. Uckelman and contributors. Use the England/Middle English records, not merely the dictionary's broad 500–1600 coverage. EEW is the edition of *Early English Wills* identified by the dictionary; GCCYork is its York guild corpus.

| Display | In-range witness | Entry |
| --- | --- | --- |
| John | 1402, EEW p. 11 | [John](https://dmnes.org/name/John) |
| Thomas | 1408, GCCYork p. 10; also 1415 EEW p. 22 | [Thomas](https://dmnes.org/name/Thomas) |
| William | 1402, EEW p. 11 | [William](https://dmnes.org/name/William) |
| Henry | 1402, EEW p. 10 | [Henry](https://dmnes.org/name/Henry) |
| Richard | 1426, EEW p. 74 | [Richard](https://dmnes.org/name/Richard) |
| Agnes | 1406, EEW p. 13 | [Agnes](https://dmnes.org/name/Agnes) |
| Alice | 1428, EEW p. 82 | [Alice](https://dmnes.org/name/Alice) |
| Iohane | 1417, EEW p. 28; historical spelling of Joan | [Joan](https://dmnes.org/name/Joan) |
| Margery | 1426, EEW p. 67 | [Margaret](https://dmnes.org/name/Margaret) |
| Elizabeth | 1422, EEW p. 50 | [Elizabeth](https://dmnes.org/name/Elizabeth) |

## French evidence

[Sara L. Uckelman, French Names from Paris, 1421, 1423 & 1438](https://heraldry.sca.org/names/french/paris1423.html) transcribes Jean Favier's edition of the three Paris tax rolls, *Les Contribuables parisiens a la fin de la guerre de Cent Ans* (Droz, 1970). All selected forms occur in this wholly in-range corpus. The masculine table provides 1421 counts for Jehan, Guillaume, Pierre, Charles and Jaques. The feminine table pools the three dated rolls and contains Jehanne, Marie, Collette, Perrette and Ysabeau. The report does not invent a more precise individual year for a pooled feminine row. Spellings are retained.

## German-speaking evidence

[Sara L. Uckelman, German Names from Rottweil, Baden-Wurttemberg, 1441](https://www.ellipsis.cx/~liana/names/german/rottweil1441.html) extracts Eugen Mack's edition, *Das Rottweiler Steuerbuch von 1441* (1917), pp. 126–151. All ten exact forms occur in its **1441** given-name tables. The locality is explicitly Swabian/Alemannic, not an undifferentiated modern German national sample. Male: Hans, Heinrich, Conrad, Berchtold, Caspar. Female: Ursula, Dorothea, Adelhaid, Magdalen, Othilia.

## Italian evidence

Primary research database: [Brown University, Online Catasto of 1427](https://cds.library.brown.edu/projects/catasto/), edited by David Herlihy, Christiane Klapisch-Zuber, R. Burr Litchfield and Anthony Molho.

The published [male-name extraction](https://www.s-gabriel.org/names/ferrante/catasto/) lists Giovanni, Antonio, Piero, Francesco and Lorenzo in the male given-name field. The [female-name extraction](https://www.s-gabriel.org/names/arval/catasto/) lists Caterina, Antonia, Beatrice, Giovanna and Francesca. Both explicitly derive from the **1427 Florentine Catasto**, rather than a general Renaissance name list. The female extraction notes the underlying database's ten-character field clipping; these selected forms are nine or fewer letters and are not artificially shortened for this feature.

## Asia evidence

Authoritative publisher reference entries are served through Kotobank: Shogakukan, Kodansha, Heibonsha and Yamakawa dictionaries. The listed strings are romanizations of Japanese given names, with surnames and titles omitted. The scope is Muromachi Japanese court naming, not all Asian naming.

| Display | Historical bearer and in-range evidence | Source |
| --- | --- | --- |
| Yoshinori | Ashikaga; acquired this name in 1429; died 1441 | [dictionary entry](https://kotobank.jp/word/足利義教-14303) |
| Yoshikazu | Ashikaga, 1407–1425; shogun 1423–1425 | [dictionary entry](https://kotobank.jp/word/足利義量-25230) |
| Yoshimasa | Ashikaga; this name from 1453; died 1490 | [dictionary entry](https://kotobank.jp/word/足利義政-14305) |
| Yoshihisa | Ashikaga, 1465–1489 | [dictionary entry](https://kotobank.jp/word/足利義尚-14304) |
| Yoshimi | Ashikaga, 1439–1491; this name adopted in 1464 | [dictionary entry](https://kotobank.jp/word/足利義視-14306) |
| Yasuko | Hino, 1369–1419; court appointment in 1406 | [dictionary entry](https://kotobank.jp/word/日野康子-1103718) |
| Eishi | Hino, 1390–1431; dictionary reading えいし | [dictionary entry](https://kotobank.jp/word/日野栄子-1103658) |
| Shigeko | Hino, 1411–1463 | [dictionary entry](https://kotobank.jp/word/日野重子-120568) |
| Tomiko | Hino; married Yoshimasa in 1455; died 1496 | [dictionary entry](https://kotobank.jp/word/日野富子-120587) |
| Sukeko | Fujiwara/Hinonishi; title in 1425; died 1440 | [Kohanmon-in](https://kotobank.jp/word/光範門院-1074312), [Shoko entry, explicit すけこ reading](https://kotobank.jp/word/称光天皇-18324) |

Yoshitane was excluded: living during the century does not prove that a later adopted name was used before 1500. Conflicting modern birth years for Tomiko do not affect the selected 1455/1496 evidence. No post-1500 name adoption is used as evidence.

## Africa evidence

The sample is geographically **Egypt**, specifically Mamluk Cairo's court. Its bearers had varied family origins; this is not a claim that these names represent every African society or ethnicity.

Male source: Bernard O'Kane, *The Mosques of Egypt*, [American University in Cairo Press publisher record and dated monument contents](https://aucpress.com/9789774167324/). The monuments attest the patrons' names: Faraj (Cairo 1400–1411), Shaykh (1416–1420), Barsbay (1423–1424), Inal (1451–1456), Qaytbay (1472–1474). Titles and patronymics are omitted from the display.

Female source: Amina Karam, *Women, Architecture and Representation in Mamluk Cairo* (AUC MA thesis, 2019), [official AUC repository record](https://fount.aucegypt.edu/etds/787/), **p. 81**. Its dated pilgrimage discussion identifies Julban (1431), Mughal (1446–1447), Zaynab (1456), Shakarbay (1464), and Fatimah (1475). The passage cites contemporary chronicles by al-Maqrizi, Ibn Taghribirdi, Ibn Hajar, al-Sakhawi, al-Malti and Ibn Iyas, alongside Kathryn Johnson's research. This is gendered evidence for named women, not guesses from a modern baby-name list.

Access limitation: the official thesis landing record and the search-indexed p. 81 passage were inspected; the complete PDF endpoint returned 403. The [indexed repository mirror](https://fileserver-az.core.ac.uk/download/pdf/333723915.pdf) exposed the relevant passage through search but also blocked a full download. Do not describe this as a complete PDF review. ASCII display removes transliteration diacritics without truncating names.

## Russia evidence

[Names from Muscovite Judgment Charters](https://heraldry.sca.org/kwhss/2013/Marya_Kargashina/muscovite_namesrev.htm) extracts Ann Kleimova, *Justice in Medieval Russia: Muscovite Judgment Charters of the Fifteenth and Sixteenth Centuries*, *Transactions of the American Philosophical Society* 65(6), 1975. Only explicitly dated fifteenth-century rows are used here, not its undated or sixteenth-century rows.

| Display | Dated witness |
| --- | --- |
| Ivan | Ivan Khvoshchinskii, judge, 1462–1464 |
| Vasilii | Vasilii Fedorovich Saburov, boiar, 1485–1490 |
| Dmitrii | Dmitrii Ivanovich, prince, 1498 |
| Iurii | Pskov hundredman, 1483 |
| Andrei | Andrei Vasil'evich Bol'shoi, 1462–1464 |
| Mariia | Princess Mariia, 1466 |
| Stefanida | Landowner, 1466 |

Three additional dated female records come from Paul Goldschmidt's *Dictionary of Period Russian Names*, which identifies the underlying chronicle/document editions:

- [Marfa](https://heraldry.sca.org/names/paul/ma.html): Marfa Isakova, 1471, Nov p. 502.
- [Sofiia](https://heraldry.sca.org/names/paul/si.html): Great Princess Sofiia, 1462, Nov p. 495.
- [Ponaria](https://heraldry.sca.org/names/paul/a.html): source form Ponar'ia Bibishkina, 1435–1447, RIB II p. 21. Display omits the transliterated soft-sign apostrophe; it is not a fabricated shortened name.

The first source itself describes its transliteration as apparently Library of Congress style. Iurii, Mariia and Sofiia preserve those attested romanized forms instead of silently replacing them with modern English spellings.

## Mediterranean evidence

This is a **Catalonian/Crown of Aragon** sample independent of the Italian and French pools. DMNES gives exact dated spellings extracted from Pedro Miguel Carbonell's records, edited by Manuel de Bofarull y de Sartorio, [*Opusculos Ineditos del Cronista Catalan Pedro Miguel Carbonell*, volume I (Barcelona, 1865)](https://dmnes.org/bib/carbonell-I). Most entries occur in Latin records; Andreu also appears as Catalan. These are regional record spellings, not a claim that every row's documentary language is vernacular Catalan.

| Display | Dated source witness | DMNES entry |
| --- | --- | --- |
| Andreu | 1489, p. 37 | [Andrew](https://dmnes.org/name/Andrew) |
| Baltasar | 1488, p. 21 | [Balthasar](https://dmnes.org/name/Balthasar) |
| Gabriel | 1487, p. 13 | [Gabriel](https://dmnes.org/name/Gabriel) |
| Manuel | 1487, p. 10 | [Emmanuel](https://dmnes.org/name/Emmanuel) |
| Ramon | 1488, p. 22 | [Raymond](https://dmnes.org/name/Raymond) |
| Elionor | 1487, p. 11 | [Eleanor](https://dmnes.org/name/Eleanor) |
| Eulalia | 1487, p. 11 | [Eulalia](https://dmnes.org/name/Eulalia) |
| Blanquina | 1487, p. 11 | [Blanche](https://dmnes.org/name/Blanche) |
| Francina | 1487, p. 11 | [Frances](https://dmnes.org/name/Frances) |
| Isabel | 1487, p. 11 | [Isabel](https://dmnes.org/name/Isabel) |

The broader Castilian account-book collection spanning 1477–1504 was considered but not used because its individual rows did not establish pre-1500 dates.

## Validation ownership

Focused JUnit coverage in `MercenaryNamePoolTest` checks the eight exact labels, five names per sex and region, all 80 names distinct and within nine letters, immutable pool exposure, coverage of all selectable entries, repeatable UUID selection including negative hashes, and the distinction between legacy and new selectors. Existing `MercenaryIdentityTest` remains root-owned for contract/entity compatibility changes.

Worker verification is limited to source inspection and static pool checks. The root controller must run the approved build/test gates and verify new-hire persistence, legacy missing-field fallback, save/load, UI/entity name consistency, and release evidence. No historical source is queried by the game.
