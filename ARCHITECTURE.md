# SparkTraits Architecture Constitution

This document is mandatory for all future agents working in this repository.
It defines the target architecture and the governance rules for changing
existing modules. It is not a blanket authorization to refactor old code.

SparkTraits is a trait layer on top of Wathe, NoellesRoles, and optional Spark
family mods. The main architectural risk is turning trait behavior, Wathe
hooks, client rendering, and cross-mod compatibility into god classes. The
rules below exist to keep Modules deep, seams narrow, and unrelated traits
untouched.

## Mandatory Rules

1. Read this file before changing code.

2. Protect `src/main/java/dev/caecorthus/sparktraits/api/` as the public
   Interface for trait definitions, trait lifecycle events, and downstream
   callers.

   - Default change mode is backward-compatible extension.
   - Do not change existing method semantics, record fields, lifecycle ordering,
     null-handling, visibility behavior, or selection semantics unless the task
     explicitly approves a breaking change.
   - A breaking public Interface change requires a reason, scope, downstream
     impact list, version plan, and verification plan before editing.

3. Existing legacy modules are not free to rewrite.

   Before deleting, moving, renaming, splitting, merging, or substantially
   changing any existing source Module, first provide:

   - Reason: why the old Module cannot stay as-is.
   - Scope: exact packages and files to be touched.
   - Impact: behavior, public Interface, downstream mods, tests, and release
     risk.
   - Verification plan: local tests, build commands, and downstream checks.

   Wait for explicit owner approval before making that change.

   The approval request must be specific enough to protect the old architecture.
   A module-level label is not enough by itself. For each proposed board of
   work, include:

   - Board: the named architecture area being changed.
   - Reason: the friction that makes the old Module shape unsafe or costly to
     keep.
   - Old code scope: exact packages, files, and existing methods/helpers that
     will be moved, deleted, renamed, or rewritten.
   - New Module shape: the proposed package/module name and its intended
     Interface, including allowed responsibilities.
   - Forbidden scope: files, methods, policies, public Interface semantics,
     ordering, fallback behavior, and downstream contracts that must not
     change.
   - Behavior invariants: null behavior, fallback behavior, event ordering,
     priority, role isolation, trait isolation, sync visibility, NBT keys, and
     other semantics that must be preserved.
   - Downstream impact: whether downstream mods must change imports,
     registrations, protocol expectations, component ids, tests, or release
     expectations.
   - Verification plan: exact local tests, build commands, static checks, and
     downstream searches/checks.

   If any of these items are unknown, say so and perform a read-only review
   before requesting approval. Do not fill gaps by making assumptions during
   implementation.

4. `Legacy`, `Pending Refactor`, and `Deletion Candidate` labels are direction
   markers only. They are not automatic permission to edit or delete.

5. New behavior must not be added to legacy catch-all modules.

   If a task touches one of those modules and the owner approves a change, move
   the behavior into the owning target Module instead of making the catch-all
   deeper.

6. Root `impl/` is a transition area, not a destination.

   Do not add new source files directly under
   `src/main/java/dev/caecorthus/sparktraits/impl/` without explicit approval.
   New or migrated behavior should live in a domain subpackage listed in the
   target architecture below.

7. `mixin/` classes are thin Adapters at Wathe, NoellesRoles, Minecraft, or
   Fabric seams.

   A mixin may:

   - Locate the injection point.
   - Read the minimum required context.
   - Convert external types into the owning Module's Interface.
   - Delegate to a domain Implementation Module.

   A mixin must not own trait selection, assignment planning, effective-team
   rules, win rules, economy rules, gun punishment rules, text rules, sound
   rules, rendering policy, or cross-mod compatibility policy.

8. Every meaningful Seam must preserve unrelated roles and traits.

   If a change is about one role, trait, command, visual, sound, packet, or
   policy, prove the change does not broaden into unrelated behavior. Prefer
   narrow helpers, per-trait predicates, and local hooks over broad audience or
   faction rewrites.

9. Comments must be English and Chinese when they explain:

   - Public Interface semantics.
   - Wathe, NoellesRoles, Minecraft, Fabric, or Spark-family Seam behavior.
   - Mixin injection reasons.
   - Cross-mod compatibility rules.
   - Protocol, sync, NBT, or visibility compatibility rules.
   - Legacy retention or migration reasons.

   Do not add noise comments to self-explanatory code.

10. Tests should cross the same Interface as callers.

    Prefer testing domain Modules through their real Interface instead of
    reaching into private helper details. Package-private pure rules are allowed
    when they preserve Locality and keep Fabric/CCA-heavy objects out of plain
    unit tests.

11. Metadata and resources must match code.

    Keep `fabric.mod.json`, mixin configs, CCA component ids, networking packet
    ids, language keys, sounds, shaders, and assets aligned with registered
    code. Do not leave stale ids in metadata after deleting or renaming the
    owning Module.

12. If a change triggers downstream migration, create or update
    `DOWNSTREAM_MIGRATION_NOTES.md` in the repo root.

    Triggering changes include:

    - Any `api/` public Interface change.
    - Any trait lifecycle ordering, assignment ordering, visibility, sync, NBT,
      or component id semantic change.
    - Any command name, command argument, or user-facing feedback contract
      change.
    - Any login version handshake protocol or rejection behavior change.
    - Any `fabric.mod.json` dependency, entrypoint, or CCA declaration change.
    - Any change requiring downstream import, registration, behavior, test, or
      release updates.

    Pure internal movement does not require downstream notes if public behavior
    stays identical.

## Target Architecture

Only `api/` is the public downstream Interface unless this document says
otherwise. All other packages are internal Implementation or Adapter modules.

```text
src/main/java/dev/caecorthus/sparktraits/
  SparkTraits.java

  api/
    Trait.java
    TraitDefinition.java
    TraitRegistry.java
    TraitSelectionContext.java
    TraitAudience.java
    TraitAssignmentReason.java
    TraitRemovalReason.java
    TraitVisibility.java
    event/

  component/
    SparkTraitsComponents.java
    SparkTraitsDataComponentTypes.java
    TraitPlayerComponent.java
    TraitWorldComponent.java

  impl/
    registry/
    selection/
    assignment/
    lifecycle/
    effective/
    traits/
      global/
        pig/
      civilian/
        depression/
        impostor/
        laststand/
        police/
      killer/
        conscience/
      neutral/
        arrogant_asf/
    command/
      admin/
    resource/
    compatibility/
      noellesroles/
      sparkwitch/

  mixin/
    thin server/common Adapters only

  net/
    version/

  voice/

src/client/java/dev/caecorthus/sparktraits/client/
  SparkTraitsClient.java
  hud/
  audio/
  render/
  text/
  net/version/
  mixin/
    thin client Adapters only
```

### `api/`

`api/` is the stable public Interface for traits and trait lifecycle events.
Internal Modules may implement this Interface, but the public package must not
expose internal Implementation classes.

Keep state declarations and behavior seams separate:

- `Trait` and `TraitDefinition` describe trait metadata and lifecycle hooks.
- `TraitRegistry` owns trait registration and lookup.
- `TraitSelectionContext` describes the data needed by selection predicates.
- `TraitEvents` exposes assignment, removal, and reveal lifecycle seams.

### `component/`

`component/` owns Cardinal Components registration, storage, sync, visibility
filtering, and NBT keys.

It must not own gameplay rules unless the rule is inseparable from preserving a
stored invariant. Trait behavior belongs to `impl/traits/...`, assignment
behavior belongs to `impl/assignment/`, and round/world lifecycle behavior
belongs to `impl/lifecycle/`.

Stable component ids and key meanings must be preserved unless an approved
migration plan says otherwise:

- `sparktraits:traits`
- `sparktraits:world`

If `fabric.mod.json` declares a component id, code must register it. If code
stops registering a component id, metadata must be updated in the same change.

### `impl/registry/`

Owns built-in trait registration and trait catalog composition.

Target Modules:

- `BuiltInTraitRegistry`
- `TraitCatalog`
- trait group registration delegates

This package may assemble definitions from `impl/traits/...`, but it must not
own selection rules, assignment rules, or runtime behavior.

### `impl/selection/`

Owns per-player trait candidate filtering and random slot selection.

Allowed responsibilities:

- slot count and slot chance interpretation
- random candidate collection
- weighted selection
- trait compatibility checks
- audience and role eligibility checks
- unique-per-game candidate filtering

Forbidden responsibilities:

- rewriting roles
- assigning traits to players
- mutating component state except through a narrow assignment result
- firing lifecycle hooks
- implementing a specific trait's runtime effect

### `impl/assignment/`

Owns round trait plans before Wathe sends welcome information.

Allowed responsibilities:

- pending/admin lock application
- random selection orchestration
- unique trait limit enforcement
- forced required traits after role-changing steps settle
- Conscience compensation killer planning
- final write of planned traits to player/world components

Forbidden responsibilities:

- per-tick runtime behavior
- client rendering or sound
- Wathe mixin injection details
- unrelated role selection policy beyond the explicit compensation behavior

### `impl/lifecycle/`

Owns registration of runtime event hooks and round/player cleanup orchestration.

This Module may register Fabric, Wathe, or trait lifecycle hooks and delegate to
owning Modules. It must not contain the business rules that those Modules own.

### `impl/effective/`

Owns effective-team semantics created by alignment-flipping traits.

Target submodules should keep these responsibilities separate:

- `alignment/`: original role, effective killer/civilian, public killer count.
- `vision/`: instinct highlight colors, cohort display, hidden target rules.
- `economy/`: task money, passive money, kill reward decisions.
- `gun/`: innocent-shot punishment and gun-victim semantics.
- `roundend/`: win deferral, neutral blockers, unsupported Impostor cleanup.
- `blackout/`: blackout immunity and blackout-related effective behavior.

This package must not become a new `EffectiveTraitService` god Module. Add a
new submodule when a rule belongs to a different policy family.

### `impl/traits/`

Owns concrete built-in trait behavior.

Each complex trait or tightly-related trait family should have a package:

- `global/`: traits available across factions, such as `cautious`,
  `fast_hands`, `steady`, and `excellent_physique`.
- `global/pig/`: Pig trait and Pig God compatibility behavior.
- `civilian/`: traits any original civilian-side role can receive, plus shared
  civilian-side runtime helpers.
- `civilian/depression/`: Depression runtime, psycho state, sounds, stamina,
  and client-facing state decisions.
- `civilian/impostor/`: Impostor-specific runtime behavior.
- `civilian/laststand/`: Last Stand revival and Final Moment behavior.
- `civilian/police/`: civilian-side traits restricted to Wathe's original
  Vigilante and Veteran roles.
- `killer/`: killer-only trait definitions and killer-only runtime helpers.
- `killer/conscience/`: Conscience-specific runtime behavior.
- `neutral/arrogant_asf/`: Arrogant ASF behavior for Corrupt Cop.

Small data-only traits may remain simple, but new multi-method behavior must
live in the owning trait package instead of a shared catch-all.

### `impl/command/admin/`

Owns SparkTraits admin command registration and command feedback rules.

The aggregate command Module registers command trees. Testable parsing,
selection, validation, conversion, and feedback rules should live in small
package-private rule Modules.

### `impl/resource/`

Owns server/common resource registration such as particles, sounds, and data
component types that are not part of the public `api/` Interface.

### `impl/compatibility/`

Owns optional cross-mod compatibility Modules that avoid hard dependencies or
centralize external ids.

Target packages:

- `compatibility/noellesroles/`: NoellesRoles role ids, packet compatibility,
  and behavior seams.
- `compatibility/sparkfactionapi/`: optional SparkFactionAPI effective-faction
  resolver bridges for SparkTraits alignment flips.
- `compatibility/sparkwitch/`: SparkWitch role ids and optional mana bridge.

Compatibility Modules should expose small internal Interfaces. They must not
own SparkTraits trait behavior that belongs in `impl/traits/...`.

### `mixin/`

Mixin modules are Adapters. They should be small enough that deleting the mixin
would move injection knowledge, not gameplay rules.

If a mixin currently contains multiple responsibilities, do not add a third.
After approval, extract each responsibility into the owning domain Module.

### `net/`

Version networking should converge toward:

```text
net/version/
  VersionProtocol.java
  ServerVersionHandshake.java
  ServerConfirmPacket.java
  ServerConnectionState.java

client/net/version/
  ClientVersionHandshake.java
```

Protocol constants, packet read/write behavior, compatibility checks,
disconnect messages, and play-stage confirmation behavior must be shared.
Server and client Adapters must not each invent their own channel or message
semantics.

### `voice/`

`voice/` owns the voicechat plugin Adapter. It may read trait state and delegate
to owning Modules, but it must not own Depression or Last Stand gameplay rules.

### `client/`

Client behavior should converge toward:

- `client/hud/`: HUD overlays and actionbar-adjacent display decisions.
- `client/audio/`: music controller, sound instances, and sound access helpers.
- `client/render/`: entity rendering, shader/post-processing, and model/skin
  decisions.
- `client/text/`: client-only text formatting and translation fallback helpers.
- `client/net/version/`: client version handshake and server-confirm behavior.
- `client/mixin/`: thin client Adapters only.

Client Modules that render or play sounds must gate SparkTraits-only behavior
on confirmed SparkTraits server state when they could otherwise activate on
ordinary servers.

## Architecture Logs

Backlog candidates, pending-refactor examples, and legacy registers live in
[`ARCHITECTURE_LOGS.md`](ARCHITECTURE_LOGS.md). They are planning records,
not automatic permission to move, rename, delete, split, or merge existing
modules.

## Approval Template For Legacy Changes

Use this before modifying old architecture:

```text
Board:
Reason:
Old code scope:
New Module shape:
Forbidden scope:
Behavior invariants:
Downstream impact:
Verification plan:
Downstream notes needed: yes/no
```

No approval, no edit.

## Verification Expectations

For documentation-only changes:

```bash
./gradlew test
git diff --check
```

For internal code movement with behavior preserved:

```bash
./gradlew clean test
./gradlew build
git diff --check
```

For public Interface, networking, component, metadata, or cross-mod behavior
changes:

```bash
./gradlew clean test
./gradlew build
git diff --check
```

Then identify affected downstream repos and run the smallest meaningful
downstream compile/test/build checks. At minimum, search downstream imports and
usage before claiming no downstream impact.
