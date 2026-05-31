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
- Added foundational types: `VendorValueCategory`, `VendorPrice`, `GearSetDefinition`, and `VendorPricingService`.
- Added starter isolated hardcoded set definitions (structured for later JSON migration under `data/cosmicdungeon/vendor_prices/*.json`).
- Included starter pricing for the D2 T1 Judicator chainmail set:
  - `visor_of_the_resolute`
  - `cuirass_of_purpose`
  - `chausses_of_the_pledge`
  - `sabatons_of_the_unheard_oath`
- Starter values: 10 Trace per individual piece, 100 Trace for complete set detection.
- Added `/currency value` to report the held item sell value/debug source.
- Added `/currency value inventory` to report detected complete sets and notable inventory values.
- Scope boundary remains unchanged: no vendor NPCs, no vendor GUI, no item removal in pricing evaluation.


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
  - D1 Torch Flower best count (0-6)
  - D1 completion flag for >=3 Torch Flowers
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
- Village access unlocks once D1 is completed with at least 3 Torch Flowers.
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
- Added starter pricing support for **D2 T1 Judicator set**:
  - Full set payout: **100 Trace**
  - Individual piece payout: **10 Trace**
  - Pieces: `visor_of_the_resolute`, `cuirass_of_purpose`, `chausses_of_the_pledge`, `sabatons_of_the_unheard_oath`.
- Added player feedback messages for success and common failures, including capacity/ineligible/incomplete set states.

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
- Village gate requirement now enforced for these D1 vendors, including D1 completion expectation with at least 3 Torch Flowers.
- TODO:
  - Replace current string-based `requiredNpcSystem` with a typed enum/registry-backed id when D3+ systems are introduced.
  - Add localized translatable components for each denial reason and per-profile lock flavor text.


## Trading Foundation (1.5.0)
- Added direct player-to-player trading foundation with item offers plus Attunement Fragment (Trace) balance transfers via CurrencyService.
- Added normal-player trade commands: `/trade <player>`, `/trade accept <player>`, `/trade deny <player>`, and `/trade cancel`.
- Trade invites now store inviter/receiver metadata with game-time timestamps, expire after 30 seconds, and use a 3-second per-requester cooldown to reduce spam.
- Incoming invite chat now includes clickable green `[Accept Trade]` and red `[Deny Trade]` buttons that run the matching `/trade accept <player>` or `/trade deny <player>` fallback commands.
- Added validation to block self-trades, offline targets, dead/spectator participants, and new requests/accepts while either player is already in an active trade.
- Added disconnect/menu-close lifecycle cleanup so pending invites involving the player are removed, active trades are cancelled once, offered items are returned, and the remaining player is notified when applicable.
- Hardened trade finalization with preflight balance/currency-capacity/inventory-capacity validation, atomic currency rollback to original balances on failure, safe item returns on cancel/failure, and read-only other-player offer slots.
- Added trade network payloads via ModNetwork for request/accept/currency/ready/confirm/cancel plus the CAPS LOCK look-target trade request payload; typed `/trade <player>` remains the distance-free fallback, while the hotkey is server-validated for same-dimension online players within close range.
- Added the **Trade Request** keybind in the existing **Cosmic Dungeon** Controls category, defaulting to `CAPS LOCK`; pressing it while looking at another player within 3 blocks sends the same invite path as `/trade <player>`, pressing it without a valid target shows `Look at a player within 3 blocks to request a trade.`, and pressing it while a screen is open does nothing.
- Recorded the current trade GUI PNG coordinate map in `docs/Trading/Trade_GUI_Coordinate_Map.md`. Current assets provide exact slot positions for the trade offer rows and vanilla inventory/hotbar, plus standalone accept/deny 16x16 icons. The PNGs do not currently encode text anchors, player preview bounds, currency denomination regions, button placement, or hover/disabled states, so those coordinates must not be treated as asset-derived until new art is provided.
