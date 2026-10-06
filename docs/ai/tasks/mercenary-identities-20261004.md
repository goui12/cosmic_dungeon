# Batch 1 - menus and mercenary identities

Authorized by Cameron on 2026-10-04: complete Batch 1 only; batches 2-9 remain planned.
Source of behavior is his current explicit request and the saved rollout plan.
Branch: feature/mercenary-identities-20261004. Release: 1.5.3-alpha.1.

## Scope and integration ownership
Own D1PartyService/Rules/Lobby/HudService for menu readiness, lobby identity and
snapshot labels. Own MercenaryContract/Entity/Respawns for derived names and
existing-entity load behavior; client MercenaryRenderer/RenderState and D1PartyHud.
No registry, ModNetwork, packet codec, currency, inventory, class-chest contents,
wolf, combat or dungeon-reset changes. No other active writer uses these surfaces.

Each hire reserves one random UUID in the transient lobby. Contract UUIDs already
persist through native entity/run saves; both name and skin derive from that ID.
Names use a frozen pool of 100 distinct English-inspired names, each <=9 letters.
Nine bundled wide skins match the current model/armor and require no web lookup,
extra textures or dependencies. HUD retains existing health/respawn status.
Older entities derive their new label from the existing contract on load; no IDs,
NBT keys, fields, schema versions, spawner data or migrations change.

Leader Ready preserves the existing menu; successful nonleader Ready keeps its
previous dismissal. Server admission, stale revisions, all-ready checks and
leader-only queue submission remain authoritative.

## Validation
Passed: Java 21 build, 272 native tests (nine new), two loading-helper tests,
2,001 source JSON files, diff checks and both packaged versions/service metadata.
Publisher safeguard suite: 11 checks passed. CI clean build/GameTests, release
publication and stopped/closed target deployment are remaining delivery gates;
record their exact run/commit/receipt results in the private operational checkpoint.
No datagen needed: no generated resources, models, recipes or loot tables changed.
No client/server launch authorized; GameTests run in the existing CI workflow.

## Manual QA (pending)
1. As leader, begin readiness and press Ready: Tamsin stays open. Wait for the other
   member, then press Start Adventure without reopening. Nonleaders cannot start.
2. Hire several companions: short named lobby/HUD rows and varied skins, armor
   and held items intact. Names/skins should persist after entry/relog/death/revival.
3. Inspect normal and recipe-book inventory HUD layouts: no owner/Mercenary label;
   health bars and rest countdown remain readable.

Eight later batches: wolves/drops; recovery/payments; shared skills; Theurgist
potions; Bogatyr skills; Pyroclast fireworks; Dragoon lightning; resurrection.
Follow-up: verify CurseForge app delivery of both JARs after companion approval.
