# Tamsin map and party controls - 2026-09-25

Cameron requests an impressive old damaged tattered map artifact, around 516-square,
a route leading somewhere, Start Adventure! instead of Join queue, one Ready/not ready
toggle, and class names with initial capitals. He explicitly confirmed TEST off/client
closed and authorized commit/push and replacement of this plus preceding trade/region work.

Branch: feature/tamsin-map-party-ui-20260925 from 8e1d5e56 (region PR195; trade PR194).
Scope: ClassSelectorScreen, D1PartyPanel, D1PartyPresentation, TamsinMapLayout,
D1PartyPresentationTest, tamsin_d1_map.png and its metadata, task/release documentation,
and the one-line region-guide title encoding correction.
No exclusive server integration hotspot, transaction, packet, registry or saved-data changes.

Built-in ImageGen created the new transparent parchment master; the runtime PNG is
Lanczos-resampled to 516 x 516 with genuine alpha. Connected caverns, stone halls,
stairs/bridges and a red route lead to Base Camp. Full prompt is recorded alongside this
task; only this decorative briefing artwork is generated, not authoritative world geometry.
Current user spelling JWH governs this artwork. Existing lore's JHW wording is preserved
in its source documentation; this request does not rewrite wider canon.
PNG SHA256: 515c34ef094d463da40b90728d9c30e90855b57e5334503a53b8bfbed6e12a23.
Runtime texture: src/main/resources/assets/cosmicdungeon/textures/gui/tamsin_d1_map.png.
Map panel expands within viewport, uses square full-image UVs and retains Continue/class guidance.
The previous schematic fallback is replaced by an unavailable-image message if an override removes the asset.

Readiness label/action derives from the local player's server-confirmed roster entry.
Ready sends ready; not ready sends unready, including queued withdrawal. PREPARING stays
disabled. Start solo/Begin ready check are separate setup controls; leader-only Start
Adventure! sends the existing queue action only after everyone is ready. Existing unready
behavior cancels the party ready check/queue; no lobby behavior is changed by this UI task.
Roster class text reuses the selector's translated names; raw IDs remain unchanged.

No datagen for this hand-authored/generated bitmap or UI-only source. No migration.
Validation: Java21 build and64JUnit cases passed, including four new readiness/map bounds
checks. All1,997source JSON files plus map metadata parsed.516square RGBA/transparency
and transfer hash verified visually and mechanically. Original selector Unicode restored
after diff review; the preceding region-guide title em dash is also restored in this checkpoint.
Final rebuilt JAR/hash/diff checks, normal source push and committed receipt follow.
No GameTest/dedicated server launch, world/config edit, authentication change or server restart.
Deployment: use existing safe script after rechecking closed client; verify identical installed
TEST/client hashes and current-test publication. Preserve unrelated mods and staged old JAR deletion.
Remaining manual QA: map display, names, ready toggle, group/solo setup, leader start;
plus prior trade interactions and instance-region outlines. Zero additional planned batches.
Potential future improvement: tune map scale after native viewing on both testers' GUI settings.
