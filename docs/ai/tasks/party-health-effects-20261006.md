# Batch 14 - Complete group HUD
Authorized October 6: TEST stopped; complete Batch 13 deployment then Batch 14 only.
Base fb195f1eb929c980c46711f0816c06f1c5c7161e; branch feature/party-health-effects-20261006.
Batch 13 exact published build installed on stopped TEST; ADMIN matched; current-test a94958b1.
Ownership: PartyPayloads/ModNetwork, party snapshot/mercenary status, client HUD/layout, native GameTest registration.
Changes: server-authoritative human and mercenary vitals/effects; active-run cards; no gameplay transactions or persistence changes.
Protocol 18 requires both ends updated. No datagen or migration.
Validation pending: Java21 build, focused codec/layout/native snapshot tests, CI clean build/native GameTests, review and release.
Manual QA: full party including self/mercs; damage/heal/effect add/remove; death/relog/outside dungeon; GUI scales and scrollbar; recipe-book compact hover; revive/skills regression.
Canon: same-day Google OAuth expiry at 15:34Z, cached sources plus explicit approved user requirements; no fresh semantic audit claimed.
