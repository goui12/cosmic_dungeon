# Economy & Currency Guide — 1.5

Attunement Fragment currency is an account-based player balance system. World-item tender exists for pickup and drops, but normal spending uses the server-side account balance.

## Denominations

| Denomination | Trace value |
| --- | ---: |
| Trace | 1 |
| Mark | 10 |
| Seal | 100 |
| Crown | 1,000 |
| Anchor | 10,000 |

Balances are stored internally as total Trace. The display can normalize that total into larger denominations for player readability.

## Player behavior

- Picking up Attunement tender items deposits the full stack into the player's balance.
- Pickup is all-or-nothing: if the full stack would exceed capacity, the stack remains in the world.
- Players can check their balance and inspect item/inventory sell value with currency commands.
- Vendor purchases, vendor buyback, and player trades all operate on the same account balance.

## Operator behavior

Use [Commands: Currency](../commands/In_Game_Commands.md#currency-attunement-fragment-economy-foundation) for balance, mutation, capacity, and value-inspection commands. Self-balance/value checks are player-safe; other-player reads and mutations require developer or console authority.

## Related topics

- [Vendor](../Vendor.md) explains how shops withdraw or deposit currency.
- [Trading Guide](../Trading/Trading_Guide.md) explains direct player-to-player currency offers.
- [Class Restrictions & Inventory](../Classes/Class_Restrictions_and_Inventory.md) explains class-attuned item sell values.

## Changelog

- **1.5:** Added account balances, tender-item pickup, denominations, capacity checks, value commands, vendor integration, and trade integration.

## Dungeon Group Split payouts

[Dungeon Group Split](../DungeonLifecycle/Dungeon_Group_Split.md) adds Trace to eligible dungeoneers when dungeon mobs die. The payout pool is based on the mob max-HP heart value and is divided by eligible nearby, same-world, non-AFK dungeoneers through the same account deposit path as other currency grants.
