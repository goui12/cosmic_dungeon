# D1 next readiness passes, 2026-09-23

These are three proposed implementation passes followed by cumulative licensed TEST.
Cameron's current request authorizes texture import, attribute verification and planning;
none of the three implementations below was started during that pass.
[Current texture checkpoint](D1_TEXTURE_IMPORT_2026-09-23.md).

## 1. R01: repair-service compatibility

Current problem: RepairComponents.key/validFor requires REPAIR_COMPONENT even for ordinary approved vanilla materials. Cameron's authored, unmarked supplies therefore fail repair-service validation.

Change material recognition in quotation, reservation and final commit so approved vanilla supply IDs can be consumed as-is. Preserve the same counts, eligibility, ownership, price, timed session, return and rollback rules. Do not mutate or mark stacks on chest opening, pickup, startup or migration. Reject ambiguous/malformed special components consistently across preview and commit.

The distinct weapon-shaped kits need an explicit recognition rule: an ordinary bow, crossbow, trident or mace must not be silently consumed as a kit. Reuse existing marked-kit compatibility. If unmarked named kits must also work, decide their exact source-backed identity before accepting them. The ordinary-material correction need not wait for changing chest content.

Relevant surfaces: RepairComponents, DragoonRepairRules, repair quote/session/transaction services and existing repair regression checks.
Validation: unmarked materials, existing marked materials, damaged/enchanted supplies, ordinary weapons versus kits, insufficient inventory, cancellation, disconnect and exact-once consumption/return.

## 2. R04: server-enforced approved crafting

Current problem: there is no general approved-recipe policy. Existing guards cover repair-component misuse and some ordinary repair paths; hiding recipe-book entries does not enforce crafting restrictions.

Build an explicit recipe-ID allowlist from retained Debloat-linked rules. Put configurable policy under a Crafting section in CosmicDungeon.config. Resolve recipes by ID/type and revalidate on recipe reload; reject unknown/unapproved recipes before input consumption. Keep any developer override tied to the established access system.

Cover 2x2 inventory, crafting table and Crafter paths. Define automation separately because a Crafter has no player class. Keep approved brewing, Dragoon repair and Pyroclast transmutation as their existing class services. Review smelting/smithing and other conversion routes against the economy rules rather than assuming a crafting-grid gate covers them.

Before implementation, record the exact approved recipe IDs, class gates, automation policy and any builder exception. Derive the initial list from retained sources; only unresolved exceptions need Cameron's choice.
Validation: manual/recipe-book/shift crafting, full inventories, server reload, changed data packs, automation, bucket/bottle remainders and value-increasing conversion loops.

## 3. R05: shared Trace balance displays

Current problem: trade/vendor/repair menus have balance views, but dedicated HUD, ordinary inventory and class-chest panels are missing.

Extract a shared read-only denomination renderer from TradeScreen using the five existing Anchor/Crown/Seal/Mark/Trace item icons and the existing formatter. Add a compact HUD and inventory/class-chest side panels with GUI-scale/overlap handling. No new PNG is required.

Read the existing UUID account only. Extend established synchronization for an initial snapshot and bounded changes; no separate money store, per-frame packets or click-to-credit account balance. Clear transient state on logout/dimension/session changes. Keep requested chest shift-click behavior and every authored stack unchanged.

Physical chest-item transfer and account credit are separate. Any new treasure-credit feature needs a trusted unpaid reward identity and durable single-use claim. Existing denomination stacks may already have been credited; do not auto-redeem them or add the displayed personal balance again. This decision need not block read-only balance panels.

Relevant surfaces: TradeScreen denomination renderer, MenuBalanceRefresh, client GUI hooks and established account/network synchronization.
Validation: GUI scales/window sizes, rewards/purchases while panels remain open, reconnect/death/instance reset, large balances, no duplicate credit, no stale account data and bounded packet traffic.

## Cumulative acceptance after these passes

R08 includes imported ammunition rendering and combat, repair flows, every recipe entry path,
balance synchronization, world bindings, global NPC replacement and simultaneous Watson instances.
Use the licensed TEST environment with matching jars and a consistent backup. No launch/deploy
is authorized by this plan.

Status: 3 implementation areas proposed; 1 cumulative TEST area; 0 newly scheduled numbered batches.
The broad101 audit dispositions remain45 implemented-unverified /20 partial D1 /
1 preserved-verification-pending /8 author-owned outside AI work /27 deferred D2+.
