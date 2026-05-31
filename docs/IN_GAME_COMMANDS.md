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
- Added normal-player trade commands: `/trade <player>`, `/trade accept <player>`, `/trade deny <player>`, `/trade cancel`.
- Added pending invite metadata with 30-second expiration, 3-second requester cooldown, clickable accept/deny chat buttons, active-trade validation, logout/menu-close cleanup, safe item returns, and read-only other-player offer slots.
- Added server-authoritative double-confirm flow with ready + confirm states and hardened atomic currency transfer rollback.
- Added trade network payloads via ModNetwork for request/accept/currency/ready/confirm/cancel, server-to-client `S2C_TradeState`, and the CAPS LOCK look-target trade request hotkey payload. The hotkey uses the existing Cosmic Dungeon Controls category, does nothing while screens are open, and shares the same invite path/cooldown as `/trade <player>` after server range/dimension/look validation. `S2C_TradeState` is view-specific and keeps trade screens synced with self/other names, Trace balances, Trace offers, ready/confirm status, and status messages while item offers remain menu-slot synchronized.
