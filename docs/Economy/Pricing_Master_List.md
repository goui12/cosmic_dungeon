# Pricing Master List Notes

## Current implemented pricing sources

- Currency balances are stored as total Trace and displayed through Trace denominations.
- Vendor buy offers are defined in vendor profile JSON.
- Vendor buyback values can come from class-attuned item trace metadata and profile buyback rules.
- Player-to-player trading uses the same account balance but does not enforce vendor price tables.

## Design/future pricing notes

The pricing master list is a balancing reference for future vendor stock, buyback, faction pricing, and repair-cost work. Values in this document are not live unless encoded in vendor profile data, item metadata, or server-side pricing code. Keep this as a developer/world-designer reference and do not dump the full list into the player H help menu.

## Related docs

- [Economy and Currency](Economy_and_Currency.md)
- [Vendor](../Vendor.md)
- [Elias Centvin](../Vendors/Elias_Centvin.md)
