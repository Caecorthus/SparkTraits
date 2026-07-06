# SparkTraits Architecture Logs

This companion file keeps architecture backlog records, pending-refactor
examples, and legacy registers out of the core constitution.

These records are approved architecture investigation candidates, not automatic
edit permission. Each item still requires the approval template in
`ARCHITECTURE.md` before code is changed.

## Architecture Backlog

### 1. Root `impl/` Decomposition

Priority: Highest.

Files:

- `impl/*.java`

Problem: the root `impl/` package is a catch-all for registration, selection,
assignment, lifecycle, commands, resources, effective alignment, and concrete
trait behavior. This lowers Locality because understanding one trait often
requires scanning unrelated Modules.

Direction: move existing behavior into the target subpackages in
`ARCHITECTURE.md` in small, behavior-preserving slices. Do not move everything
in one giant change.

Risk: High. Preserve trait ids, registration ordering, lifecycle ordering,
visibility, sync, NBT, command names, and localization keys.

### 2. Effective-Team Rules

Priority: Highest.

Files:

- `impl/EffectiveTraitService.java`
- `mixin/GameFunctionsMixin.java`
- `mixin/MurderGameModeMixin.java`
- `client/mixin/WatheClientMixin.java`

Problem: effective alignment, win deferral, economy, gun punishment, blackout,
cohort, instinct, and unsupported Impostor cleanup share one large Module.

Direction: split by policy family under `impl/effective/`, keeping event
registration in lifecycle/Adapter Modules and rule evaluation in the owning
Module.

Risk: High. Preserve `CheckWinCondition.EVENT` ordering, first-non-null/block
semantics, spectator/client visibility, and Conscience/Impostor exceptions.

### 3. Trait Assignment Planning

Priority: High.

Files:

- `impl/TraitAssignmentService.java`
- `impl/TraitSelector.java`
- `impl/TraitRules.java`
- `impl/TraitLockValidationService.java`
- `impl/TraitRoleEligibility.java`

Problem: planning, random selection, locks, compatibility, role eligibility,
forced traits, and role compensation are close together but not separated by a
small Interface.

Direction: make assignment produce an explicit round trait plan and delegate
candidate selection/lock validation to `impl/selection/`.

Risk: High. Preserve pending/admin lock behavior, three-slot max behavior,
unique trait semantics, Conscience compensation, forced Pig/Arrogant behavior,
and random Depression cap behavior.

### 4. Component Storage And Sync Visibility

Priority: High.

Files:

- `component/TraitPlayerComponent.java`
- `component/TraitWorldComponent.java`
- `component/SparkTraitsComponents.java`
- `component/SparkTraitsDataComponentTypes.java`

Problem: CCA storage Modules also contain lifecycle calls, per-tick poison
behavior, owner/spectator visibility rules, public flags, NBT, and sync packet
compatibility.

Direction: keep storage, sync, NBT, and stable ids in `component/`; move
gameplay decisions to owning Modules. Preserve packet backward compatibility
when adding fields.

Risk: High. Sync visibility is part of gameplay secrecy; do not leak hidden
traits, owner-only Depression targets, or spectator-only information.

### 5. Thin Mixin Adapters

Priority: High.

Files:

- `mixin/PoisonGasCloudEntityMixin.java`
- `mixin/NoellesRolesPacketMixin.java`
- `mixin/MurderGameModeMixin.java`
- `client/mixin/WatheClientMixin.java`
- other `mixin/*`

Problem: several mixins own substantial rules instead of only adapting external
seams.

Direction: for each touched mixin, extract gameplay/text/rendering policy into
the owning Module and leave injection knowledge in the Adapter.

Risk: Medium-high. Preserve injection methods, ordinals, remap flags, fallback
method names for NoellesRoles jar compatibility, and client/server side
separation.

### 6. Complex Trait Families

Priority: Medium-high.

Files:

- `impl/traits/civilian/depression/DepressionTraitService.java`
- `impl/traits/civilian/laststand/LastStandService.java`
- `impl/traits/civilian/laststand/LastStandFinalMomentService.java`
- `impl/traits/civilian/police/VigilanteVeteranTraitService.java`
- `impl/traits/killer/KillerTraitService.java`
- `impl/traits/killer/conscience/Conscience*`
- `impl/traits/civilian/impostor/Impostor*`

Problem: several trait families are large enough to become god Modules inside a
single trait. Each combines selection predicates, runtime state, event
registration, sound, combat, movement, vision, and cleanup.

Direction: split each complex trait family internally by responsibility only
when a real task touches it. Keep the external trait-family Interface small.

Risk: Medium-high. Do not split simple data-only traits just to make more files.
Use the deletion test: if deleting the new Module would only move complexity to
callers, it is too shallow.

### 7. Client Architecture

Priority: Medium.

Files:

- `client/*.java`
- `client/mixin/*.java`
- `client/net/*`

Problem: HUD, audio, rendering, text, and network confirmation are mixed in one
client package, and some client mixins hold policy decisions.

Direction: migrate toward `client/hud/`, `client/audio/`, `client/render/`,
`client/text/`, and `client/net/version/` while keeping mixins thin.

Risk: Medium. Preserve confirmed-server gating and do not activate SparkTraits
visuals, sounds, or inputs on ordinary servers.

### 8. Command Rules

Priority: Medium.

Files:

- `impl/SparkTraitsCommands.java`

Problem: command tree registration, trait validation, suggestions, percentage
conversion, and feedback text live together.

Direction: move to `impl/command/admin/`, keeping registration as an aggregate
and testable formatting/validation as small rule Modules.

Risk: Medium-low. Preserve command literals, permission level 2 behavior,
feedback wording, and default namespace handling.

### 9. Version Protocol

Priority: Medium.

Files:

- `net/SparkTraitsVersionHandshake.java`
- `net/SparkTraitsVersionCheck.java`
- `net/SparkTraitsPackets.java`
- `net/SparkTraitsServerConfirmS2CPacket.java`
- `net/SparkTraitsServerConnection.java`
- `client/net/SparkTraitsClientVersionHandshake.java`

Problem: login-stage and play-stage confirmation behavior is split by side and
class, but protocol constants and semantics should remain shared.

Direction: converge toward `net/version/` and `client/net/version/` with a
shared protocol Module.

Risk: Medium. Preserve proxy-safe unanswered-login behavior, same-version
play-stage confirmation, mismatch disconnect messages, and jar metadata guards.

### 10. Metadata And Resource Consistency

Priority: Medium.

Files:

- `src/main/resources/fabric.mod.json`
- `src/main/resources/sparktraits.mixins.json`
- `src/client/resources/sparktraits.client.mixins.json`
- language, sound, shader, texture resources

Problem: metadata and resource declarations can drift from registered code.

Direction: add or update tests/guards when changing component ids, mixin lists,
language keys, sounds, shader files, or entrypoints.

Risk: Medium. Stale metadata can fail at runtime even when unit tests pass.

## Legacy Register

This register records known non-target Modules. It does not authorize edits.

### Root `impl/*.java`

Status: Pending Refactor.

Target direction: no new files in root `impl/`; migrate touched behavior to
target subpackages with approval.

Rule: root `impl/` may remain as a compatibility transition area while slices
move out. Do not make it deeper.

### `impl/EffectiveTraitService.java`

Status: Pending Refactor.

Target direction: `impl/effective/` submodules by policy family.

Rule: do not add unrelated economy, gun, vision, blackout, or round-end policy
to this Module. Put new behavior in the owning effective submodule after
approval.

### `impl/TraitAssignmentService.java`

Status: Pending Refactor.

Target direction: `impl/assignment/` plus `impl/selection/`.

Rule: do not add new trait runtime behavior here. This Module owns round trait
planning only.

### `impl/TraitGameHooks.java`

Status: Pending Refactor.

Target direction: `impl/lifecycle/`.

Rule: event registration may aggregate and delegate, but rule decisions belong
to owning Modules.

### `component/TraitPlayerComponent.java`

Status: Pending Refactor.

Target direction: storage/sync/NBT only, with behavior delegated to owning
Modules.

Rule: preserve owner/spectator/regular-player visibility semantics. Do not leak
hidden traits or owner-only state.

### `component/TraitWorldComponent.java`

Status: Pending Refactor.

Target direction: world storage/sync/NBT only.

Rule: preserve disabled trait ids, unique trait state, round/death snapshots,
Final Moment flags, and slot chance persistence unless a migration is approved.

### Large trait family Modules

Status: Pending Refactor.

Known examples:

- `impl/DepressionTraitService.java`
- `impl/LastStandService.java`
- `impl/LastStandFinalMomentService.java`
- `impl/VigilanteVeteranTraitService.java`
- `impl/KillerTraitService.java`

Target direction: owning packages under `impl/traits/...` with a small external
Interface per trait family.

Rule: split only when it improves Locality. Do not create shallow pass-through
helpers.

### Mixed-responsibility mixins

Status: Pending Refactor.

Known examples:

- `mixin/PoisonGasCloudEntityMixin.java`
- `mixin/NoellesRolesPacketMixin.java`
- `mixin/MurderGameModeMixin.java`
- `client/mixin/WatheClientMixin.java`

Target direction: thin Adapters that delegate to owning Modules.

Rule: mixins may keep injection knowledge, local capture, ordinals, and
compatibility method-name fallbacks. Gameplay rules should move out after
approval.
