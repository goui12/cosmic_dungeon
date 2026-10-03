# Class skill balance draft — 2026-10-03

Cameron delegated drafting and implementation without another approval. These are initial
tunable balance choices, not values claimed to be written in lore. Runtime implementation
is batch 7. No weapon access, authored loadout or class unlock changes are implied.

## Shared progression

- Per player UUID, class ID and skill ID. New skills start at level 0; default cap 50.
- A direct qualifying weapon kill grants 10 XP. Environmental and companion finishing
  damage grants kill statistics but no experience to the weapon merely being held.
- XP to advance from level L to L+1: 250 + 50*L. Total to level L:
  250*L + 25*L*(L-1). Level 10: 4,750 XP / 475 kills;
  level 25: 21,250 XP / 2,125 kills; level 50: 73,750 XP / 7,375 kills.
- Player/companion/NPC targets are excluded from combat training. Track the actual attack
  weapon and class on projectiles; changing held items must not relabel the killing weapon.
- Effective potion healing or a newly applied/improved useful buff grants 2 XP per use.
  No XP for throwing at nothing, overhealing or repeatedly replacing an equal active buff.
  One potion action grants at most 10 XP, including targets and any kills; configurable
  support-credit interval defaults to 3 seconds per recipient/effect.
- Progress survives death, forfeit and switching away/back. No retrospective XP from old
  aggregate kill totals, because they lack weapon identity.
- Values below are maximum incremental bonuses at level 50; scale linearly by L/50.
  Damage/effect multipliers apply once, after existing class calculations, without double
  counting a projectile and its payload. Keep rounding and caps explicit.

## Weapon and skill mapping

| Class | Documented combat equipment / skill | Bonus at level 50 |
| --- | --- | --- |
| Bogatyr | Sword | +25% sword damage |
| Bogatyr | Bow | +20% direct bow damage |
| Dragoon | Trident, melee and thrown share one skill | +20% trident damage; +5 percentage points chain-lightning chance (3% default becomes 8%; bounded at 100%) |
| Judicator | Mace | +25% mace damage |
| Judicator | Bow and its support arrows | +20% direct bow damage; +25% supported healing amounts |
| Pyroclast | Sword | +25% sword damage |
| Pyroclast | Bow | +20% direct bow damage |
| Pyroclast | Crossbow / firework launcher | +15% direct bolt or rocket damage; no radius or target-count increase |
| Theurgist | Mace | +25% mace damage |
| Theurgist | Bow and its support arrows | +20% direct bow damage; +25% supported healing amounts |
| Theurgist | Potions | +25% instant healing/damage and +25% timed beneficial/harmful effect duration; no automatic whole amplifier-tier jump |
| Venefex | Sword | +25% sword damage |
| Venefex | Bow and class arrows | +15% direct damage; +25% class-arrow debuff duration |
| Deadeye | Sword (when class is available) | +25% sword damage |
| Deadeye | Bow (when class is available) | +20% direct bow damage; 10% shorter draw time |
| Metalmancer | Resonance magnet direct ore/projectile attacks (when class/mechanic is available) | +20% direct attack damage |

Shields, bags, banners, repair anvils and the Theurgist utility pick do not gain a kill-based
weapon skill merely because they appear in a loadout. Metalmancer staff/file/golem utility
and later Theurgist rod mechanics remain documented future mechanics; this table does not
silently implement them or convert pet kills into staff weapon kills. Existing class/dungeon
availability remains authoritative. Judicator's documented melee weapon is a mace, not a sword.

## Offline source coverage

Read the local mirrored Items and Armor document bodies for all eight classes.
No live Google freshness claim; these are the explicitly requested offline sources.
The Theurgist document's deferred D2/D3 notes are not authority to unlock future mechanics.

| Class | Google document ID |
| --- | --- |
| Bogatyr | 1EBc7RDMA5Sm8TQ1uEG4kkPeiFRHBwOygAW_WLtGiUjg |
| Dragoon | 1uG80jIWpLKZvTGCmbHStqJs565iqEvqhr6N5oYBIHOE |
| Judicator | 1cY_czWEYbUEg_EQmaSANhOFe326gTDVKfL9XaEFGmQo |
| Pyroclast | 16FD3wxi-Uen_DRzItDHdSrSZvkGNwa_r-ZeUYiswxoE |
| Theurgist | 1l9ox2pQUSPy0_J3h7ljPOaVOFtMFkoHq_rFSGK4iqeM |
| Venefex | 1JXqPdwWxateRMGpAuoV1ub8asNL7iMeyrBtzqTuUwm8 |
| Deadeye | 1Y1T-L7qRv3GWr11fcq9vuq_rO6yORnbqc1WmYplGVWg |
| Metalmancer | 16Eq0yp7NTI57AK22s8vXrkoRYFp6BYrcdNjfbehWrcw |

Authoritative names/components/quantities in placed class chests are unchanged.
