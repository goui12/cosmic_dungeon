# Batch 9: mercenary field behavior

Authorized: Cameron, 2026-10-04. Chained on Batch 8 aa54ebabe5380f8d686a184d8e2ee22b8bff9c27; one branch and draft PR. Stop before Batch 10.

Implement three approved actions: follow and safe teleport recovery with ally-safe combat; permitted chest/ingredient/dropped-item collection and native automatic brewing; configurable timed fallback healing and individual potion cooldowns.

Expected files: mercenary package, companion/combat integration, class-chest permission accessor if needed, native JUnit tests, configuration example, relevant AI/player documentation and unique release fragment. Single writer owns mercenary entity persistence/configuration, inventory transfer, movement/teleport and combat integration. No concurrent task owns these hotspots.

Server authority validates active run, identity, hirer and dimension. Bounded/throttled spatial work; no full-world scans, chunk forcing, dependencies or new packets. Reuse native navigation, combat/brewing and existing ownership rules. Whole authored stacks retain components; no chest reauthoring, bound-item theft or personal-slot bypass. Add optional entity timer data with old-shape defaults; preserve all existing registry/save IDs. Spawner/preset data unchanged.

Validation: Java 21 native JUnit plus build; old/default and round-trip timer tests, inventory conservation, ownership, valid brewing/remainders, potion safety and cooldowns; all src JSON, scoped diff/doc links, offline checks. Datagen only if generated resources change. No destructive clean or GameTest/dedicated launch. Licensed QA covers follow/stuck recovery, combat/friendly potion boundaries, collection/brewing and fallback timing. Complete standing scoped push/draft PR, stopped TEST/closed installed-client deployment, current-test/updater and fresh development-client handoff.
