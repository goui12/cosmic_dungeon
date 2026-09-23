# D1 authored rewards and NPC bindings

Cosmic Spawners previously rebuilt entity spawn data when adding provenance and omitted
native spawn data/potentials on load. That could lose authored reward metadata and equipment.
Spawn entries now retain their data and receive provenance on a copy; malformed tags pause
the spawner for review. Existing preset versions and one-shot settings remain intact.

Server developers can configure reviewed per-spawner rewards in CosmicDungeon.config.
Reward resolution rejects ambiguous provenance, duplicate rules and non-integer overrides.
Registered neutral/custom mobs can qualify; authoritative NPC roles cannot generate mob rewards.

Vendor authoring now targets the actual visible crosshair entity, refuses conflicting roles,
requires native Creakings for Beluzon and retains original NPC flags for later clear operations.
Offer tiers remain within their profile's dungeon system. Existing personal access and prices
are retained. No world placement or automatic profile migration is performed.

Java21 offline build,10,794 checks, two configuration round trips and source JSON validation
passed. Full native spawning, entity load/restart, menus and multiplayer remain untested.
No new required texture; no deployment. Details: docs/ai/D1_BATCH_34.md.
