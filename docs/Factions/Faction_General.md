# Faction General Notes

## Current implemented behavior

Cosmic Dungeon currently has server-side player faction data, tier definitions, and faction command support. The implemented source defines faction progression concepts and the active JHW faction path used by progression/vendor access checks.

## Player-facing principle

Factions should be documented as relationships and unlock tracks that may affect access to content when a system explicitly checks them. Do not assume every NPC, vendor price, teleport service, or class interaction changes with faction unless that support exists in source or profile data.

## Design-note integration

Faction design notes should inform concise help-menu copy only after the behavior is implemented. Markdown may track future faction tiers, NPC reactions, vendor disposition, and faction-specific services as design direction, but those sections must remain clearly marked as design/future.

## Related docs

- [Progression, Factions and Unlocks](../Progression/Progression_Factions_and_Unlocks.md)
- [NPC/Vendor Faction Notes](NPC_Vendor_Faction.md)
