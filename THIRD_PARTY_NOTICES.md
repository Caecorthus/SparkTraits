# Third-Party Notices

## StarRailExpress Depression grayscale

The following files adapt the `insanity` shader program from
[catmoon-train/StarRailExpress](https://github.com/catmoon-train/StarRailExpress) at commit
`f8d84137e36e08349a10542438944ef2f010ec37`, credited upstream to the Catmoon Train Team:

- `src/client/resources/assets/minecraft/shaders/program/sparktraits_depression_insanity.fsh`, from
  `src/main/resources/assets/minecraft/shaders/program/insanity.fsh`
- `src/client/resources/assets/minecraft/shaders/program/sparktraits_depression_insanity.json`, from
  `src/main/resources/assets/minecraft/shaders/program/insanity.json`

SparkTraits renames the program to its own identifier, replaces the fixed 1.2 output multiplier
with a `Brightness` uniform, and drives the desaturation from the Depression trait's sanity level.

The upstream README and root license declare GPL-3.0-only. The upstream-derived portion remains
covered by GPL-3.0-only; SparkTraits' AGPL-3.0-only work is combined with it under GPLv3 section 13.
A verbatim copy of the upstream license is included at `licenses/StarRailExpress-GPL-3.0-only.txt`.
