# Class skills Batch 1 — shared foundation

Cameron authorized Batch 1 of the new six-batch series. Baseline main: c5a7991e54a329f61cb59389695260072ebeacef. Task branch: feature/class-skills-b1-20261115. Development checkout: C:\Users\Cameron\Documents\mod_development\cosmic_dungeon_skills_20261115.

## Delivery contract

Build version 1.8.9-beta.1 is an unpublished source beta. Batches 1–5 update local development files and GitHub only. No TEST server, local client, CurseForge, release tag or published artifact update before Batch 6 finishes. Batch 6 delivers the completed official stable build. This turn stops after Batch 1; five cards remain. See [approved full plan](CLASS_SKILLS_BATCHES_20261115.md).

## Implemented boundary

- Shared Positive Potions, Negative Potions, Wolves, Fireworks, Chain Lightning and Smite identities, actor-neutral labels and snapshot format.
- Additive versioned per-player signature storage in existing progression SavedData. Missing Positive Potions starts from exact historic theurgist.potions XP; useful support continues independently beyond the weapon cap. Unknown track entries survive codec round trips. Invalid versions/counts fail closed. Existing weapon XP and unrelated saved fields remain intact.
- Existing mercenary counts and curves are read without conversion. Smite uses a new successful-cast counter; old Combat counts are retained and do not become Smite casts. Mercenary progression remains per run.
- Generic native Controls entries: Primary class skill G, Class skill 1 Z, Class skill 2 X, Class skill 3 C. Class switching retains bindings. Inventory shows the slot number/name and actual binding in small italics. New slots are gray and unassigned until later batches. Existing Bogatyr utility and resurrection bindings survive.
- Server-owned bounded snapshots and slot requests; active run/class/revision validation and rate limiting. Requests contain no client-selected ability power. Periodic acknowledgements prevent throttled actions from leaving the client waiting indefinitely.
- Inventory Skills panel retained alongside the brewer palette, with layout reservations and signature progress. Existing crafting, resurrection and combat effects retain their behavior.
- Wire protocol 36. No new buff/combat effects or gameplay activation for the four generic slots yet.

Player Positive Potions keeps the existing XP scale with an independent ceiling of 100. New non-potion tracks use successful-action counts (triangular levels 0–100); Smite starts at 1 and advances at 10/30/60 successful activations. Existing mercenary curves stay intact. Successful-action integration for the other new player tracks is explicit remaining work in Batches 3/4/6; stored starting levels do not imply implemented earning hooks.

## Verification and review

Local Java 21 Gradle build, targeted regression suites, JSON parsing and diff checks are recorded in the operational Batch 1 receipts. GitHub's full Integration Gate must pass on the exact pushed head, including clean build and native GameTests. Native save/reload fixtures cover exact legacy potion XP, new-entity/class-switch state, independent mercenary run counters and Smite/Combat separation. No local game or GameTest server is launched.

The cooperative review found a potential pending-request deadlock after a throttled action; bounded deferred acknowledgement and regression coverage resolve it. Final validation results, exact commit, PR and CI run IDs are recorded in the controller completion receipt rather than pre-claimed here.

Manual native GUI/keyboard/co-op acceptance remains pending until the completed build is delivered. Automated tests do not substitute for screenshots or real play. All unresolved recommendations and sources remain in the [active review ledger](BATCH_1_7_20_REVIEW_NOTES_20261012.md).

## Save compatibility and manual acceptance

No destructive migration is needed: the optional signature_skills field is absent from old saves and reads as an empty map; missing Positive Potions falls back to the exact old XP until the first useful support award writes its own entry. Unknown skill IDs inside the version-1 values map survive a load/save cycle. Unsupported signature versions and invalid values are rejected rather than reset. The existing SavedData ID and weapon/mercenary keys remain unchanged. An older build cannot preserve newly introduced signature records when it rewrites progression data; do not use an old executable to save a world after this new feature has accumulated progress.

After Batch 6 distribution, open each class inventory at two GUI scales: verify the signature, slot number/name and actual italic key, gray unused slots, potion-list scrolling and preserved resurrection/wolf commands. Remap and unbind G/Z/X/C in native Controls, switch class and reconnect: verify the same saved bindings, correct class actions and no action through chat/modals/dragging. Perform the party/co-op lifecycle acceptance retained in the active ledger. These are pending checks, not claims of played validation.

## File groups

Progression: SignatureSkill, SignatureProgress, SignatureSkills, PlayerProgressionData and the existing SkillPotions support hook. Network/lifecycle: ClassSkillSlot, ClassSkillPayloads, ClassSkillActions, ClassSkillActionEvents, ModNetwork and ModNetworkClient. Client: ClassAbilityClient, SkillInputPolicy, SkillKeybindClient, SkillsPanelClient, SkillBindingLabels and English Controls labels. Tests: signature persistence/native fixtures, payload authority/bounds, request lifecycle, key policy and panel/binding layout. Guides/release fragment/version/remaining-plan/ledger are updated with this boundary.
