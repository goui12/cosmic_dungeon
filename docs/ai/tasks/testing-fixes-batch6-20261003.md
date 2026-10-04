# Batch 6 - Instance difficulty and cumulative completion

Authorized: Cameron, 2026-10-03. Base: Batch 5 / 3e6561b9 / PR #204.
Branch: feature/testing-fixes-batch6-20261003. Single writer; no parallel agents.

## Scope

1. Leader-selected Easy/Hard/Insane/Ridiculous at Tamsin. Each factor is independently configurable and frozen per run. Health/damage/spawn-count defaults 0.5/1/1.5/2; spawn delay varies inversely. Easy shortens harmful effects. Insane has a 0.1% chance of five-second blindness per damage event. Ridiculous armor wear is 10% faster. Bosses remain single-spawn; loot and authored spawner/preset/chest data are unchanged.
2. Successful completion grants that difficulty and every lower completion advancement. Failed/forfeited runs grant none; offline recovery and existing NORMAL records remain supported.

## Owned integration surfaces

Run registry codec and lifecycle entry; completion progress and inventory-handoff completion side effect; party View codec/protocol and Tamsin readiness; Cosmic spawner transient runtime; difficulty combat/effect events and mixin registration; advancement generation and language strings. No concurrent writer exists.

Optional run-profile field defaults to the legacy baseline. Additive storage only, round-trip tests required; no spawner schema changes. Server owns settings, permission/revision checks and awards. No server.properties change.

## Validation / handoff

Regression tests for settings, old/new run round trips, independent instance profiles, personal readiness invalidation, spawn caps/bosses, effect/armor rules and cumulative completion including legacy/offline recovery. Java 21 tests/offline checks, server datagen for advancements, inspect generated output, full build, JSON and scoped diff checks. No clean or dedicated/GameTest launch. Manual simultaneous-instance combat and completion/UI QA remains required.

Scoped source commit and normal push; chained draft PR. Matching stopped TEST and closed installed-client deployment, current-test feed, updater verification. Existing Batch 5 development client is open; do not close it or launch a duplicate. Stop after Batch 6; five batches 7-11 then remain.
