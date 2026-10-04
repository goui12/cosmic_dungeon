# Mercenary container copies — October 4

Cameron explicitly requested that mercenaries obtain copies of useful container contents so players retain the originals, and never collect door keys. Branch: feature/mercenary-copy-loot-20261004; dependent on a28b59e1 / feature/tamsin-input-mercenary-crash-20261004 (PR211). One new scoped task/PR; no merge.

Scope: MercenaryCollection, MercenaryInventory, MercenaryEquipment, MercenaryEntity, new MercenaryLootMemory, MercenaryEntryTest, new MercenaryCopyLootTest, this task card, a verification report and one release fragment. This task exclusively owns mercenary inventory acquisition and optional entity NBT serialization for its copy receipts. No other registry, transaction, network or spawner hotspot is changed.

Container and starter equipment acquisition copies authored ItemStacks without removing or rewriting source stacks. Use existing ownership/class/chest permissions, native equipment upgrade rules and native brewing recipes. Reject door keys/lock-bearing stacks, unusable ammunition, unrelated items and gear downgrades. Ground pickups still consume real useful items. Preserve old equipment in supplies on upgrade.

Persist bounded per-mercenary receipts keyed by dimension, block position and slot so visits, consumption, death/rest and reload do not refill a copied slot. Old entities keep existing equipment/supplies and start with empty receipts. Invalid new receipt data blocks further container copies. No placed spawner, door, preset or packet migration; no authored content changes.

Validation: Java21 build/native JUnit, all src JSON and diff checks; source preservation/player extraction, class/ownership/key filters, full inventories, independent mercenaries, repeat visits, native upgrade decisions, starter copies, unopened loot tables, extraction vetoes, receipt compatibility/round trips/capacity. Existing starter tests now reflect the explicitly requested useful-only behavior; ownership checks retained. No clean, datagen or local server/GameTest launch under the existing workflow boundaries.

Pending licensed QA: enter with mercenary, verify starter gear, approach a barrel then loot its unchanged contents/key; confirm useful upgrades/ammunition work; revisit after consumption and server reload without duplicate copies; check friendly-fire protection remains. Deployment requires a fresh stopped-TEST confirmation and independently closed client. Active gameplay is not closed or duplicated.

See [verification report](../MERCENARY_COPY_LOOT_20261004.md). Zero numbered D1 implementation batches remain.
