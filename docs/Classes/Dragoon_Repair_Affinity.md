# Dragoon Repair Affinity

Repair Affinity is live as a player-to-player Dragoon repair interface.

## Live behavior

- A Dragoon starts with `/repair <player>`; the target uses `/repair accept <player>` or `/repair deny <player>`.
- Invites expire after 30 seconds.
- Both players must stay online, alive, non-spectators, in the same dimension, within 8 blocks, and in the interface.
- The customer owns the damaged item slot. The Dragoon can inspect requirements but cannot take or move the customer's item.
- The customer may offer an optional labor fee using account currency. No physical currency item slot is used.
- The Dragoon supplies the required repair material from inventory. Materials and currency are consumed/transferred only when final server validation succeeds.
- Repair amount uses 25% max-durability units. This reconciles older 1-3 severity notes with a 1-4 unit model so a fully damaged item can be fully repaired.

## Supported repairs

- Leather armor: Leather Patch.
- Gold armor/tools/weapons: Gold Ingot.
- Chainmail armor: Chain Link.
- Iron armor/tools/weapons: Iron Ingot.
- Diamond armor/tools/weapons: Diamond.
- Netherite armor/tools/weapons: Netherite Repair Fragment.
- Copper gear is not tagged or registered by this source version, so it is not silently added.

Unsupported special items, including bows, crossbows, tridents, elytra, fishing rods, and unlisted modded items, show a friendly unsupported message.

## Not part of this system

- Direct Elias shop repair service fallback is not implemented here.
- D2 Deadeye exceptions are not implemented here.
- Repair session state is in memory only and does not change world saved-data formats.
