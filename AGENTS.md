# Advanced Spark

## Required Context

Before changing code, read `CONTEXT.md`. The scope and compatibility rules in
this file are binding.

Use the workflow skill at
`/Users/kricy/.codex/skills/using-superpowers/SKILL.md` for this repository.

When a requested change changes a public API, component id, trait id,
packet/NBT schema, lifecycle order, or dependency contract, describe the exact
scope, invariants, downstream impact, and verification plan, then wait for the
owner's explicit approval.

## Scope

- Keep changes narrow to the requested trait, role, packet, or integration.
- Check SparkTraits, Wathe, and NoellesRoles ownership before editing; inspect
  SparkFactionAPI, SparkWitch, SparkStrength, or SparkAssist when a downstream
  contract is involved.
- Preserve unrelated roles and traits, exact ids, gameplay values, event order,
  component ids, sync visibility, and fallback behavior.
- Downstream mods must use `dev.caecorthus.sparktraits.api`, never `impl` or
  `component` internals.
- Use English and Chinese comments for non-obvious public contracts and complex
  behavior.

## Workflow

- Use Java 21 for every verification command.
- Write a focused failing test before changing testable behavior, then make it
  pass with the smallest implementation.
- Tests are allowed and expected for pure rules, protocol contracts, and public
  API behavior. Do not add production-only reset hooks for tests.
- Use subagents for independent work and communicate with them in English.

## Repositories

- SparkTraits: https://github.com/Caecorthus/SparkTraits
- Wathe: https://github.com/XruiDD/TrainMurderMystery
- NoellesRoles: https://github.com/XruiDD/NoellesRoles
- SparkFactionAPI: https://github.com/Caecorthus/SparkFactionAPI
- SparkWitch: https://github.com/Caecorthus/SparkWitch
- SparkStrength: https://github.com/Caecorthus/SparkStrength
- SparkAssist: https://github.com/Caecorthus/SparkAssist
