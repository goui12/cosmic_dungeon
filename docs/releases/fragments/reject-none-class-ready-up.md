# Class Selector ready eligibility

- Kept `None` as the no-class/reset selection while preventing it, invalid class IDs, and disabled classes from readying a Dungeon 1 player.
- Selecting `None` now removes any existing ready state and cancels the countdown when the party is no longer full.
- Added a Dungeon 1 startup safeguard so the no-class value can never request a `_none.schem` schematic; unoccupied logical slots continue to use `blankslot`.
