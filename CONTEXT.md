# SparkTraits Domain Context

## Trait Audience

A trait audience is the set of roles that may receive a trait at selection or
assignment time. SparkTraits packages concrete trait behavior by obtainable
audience first:

- Civilian traits are available to original civilian-side roles.
- Killer traits are available to killer-side roles.
- Neutral traits are available to neutral roles.
- Global traits are available across role factions unless a trait adds a
  narrower predicate.

Alignment-flipping traits are still owned by the audience that can receive
them. For example, Impostor lives under `civilian/` even though it makes the
player effectively killer-sided, and Conscience lives under `killer/` even
though it makes the player effectively civilian-sided.
