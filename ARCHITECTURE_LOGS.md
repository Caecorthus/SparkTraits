# SparkTraits Architecture Logs

Append-only record of architecture decisions. This file records decisions; it
does not grant permission for later refactors.

## 2026-07-09 - Current Constitution Restored

- Replaced the deleted legacy board document with a concise constitution based
  on the live audience-first package layout.
- Declared all code-size numbers advisory review triggers. Readability,
  cohesion, behavior, and ownership take priority over line counts.
- Allowed focused tests for pure rules, public contracts, protocol visibility,
  and regressions. Production-only reset hooks remain forbidden.
- Added the current dependency direction, tested dependency floors, optional
  integration rules, sync visibility matrix, packet/NBT contracts, watch-only
  hotspots, approval template, and Java 21 verification commands.

## 2026-07-09 - Public Query Facade

- Added `dev.caecorthus.sparktraits.api.SparkTraitsApi` as the supported,
  null-safe query boundary for active traits, Last Stand round consumption, and
  final-moment state.
- Added reflection-descriptor and null-safety contract tests for all three
  facade methods used by SparkWitch and SparkStrength.
- Downstream optional integrations should reflect or compile against this
  facade rather than `impl` or `component` classes.
- No trait id, component id, lifecycle order, NBT key, or gameplay value
  changed.

## 2026-07-09 - Recipient-Specific Revealed Traits

- Added a pure recipient-visibility rule and JUnit 5 regression coverage.
- Regular recipients receive no revealed trait ids; owners receive the revealed
  set; spectator/creative recipients receive the active set.
- Packet field order is unchanged. Only the contents of the existing third
  trait-id field are filtered by recipient.
- TDD evidence: the focused test first failed because the visibility rule was
  absent, then passed after the minimal helper and packet delegation were added.

## 2026-07-09 - NoellesRoles Metadata Alignment

- Centralized `1.7.6-h1.5.6-spark` in `gradle.properties`.
- The local jar filename and exact loader dependency now resolve from that same
  property. An open-ended lower bound is intentionally avoided because Fabric
  SemVer could admit a plain upstream `1.7.6` without the Spark fork symbols.
- Added `verifyArchitecture` to catch missing governance/API files, a missing
  aligned jar, or metadata drift.

## 2026-07-10 - Arrogant ASF Trait Retirement

- Retired the exact trait id `sparktraits:arrogant_asf`; Corrupt Cop ability
  ownership moved out of SparkTraits under the approved cross-mod migration.
- Removed built-in registration, forced assignment, gameplay, client HUD/audio,
  mixins, sound registration/resources, and localization for the retired trait.
- Player, world, and round/death snapshot state now filter only that exact legacy
  id at ingress; unrelated trait ids remain unchanged.
- Player sync field 17 remains reserved: writers emit `false`, and readers
  optionally consume and discard the trailing boolean without reordering fields.
- The remaining NoellesRoles packet adapters keep both verified lambda layouts:
  the local SparkTraits jar uses `$0/$1/$2/$4-$9/$12-$14`, while the coordinated
  SparkStrength jar uses `$31/$32/$34-$40/$43-$45` for the same packet sequence.
- TDD evidence: focused retirement/migration tests failed against the old
  registration and live field, then passed after the narrow tombstone/filter.

## 2026-07-10 - Match-Defining Replay Events

- Reused Wathe's existing death and global-event replay APIs without adding a
  SparkTraits packet, component field, persistent NBT key, or public API.
- Added stable global event ids for successful Last Stand, Final Moment start,
  and each actual conversion to `wathe:loose_end`.
- Kept `sparktraits:self_realization` on Wathe's existing death record and added
  only its missing replay localization, avoiding a duplicate death line.
- Excluded startup Impostor/Conscience effective-alignment flips and Conscience
  compensation assignment from replay by explicit owner scope.
- Preserved ordering: Wathe death first, successful Last Stand second; Final
  Moment start first, then one conversion event per converted Loose End.
