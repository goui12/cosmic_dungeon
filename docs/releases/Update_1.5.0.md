# Update 1.5.0 Notes (Initial Implementation)

## Attunement Fragment economy account foundation

This is the first 1.5.0 implementation pass and introduces backend account plumbing only.
No vendors, purchase GUI, or gameplay sinks/sources are included in this update.

### Currency model

Legal-tender denominations (non-vanilla currency):

- TRACE
- MARK
- SEAL
- CROWN
- ANCHOR

Exchange ratio is decimal by tier:

- 10 Trace = 1 Mark
- 10 Mark = 1 Seal
- 10 Seal = 1 Crown
- 10 Crown = 1 Anchor

All balances are stored internally as total `Trace` using `long`.

### Persistence and account data

- Added persistent player economy data via overworld `SavedData`.
- Balance data persists across server restarts.
- Per-player capacity override support is included.
- Default account capacity is currently `10,000 Trace`.

### Commands

Added `/currency` root command:

- `/currency balance`
- `/currency balance <player>`
- `/currency add <player> <denomination> <amount>`
- `/currency remove <player> <denomination> <amount>`
- `/currency set <player> <denomination> <amount>`
- `/currency clear <player>`
- `/currency capacity <player> <traceAmount>`
- `/currency value`
- `/currency value inventory`

Permission model:

- Any player can run self-balance.
- Other-player balance checks and all admin mutations require developer/console authority.

### Scope boundary

This update intentionally does **not** include:

- vendors
- economy GUI/menu screens
- world sinks/sources beyond pickup auto-storage

These are planned for future 1.5.x follow-up work.


## 1.5.0 Follow-up: Attunement fragment item tender + auto-storage

- Added five legal-tender item forms: `attunement_trace`, `attunement_mark`, `attunement_seal`, `attunement_crown`, and `attunement_anchor`.
- These items are drop-compatible as world entities but auto-deposit into player currency balance on successful pickup.
- Pickup uses all-or-nothing capacity checks: if full capacity blocks the full stack value, no partial deposit occurs and the entity remains in-world.
- Capacity-denied pickup sends a short, rate-limited actionbar denial message.
- No crafting recipes were added for these tender items.


## 1.5.0 Follow-up: Vendor pricing foundation (no vendors yet)

- Added a new pricing package: `net.goui.cosmicdungeon.economy.pricing`.
- Added foundational types: `VendorValueCategory`, `VendorPrice`, and `VendorPricingService`.
- Pricing now supports class-attuned items as the source of truth for sell value.
- The previous tiny hardcoded D2 T1 Judicator chainmail seed is no longer used. Complete-set detection now uses attuned armor metadata instead: a helmet, chestpiece, pants/leggings, and boots with the same class, dungeon, and tier form a sellable class armor set.
- Any class-attuned item, including a single armor piece, can still sell individually for its stored Trace value. Full armor-set payout is 125% of the four matching armor pieces' combined stored Trace values, so four 100-Trace pieces sell as a 500-Trace set.
- Added `/currency value` to report the held item sell value/debug source.
- Added `/currency value inventory` to report detected complete sets and notable inventory values.
- Scope boundary remains unchanged: no vendor NPCs, no vendor GUI, no item removal in pricing evaluation.



## 1.5.0 Follow-up: Class item attunement foundation
- Added developer command `/classitem attune <class_name> <dungeon_number> <tier_number> <trace_value>` for turning an existing held vanilla/customized item into CosmicDungeon class gear.
- Added `/classitem clear` to remove only CosmicDungeon class-item metadata from the held item.
- Attunement uses persistent NeoForge data components: `class_attunement`, `class_item_dungeon`, `class_item_tier`, and `class_item_trace_value`.
- Class ids are accepted case-insensitively from playable `ClassKeys`, excluding `none`, and stored as canonical lowercase values. Dungeon accepts both `d1` and `1`; tier is limited to 1-10; Trace is zero or greater.
- Added a dynamic client tooltip line that shows only the attuned class display name at the bottom, bold/italic, in the configured class color. The tooltip is not lore and normal items remain unchanged.
- Updated vendor pricing so valid attuned items return their stored Trace value with debug sources like `class_attuned:judicator:d1:t4`; zero-valued attuned items report `class_attuned_zero:...`. Matching attuned armor pieces can also be sold together through full-set detection for the 125% set payout.

## 1.5.0 Follow-up: Faction foundation (JHW baseline only)

- Added faction foundation package: `net.goui.cosmicdungeon.faction`.
- Added `FactionTier` ladder: HOSTILE, SUSPICIOUS, INDIFFERENT, CORDIAL, FAVORABLE, WARMLY, ALLY.
- Added faction definition registry with `cosmicdungeon:jhw` baseline:
  - scale: 0-500
  - default starting value: 80 (SUSPICIOUS)
  - persisted per-player in overworld `SavedData`
- Added service API for read/set/adjust/tier checks with min/max clamping.
- Added `/faction` command root with get/set/add/list subcommands.
- Permission model keeps AccessPolicy as central authority:
  - self-read is public
  - reading or mutating other players requires developer/console authority

Scope boundary: this pass does not add vendors, NPC interactions, achievement triggers, atonement dungeons, or hostile behavior hooks.


## 1.5.0 Follow-up: Long-term progression foundation (D1/D2 unlock data only)

- Added persistent long-term progression package: `net.goui.cosmicdungeon.progression`.
- Added `PlayerProgressionData` SavedData keyed by player UUID with:
  - D1 Lesser Bloom best count (0-6), with legacy Torch Flower save keys migrated on load
  - D1 completion flag for >=3 Lesser Blooms
  - Lesser Blooms total
  - Cavern Residue total
  - Village access unlock flag
  - D1 NPC unlock tier (0-4)
  - D2 NPC unlock tier (0-4, reserved for future hooks)
  - generic string progression flags set for future expansion unlocks
- Added `ProgressionService` API for reads/writes and derived calculations.
- D1 NPC unlock tiers now derive from Lesser Blooms:
  - 0-4 => tier 0
  - 5-9 => tier 1
  - 10-14 => tier 2
  - 15-19 => tier 3
  - 20+ => tier 4
- Village access unlocks once D1 is completed with at least 3 Lesser Blooms.
- Added `/progression` command root for debug/admin mutation workflows.
- Permission model keeps AccessPolicy as central authority:
  - self-read allowed
  - reading others or mutating progression requires developer/console

Scope boundary: this pass does not wire dungeon completion events, collection events, NPC vendor logic, or any gameplay trigger hooks yet.


## 1.5.0 Follow-up: Achievement + advancement foundation

- Added new package: `net.goui.cosmicdungeon.achievement` with:
  - `CosmicAchievementIds` for centralized advancement `ResourceLocation` ids.
  - `CosmicAdvancementUtil` safe server-side grant helper (`triggered` default criterion).
  - `AchievementCounterData` persisted per-player counters/bitmasks for incremental trigger implementation.
- Added developer/console-only `/achievement` command root:
  - `/achievement grant <player> <achievementId>`
  - `/achievement counters <player>`
  - `/achievement counters reset <player>`
- Added advancement datagen provider (`ModAdvancementProvider`) and registered NeoForge `AdvancementProvider` in `DataGenerators` server datagen path.
- Achievement advancements are now generated from datagen instead of manually maintained JSON files in `src/main/resources`.

Scope boundary: this pass provides framework + tooling only; gameplay/event triggers for these achievements are intentionally not wired yet.


## 1.5.0 Follow-up: D1 Plant Flags achievement foundation

- Added package `net.goui.cosmicdungeon.achievement.plantflags` with persisted state machine foundation for D1 Plant Flags.
- Added `PlantFlagData` SavedData including active run/session id, planted player UUID set, disconnect cooldown timestamp, completion flag, and configurable region bounds.
- Added `PlantFlagService` with APIs to record banner planting, clear per-run state, compute online eligible players, evaluate cooldown/eligibility, and complete once ready.
- Added event hooks for banner placement and disconnect handling:
  - Banner placements only count when the placed block is a banner and inside configured Plant Flags region.
  - Disconnects/link-dead events trigger a 5-minute cooldown lockout before completion can occur.
- Added `/plantflags` command root:
  - `/plantflags status` (public)
  - `/plantflags reset` (developer/console)
  - `/plantflags setregion pos1` and `/plantflags setregion pos2` (developer/console)
  - `/plantflags complete-debug` (developer/console)
- Completion behavior currently grants `cosmicdungeon:achievements/plant_flags` and broadcasts:
  - `"The planted banners stir. JHW answers."`
- Scope boundary preserved: no new JHW entity was introduced in this pass. A TODO marker is included where physical JHW summon/NPC interaction flow will later replace the placeholder broadcast.


## 1.5.0 Follow-up: Binding Idol achievement tracking hooks + debug commands

- Implemented `net.goui.cosmicdungeon.achievement.BindingIdolAchievements` as the central tracker for Binding Idol achievement counters and threshold grants.
- Added API hooks:
  - `recordReturnedThroughBindingIdol(ServerPlayer revivedPlayer)`
  - `recordProvidedBindingIdol(ServerPlayer provider, ServerPlayer receiver)`
- Both hooks update `AchievementCounterData` and grant threshold achievements for:
  - returns from death through a binding idol: 1/5/10/50/100
  - provides binding idol to a dungeoneer: 1/5/10/50/100
- Added developer/console debug commands under `/achievement`:
  - `/achievement idol return <player>`
  - `/achievement idol provide <provider> <receiver>`
- Scope note: no standalone Binding Idol gameplay system was introduced in this pass; only achievement API hooks and debug wiring were added for safe incremental integration.


## 1.5.0 Follow-up: Vital Exchange I-IV foundation

- Added `net.goui.cosmicdungeon.achievement.VitalExchangeAchievements` as the central API for Vital Exchange grants:
  - `recordVitalExchange(ServerPlayer provider, ServerPlayer receiver, ItemStack providedStack)`
- Trigger eligibility rules implemented:
  - provided stack quantity must be greater than 0
  - receiver must currently be `Deadeye` (via `ClassNbtUtil` + `ClassKeys`)
  - both provider and receiver receive the corresponding achievement grant
- Item-to-achievement mapping implemented:
  - `scintilla_vitalis` -> `vital_exchange_1`
  - `lux_vitalis` -> `vital_exchange_2`
  - `mending_sting` -> `vital_exchange_3`
  - `verdant_jolt` -> `vital_exchange_4`
- Current item registration status in `ModItems`:
  - `scintilla_vitalis` and `lux_vitalis` are already registered
  - `mending_sting` and `verdant_jolt` are not yet registered; this pass uses stable `ResourceLocation` TODO constants to avoid inventing item behavior
- Added developer/console debug command wiring under `/achievement`:
  - `/achievement vitalexchange <provider> <receiver> <item>`

Scope boundary: this pass does not add a full player trade HUD/system; it only adds achievement service + debug trigger wiring for safe incremental integration.

## D1 Environmental Achievement Trackers

Added isolated D1 tracker services under `net.goui.cosmicdungeon.achievement.d1`:
- `TiredNotBrokenTracker`
- `SixfoldVigilTracker`
- `CycleOfRecordedSoundTracker`
- `SynchronousPealTracker`

Added `/achievement d1regions` tooling:
- `/achievement d1regions status`
- `/achievement d1regions set <regionName> pos1`
- `/achievement d1regions set <regionName> pos2`

Added debug grant command:
- `/achievement d1 debug <achievementName> <player>`

Default region keys expected by the trackers:
- `d1_woodland_manor`
- `d1_wither_room`
- `d1_camp_5`


## 1.5.0 Follow-up: Vendor profile loading foundation (no NPC assignment, no GUI)

- Added package `net.goui.cosmicdungeon.vendor` with `VendorProfile`, `VendorOffer`, and `VendorProfileManager`.
- Vendor profiles now load from datapack JSON under: `data/cosmicdungeon/vendor_profiles/<path>.json`.
- Added JSON validation for profile IDs and item IDs, with clear log errors for invalid entries without hard-crashing the server.
- Added `/vendor` command root for developer/console diagnostics:
  - `/vendor list`
  - `/vendor reload`
  - `/vendor profile <profileId>`
- Added sample profile: `data/cosmicdungeon/vendor_profiles/d1/general_supply_vendor.json` with starter offers:
  - bread for 6 Trace
  - torch for 2 Trace
  - arrows for 3 Trace

Scope boundary remains explicit:
- no villager/NPC vendor assignment in this pass
- no vendor GUI/menu/screens in this pass

## 1.5.0 Follow-up: Vanilla villager vendor shell assignment + interaction lockout

- Added `VendorAssignmentService` to persistently attach a vendor profile id to vanilla villagers using entity persistent NBT (`cosmicdungeon.vendor_profile_id`).
- Added `/vendor` developer/console subcommands for shell lifecycle management:
  - `/vendor assign <profileId>`: assigns profile to looked-at villager (6-block range)
  - `/vendor clear`: clears assignment from looked-at villager
  - `/vendor info`: reports assignment on looked-at villager
  - `/vendor spawn <profileId>`: spawns villager at player location and assigns profile
- Assignment behavior now applies vendor shell setup:
  - custom name from `VendorProfile.displayName`
  - `setPersistenceRequired()`
  - `setNoAi(true)`
  - `setInvulnerable(true)`
- Added `VendorInteractionEvents` for right-click handling:
  - assigned vendor shells cancel vanilla villager trading interaction
  - normal (unassigned) villagers remain unchanged
  - right-click feedback now shows vendor name, profile id, player currency balance, and locked/unlocked status
- Added progression/faction gating checks for interaction status evaluation:
  - faction tier checks through `FactionService` + `FactionTier`
  - village access check through `ProgressionService.hasVillageAccess`
  - NPC tier checks through `ProgressionService` D1/D2 unlock tiers
- Included explicit TODO guard for unknown `requiredProgressionFlag` mappings instead of faking progression logic.

Scope boundary for this pass:
- no vendor GUI/menu opening yet
- no buy/sell execution yet
- this pass is assignment + interaction interception only

## Vendor Menu (1.5.0)
- Added first custom server-authoritative vendor purchase flow for assigned vendor villagers.
- Assigned vendor right-click now opens custom Vendor menu when unlocked; locked vendors return reason messages.
- Purchase requests are validated server-side (vendor assignment/profile/offer/unlock/currency/range/inventory space).
- Transactions are atomic: currency is never lost on failed delivery and item is never granted without deduction.
- Added dedicated vendor network payloads under ModNetwork registration and client vendor screen integration.


## Vendor Buyback (Added May 26, 2026)
- Added vendor buyback flow so assigned vendors can purchase eligible items and pay Attunement Fragment balance directly.
- Added sell actions in vendor UI: **Sell Held** (main-hand selected hotbar slot) and **Sell Set** (first detected complete eligible set).
- Added atomic server validation for sell requests: vendor assignment/range checks, buyback eligibility, positive value checks, and currency capacity checks before commit.
- Added C2S payloads for selling inventory slots and detected sets.
- Buyback pricing now reads valid class-item attunement metadata. Any class-attuned item, including armor, pays its stored Trace value when sold individually; class armor can also be bought as a complete four-piece set (helmet, chestpiece, pants/leggings, boots) when all pieces share the same class, dungeon, and tier, paying 125% of the four pieces' combined stored Trace values. The previous hardcoded D2 T1 Judicator seed is no longer the economy source.
- Added player feedback messages for success and common failures, including capacity/ineligible/incomplete set states.
## 1.5.0 Content Registry Update: Lesser Bloom and Cavern Residue

- Added `cosmicdungeon:lesser_bloom` as the canonical Dungeon 1 flower collectible. Torch Flower terminology was placeholder-only and Dungeon 1 progression, commands, vendor requirements, and save fields now use Lesser Bloom naming.
- Lesser Bloom behaves like a normal small flower, uses crossed flower rendering, can be placed on valid flower surfaces, supports flower pots via `cosmicdungeon:potted_lesser_bloom`, and appears under the Dungeon Items creative tab.
- Added `cosmicdungeon:cavern_residue` as a real mod item and placeable low-hardness block. Its placed block uses a separate Blockbench-style block model from the flat 16x16 inventory item model, appears under Dungeon Items, and drops the `cavern_residue` item when broken.
- Cavern Residue progression counters now have a real mod registry item/block to count against in progression, advancement, vendor, and container-counting integrations as those hooks are expanded.

## Vendor Access Gating Integration (Added May 26, 2026)
- Added `VendorAccessService` as the single evaluator for per-player vendor access outcomes and readable denial reasons.
- Extended `VendorProfile` schema with:
  - `requiredVillageAccess` (boolean)
  - `requiredNpcSystem` (string, e.g. `D1` / `D2`)
  - `requiredNpcTier` (int)
  - `requiredFaction` / `requiredFactionTier` (existing faction gates retained)
- Right-click vendor interaction now checks the centralized access evaluator before opening the menu.
- Added `/vendor access <profileId>` developer/console command to verify access state for the executing player.
- Added D1 sample vendor profiles and tiered unlock requirements:
  - `cosmicdungeon:d1/general_supply_vendor` -> D1 tier 1 (5 Lesser Blooms)
  - `cosmicdungeon:d1/weapon_supplier` -> D1 tier 2 (10 Lesser Blooms)
  - `cosmicdungeon:d1/brewing_store` -> D1 tier 3 (15 Lesser Blooms)
  - `cosmicdungeon:d1/save_teleport_npc` -> D1 tier 4 (20 Lesser Blooms)
- Village gate requirement now enforced for these D1 vendors, including D1 completion expectation with at least 3 Lesser Blooms.
- TODO:
  - Replace current string-based `requiredNpcSystem` with a typed enum/registry-backed id when D3+ systems are introduced.
  - Add localized translatable components for each denial reason and per-profile lock flavor text.


## 1.5.0 Follow-up: Vendor command aliases and chat colors
- Vendor commands that accept `<profileId>` now resolve exact full `ResourceLocation` IDs first, then short last-path aliases like `brewing_store` from `cosmicdungeon:d1/brewing_store`.
- Ambiguous short aliases now fail clearly and list the matching full IDs instead of choosing one implicitly.
- Vendor profile suggestions include clean short aliases before full IDs, while `/vendor list` presents short names first with full IDs as secondary technical detail.
- Vendor command responses and interaction/purchase/sale feedback now use consistent readable colors for success, errors, vendor names, prices/balances, and technical IDs.

## Trading Foundation (1.5.0)
- Added direct player-to-player trading foundation with item offers plus server-authoritative Attunement currency balance transfers via CurrencyService; persistent balances remain account data and are not converted into loose inventory items.
- Added normal-player trade commands: `/trade <player>`, `/trade accept <player>`, `/trade deny <player>`, and `/trade cancel`.
- Trade invites now store inviter/receiver metadata with game-time timestamps, expire after 30 seconds, and use a 3-second per-requester cooldown to reduce spam.
- Incoming invite chat now includes clickable green `[Accept Trade]` and red `[Deny Trade]` buttons that run the matching `/trade accept <player>` or `/trade deny <player>` fallback commands.
- Added validation to block self-trades, offline targets, dead/spectator participants, and new requests/accepts while either player is already in an active trade.
- Added disconnect/menu-close lifecycle cleanup so pending invites involving the player are removed, active trades are cancelled once, offered items are returned, and the remaining player is notified when applicable.
- Hardened trade finalization with preflight balance/currency-capacity/inventory-capacity validation, atomic currency rollback to original balances on failure, safe item returns on cancel/failure, and read-only other-player offer slots.
- Expanded the trade container to the real 256x256 `trade_window.png` layout: 9 read-only partner offer slots, 9 writable own offer slots, 27 player inventory slots, and 9 hotbar slots. Container indices are partner offer `0-8`, own offer `9-17`, player inventory `18-44`, and hotbar `45-53`.
- Own offer slots and local Attunement currency controls can only be edited while the player's offer is not ready/locked; item or currency changes made before acceptance reset both players' ready/confirm state, while accepted offers reject further edits until the player cancels the active trade. Shift-click routes only between the player's inventory/hotbar and their own offer slots.
- Replaced the trade screen's freeform Trace amount input with clickable fake `ItemStack` denomination controls rendered from `ModItems.ATTUNEMENT_ANCHOR`, `ATTUNEMENT_CROWN`, `ATTUNEMENT_SEAL`, `ATTUNEMENT_MARK`, and `ATTUNEMENT_TRACE`. Local and partner balances are normalized from synced Trace totals using `CurrencyDenomination` values and displayed highest-to-lowest as Anchor, Crown, Seal, Mark, Trace in vertical left-gutter columns that stay clear of the item offer slots; only the local column is interactive, with hover borders, left/right click add/remove, and Shift x10 behavior.
- Offered currency is now rendered in each offer area as fake normalized denomination `ItemStack` summaries with formatted tooltips; these summaries are laid out horizontally inside the offer-summary rows, are not container slots, cannot be picked up or shift-clicked, and do not interfere with real item offer slots.
- Reworked `TradeScreen` into the final PNG-driven trade GUI: it uses `cosmicdungeon:textures/gui/container/trade_window.png` as the full background through the screen's normal container render path, suppresses vanilla labels/background overlays, renders synced self/partner names and clean status text, adds the local player preview using the `ExtraInventoryScreen`/`InventoryScreen.renderEntityInInventoryFollowsMouse` pattern, and draws the PNG accept/deny icons directly with 1px/2px procedural hover borders on own-side controls only. Currency icons, button art, text, and hover borders render after the real menu slots so they remain visible over the PNG without becoming pickupable slots. The active-session accept icon keeps the server-authoritative two-phase flow (first click marks the current offer accepted/ready and locks local item/currency edits; after both players are ready, clicking the same icon confirms/finalizes), the active-session deny icon sends cancel, and the partner accept icon is a non-clickable synced status indicator with dim/off rendering until the partner accepts.
- Added trade network payloads via ModNetwork for request/accept/currency adjustment/legacy currency update/ready/confirm/cancel, the CAPS LOCK look-target trade request payload, and server-to-client `S2C_TradeState` sync. The trade GUI uses `C2S_AdjustCurrencyOffer` denomination-id + signed delta-count packets; the server validates the denomination, rejects currency edits after the sender has accepted, converts with `CurrencyDenomination`, clamps the offered total to `[0, player balance]`, resets ready/confirm state on legal pre-acceptance changes, and syncs both players. Trade state is view-specific per player and keeps the screen updated with self/other names, Trace balances, Trace offers, ready/confirm status, and status messages while item offers continue syncing through menu slots; typed `/trade <player>` remains the distance-free fallback, while the hotkey is server-validated for same-dimension online players within close range.
- Added the **Trade Request** keybind in the existing **Cosmic Dungeon** Controls category, defaulting to `CAPS LOCK`; pressing it while looking at another player within 3 blocks sends the same invite path as `/trade <player>`, pressing it without a valid target shows `Look at a player within 3 blocks to request a trade.`, and pressing it while a screen is open does nothing. The same close-range look-target detection now also drives a lightweight client-only HUD prompt above the hotbar that displays the looked-at player name plus `Press CAPS LOCK to request trade`; the prompt hides while screens or the debug overlay are open, never opens the trade GUI, and never sends packets on its own.
- Refactored Cosmic Dungeon client keybinding category registration so the shared **Cosmic Dungeon** Controls category is registered only once while preserving the spawner preset and trade request keybinds.
- Recorded the current trade GUI PNG coordinate map in `docs/Trading/Trade_GUI_Coordinate_Map.md`. Current assets provide exact slot positions for the trade offer rows and player inventory/hotbar, plus standalone accept/deny 16x16 icons. The final implemented text anchors, player preview bounds, button/status-indicator placement, currency controls, and hover regions are documented there as procedural coordinates layered over the PNG without changing the audited real slot coordinates.
