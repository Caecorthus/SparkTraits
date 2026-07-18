# Spirit Sleuth Implementation Plan

> **For agentic workers:** Use the approved scope below and keep every change attributable to `sparktraits:spirit_sleuth`.

**Goal:** Add the universal `Spirit Sleuth / 灵探` trait so its living owner can see the translucent floating heads of truly dead spectators, and add the matching SparkAssist Guidebook entry beside other universal traits.

**Architecture:** Keep the gameplay predicate in a pure trait-local rule, keep the renderer mixin as a thin client adapter, and rely on the existing owner-visible active-trait sync. Wrap only vanilla `LivingEntityRenderer.render`'s `LivingEntity.isInvisibleTo(PlayerEntity)` query; preserve head/hat-only model pose, translucency, depth, frustum, range, equipment, and nameplate behavior.

**Tech Stack:** Java 21, Fabric, Yarn 1.21.1, Sponge Mixin, MixinExtras, Wathe, JUnit 5, SparkAssist authored Guidebook JSON.

## Approved Contract

- Public trait id: `sparktraits:spirit_sleuth`; English name `Spirit Sleuth`; Chinese name `灵探`.
- Universal audience and default roll weight; retain existing global role-eligibility exclusions.
- Reveal only when the viewer owns the active trait, the viewer is not a spectator, the target is a spectator, and Wathe no longer considers the target playing and alive.
- Do not reveal Last Stand pending deaths, swallowed players, or other living temporary-spectator states.
- Do not add or change public APIs, components, packets, NBT, lifecycle order, dependency metadata, or versions.
- Guidebook order `780`, after `体质优异` and before `背水一战`; color `#B8A7FF`.
- Preserve unrelated dirty work in both repositories. Do not commit or push without a separate owner request.

## Tasks

- [x] Add focused failing tests for the pure visibility truth table and exact mixin/config contract.
- [x] Add `SpiritSleuthTrait`, `SpiritSleuthVisibilityRules`, built-in registration, localization, and the client mixin.
- [x] Add failing SparkAssist resource/order/localization assertions, then the authored Guidebook JSON and fallback names.
- [x] Run focused tests, Java 21 clean builds, `git diff --check`, and packaged-jar resource/metadata inspection in both repositories.
- [x] Review the final diff against `AGENTS.md` and `CONTEXT.md`; report live-client cases not exercised automatically.
