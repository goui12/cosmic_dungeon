# D1 Batch30: reviewed legacy Chop recovery

- Add developer-only online-owner Chop inspection and expiring preview/apply.
- Preserve one reviewed item's quantity/components and self-owned token through the existing
  verified Chop save journal; retain exact original item/ownership evidence in its receipt.
- Keep duplicates, foreign/partial ownership, retained runs and orphan custody intact.
- Block conflicting purchases/automatic adoption while stored belongings remain unresolved.
- Guard item-adoption preview/apply/undo against pending recovery, cursor and session/run changes.
- Preserve old save IDs and spawner/preset formats; optional review metadata needs no bulk migration.
- Keep legacy physical-currency conversion and complex orphan mappings as detailed code TODOs.

8,457 offline checks, two configuration round trips and Java21 build passed.
No new PNG or datagen output; licensed native/multiplayer acceptance remains pending.
No deployment, active world/config edit or push. Back up the complete TEST save before adoption.
