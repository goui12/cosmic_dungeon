# Mercenary class roles — 2026-10-04

Status: implemented; Java 21 build and 261 native tests passed. Cameron resolved the deployment blockers on 2026-10-04: the latest stop indicator is sufficient, and obsolete rollback cache copies may be removed. Apply and verify the built JAR under the updated AGENTS.md policy.
Branch: feature/mercenary-class-roles-20261004, stacked on docs/agent-deployment-policy-20261004 (PR 213).
This is a requested follow-up; zero numbered D1 implementation batches remain.

## Approved behavior
Cameron's current instructions control this change:
- Only Theurgist mercenaries brew. Other classes follow the leader and attack mobs.
- Theurgists brew instantly at a nearby brewing stand.
- Resolved choice: use the mercenary's own copied supplies; preserve player items in the stand.
- Bogatyrs additionally summon one tamed dog every two minutes, maximum five per mercenary.
- Dogs cannot attack allies, take allied damage, or receive negative effects from allies.
No unresolved gameplay decisions are inferred from the older shared potion behavior.

## Scope and owned integration surfaces
Owner: this task only. Mercenary entity optional NBT cooldown, new wolf bond marker, and existing D1RunData roster/dismissal namespaces; mercenary inventory usefulness/atomic private brewing; lifecycle cleanup and Bogatyr permanent-pet enrollment exclusion.
Common server AI, collection and protection hooks are in scope. No central mod/network/registry registration changes, new packets, dependencies, world scans or forced chunk loads.
Existing gear/ammunition rules, copy-only chest access and key exclusion remain in force.
See [implementation and QA report](../MERCENARY_CLASS_ROLES_20261004.md) for the exact files.

## Validation and sources
Use Java 21 build, native JUnit regression tests, all-src JSON validation and diff checks. Preserve the historical tracked JAR; no destructive clean. No datagen inputs change. Dedicated/GameTest launch is not authorized; live gameplay remains pending.
Scoped metadata refresh of the Theurgist, Bogatyr and Wolf source Docs failed in the existing Google authorization refresh. Private receipt: Google Docs and Sheet/Audit/Mercenary_Class_Roles_20261004/sources.json. Do not describe the current canon as freshly audited.
Deploy to independently verified stopped targets under AGENTS.md; do not launch the client.

## Deployment follow-up
Cameron explicitly authorized JAR replacement, accepted the latest-session "Stopping the server" indicator, and removed the historical rollback retention requirement. AGENTS.md and docs/LOCAL_AI_WORKFLOW.md are updated together. No gameplay/schema changes in this follow-up; no datagen or new gameplay tests apply. Rebuild provenance, recheck source/target identity, remove the approved obsolete cache copies, deploy and verify matching hashes/current-test without launching Minecraft.
