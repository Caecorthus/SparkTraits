# SparkTraits Architecture Constitution

Status: current live-code constitution, updated 2026-07-10.

SparkTraits adds passive trait behavior to Wathe and NoellesRoles. Its primary
architecture is audience-first: concrete traits live with the roles that can
receive them, while shared selection, assignment, effective-alignment, storage,
and integration rules live behind narrow modules.

## Mandatory Rules

1. Preserve public behavior by default. Trait ids, component ids, packet field
   order, NBT keys, event order, gameplay values, null behavior, and fallback
   behavior change only when the task explicitly approves that change.
2. `api/` is the only supported downstream interface. Extend it compatibly.
   Other mods must not import or reflect into `impl/`, `component/`, `mixin/`,
   `client/`, or other internal packages.
3. Concrete trait ownership is based on obtainable audience, not effective
   alignment. Impostor remains civilian-owned; Conscience remains
   killer-owned.
4. New behavior belongs in its owning domain module. Do not grow a broad
   service merely because it already sees the relevant event.
5. Mixins are thin external adapters: capture the minimum context and delegate.
   They do not own selection, alignment, economy, victory, rendering, or trait
   policy.
6. Optional integrations must check availability, use public APIs, and fail
   without changing standalone SparkTraits behavior. Reflection is acceptable
   only at a genuinely optional public API boundary.
7. Protect unrelated roles and traits with a narrow predicate and focused
   verification. Broad faction or audience rewrites require explicit approval.
8. Tests are allowed for pure rules, public contracts, sync visibility, packet
   policy, and regressions. Never expose production-only reset or mutation hooks
   just to make tests convenient.
9. Existing watch-only hotspots are not automatic refactor work. Size alone is
   not a reason to split them.
10. Readability and correct ownership outrank numerical code-shape targets.
    Rough review triggers are more than 5 parameters, roughly 70 lines per
    method, roughly 300 lines per class, or deep nesting. These are advisory,
    not hard caps. External overrides, mixin signatures, cohesive tables, and
    readable orchestration may exceed them.

## Live Module Map

| Module | Ownership |
| --- | --- |
| `api/` | Stable trait types, registry/events, and downstream query facade |
| `component/` | Player/world state, persistence, and recipient-aware sync |
| `impl/selection/` | Eligibility, conflicts, slot chance, and selection |
| `impl/assignment/` | Assignment planning and role-assignment integration |
| `impl/effective/` | Effective alignment, death, economy, and gun rules |
| `impl/replay/` | Wathe replay ids and recording adapters; no gameplay policy |
| `impl/traits/civilian/` | Traits obtainable by civilian-side roles |
| `impl/traits/killer/` | Traits obtainable by killer-side roles |
| `impl/traits/neutral/` | Traits obtainable by neutral roles |
| `impl/traits/global/` | Traits available across role factions |
| `impl/compatibility/` | Hard-dependency adapters and optional public bridges |
| `impl/lifecycle/` | Event registration and lifecycle ordering only |
| `impl/registry/`, `impl/resource/` | Built-ins and resource registration |
| `impl/command/admin/` | Administrative command adapters and validation |
| `mixin/` | Thin server/common adapters into external code |
| `net/version/` | Version-confirmation protocol |
| `voice/` | Optional Simple Voice Chat entrypoint |
| `client/` | Client hooks, HUD, audio, text, and rendering |

Dependency direction is external hooks/entrypoints -> owning implementation
module -> `api/` and `component/`. Components may use trait definitions from
`api/`. The public `SparkTraitsApi` facade is the deliberate outward gateway
and may delegate to internal components/services. Internal implementation must
not become part of a downstream contract.

## Public Interface

Stable public types include `Trait`, `TraitAudience`, `TraitDefinition`,
`TraitSelectionContext`, `TraitVisibility`, assignment/removal reasons,
`TraitRegistry`, and `api/event/TraitEvents`.

`SparkTraitsApi` is the null-safe downstream query facade:

```java
boolean hasActiveTrait(PlayerEntity player, Identifier traitId)
boolean hasLastStandTriggeredThisRound(ServerWorld world, UUID playerUuid)
boolean isFinalMomentActive(World world)
```

Backward-compatible additions are the default. A breaking change requires a
version plan and downstream updates for SparkWitch, SparkStrength, and any other
known consumer.

## Dependency Contracts

The versions below are the current tested baseline and loader floor:

| Dependency | Contract |
| --- | --- |
| Java | `>=21`; build and verification use Java 21 |
| Minecraft | `1.21.1` |
| Fabric Loader | `>=0.17.2` |
| Fabric API | `0.116.7+1.21.1` tested; loader accepts compatible Fabric API |
| Wathe | local `wathe-1.5.6-spark-1.21.1.jar`; exact loader version |
| NoellesRoles | local `noellesroles-1.7.6-h1.5.6-spark.jar`; exact loader version because internal fork symbols are imported directly |
| Ratatouille | tested/floor `1.4.3-1.21.1` |
| Cardinal Components | tested/floor `6.1.1` for base/entity/world |

SparkFactionAPI is optional. Its effective-faction resolver is registered only
when `sparkfactionapi` is loaded; bridge failure leaves native SparkTraits
alignment behavior unchanged. Simple Voice Chat is compile-only plus local
runtime support and is discovered through the `voicechat` entrypoint; it is not
a loader dependency. Carpet is development runtime only.

## Sync Visibility Contract

`TraitPlayerComponent.shouldSyncWith` intentionally allows all recipients, so
`writeSyncPacket` must filter every sensitive field. A recipient who is both
owner and spectator follows spectator visibility for active/revealed trait ids,
while pending traits and other owner-only state remain owner-visible.

| Synced state | Owner | Spectator/creative | Other player |
| --- | --- | --- | --- |
| Active trait-id field | Owner-visible active traits | All active traits | Empty |
| Pending trait ids | All pending traits | Empty unless also owner | Empty |
| Revealed trait-id field | Revealed traits | All active traits | Empty |
| Serial-killer role, Depression targets/countdown | Visible | Hidden unless also owner | Hidden |
| Killer/Last Stand/Going Dark/Conscience/Impostor/Cautious/Psycho/Pig flags | Visible | Visible | Visible; flags reveal no trait ids |
| Conscience poison ticks | Policy-gated poisoner/Toxicologist view | Visible | Policy-gated |

Player sync packet field order is append-only:

1. active trait ids
2. pending trait ids
3. revealed trait ids
4. killer-instinct-hidden flag
5. Last Stand pending flag
6. Going Dark instinct-hidden flag
7. Conscience instinct flag
8. Impostor instinct flag
9. visible Conscience poison ticks
10. optional serial-killer murderer role
11. Cautious sound-suppression flag
12. owner Depression suicide ticks
13. Depression psycho-active flag
14. optional owner Depression attacker UUID
15. optional owner Depression counter-target UUID
16. Pig-active flag
17. reserved legacy Arrogant ASF tombstone, always written `false`

Readers consume field 17 only when bytes remain and discard its value. The slot
must not be reused or reordered. The retired exact id `sparktraits:arrogant_asf`
is filtered from player active/pending/revealed state, world disabled/unique
state, and round/death snapshots at component-state ingress.

World sync packet order is also append-only: disabled trait ids, used unique
trait ids, death-snapshot count and UUID/trait-id entries, trait-slot chance,
final-moment-active flag, then final-moment loose-end UUIDs. New trailing fields
must retain readable-byte fallback in the matching reader.

NBT compound ordering is not semantically significant, but keys and meanings
are stable. Player persistence writes `ActiveTraits`, `PendingTraits`,
`RevealedTraits`, optional `SerialKillerMurdererRole`, optional
`ConsciencePoisonTicks`/`ConsciencePoisoner`, optional
`BloodthirstyKillCount`, and optional `CorneredLastKillerRewardPaid`. World
persistence writes `DisabledTraits` and `TraitSlotRollChance`. All other current
component fields are intentionally round/runtime state unless explicitly
approved otherwise.

## Replay Contract

SparkTraits uses Wathe's existing death and global-event replay APIs. The stable
global event ids are `sparktraits:last_stand_triggered`,
`sparktraits:final_moment_start`, and `sparktraits:loose_end_conversion`.
`sparktraits:self_realization` remains a normal Wathe death reason and must not
produce a duplicate global event.

Last Stand replay is recorded only after the pending transition succeeds. Final
Moment replay records its global start before one conversion line for each
player actually changed to `wathe:loose_end`. Startup Conscience compensation
and Impostor/Conscience effective-alignment flips are intentionally not replay
events. Replay adds no SparkTraits packet, component field, persistent NBT, or
public API.

## Lifecycle Order

Initialization order is version handshake, packets, data components, particles,
sounds, built-in traits, Last Stand services, lifecycle hooks, then commands.

Kill handling preserves this order: Last Stand and Depression `BEFORE` hooks;
then death snapshot, Last Stand transition, Pig death sound, effective-trait
handling, and Depression handling. A Last Stand transition syncs new spectator
views and returns. A real death then runs the independent Conscience death
dividend, killer consequences, Impostor Bodyguard, Conscience Serial Killer
consequences, and Bomb Maniac owner cleanup before trait/player-state cleanup
and spectator resync. Round finalization clears both Conscience timed services
before player traits.

## Watch-Only Hotspots

These files are cohesive but high-risk because they combine many established
contracts. Do not split or substantially rewrite them without approval:

- `impl/effective/EffectiveTraitService.java`
- `impl/traits/civilian/depression/DepressionTraitService.java`
- `impl/traits/civilian/laststand/LastStandService.java`
- `impl/traits/civilian/laststand/LastStandFinalMomentService.java`
- `impl/assignment/TraitAssignmentService.java`
- `component/TraitPlayerComponent.java`
- `impl/traits/civilian/police/VigilanteVeteranTraitService.java`
- `impl/traits/killer/KillerTraitService.java`

Small surgical fixes inside a hotspot are allowed only when the owner has
approved that exact behavior and packet/lifecycle invariants remain explicit.

## Change Approval Template

Before moving, renaming, deleting, splitting, merging, or substantially
rewriting a public, watch-only, protocol, persistence, or lifecycle module,
request approval with every field below:

- **Area:** named module or contract.
- **Reason:** concrete problem that cannot stay local.
- **Old scope:** exact files, methods, ids, and fields affected.
- **New shape:** proposed owner, public interface, and responsibilities.
- **Forbidden scope:** unrelated roles/traits and contracts that must not move.
- **Invariants:** ids, values, null/fallback rules, ordering, visibility, NBT,
  packets, and lifecycle behavior to preserve.
- **Downstream impact:** imports/reflection, dependency floors, metadata, and
  release coordination.
- **Verification:** exact focused tests, Java 21 commands, artifact inspection,
  and downstream searches.

Wait for explicit owner approval. A backlog label, large line count, or
architecture ideal is not approval.

## Verification

Run from the repository root:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew test
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew verifyArchitecture
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew clean build
```

`verifyArchitecture` checks the governance files, public facade contract test,
local NoellesRoles jar, and matching exact metadata version. A release also inspects the
remapped jar's `fabric.mod.json` and required runtime classes through
`verifyModJarVersion`, which is wired into `build`.
