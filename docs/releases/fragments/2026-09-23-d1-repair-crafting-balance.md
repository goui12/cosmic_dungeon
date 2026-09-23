# D1 repair, crafting policy and account panels

- Accept approved unmarked vanilla raw repair materials in service logic; retain marked
  weapon-kit identity and all existing custody/payment/return safeguards.
- Add server recipe-ID allowlists in CosmicDungeon.config, with separate class and
  automation permissions, stale-result checks and native remainder handling.
- Show the existing shared account on the HUD, ordinary inventory and class chests using
  the five existing currency icons. No new PNG or saved balance store.
- Protocol 7 requires matching client/server builds; no registry or save migration.
- Java21 build, 22,387 offline checks, two config round trips and 1,994 JSON checks passed.
  Native licensed TEST acceptance remains pending; no deployment occurred.

See [implementation and exact files](../../ai/D1_READINESS_IMPLEMENTATION_2026-09-23.md).
