# Theurgist Help Guide

## Class ID

- `theurgist`

## Availability

- Selectable class ID exists.
- Current selector availability: not separately documented as disabled.

## Overview

Theurgist has exclusive access to brewing stands. Brewing stands used by Theurgists complete valid potion brews instantly instead of waiting through the vanilla brew timer. Other class-specific design details are currently TBD.

## Inventory potion crafting (1.7.10 Alpha)

Inside an active dungeon, open your inventory and click **Craft...**. Theurgists see beneficial splash recipes. Venefex inventories use the same controls for harmful recipes. Both classes can craft drinkable Turtle Master; neither can craft its splash version.

Choose Normal, Strong or Extended variants. A green tile counts crafts affordable from your own materials. A blue count uses consenting online teammates; a light-red tile with red0 is unavailable. Hover for the native potion effects and whether own or shared materials are needed. Counts are alternatives, not a promise that all recipes can be bought simultaneously.

Each click consumes one complete vanilla recipe and creates one potion on your cursor. Click the same recipe again to stack identical crafted potions up to64. A different/full cursor spends nothing. Shift-click makes one potion directly into inventory, preferring the hotbar. No potion hotkeys are used.

The fuel graphic left of the palette shows remaining charges: one blaze powder supplies20 brewing steps. Already-brewed intermediate potions skip completed steps. Strength needs separate powder for its ingredient and fuel when the gauge is empty. Unused fuel survives saves and reconnects.

Own ingredients take priority. If group materials are needed, only consenting online group members with available inventories contribute; the least-recent donors are preferred and notified with exact item quantities. Sharing metrics track use and contribution. Full destinations, changed consent or missing materials cancel the purchase without spending.

Requests, random-potion skills and Brewing Supplies are retired. Theurgist and Venefex have no separate Skills window. All potions remain tradable; the Theurgist's **Resurrect...** button sits below Craft.

## Resurrection (1.7.9 Alpha)

Open your inventory and choose **Resurrect...**, then select a dead group member. The teammate has 60 seconds to accept or decline on their death screen. A successful accepted resurrection returns them to their latest death position with the existing five-second protection.

Player resurrection is available immediately, costs no items or Brewing Supplies, and has a three-minute cooldown after success. Declining, expiry and unsuccessful attempts do not start it. The cooldown survives deaths, reconnects and run cleanup; server downtime pauses game-time cooldowns. Resurrection preserves the existing death-loot rules and does not recreate missing physical drops.

The party hover tooltip and death screen show each Theurgist's cooldown or why they are unavailable. Mercenary Theurgists retain their existing level10 positive-potion unlock and cooldown rules. The full responsive H guide is planned in Batch19.

## Abilities

- Use brewing stands exclusively; valid potion brews complete instantly once brewing starts.
- Eon Penrose sells Theurgist-only brewing equipment, Healing II, Regeneration, and Lingering Healing offers, plus shared Theurgist/Judicator healing offers.
- Other abilities: TBD.

## Benefits

- Access to Theurgist-attuned equipment, when such equipment is authored.
- Access to Theurgist class-locked chests.
- Access to brewing stand interaction and instant potion brewing.
- Theurgist-specific item assets exist in the current content set.

## Incentives

- Choose this class when brewing stand access is required.
- Other incentives: TBD.

## Team roles

- Brewing/support role, based on exclusive brewing stand access and instant potion brewing.
- Additional team role details: TBD.

## Class restrictions and access

- Can use/wear equipment attuned to `theurgist`.
- Can open Theurgist class-locked chests.
- Only Theurgists can use brewing stands; brewing stands instantly finish valid potion brews.
- Other class-specific restrictions: TBD.

## Known gaps / TBD

- Define full ability list.
- Define final intended party role.
- Define full player-facing incentives.
- Define any class-specific commands, resource systems, or progression hooks.
