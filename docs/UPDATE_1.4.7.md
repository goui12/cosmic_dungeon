# Cosmic Dungeon 1.4.7 Update

## Overview
Version **1.4.7** focuses on RF redstone reliability, region tooling improvements, first-pass basic mob integration, and a large dungeon-building block content drop.

## RF TX/RX system updates
- RF receiver output behavior was improved so receivers now correctly power adjacent blocks based on attachment direction.
- RF transmitter/receiver configuration flow remains frequency-based, but the redstone response is now more consistent for automation builds.
- RF blocks are available in the **Dungeon Items** creative tab.

## Region updates
- Region creation now supports copying flags from another region during create:
  - `/region new <name> copy <sourceRegion>`
  - `/region create <name> copy <sourceRegion>`
- This copies flag settings from the source region while still using your current wand selection for the new region bounds.

## Basic mob added (behavior bypassed for now)
- The **Cthonian Gnawling** has been added as a basic mob/entity integration target.
- Its behavior/AI and animation behavior are currently bypassed temporarily for stability and crash isolation, so it is intentionally non-behavioral in this patch.

## New blocks
- **255** new dungeon building blocks were added.
- You can find them in the **Dungeon Building** creative mode tab.

## Creative mode tab locations (quick reference)
- **Dungeon Items tab**: RF Transmitter and RF Receiver.
- **Dungeon Building tab**: the new set of 255 building blocks.

## Commands (new or changed)
### New/changed
- Added region-copy variants:
  - `/region new <name> copy <sourceRegion>`
  - `/region create <name> copy <sourceRegion>`

### Other command changes
- No other brand-new command families were introduced as part of this 1.4.7 update.
