# Dungeon 1 canon/config implementation candidate

Not release-ready: keep this fragment separate until remaining D1 work and multiplayer acceptance finish.

- Add server CosmicDungeon.config and all_vendors_prices.config with class/NPC sections and current prices.
- Separate instance objectives from persistent lifetime Bloom/completion statistics.
- Implement D1 Watson/Bloom, owned Chop travel, vendor/faction, repair, class effect and achievement paths.
- Add save fields/IDs, normal-save recovery and offline migration/reward/config checks.
- Preserve authored vanilla equipment, placed block/item IDs, spawner formats and world data.
- Generate effect sprite aliases and name-only advancements through NeoForge datagen.

Selector/repair payloads require matching client/server jars.
Crash atomicity, Tamsin interfaces, legacy loot provenance and other D1 gaps remain TODOs.
No new D1 custom item texture is required. No deployment or runtime acceptance occurred.
See ../../ai/D1_IMPLEMENTATION_20260916.md for limits, migration/rollback and manual QA.
