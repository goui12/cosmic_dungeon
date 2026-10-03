# Testing fixes, batch 1 — 2026-10-03

## Authority and scope

Cameron's testing report, eighteen answers and final four answers authorize this rollout in
2–3-change batches. This task owns exactly three gameplay changes:
1. Permanent first-eligible ownership for attuned gear, voluntary/death drops, and centered death drops.
2. Actual player/companion/environmental kill attribution and immediate eligible group splits.
3. Bogatyr wolf-egg taming and protection of all friendly players, NPCs and pets.

The new explicit ownership/drop instruction supersedes the class-issued no-drop sentence in
Gear Trading and Vendor Sales 2.0 (doc 1byHfuC0G_lb0IRrgO3kblLYP06AY8gJWm9bJOMlrFIc,
modified 2026-08-18) and the previous prohibition on automatic ownership metadata at pickup.
Only the requested ownership field is added; class-chest contents, names, quantities,
enchantments, payloads and loadouts are not reauthored.
Wild-wolf bone taming and existing companion cap/recovery remain; egg auto-taming is explicit
new authority beyond Wolf Internal (10-3IgopUqHKyPHuZDKlpa64JMYXmq-_8GgKtQFhLX3c,
modified 2026-04-25).

Branch: feature/testing-fixes-batch1-20261003
Base: feature/slot-chest-ownership-20260926, 256bd5dd2fb3c828588a097f4fde780cde2407e3.
One task / branch / draft PR. No merge or direct main update.

## Integration and persistence

Single writer owns item access/movement and death-currency drop positioning, kill-credit
hooks, wolf lifecycle hooks and common mixin registration. No network packet, item/entity
registry, world format or spawner preset change.

Additive item CUSTOM_DATA key cosmicdungeon_attuned_owner_v1 stores the original UUID.
Absent ownership claims only on eligible inventory acquisition (one login pass for existing
carried gear); malformed ownership is preserved and denied, never silently reassigned.
No unloaded chest/world scan. Native component codecs preserve the owner and authored fields.
Partial/failed inventory adds return unclaimed remainders without a new owner.
An already bound owner can recover the item after changing class; existing class-use limits remain.
Private storage, automation and trade restrictions remain.

Additive entity persistent key cosmicdungeon_last_damaging_player_v1 records actual positive
damage from a player or owned companion. Environmental death without such attribution pays
nothing. No damage history table, scan, timer, deferred payout or new SavedData.
D1's existing successful-run statistics remain until the separately planned lifetime leaderboard.

Egg-spawned wolves reuse the existing Bogatyr owner metadata and native tame ownership.
Both ordinary egg placement/fluid use and egg-on-wolf offspring must be covered.
Friendly targeting and final incoming damage are independently guarded.
Existing managed pet identity, cap, archive and travel rules remain.

## Validation and manual acceptance

Java 21 compilation, native JUnit owner/codec/kill-credit/ally regressions, existing offline
checks, JSON and scoped diff checks. Handwritten logic/common mixin JSON requires no datagen.
Use build-local.ps1 -Task build for successful TEST artifact publication.
No clean (preserve preexisting staged historical-JAR deletion), GameTest/dedicated server launch,
live-world edits or server.properties changes.

Native client startup checks mixin loading; no automatic world entry.
Licensed TEST acceptance: same-class nonowner pickup, owner recovery after death/relog,
ordinary/shift/full/partial inventory and projectile pickup, drop coordinates, attribution after a cliff death,
no reward for untouched environmental mobs, both wolf-egg paths/cap, friendly retaliation,
and existing wolf archive/rejoin. No claim of gameplay acceptance from compilation.

Final evidence and exact allowlist are recorded in the task's local cache and completion notes.
