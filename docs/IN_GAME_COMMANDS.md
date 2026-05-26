# Deprecated: In-Game Commands (Moved)

This page has been replaced for the 1.4.8 developer documentation overhaul.

Use the new command reference:
- [Commands / In_Game_Commands](./commands/In_Game_Commands.md)

For command subsets embedded by system area:
- [Region Protection Guide](./regions/Region_Protection_Guide.md)
- [Spawner Systems](./Spawners/Spawner_Systems.md)
- [Redstone RF Signal Pipeline](./RedstoneRF/Redstone_Signal_Pipeline.md)
- [Dungeon Flow and Locks](./Progression/Dungeon_Flow_and_Locks.md)


## Trading Foundation (1.5.0)
- Added direct player-to-player trading foundation with item offers plus Attunement Fragment (Trace) balance transfers via CurrencyService.
- Added trade invite, accept, and cancel commands: `/trade <player>`, `/trade accept <player>`, `/trade cancel`.
- Added server-authoritative double-confirm flow with ready + confirm states and cancellation item return behavior.
- Added trade network payloads via ModNetwork for request/accept/currency/ready/confirm/cancel.
