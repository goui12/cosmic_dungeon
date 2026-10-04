# Task: testing fixes batch 5

Base05685591 / Batch4 PR203; chained branch and draft PR, no merge.
Exactly three areas: leader-selected maximum1–6; mobile personal readiness/group HUD;
invitation HUD and inventory-only HUD controls. Initial Ready at Tamsin closes the menu.
Existing solo authorization permits one member; maximum is never a required total.
Old selector MaxPlayers supplies the default, while each leader can select1–6 independently.
Members may move throughout the same starting dimension, without needing the NPC menu
or selector proximity. Leader begins/starts at Tamsin; all members still confirm personally.
Not Ready withdraws that member and cancels any submitted countdown without erasing others.
No join/class/leave-lobby actions grant access to active dungeon rosters.

Single writer owns lobby/session, network registration/codecs/client dispatch, selector
capacity/entry guards and client HUD integration. Expected src changes include tamsin
lobby/service plus cohesive HUD helper, PartyPayloads, ModNetwork/Client, selector block
entity/entry, Config's retained legacy minimum comment, party panels and currency placement.
Preserve saved NBT keys/data shapes/registry IDs and authored chests; no migration.
Protocol update requires matching client/server builds. Temporary HUD/cache only, bounded
by connected participants; existing poll and delta sync, at most6 roster rows, no world scans.

Validation: Java21 build, focused lobby/codec/HUD-layout tests, d1OfflineChecks, all src JSON
and scoped diff. No datagen applies; no clean or dedicated/GameTest launch. Native startup
and stopped TEST/closed installed-client deployment per standing pipeline. Gameplay QA:
underfilled/solo capacity, concurrent personal toggles/stale actions, moving outside selector
range, invite acceptance/expiry, inventory HUD at normal GUI scales and active-run guards.
