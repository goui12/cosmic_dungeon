# Task: testing fixes batch 4

Base: dbf9c3ec / feature/testing-fixes-batch3-20261003. Chained draft PR; no merge.

Implement exactly three accepted areas: named Tamsin groups/create/leave; LFG class
advertising and invitations by every member, including non-LFG friends; player class
selection exclusively through Tamsin. Existing readiness and capacity remain batch 5.

Expected files: npc/tamsin lobby/service/flow; network/PartyPayloads and ModNetwork;
client/screen/D1PartyPanel and ClassSelectorScreen; ClassSelectorMenu and selector
session/block guards; focused tests; rollout/report/release fragment; AGENTS communication rule.
Exclusive single writer: networking, menu/session, class authorization, lobby coordination.
No registry IDs or saved-world formats change. Temporary lobby data only; no migration.
Protocol bump requires matching client/server jars. Server validates NPC/session/range,
active-run exclusion, invitation group identity, revision, capacity and eligibility.
LFG uses bounded pages and existing throttled delta sync; no new threads or runtime dependencies.

Validate Java21 build, native lobby/codec tests, d1OfflineChecks, src JSON and scoped diff.
No datagen applies; preserve historical tracked JAR, no clean or dedicated/GameTest launch.
Manual TEST: create/name/leave/disband; member invitations/LFG/class labels/expiry;
Tamsin class switching and rejection during active runs. These are pending tests, not questions.
