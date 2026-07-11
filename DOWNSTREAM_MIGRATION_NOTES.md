# Downstream Migration Notes

## Public SparkTraits Query Facade

Downstream mods must stop importing or reflecting into SparkTraits internal
services/components. Use `dev.caecorthus.sparktraits.api.SparkTraitsApi`:

```java
SparkTraitsApi.hasActiveTrait(player, traitId);
SparkTraitsApi.hasLastStandTriggeredThisRound(serverWorld, playerUuid);
SparkTraitsApi.isFinalMomentActive(world);
```

These methods are static and null-safe. Optional integrations may continue to
use reflection to avoid a hard loader dependency, but the reflected class must
be `dev.caecorthus.sparktraits.api.SparkTraitsApi`, not an `impl` or `component`
class.

Known replacements:

- SparkStrength active/effective trait probes should call `hasActiveTrait` for
  the exact trait id they need instead of reflecting
  `impl.effective.EffectiveTraitService`.
- SparkWitch Last Stand and final-moment probes should call
  `hasLastStandTriggeredThisRound` and `isFinalMomentActive` instead of
  reflecting `LastStandService` or `TraitWorldComponent`.

No existing trait id, component id, API trait definition, or lifecycle event
was renamed by this migration.

## Sync Privacy

The existing third player-component sync field is now recipient-filtered:

- regular recipients receive no revealed trait ids;
- the owner receives `revealedTraits`;
- spectator/creative recipients receive `activeTraits`.

Packet field order is unchanged. Downstream client code must not rely on seeing
another regular player's revealed trait ids. Public boolean flags used for
rendering and instinct behavior remain available.

## NoellesRoles Version Contract

The tested local artifact is
`noellesroles-1.7.6-h1.5.6-spark.jar`. SparkTraits metadata now requires
exactly `noellesroles 1.7.6-h1.5.6-spark`; the Gradle dependency filename and
metadata version share `noellesroles_version` from `gradle.properties`. An
ordinary `1.7.6` build is not treated as compatible with this Spark fork.

## Existing Optional Bridges

The SparkFactionAPI effective-faction bridge remains optional and fail-neutral.
The Simple Voice Chat entrypoint remains optional and does not create a loader
dependency. Audience-first internal package ownership remains unchanged.
