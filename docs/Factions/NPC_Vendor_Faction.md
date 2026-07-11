# NPC and Vendor Faction Design Notes

## Current implemented behavior

Vendor profiles can require progression flags, NPC tiers, system tiers, village access, and faction tier requirements at the profile/access level. Buy offers can also use `requiredFactionTier`; this is evaluated against the profile-level faction id and blocks both GUI purchasing and manipulated purchase packets when unmet. Vendor buyback pricing uses encoded item trace values and optional profile buyback rules.

## Not currently live

The following are design/future unless source support is added later:

- NPC faction reputation altering prices globally.
- Class-restricted vendor offers beyond current profile/offer access fields.
- Vendor faction stock reshuffling by faction mood.
- Offer-level faction pricing multipliers not represented in current vendor code.

## NPC/vendor faction design ranges

The NPC/vendor faction design note remains planned design unless a future prompt creates a dedicated active NPC/vendor faction implementation: starting value `0`, Hostile `-100` to `4`, Cordial `5` to `49` with list price plus 10%, Warmly `50` to `99` at list price, and Ally `100` cap with list price minus 10%. Lesser Blooms granting faction 1:1 and NPC kills reducing faction at 26:1 are also design-only in current code.

## Documentation rule

Player help may mention only current access requirements and visible vendor behavior. Operator docs may describe planned faction/vendor pricing concepts as future design notes.
