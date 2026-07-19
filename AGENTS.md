# Cosmic Dungeon AI Development Contract

Cosmic Dungeon is a Java 21 NeoForge mod for Minecraft 1.21.10. These rules apply to all AI-assisted work in this repository.

## Task, Branch, and Pull Request Discipline

- One planned task card equals one branch and one pull request.
- Follow-up corrections for the same task remain on that same branch and pull request.
- Never commit, push, or merge directly into `main`.
- Never merge a pull request unless the user explicitly instructs you to do so.
- Before editing, state:
  1. The intended behavior.
  2. The files and directories expected to change.
  3. Any central integration files that may be required.
  4. Saved-data, registry, networking, migration, or client/server implications.
  5. Required automated and manual testing.
- Keep every task narrowly scoped.
- Do not perform unrelated cleanup, formatting, refactoring, renaming, or documentation changes.
- Do not silently expand the requested feature.
- Stop and report high-risk conflicts rather than guessing at a resolution.

## Existing Architecture and Reuse

- Study adjacent code and similar existing implementations before adding imports, registries, packets, menus, events, saved data, commands, services, or helper classes.
- Reuse established project foundations and patterns.
- Do not create duplicate networking, registration, persistence, access-control, transaction, or utility systems when an existing system can be extended safely.
- Preserve existing IDs, behavior, compatibility contracts, and server-authoritative boundaries unless the task explicitly requires a change.

## Exclusive Integration Hotspots

- Exclusive integration hotspots are single-writer surfaces.
- Within one planner wave, only one active task may modify a hotspot or any file whose purpose is to register into, control, serialize, migrate, or centrally coordinate that hotspot.
- Read-only inspection of a hotspot is allowed by multiple tasks.
- Every task card must declare which exclusive hotspots it owns.
- If a task unexpectedly discovers that it must modify a hotspot owned by another active task, stop before editing and report the dependency or conflict.
- Tasks requiring the same hotspot must be placed in separate planner waves or intentionally chained as dependent branches.
- Never automatically resolve conflicts in these surfaces without first explaining both competing intents and receiving explicit direction.

Exclusive hotspots include:

- All registry, `DeferredRegister`, and central registration classes for items, blocks, entities, block entities, menus, particles, sounds, data components, creative tabs, and similar registrations.
- `CosmicDungeonMod.java`.
- `ModNetwork.java`, `ModNetworkClient.java`, payload registration, payload codecs, and packet dispatch handlers.
- `ModMenus.java` and central menu registration.
- `HelpMenuContent.java`.
- SavedData, NBT, serialized schemas, preset versions, storage versions, and migration classes.
- Currency, vendor, trade, inventory-transfer, and Dragoon Repair transaction services.
- Dungeon reset, world snapshot, entity-storage restoration, and rollback services.
- Rift, RD destination, teleportation, safe-teleport, Access Policy, and class-enforcement code.
- `docs/releases/Update_1.5.1.md`.

The AI planner may designate additional files as exclusive hotspots after reviewing the architecture index.

## CosmicDungeonMod.java

- Treat `CosmicDungeonMod.java` as a high-risk foundation file.
- Keep changes to it minimal.
- Use it only for registration, event subscription, initialization, and delegation.
- Do not place feature logic, transaction logic, persistence logic, or large command implementations in it.
- Put functional logic in dedicated object-oriented classes and call those classes from the core entry point.

## Client and Server Safety

- Gameplay-sensitive behavior must remain server-authoritative.
- Never rely only on a client GUI, disabled button, hidden row, client config, or client packet state to enforce access.
- Revalidate permissions and transaction conditions on the server.
- Prevent client-side workarounds for class restrictions, access policy, vendors, currency, inventories, trades, repair, teleportation, progression, achievements, or dungeon systems.
- Verify compatibility with:
  - Dedicated servers.
  - Integrated servers.
  - Shared client/server mod jars.
  - Login, logout, death, dimension change, disconnect, and server-stop lifecycle events where relevant.
- Do not import or initialize client-only classes from common or dedicated-server code paths.

## Regression Boundaries

For every relevant change, inspect and report possible effects on:

- Access Policy.
- Player classes and class-attuned items.
- Teleportation, rifts, and RD destinations.
- Cosmic Mob Spawners.
- Doors and keys.
- Vendors, trades, inventories, and currency.
- Progression, factions, and achievements.
- Networking and menu/session state.
- Entity and block-entity persistence.

Do not modify these systems merely to claim they were verified. Inspect the affected boundaries, run relevant tests, and report what was and was not exercised.

## Persistent World Data and Safe Updates

- Treat all world, entity, block-entity, SavedData, NBT, preset, and external data formats as high risk.
- Preserve compatibility with existing 1.5.0 worlds when updating to 1.5.1.
- This includes, where applicable:
  - Cosmic Mob Spawners.
  - Doors and keys.
  - Rifts and RD destinations.
  - Vendor, currency, faction, progression, and achievement data.
  - Entity and block-entity data.
  - Preset files and versioned JSON formats.
- Do not rename or remove registry IDs, NBT keys, serialized field names, data versions, preset fields, or saved-data identifiers without a backward-compatible migration.
- When storage changes are required:
  1. Identify every supported old version or data shape.
  2. Load old data without discarding fields.
  3. Convert it automatically into the new representation.
  4. Update the stored version only after successful conversion.
  5. Preserve a safe failure path instead of partially corrupting data.
  6. Add migration and round-trip regression tests where practical.
  7. Document backup, upgrade, rollback, and compatibility expectations.
- If storage does not change, explicitly state that no migration is required.

## Critical Cosmic Spawner Protection

The development team has placed hundreds of Cosmic Spawners in live worlds. Their work must never require manual recreation because of a code or data-format update.

For every Cosmic Spawner change:

- Determine whether block-entity NBT, saved data, preset NBT, preset JSON, field names, data versions, or file formats change.
- Preserve all existing placed spawners and authored presets.
- Detect old versions or old data shapes automatically.
- Provide a backward-compatible automatic migration covering every affected persisted spawner.
- Use the safest architecture supported by the existing code, such as load-time migration with resave, a controlled one-time migration, or another complete strategy.
- Do not silently ignore unloaded chunks or old preset files when immediate full conversion is required.
- Never require developers to replace or manually rebuild hundreds of spawners.
- Log migration detection, success, failure, and useful summary information during the applicable server startup or data-loading process.
- Add tests proving representative old data loads correctly, preserves behavior, and saves in the new format.
- Provide explicit server backup and update instructions.
- If the spawner storage format remains unchanged, clearly state that no migration is required.

## NeoForge Data Generation and Resources

- Use NeoForge datagen wherever the project and professional NeoForge conventions expect generated JSON.
- Follow the repository’s existing separation between client and server generated resources.
- Do not create both generated and hand-authored versions of the same resource.
- Run the relevant client and/or server datagen tasks when changing:
  - Item or block models.
  - Item definitions.
  - Blockstates.
  - Tags.
  - Recipes.
  - Loot tables.
  - Advancements.
  - Other resources already managed by project datagen.
- Inspect generated changes before committing them.
- Do not run or modify datagen when it is unrelated to the task.
- Hand-authored configuration or profile JSON may remain hand-authored when that is the project’s established design.

## Documentation

- Update documentation only when relevant to the requested change.
- Add or refactor sections and subsections when a feature creates a genuinely new topic or category.
- Update related pages when behavior or contracts change.
- Add useful cross-links so related documentation can be navigated without unnecessary duplication.
- Do not expose developer-only commands or unfinished mechanics in player-facing documentation unless clearly marked and intentionally requested.
- Normal feature pull requests must not directly edit `docs/releases/Update_1.5.1.md`.
- Normal feature pull requests must instead create one unique release fragment under:
  `docs/releases/fragments/<task-or-pr>-<short-name>.md`
- Only a dedicated release-assembly task should combine fragments into `docs/releases/Update_1.5.1.md`.
- A task explicitly designated as release assembly may edit the main update document.

## Required Validation

Use Java 21.

For code, runtime data, resource, Gradle, or workflow changes:

- Run `./gradlew clean build`.
- Run `./gradlew runGameTestServer`.
- Validate all changed JSON, and preferably all JSON under `src`.
- Run relevant client and server datagen when applicable.
- Run `git diff --check`.
- Add or update regression tests for server-side bug fixes whenever practical.
- Never remove, weaken, skip, or rewrite a valid test merely to obtain a passing build.
- If `./gradlew compileJava` or another Gradle command fails before compilation because the environment is using the wrong Java runtime, switch the environment to Java 21 and rerun it.
- Do not describe a Java-version mismatch as a source-code failure.

For documentation-only changes:

- Run appropriate formatting, link, search, or diff checks.
- State clearly why runtime build, GameTests, JSON validation, or datagen were not applicable if they were not run.

Compilation alone does not prove that runtime behavior, transactions, persistence, networking, or GUI layout are correct.

## Manual Minecraft Testing

- Report every remaining behavior requiring in-client or dedicated-server manual testing.
- Provide exact, short testing steps.
- Distinguish clearly between:
  - Automated tests that passed.
  - Code-reviewed behavior.
  - Manual testing that was performed.
  - Manual testing that still remains.
- Never claim a GUI, rendering, interaction, multiplayer, or live-world migration behavior was tested when it was only inspected in code.

## Completion Report

At the end of every task, report:

1. What changed.
2. Exact files changed.
3. Automated validation performed and results.
4. Datagen performed or why it was not applicable.
5. Saved-data or migration effects.
6. Client/server and security implications.
7. Remaining manual Minecraft QA.
8. One concise sentence describing a possible future improvement.

Do not claim certainty beyond the evidence produced by the build, tests, code review, or manual QA.
