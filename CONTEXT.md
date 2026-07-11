# SparkTraits Domain Context

## Audience-First Ownership

A trait's package follows the roles that can receive it at selection or
assignment time:

- `impl.traits.civilian`: original civilian-side audiences.
- `impl.traits.killer`: killer-side audiences.
- `impl.traits.neutral`: neutral-role audiences.
- `impl.traits.global`: audiences across role factions unless narrowed by the
  trait itself.

Effective alignment does not move ownership. Impostor is civilian-owned even
though it becomes effectively killer-sided. Conscience is killer-owned even
though it becomes effectively civilian-sided. Pig is global.

## Core Flow

Built-in trait definitions register first. Selection evaluates audience,
role-specific eligibility, conflicts, uniqueness, disabled traits, and slot
chance. Assignment writes pending/active player state and invokes trait events.
Lifecycle adapters delegate gameplay to the owning audience/domain service.
Effective alignment and downstream SparkFactionAPI resolution use live traits,
with death snapshots preserving round-end meaning after active traits clear.

Player and world components own state, persistence, and synchronization; they
do not define downstream API. `SparkTraitsApi` is the supported query facade.

## Gameplay Language

**Bomb Maniac Mode / 炸弹狂模式**:
A 20-second Conscience Bomber shop mode that grants one temporary hand grenade
with unlimited uses, no item cooldown, and 1.5 times the ordinary throw distance.
_Avoid_: 15-second mode, timed bomb

Its TNT-icon, 350-coin shop purchase starts a dedicated three-minute cooldown
immediately; the mode expires with two minutes and forty seconds remaining.

The hand grenade remains the impact-triggered Wathe grenade. The NoellesRoles
timed bomb carried and transferred between players is a separate mechanic.
Bomb Maniac grenades do not kill effectively civilian players, including the
Conscience Bomber. Effective killers, neutral players, and the exact SparkWitch
roles `sparkwitch:grand_witch`, `sparkwitch:accomplice`, and
`sparkwitch:murderous_witch` remain vulnerable. `sparkwitch:apprentice_witch`
is not part of this exception and follows its ordinary effective alignment.
Bomb Maniac properties are fixed when the grenade is thrown. Expiry removes
the temporary held grenade but does not downgrade an in-flight grenade.
The temporary grenade is marked and bound to its purchaser. Ordinary grenades
remain unchanged; the temporary item cannot be transferred and is removed when
it is dropped, the mode expires, its owner truly dies or resets, the owner
reconnects, or the round ends. A Last Stand false death does not remove it.

**Conscience death dividend / 善良死亡分红**:
An independent 50-coin award paid to a Conscience killer when a player dies,
stacking with the owner's existing direct-kill or Serial Killer reward.

A dividend requires another player's confirmed real death while the Conscience
owner is alive. Last Stand false death, the owner's own death, and deaths after
the owner has died do not pay it; cause and faction otherwise do not matter.

**Conscience passive income / 善良被动收入**:
The normal Wathe killer income cadence granted to every living Conscience
killer until the standard 200-coin passive-income threshold.

**Cornered teammate payout / 走投无路同伙奖励**:
A Cornered owner receives 75 coins for an ordinary teammate death, or one
combined 200-coin payout when that death leaves the owner as the sole teammate.
The two amounts never stack on the same death.

**Demon Hunter police-trait boundary / 猎魔人警类词条边界**:
The Demon Hunter does not roll Vigilante-only or Veteran-only traits. Its pistol
is not adapted to police-trait weapon behavior.

**Faction-scoped roll preference / 阵营专属词条倾向**:
After eligibility filtering, every non-`UNIVERSAL` trait candidate has 1.5 times
its base random-roll weight for an eligible player. Universal candidates keep
their base weight. This changes neither slot chance nor locks, forced
assignments, eligibility, or other non-random assignment paths.

**Showman witnesses / 作秀围观者**:
On a real kill, Showman pays 8 coins for each other living player within 8
blocks of the victim, regardless of faction, up to 10 players. Neither the
victim nor the killer counts.

**Bloodthirsty stack / 嗜血层数**:
Each confirmed owner kill reduces the knife cooldown by 5 percent per counted
stack. Counted stacks remain capped at `floor(round player count / 3)`.

**Plunderer share / 掠夺者份额**:
On a confirmed owner kill, Plunderer transfers `floor(victim balance / 3)` to
the owner, with no minimum one-coin transfer.

**Paranoid psycho bonus / 偏执狂疯魔加成**:
Each successful Psycho Mode start keeps Paranoid's existing 20-second duration
extension and adds one armour layer to the current Psycho Mode armour value.

## Stable Contracts

- Component ids: `sparktraits:traits` and `sparktraits:world`.
- Trait identifiers and localized presentation are part of saved/server-client
  behavior and must not be changed incidentally.
- Regular clients may receive public rendering/instinct flags, but never another
  player's active, pending, or revealed trait identifiers.
- Spectator/creative clients may inspect active trait state; owners may inspect
  their own owner-visible and revealed state.
- Packet field order, NBT keys, kill/reset/finalize ordering, and optional bridge
  fallbacks are compatibility contracts.
- Replay records only match-defining runtime transitions: successful Last Stand,
  Final Moment start, and actual conversion to `wathe:loose_end`. Startup
  effective-alignment flips and compensation assignments are excluded.
- `sparktraits:arrogant_asf` is retired. Component state filters that exact id,
  while player sync field 17 remains a written-false/read-and-discard tombstone.

## Integrations

Wathe and NoellesRoles are hard gameplay dependencies. SparkFactionAPI is an
optional effective-faction bridge. Simple Voice Chat is an optional plugin used
for Depression psycho voice suppression. SparkWitch and SparkStrength are known
downstream consumers and should query the public facade rather than internals.
