# Repair owner-local custody

Active repair targets, native cursor items and exact provider component lots now share their
owner's vanilla player save. Repair Custody is an additive repair_custody_v1 entry under the
existing CosmicDungeon player root; old saves without it need no conversion. The native
Entity.saveWithoutId HEAD hook captures live menu state before NeoForgeData and inventory.
Normal cancellation queues items without world drops; original inventory scope is preserved.
Death uses existing root cloning and protected-return storage; /repair claim also handles returns.

Legacy cosmicdungeon_repair_recovery_v1 records remain preserved and strictly decoded. They
lack paired player receipts, so automatic delivery is held for complete save/backup review.
No registry/packet changes, generated resources, new textures or active-world migration.
Single writer: repair services/menu/events, protected-return helper, player-save proof and mixin.

Validation:1682 offline checks plus2config round trips; Java21 offline build. New65 checks cover
ownership, exact native NBT/cursor/component images, bad/future schemas and player-file readback.
Manual cumulative QA remains: open-menu save/restart, close/death/logout/dimension/full inventory,
exact components and equipped data, dedicated/integrated owner saves, licensed multiplayer.

M24/M25 are still partial: Batch22 must coordinate the participant images with the account
commit decision. Back up full player/world/config files before eventual upgrade or rollback;
older jars do not understand custody entries. No actual deployment or game launch occurred.
Next improvement: durable item/payment decisions and one-time participant acknowledgement.
