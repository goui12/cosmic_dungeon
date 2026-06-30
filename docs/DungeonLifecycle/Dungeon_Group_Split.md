# Dungeon Group Split

Dungeon Group Split pays Attunement Fragment currency when dungeon mobs die. For currency denominations and account behavior, see [Economy & Currency](../Economy/Economy_and_Currency.md). For AFK status that suppresses payout eligibility, see [Dungeon AFK Handling](./Dungeon_AFK_Handling.md).

## Trace payout source

When a hostile monster spawned by a [Cosmic Mob Spawner](../Spawners/Spawner_Systems.md) dies in an active dungeon run dimension, the server builds a Trace pool from the mob's max HP expressed as hearts: `floor(max_hp / 2)`. A 40 HP mob therefore creates a 20 Trace pool. Player-summoned Metalmancer golems, passive animals, players, and any other non-Cosmic-Spawner deaths do not create Group Split pools. Cosmic Mob Spawners mark their spawned mobs with a server-authored `cosmic_spawner_<x>_<y>_<z>` tag before spawning, including base-entity-only spawners that have no optional preset, boss one-shot flag, or mob cap.

## Eligible dungeoneers

The split is resolved server-side at death time from the active dungeon run that owns the killed mob's dimension. A run member is eligible only when all of these checks pass:

- The member is online, not removed, and not a spectator.
- The member is in the exact same world/dimension as the killed mob; players in the lobby, overworld, or another dungeon world do not count.
- The member is within 100 blocks linear distance of the killed mob.
- The member is not marked AFK by the dungeon AFK flag described in [Dungeon AFK Handling](./Dungeon_AFK_Handling.md).

Only eligible run members are counted in the divisor and only eligible run members receive Trace. For example, a 20 Trace pool with four eligible dungeoneers nearby pays 5 Trace each; if one of those dungeoneers is AFK, the pool is split three ways instead.

## Remainders, capacity, and small mobs

Group Split currently uses integer Trace. If a split does not divide evenly, the remainder stays unissued rather than being granted to a random or first player. If the per-player share would be 0 Trace, no message or balance change is emitted.

Payouts use the same [CurrencyService](../Economy/Economy_and_Currency.md) deposit path as other Trace grants. If a player lacks enough Trace capacity for their share, that player's deposit is skipped and they receive a capacity warning.

## Server/client and storage safety

Group Split is enforced entirely on the server from the NeoForge living-death event. Clients only receive the system message after the server has attempted the CurrencyService deposit, so client-side workarounds cannot opt into a split or bypass AFK, dimension, run-membership, or distance checks.

The feature does not add required saved fields to entity save data, block-entity save data, Cosmic Mob Spawner storage, rift/RD data, door/key data, class selector data, teleportation data, or access-policy records. Cosmic spawner tags are injected into runtime spawn data on the server before new mobs spawn, so updating a 1.5.0 server to 1.5.1 requires no migration for mob spawners, doors/keys, [rifts/RD](../Rifts/Rift_System_Guide.md), [class systems](../Classes/Class_Selector_System.md), teleportation/rifts, or region/access policies for this feature.
