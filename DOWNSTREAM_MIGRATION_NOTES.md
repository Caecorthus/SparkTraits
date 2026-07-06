# Downstream Migration Notes

## Trait Audience Package Cleanup

SparkTraits internal trait packages now use audience-first ownership:

- `impl.traits.civilian.*` for original civilian-side traits.
- `impl.traits.killer.conscience.*` for the killer-only Conscience trait family.
- `impl.traits.global.pig.*` for the universal Pig trait family.
- `impl.traits.neutral.arrogant_asf.*` for the Corrupt Cop-only Arrogant ASF trait.

Public trait ids, component ids, lifecycle ordering, and the public `api/`
package are unchanged.

SparkWitch's weak Last Stand reflection bridge was updated from
`dev.caecorthus.sparktraits.impl.traits.laststand.LastStandService` to
`dev.caecorthus.sparktraits.impl.traits.civilian.laststand.LastStandService`.

## Simple Voice Chat Entrypoint

SparkTraits now declares its existing `SparkTraitsVoiceChatPlugin` through the
`voicechat` entrypoint so Simple Voice Chat can discover the Depression voice
mute bridge when that optional mod is present.

This does not add a hard runtime dependency on Simple Voice Chat. Servers
without Simple Voice Chat keep the same loader dependency requirements.
