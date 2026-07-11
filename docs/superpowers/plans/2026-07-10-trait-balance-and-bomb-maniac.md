# Trait Balance and Bomb Maniac Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Apply the approved Conscience and killer-trait balance changes, add the Conscience Bomber's 20-second Bomb Maniac shop mode using Wathe's ordinary grenade, and boost eligible faction-scoped traits only during random weighted selection.

**Architecture:** Keep pure numbers and predicates in focused rule helpers, keep `KillerTraitService` changes surgical, and add separate Conscience economy and Bomber services for their own runtime state. Thin mixins adapt Wathe's ordinary grenade and item-cooldown gates; no new item, component id, packet, player/world NBT field, or downstream API is introduced.

**Tech Stack:** Java 21, Fabric Loader/API, Yarn 1.21.1 mappings, Cardinal Components API, Wathe, NoellesRoles, Sponge Mixin, JUnit 5, Gradle.

## Global Constraints

- Read `ARCHITECTURE.md` and `CONTEXT.md` again immediately before implementation; the approval gate below is binding.
- Work in `/Users/kricy/Documents/Codex-Projects/SparkTraits` with Java 21.
- Preserve the dirty worktree. In particular, do not revert or rewrite unrelated Final Moment, Thrust, Cautious, Depression, Arrogant ASF retirement, build, version, or resource changes already present.
- Do not give Demon Hunter Vigilante-only or Veteran-only traits. Do not change its pistol behavior.
- Bomb Maniac is available only to a NoellesRoles Bomber who has Conscience.
- Its shop entry id is `sparktraits:bomb_maniac`, its display item is TNT, its price is `350`, and its shop cooldown is `3600` ticks (3 minutes), starting only after a successful purchase.
- A successful purchase grants one privately marked `WatheItems.GRENADE`, not a new item id. The mode lasts `400` ticks (20 seconds).
- During those 400 ticks, only that marked grenade is unlimited and ignores item cooldown; ordinary grenade stacks retain Wathe/NoellesRoles behavior.
- Bomb Maniac throw speed is `0.75F`, exactly 1.5 times Wathe's ordinary `0.5F` speed.
- Bomb Maniac launch properties are snapshotted on the projectile. A grenade thrown before expiry remains special after expiry.
- Effective civilians are protected from Bomb Maniac explosions except exact role ids `sparkwitch:grand_witch`, `sparkwitch:accomplice`, and `sparkwitch:murderous_witch`. `sparkwitch:apprentice_witch` follows effective alignment and is not an exception.
- The marked grenade is owner-bound and disappears on manual drop, true death, reset, expiry, reconnect cleanup, or round finalization. A Last Stand false death must not remove it. In-flight grenades are not removed.
- Conscience death dividend is `50` coins for every other player's confirmed real death while the Conscience owner is alive. It stacks with existing direct rewards and ignores cause and faction.
- All Conscience killers use Wathe's existing passive income: `5` coins every `200` ticks while balance is below the existing `200` cap.
- Bloodthirsty changes only from 3 percent to 5 percent per counted stack; retain `floor(round player count / 3)` as the stack cap.
- Plunderer transfers `floor(victim balance / 3)` with no minimum.
- Showman pays `8` coins per other living player within 8 blocks of the victim, excludes victim and killer, and caps at 10 witnesses.
- Cornered pays one amount per teammate death: `75` normally or a combined total of `200` when that death leaves the owner as the sole teammate. Do not add `75 + 200`.
- Paranoid retains its existing 400-tick Psycho duration extension and adds one layer to the current Psycho armour on each successful start.
- After eligibility filtering, random selection uses `trait.rollWeight() * 1.5D` for every `TraitAudience` other than `UNIVERSAL`; do not alter slot chance, eligibility, locks, forced assignment, compensation assignment, or the public base weight.
- Do not change the existing Conscience timed-bomb mechanic in `ConscienceBombService` or `BomberPlayerComponentMixin`.
- Do not add a SparkWitch compile/runtime dependency or raise any dependency floor.
- Optional commit checkpoints below must be skipped unless the owner explicitly requests commits. If used, stage only the named files.

---

## Required Architecture Approval

Implementation must not begin until the owner explicitly approves this exact template. A reply such as `同意，执行` is sufficient after reviewing it.

- **Area:** Surgical changes inside watch-only `impl/traits/killer/KillerTraitService.java`; confirmed-real-death and cleanup ordering in lifecycle coordinator `impl/lifecycle/TraitGameHooks.java`; removal of the Conscience passive-income redirect in `mixin/MurderGameModeMixin.java`; new Conscience economy/Bomb Maniac services; thin Wathe grenade and client/server cooldown adapters; internal `TraitSelector` random weight calculation.
- **Reason:** The approved values and Bomb Maniac behavior cannot be expressed through current configuration or public hooks. The death dividend must be placed after Last Stand's early return, and the ordinary grenade needs stack-specific behavior despite Minecraft cooldowns being item-wide.
- **Old scope:** Existing Bloodthirsty, Showman, Plunderer, Paranoid, and Cornered constants/helpers in `KillerTraitService`; current `TraitGameHooks` Last Stand return and real-death consequence order; current `MurderGameModeMixin.sparktraits$passiveMoneyOnlyForRealKillers`; Wathe `GrenadeItem.use` and `GrenadeEntity.onCollision`; `TraitSelector.pickWeighted` using raw `rollWeight()`.
- **New shape:** Numeric killer-trait policy remains in `KillerTraitService`; `ConscienceEconomyService` owns the independent death dividend; Wathe resumes ownership of passive-income cadence/cap; `ConscienceBomberFrenzyRules` owns pure values/target policy; `ConscienceBomberFrenzyService` owns shop, marker, runtime expiry, throw reproduction, and cleanup; mixins only capture external calls and delegate; `TraitSelector.randomSelectionWeight(Trait)` remains package-private and internal.
- **Forbidden scope:** Demon Hunter; Vigilante/Veteran eligibility or weapon behavior; unrelated roles/traits; Conscience timed bombs; existing 100/150/200 direct-kill rewards and punishment rules; Cornered teammate definition; public API; trait/component ids; packets; player/world NBT; dependency metadata/floors; and unrelated dirty Final Moment/Thrust changes.
- **Invariants:** Preserve all exact existing ids; `BloodthirstyKillCount` and `CorneredLastKillerRewardPaid` persistence; component and packet schemas; Last Stand false-death early return; established lifecycle order; normal Wathe grenade consumption/cooldown; NoellesRoles Bomber 90-second normal-grenade cooldown; timed-bomb behavior; kill attribution/death reason; optional-mod fallback behavior.
- **Downstream impact:** None. No downstream import/reflection migration, dependency floor change, metadata coordination, or release coordination is required.
- **Verification:** Focused JUnit tests named below; Java 21 `test`, `verifyArchitecture`, and `clean build`; mixin descriptor inspection with `javap`; jar content/version inspection; `runClient` smoke tests covering purchase, cooldown coexistence, target protection, expiry, Last Stand, and cleanup; final scoped diff review against the dirty baseline.

## File Map

**Create**

- `src/main/java/dev/caecorthus/sparktraits/impl/traits/killer/conscience/ConscienceEconomyService.java`: independent real-death dividend.
- `src/main/java/dev/caecorthus/sparktraits/impl/traits/killer/conscience/ConscienceBomberFrenzyRules.java`: pure Bomb Maniac constants, eligibility, expiry, and target policy.
- `src/main/java/dev/caecorthus/sparktraits/impl/traits/killer/conscience/ConscienceBomberFrenzyService.java`: shop registration, marked ordinary grenade, active-mode map, throw reproduction, cooldown snapshot/restore, and cleanup.
- `src/main/java/dev/caecorthus/sparktraits/impl/traits/killer/conscience/BombManiacGrenadeAccess.java`: runtime-only projectile marker interface.
- `src/main/java/dev/caecorthus/sparktraits/mixin/ConscienceBomberGrenadeItemMixin.java`: marked-stack throw adapter.
- `src/main/java/dev/caecorthus/sparktraits/mixin/ConscienceBomberGrenadeEntityMixin.java`: launch snapshot and explosion target adapter.
- `src/main/java/dev/caecorthus/sparktraits/mixin/BombManiacServerInteractionMixin.java`: stack-aware server cooldown gate bypass.
- `src/main/java/dev/caecorthus/sparktraits/mixin/ItemCooldownManagerAccessor.java`: exact cooldown entry/tick access and restore notification.
- `src/main/java/dev/caecorthus/sparktraits/mixin/ItemCooldownEntryAccessor.java`: reads the private cooldown entry end tick.
- `src/client/java/dev/caecorthus/sparktraits/client/mixin/BombManiacClientInteractionMixin.java`: stack-aware client prediction gate bypass.
- `src/client/java/dev/caecorthus/sparktraits/client/mixin/BombManiacDrawContextMixin.java`: hides vanilla cooldown shading only for the marked stack.
- `src/client/java/dev/caecorthus/sparktraits/client/mixin/BombManiacCooldownRendererMixin.java`: hides Wathe's selected-slot cooldown text only for the marked stack.
- `src/test/java/dev/caecorthus/sparktraits/impl/selection/TraitSelectorTest.java`: faction-scoped random-weight tests.
- `src/test/java/dev/caecorthus/sparktraits/impl/traits/killer/KillerTraitServiceTest.java`: balance-rule tests.
- `src/test/java/dev/caecorthus/sparktraits/impl/traits/killer/conscience/ConscienceEconomyServiceTest.java`: dividend and passive-income contract tests.
- `src/test/java/dev/caecorthus/sparktraits/impl/traits/killer/conscience/ConscienceBomberFrenzyRulesTest.java`: Bomb Maniac pure-rule and marker tests.
- `src/test/java/dev/caecorthus/sparktraits/impl/traits/killer/conscience/BombManiacMixinContractTest.java`: source/JSON contract test for fragile mixin boundaries.
- `src/test/java/dev/caecorthus/sparktraits/impl/lifecycle/TraitGameHooksDeathOrderContractTest.java`: Last Stand and true-death ordering contract.

**Modify**

- `src/main/java/dev/caecorthus/sparktraits/impl/selection/TraitSelector.java`
- `src/main/java/dev/caecorthus/sparktraits/impl/traits/killer/KillerTraitService.java`
- `src/main/java/dev/caecorthus/sparktraits/impl/traits/killer/conscience/ConscienceSerialKillerService.java`
- `src/main/java/dev/caecorthus/sparktraits/impl/effective/economy/EffectiveEconomyRules.java`
- `src/main/java/dev/caecorthus/sparktraits/impl/lifecycle/TraitGameHooks.java`
- `src/main/java/dev/caecorthus/sparktraits/mixin/MurderGameModeMixin.java`
- `src/main/java/dev/caecorthus/sparktraits/mixin/PlayerEntityMixin.java`
- `src/main/java/dev/caecorthus/sparktraits/mixin/ItemEntityMixin.java`
- `src/main/resources/sparktraits.mixins.json`
- `src/client/resources/sparktraits.client.mixins.json`
- `src/main/resources/assets/sparktraits/lang/en_us.json`
- `src/main/resources/assets/sparktraits/lang/zh_cn.json`
- `ARCHITECTURE.md`
- `CONTEXT.md` only if implementation discoveries require correcting the already recorded final definitions; do not rewrite unrelated sections.

## Task 1: Faction-Scoped Random Weight

**Files:**
- Modify: `src/main/java/dev/caecorthus/sparktraits/impl/selection/TraitSelector.java`
- Create: `src/test/java/dev/caecorthus/sparktraits/impl/selection/TraitSelectorTest.java`

**Interfaces:**
- Consumes: `Trait.rollWeight()` and `Trait.audience()` after candidate eligibility filtering.
- Produces: package-private `static double randomSelectionWeight(Trait trait)` used only by `pickWeighted`.

- [ ] **Step 1: Write the failing tests**

Create tests with a tiny fake trait and deterministic `Random`:

```java
private static Trait trait(String path, double weight, TraitAudience audience) {
    return new Trait() {
        @Override public Identifier id() { return Identifier.of("test", path); }
        @Override public int color() { return 0; }
        @Override public double rollWeight() { return weight; }
        @Override public TraitAudience audience() { return audience; }
    };
}

@Test
void randomSelectionWeightChangesOnlyNonUniversalCandidates() {
    assertEquals(100.0D, TraitSelector.randomSelectionWeight(trait("u", 100.0D, TraitAudience.UNIVERSAL)));
    assertEquals(150.0D, TraitSelector.randomSelectionWeight(trait("k", 100.0D, TraitAudience.KILLER_ONLY)));
    assertEquals(18.75D, TraitSelector.randomSelectionWeight(trait("i", 12.5D, TraitAudience.INNOCENT_ONLY)));
    assertEquals(150.0D, TraitSelector.randomSelectionWeight(trait("n", 100.0D, TraitAudience.NEUTRAL_ONLY)));
    assertEquals(12.5D, trait("base", 12.5D, TraitAudience.KILLER_ONLY).rollWeight());
}

@Test
void weightedPickUsesBoostWithoutChangingSlotChance() {
    Trait faction = trait("faction", 100.0D, TraitAudience.KILLER_ONLY);
    Trait universal = trait("universal", 100.0D, TraitAudience.UNIVERSAL);
    Random fixed = new Random(0L) {
        @Override public double nextDouble() { return 0.55D; }
        @Override public float nextFloat() { return 0.749F; }
    };
    assertSame(faction, TraitSelector.pickWeighted(List.of(faction, universal), fixed));
    assertTrue(TraitSelector.shouldRollSlot(0.75F, fixed));
    Random boundary = new Random(0L) { @Override public float nextFloat() { return 0.75F; } };
    assertFalse(TraitSelector.shouldRollSlot(0.75F, boundary));
}
```

- [ ] **Step 2: Run the focused test and confirm the intended failure**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew test \
  --tests dev.caecorthus.sparktraits.impl.selection.TraitSelectorTest
```

Expected: compilation fails because `randomSelectionWeight(Trait)` does not exist, or the deterministic pick selects the universal trait before the implementation.

- [ ] **Step 3: Implement the minimal internal multiplier**

Add `TraitAudience` import and this helper, then use it for both the total and subtraction in `pickWeighted`:

```java
private static final double FACTION_SCOPED_ROLL_MULTIPLIER = 1.5D;

/** Changes random-choice weight only; the public base weight remains unchanged.
 *  仅调整随机候选权重，不改变公开基础权重。 */
static double randomSelectionWeight(Trait trait) {
    double baseWeight = trait.rollWeight();
    return trait.audience() == TraitAudience.UNIVERSAL
            ? baseWeight
            : baseWeight * FACTION_SCOPED_ROLL_MULTIPLIER;
}
```

```java
for (Trait candidate : candidates) {
    totalWeight += randomSelectionWeight(candidate);
}
double roll = random.nextDouble() * totalWeight;
for (Trait candidate : candidates) {
    roll -= randomSelectionWeight(candidate);
    if (roll < 0.0D) {
        return candidate;
    }
}
```

- [ ] **Step 4: Re-run the focused test**

Run the command from Step 2. Expected: PASS, including the `0.75` slot boundary assertions.

- [ ] **Step 5: Optional scoped commit checkpoint**

```bash
git add src/main/java/dev/caecorthus/sparktraits/impl/selection/TraitSelector.java \
  src/test/java/dev/caecorthus/sparktraits/impl/selection/TraitSelectorTest.java
git commit -m "feat: boost faction-scoped trait rolls"
```

## Task 2: Killer Trait Balance Rules

**Files:**
- Modify: `src/main/java/dev/caecorthus/sparktraits/impl/traits/killer/KillerTraitService.java`
- Create: `src/test/java/dev/caecorthus/sparktraits/impl/traits/killer/KillerTraitServiceTest.java`

**Interfaces:**
- Produces: `showmanReward(int)`, `plunderedAmount(int)`, `paranoidPsychoTicks(int)`, package-private `paranoidPsychoArmour(int)`, `corneredReward(boolean)`, and `shouldCountShowmanWitness(boolean, boolean, boolean, boolean)`.
- Preserves: `isCorneredTeamMember`, kill-count persistence, last-killer-paid persistence, Second Strike, Thrust, Charisma, Oppressive, and unrelated dirty hunks.

- [ ] **Step 1: Record and protect the pre-existing dirty diff**

```bash
git diff -- src/main/java/dev/caecorthus/sparktraits/impl/traits/killer/KillerTraitService.java \
  > /tmp/sparktraits-killer-service-before-balance.patch
```

Expected: the file may already contain unrelated Final Moment/Thrust work. Treat this patch as review evidence, not something to apply or revert.

- [ ] **Step 2: Write failing pure-rule tests**

```java
@Test
void bloodthirstyUsesFivePercentAndKeepsPlayerCap() {
    assertEquals(900, KillerTraitService.bloodthirstyCooldown(1000, 2, 17));
    assertEquals(750, KillerTraitService.bloodthirstyCooldown(1000, 10, 17));
    assertEquals(1000, KillerTraitService.bloodthirstyCooldown(1000, 3, 2));
}

@Test
void showmanPaysEightForOtherLivingWitnessesUpToTen() {
    assertEquals(72, KillerTraitService.showmanReward(9));
    assertEquals(80, KillerTraitService.showmanReward(11));
    assertEquals(0, KillerTraitService.showmanReward(-1));
    assertTrue(KillerTraitService.shouldCountShowmanWitness(false, false, true, true));
    assertFalse(KillerTraitService.shouldCountShowmanWitness(true, false, true, true));
    assertFalse(KillerTraitService.shouldCountShowmanWitness(false, true, true, true));
    assertFalse(KillerTraitService.shouldCountShowmanWitness(false, false, false, true));
    assertFalse(KillerTraitService.shouldCountShowmanWitness(false, false, true, false));
}

@Test
void plundererTakesFlooredThirdWithoutMinimum() {
    assertEquals(33, KillerTraitService.plunderedAmount(99));
    assertEquals(33, KillerTraitService.plunderedAmount(100));
    assertEquals(0, KillerTraitService.plunderedAmount(2));
    assertEquals(0, KillerTraitService.plunderedAmount(-10));
}

@Test
void paranoidAddsDurationAndOneCurrentArmourLayer() {
    assertEquals(1000, KillerTraitService.paranoidPsychoTicks(600));
    assertEquals(2, KillerTraitService.paranoidPsychoArmour(1));
    assertEquals(3, KillerTraitService.paranoidPsychoArmour(2));
}

@Test
void corneredUsesOneCombinedReward() {
    assertEquals(75, KillerTraitService.corneredReward(false));
    assertEquals(200, KillerTraitService.corneredReward(true));
}
```

- [ ] **Step 3: Run the focused tests and confirm old values fail**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew test \
  --tests dev.caecorthus.sparktraits.impl.traits.killer.KillerTraitServiceTest
```

Expected: failures show 3 percent, 5 coins, one quarter, missing armour helper, and old Cornered totals.

- [ ] **Step 4: Replace only the approved constants and pure helpers**

```java
public static final int SHOWMAN_MONEY_PER_PLAYER = 8;
public static final int CORNERED_TEAMMATE_REWARD = 75;
public static final int CORNERED_LAST_KILLER_REWARD = 200;

public static int bloodthirstyCooldown(int duration, int killCount, int totalPlayers) {
    if (duration <= 0) {
        return duration;
    }
    int stacks = Math.min(Math.max(0, killCount), Math.max(0, totalPlayers / 3));
    if (stacks <= 0) {
        return duration;
    }
    float multiplier = 1.0f - stacks * 0.05f;
    return Math.max(1, (int) (duration * multiplier));
}

public static int plunderedAmount(int victimBalance) {
    return Math.max(0, victimBalance) / 3;
}

static int paranoidPsychoArmour(int currentArmour) {
    return currentArmour + 1;
}

static int corneredReward(boolean becomesSoleTeammate) {
    return becomesSoleTeammate ? CORNERED_LAST_KILLER_REWARD : CORNERED_TEAMMATE_REWARD;
}

static boolean shouldCountShowmanWitness(
        boolean sameAsVictim,
        boolean sameAsKiller,
        boolean playingAndAlive,
        boolean withinRange
) {
    return !sameAsVictim && !sameAsKiller && playingAndAlive && withinRange;
}
```

- [ ] **Step 5: Adapt the runtime call sites without changing ownership**

In `PsychoModeEvents.ON_PSYCHO_START`, keep the duration extension and add the current armour layer:

```java
PlayerPsychoComponent psycho = PlayerPsychoComponent.KEY.get(player);
psycho.setPsychoTicks(paranoidPsychoTicks(psycho.getPsychoTicks()));
psycho.setArmour(paranoidPsychoArmour(psycho.getArmour()));
```

Pass `killer` into Showman's count and apply the helper for each candidate:

```java
int reward = showmanReward(countNearbyAlivePlayers(victim, killer));
```

```java
boolean sameAsVictim = player.getUuid().equals(victim.getUuid());
boolean sameAsKiller = killer != null && player.getUuid().equals(killer.getUuid());
boolean alive = game.hasAnyRole(player) && !game.isPlayerDead(player.getUuid());
boolean withinRange = player.squaredDistanceTo(victim) <= rangeSquared;
if (shouldCountShowmanWitness(sameAsVictim, sameAsKiller, alive, withinRange)) {
    count++;
}
```

Replace the two additive Cornered awards with one computed award and mark only the sole-teammate case:

```java
boolean becomesSoleTeammate = aliveTeamMembers == 1
        && isCorneredTeamMember(game.getRole(player), traits.getActiveTraitIds())
        && !traits.hasCorneredLastKillerRewardPaid();
PlayerShopComponent.KEY.get(player).addToBalance(corneredReward(becomesSoleTeammate));
if (becomesSoleTeammate) {
    traits.markCorneredLastKillerRewardPaid();
}
```

- [ ] **Step 6: Re-run tests and inspect the surgical diff**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew test \
  --tests dev.caecorthus.sparktraits.impl.traits.killer.KillerTraitServiceTest
git diff -- src/main/java/dev/caecorthus/sparktraits/impl/traits/killer/KillerTraitService.java
```

Expected: PASS. Every new hunk traces to the approved five traits; existing unrelated dirty hunks remain intact.

- [ ] **Step 7: Optional scoped commit checkpoint**

```bash
git add src/main/java/dev/caecorthus/sparktraits/impl/traits/killer/KillerTraitService.java \
  src/test/java/dev/caecorthus/sparktraits/impl/traits/killer/KillerTraitServiceTest.java
git commit -m "feat: rebalance killer traits"
```

## Task 3: Conscience Death Dividend and Passive Income

**Files:**
- Create: `src/main/java/dev/caecorthus/sparktraits/impl/traits/killer/conscience/ConscienceEconomyService.java`
- Create: `src/test/java/dev/caecorthus/sparktraits/impl/traits/killer/conscience/ConscienceEconomyServiceTest.java`
- Create: `src/test/java/dev/caecorthus/sparktraits/impl/lifecycle/TraitGameHooksDeathOrderContractTest.java`
- Modify: `src/main/java/dev/caecorthus/sparktraits/impl/lifecycle/TraitGameHooks.java`
- Modify: `src/main/java/dev/caecorthus/sparktraits/mixin/MurderGameModeMixin.java`
- Modify: `src/main/java/dev/caecorthus/sparktraits/impl/traits/killer/conscience/ConscienceSerialKillerService.java`
- Modify: `src/main/java/dev/caecorthus/sparktraits/impl/effective/economy/EffectiveEconomyRules.java`

**Interfaces:**
- Produces: `ConscienceEconomyService.deathDividend(boolean, boolean, boolean, boolean)` and `rewardAfterConfirmedRealDeath(ServerPlayerEntity)`.
- Preserves: current direct rewards from `EffectiveTraitService`/`ConscienceSerialKillerService`, including 100 ordinary, 150 Serial Killer, and 200 murderer-target rewards before adding the independent 50.

- [ ] **Step 1: Write failing dividend and dependency-contract tests**

```java
@Test
void dividendRequiresAnotherPlayersConfirmedDeathAndLivingConscienceOwner() {
    assertEquals(50, ConscienceEconomyService.deathDividend(true, true, true, false));
    assertEquals(0, ConscienceEconomyService.deathDividend(false, true, true, false));
    assertEquals(0, ConscienceEconomyService.deathDividend(true, false, true, false));
    assertEquals(0, ConscienceEconomyService.deathDividend(true, true, false, false));
    assertEquals(0, ConscienceEconomyService.deathDividend(true, true, true, true));
}

@Test
void existingDirectRewardsRemainIndependent() {
    assertEquals(100, ConscienceSerialKillerService.conscienceKillReward(false, true, false));
    assertEquals(150, ConscienceSerialKillerService.conscienceKillReward(true, true, false));
    assertEquals(200, ConscienceSerialKillerService.conscienceKillReward(true, true, true));
    assertEquals(200, 150 + ConscienceEconomyService.DEATH_DIVIDEND);
    assertEquals(250, 200 + ConscienceEconomyService.DEATH_DIVIDEND);
}

@Test
void wathePassiveIncomeContractIsFiveEveryTenSecondsWithExistingCap() {
    assertEquals(5, GameConstants.PASSIVE_MONEY_TICKER.apply(200L));
    assertEquals(0, GameConstants.PASSIVE_MONEY_TICKER.apply(201L));
    assertEquals(200, GameConstants.KILLER_PASSIVE_MONEY_CAP);
}
```

Add a source contract to the same test class:

```java
@Test
void sparkTraitsDoesNotOverrideWathePassiveKillerEligibility() throws IOException {
    String source = Files.readString(Path.of(
            "src/main/java/dev/caecorthus/sparktraits/mixin/MurderGameModeMixin.java"));
    assertFalse(source.contains("sparktraits$passiveMoneyOnlyForRealKillers"));
    assertFalse(source.contains("shouldReceiveKillerPassiveMoney"));
}
```

- [ ] **Step 2: Add the lifecycle-order failing test**

```java
@Test
void dividendRunsOnlyAfterLastStandReturnsAndBeforeKillerConsequences() throws IOException {
    String source = Files.readString(Path.of(
            "src/main/java/dev/caecorthus/sparktraits/impl/lifecycle/TraitGameHooks.java"));
    int lastStandReturn = source.indexOf("if (lastStandStarted)");
    int dividend = source.indexOf("ConscienceEconomyService.rewardAfterConfirmedRealDeath(victim)");
    int killerConsequences = source.indexOf("KillerTraitService.handleAfterRealKill");
    assertTrue(lastStandReturn >= 0 && dividend > lastStandReturn);
    assertTrue(killerConsequences > dividend);
}
```

- [ ] **Step 3: Run the focused tests and confirm failures**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew test \
  --tests dev.caecorthus.sparktraits.impl.traits.killer.conscience.ConscienceEconomyServiceTest \
  --tests dev.caecorthus.sparktraits.impl.lifecycle.TraitGameHooksDeathOrderContractTest
```

Expected: missing service, existing passive redirect still present, and missing lifecycle call.

- [ ] **Step 4: Add the focused economy owner**

```java
public final class ConscienceEconomyService {
    public static final int DEATH_DIVIDEND = 50;

    private ConscienceEconomyService() {
    }

    static int deathDividend(
            boolean confirmedRealDeath,
            boolean ownerHasConscience,
            boolean ownerPlayingAndAlive,
            boolean ownerIsVictim
    ) {
        return confirmedRealDeath && ownerHasConscience && ownerPlayingAndAlive && !ownerIsVictim
                ? DEATH_DIVIDEND
                : 0;
    }

    public static void rewardAfterConfirmedRealDeath(ServerPlayerEntity victim) {
        if (!(victim.getWorld() instanceof ServerWorld world)) {
            return;
        }
        for (ServerPlayerEntity owner : world.getPlayers()) {
            int reward = deathDividend(
                    true,
                    EffectiveTraitService.hasConscience(owner),
                    GameFunctions.isPlayerPlayingAndAlive(owner),
                    owner.getUuid().equals(victim.getUuid())
            );
            if (reward > 0) {
                PlayerShopComponent.KEY.get(owner).addToBalance(reward);
            }
        }
    }
}
```

- [ ] **Step 5: Place the dividend after the Last Stand early return**

In `TraitGameHooks`:

```java
if (lastStandStarted) {
    syncPlayerTraitsToNewSpectators((ServerWorld) victim.getWorld(), GameWorldComponent.KEY.get(victim.getWorld()));
    return;
}
ConscienceEconomyService.rewardAfterConfirmedRealDeath(victim);
KillerTraitService.handleAfterRealKill(victim, killer, deathReason);
```

Do not move or replace `EffectiveTraitService.handleAfterKill`; its existing direct rewards remain independent and therefore stack.

- [ ] **Step 6: Restore Wathe's passive path by deleting only the override**

Remove the `tickServerGameLoop` `@Redirect` named `sparktraits$passiveMoneyOnlyForRealKillers` and its now-unused imports from `MurderGameModeMixin`.

Delete only these obsolete helpers:

```java
// ConscienceSerialKillerService
shouldReceivePassiveMoney(boolean, boolean)
shouldReceiveKillerPassiveMoney(boolean, boolean, boolean, boolean)
shouldReceivePassiveMoney(GameWorldComponent, PlayerEntity)
shouldReceiveKillerPassiveMoney(GameWorldComponent, PlayerEntity)

// EffectiveEconomyRules
shouldReceiveKillerPassiveMoney(boolean, boolean, boolean, boolean)
```

Keep target validity, target highlighting, psycho price, and direct reward methods untouched.

- [ ] **Step 7: Re-run focused tests**

Run the command from Step 3. Expected: PASS. Search must show no passive-income override:

```bash
rg -n "shouldReceiveKillerPassiveMoney|passiveMoneyOnlyForRealKillers" src/main/java src/test/java
```

Expected: no production matches.

- [ ] **Step 8: Optional scoped commit checkpoint**

```bash
git add src/main/java/dev/caecorthus/sparktraits/impl/traits/killer/conscience/ConscienceEconomyService.java \
  src/main/java/dev/caecorthus/sparktraits/impl/traits/killer/conscience/ConscienceSerialKillerService.java \
  src/main/java/dev/caecorthus/sparktraits/impl/effective/economy/EffectiveEconomyRules.java \
  src/main/java/dev/caecorthus/sparktraits/impl/lifecycle/TraitGameHooks.java \
  src/main/java/dev/caecorthus/sparktraits/mixin/MurderGameModeMixin.java \
  src/test/java/dev/caecorthus/sparktraits/impl/traits/killer/conscience/ConscienceEconomyServiceTest.java \
  src/test/java/dev/caecorthus/sparktraits/impl/lifecycle/TraitGameHooksDeathOrderContractTest.java
git commit -m "feat: expand conscience economy"
```

## Task 4: Bomb Maniac Rules, Shop, Marker, and Runtime State

**Files:**
- Create: `src/main/java/dev/caecorthus/sparktraits/impl/traits/killer/conscience/ConscienceBomberFrenzyRules.java`
- Create: `src/main/java/dev/caecorthus/sparktraits/impl/traits/killer/conscience/ConscienceBomberFrenzyService.java`
- Create: `src/test/java/dev/caecorthus/sparktraits/impl/traits/killer/conscience/ConscienceBomberFrenzyRulesTest.java`

**Interfaces:**
- Produces constants `MODE_DURATION_TICKS`, `SHOP_COOLDOWN_TICKS`, `PRICE`, `THROW_SPEED`; target predicate `shouldKillTarget(Identifier, boolean)`; marked-stack predicates; shop `register()`; lifecycle `clearPlayer`, `clearAll`, and `tickWorld`.
- Consumes: NoellesRoles `BOMBER`, Conscience effective trait query, Wathe `ShopEntry`, ordinary `WatheItems.GRENADE`, and vanilla `DataComponentTypes.CUSTOM_DATA`.

- [ ] **Step 1: Write failing rule tests**

```java
@Test
void constantsMatchApprovedMode() {
    assertEquals(400, ConscienceBomberFrenzyRules.MODE_DURATION_TICKS);
    assertEquals(3600, ConscienceBomberFrenzyRules.SHOP_COOLDOWN_TICKS);
    assertEquals(350, ConscienceBomberFrenzyRules.PRICE);
    assertEquals(0.75F, ConscienceBomberFrenzyRules.THROW_SPEED);
}

@Test
void onlyConscienceBomberCanBuy() {
    assertTrue(ConscienceBomberFrenzyRules.canBuy(true, true));
    assertFalse(ConscienceBomberFrenzyRules.canBuy(true, false));
    assertFalse(ConscienceBomberFrenzyRules.canBuy(false, true));
}

@Test
void protectionUsesEffectiveCivilianWithExactWitchExceptions() {
    assertFalse(ConscienceBomberFrenzyRules.shouldKillTarget(Identifier.of("wathe", "passenger"), true));
    assertFalse(ConscienceBomberFrenzyRules.shouldKillTarget(Identifier.of("sparkwitch", "apprentice_witch"), true));
    assertTrue(ConscienceBomberFrenzyRules.shouldKillTarget(Identifier.of("sparkwitch", "grand_witch"), true));
    assertTrue(ConscienceBomberFrenzyRules.shouldKillTarget(Identifier.of("sparkwitch", "accomplice"), true));
    assertTrue(ConscienceBomberFrenzyRules.shouldKillTarget(Identifier.of("sparkwitch", "murderous_witch"), true));
    assertTrue(ConscienceBomberFrenzyRules.shouldKillTarget(Identifier.of("wathe", "killer"), false));
    assertTrue(ConscienceBomberFrenzyRules.shouldKillTarget(Identifier.of("noellesroles", "neutral"), false));
}

@Test
void expiryIsExclusiveAtFourHundredTicks() {
    assertTrue(ConscienceBomberFrenzyRules.isActive(999L, 1000L));
    assertFalse(ConscienceBomberFrenzyRules.isActive(1000L, 1000L));
}
```

- [ ] **Step 2: Run tests and confirm missing-rule failure**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew test \
  --tests dev.caecorthus.sparktraits.impl.traits.killer.conscience.ConscienceBomberFrenzyRulesTest
```

Expected: compilation fails because the rule class does not exist.

- [ ] **Step 3: Add the pure rules**

```java
public final class ConscienceBomberFrenzyRules {
    public static final int MODE_DURATION_TICKS = 20 * 20;
    public static final int SHOP_COOLDOWN_TICKS = 3 * 60 * 20;
    public static final int PRICE = 350;
    public static final float THROW_SPEED = 0.5F * 1.5F;

    private static final Set<Identifier> VULNERABLE_WITCH_ROLES = Set.of(
            Identifier.of("sparkwitch", "grand_witch"),
            Identifier.of("sparkwitch", "accomplice"),
            Identifier.of("sparkwitch", "murderous_witch")
    );

    private ConscienceBomberFrenzyRules() {
    }

    static boolean canBuy(boolean bomber, boolean conscience) {
        return bomber && conscience;
    }

    static boolean isActive(long currentTick, long expiresAtTick) {
        return currentTick < expiresAtTick;
    }

    public static boolean shouldKillTarget(Identifier roleId, boolean effectiveCivilian) {
        return VULNERABLE_WITCH_ROLES.contains(roleId) || !effectiveCivilian;
    }
}
```

- [ ] **Step 4: Add marker codec tests before the service**

The service exposes package-private NBT helpers so the private marker is testable without booting a game:

```java
@Test
void markerRoundTripsOwnerAndExpiry() {
    UUID owner = UUID.fromString("00000000-0000-0000-0000-000000000123");
    NbtCompound nbt = new NbtCompound();
    ConscienceBomberFrenzyService.writeMarker(nbt, owner, 400L);
    assertEquals(new ConscienceBomberFrenzyService.Marker(owner, 400L),
            ConscienceBomberFrenzyService.readMarker(nbt));
}

@Test
void unrelatedCustomDataIsNotRecognized() {
    NbtCompound nbt = new NbtCompound();
    nbt.putString("other_mod", "keep");
    assertNull(ConscienceBomberFrenzyService.readMarker(nbt));
}
```

- [ ] **Step 5: Implement the owner-bound marker and active-state core**

Use these exact private keys and record:

```java
private static final String MARKER_KEY = "sparktraits_bomb_maniac";
private static final String OWNER_KEY = "sparktraits_bomb_maniac_owner";
private static final String EXPIRES_KEY = "sparktraits_bomb_maniac_expires";
private static final Map<UUID, Long> ACTIVE_UNTIL = new HashMap<>();

record Marker(UUID ownerUuid, long expiresAtTick) {
}

static void writeMarker(NbtCompound nbt, UUID ownerUuid, long expiresAtTick) {
    nbt.putBoolean(MARKER_KEY, true);
    nbt.putUuid(OWNER_KEY, ownerUuid);
    nbt.putLong(EXPIRES_KEY, expiresAtTick);
}

static @Nullable Marker readMarker(NbtCompound nbt) {
    if (!nbt.getBoolean(MARKER_KEY) || !nbt.containsUuid(OWNER_KEY) || !nbt.contains(EXPIRES_KEY)) {
        return null;
    }
    return new Marker(nbt.getUuid(OWNER_KEY), nbt.getLong(EXPIRES_KEY));
}
```

Mark the ordinary Wathe stack without adding a registered component:

```java
private static ItemStack markedGrenade(UUID ownerUuid, long expiresAtTick) {
    ItemStack stack = WatheItems.GRENADE.getDefaultStack();
    NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack,
            nbt -> writeMarker(nbt, ownerUuid, expiresAtTick));
    return stack;
}

public static boolean isMarkedGrenade(ItemStack stack) {
    NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
    return stack.isOf(WatheItems.GRENADE) && data != null && readMarker(data.copyNbt()) != null;
}

public static boolean canUseMarkedGrenade(PlayerEntity player, ItemStack stack) {
    NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
    Marker marker = data == null ? null : readMarker(data.copyNbt());
    if (!stack.isOf(WatheItems.GRENADE) || marker == null
            || !marker.ownerUuid().equals(player.getUuid())
            || !ConscienceBomberFrenzyRules.isActive(player.getWorld().getTime(), marker.expiresAtTick())) {
        return false;
    }
    return player.getWorld().isClient
            || ACTIVE_UNTIL.getOrDefault(player.getUuid(), Long.MIN_VALUE) == marker.expiresAtTick();
}
```

- [ ] **Step 6: Register the TNT shop entry and start mode atomically**

```java
public static final Identifier SHOP_ID = SparkTraits.id("bomb_maniac");

public static void register() {
    BuildShopEntries.EVENT.register(ConscienceBomberFrenzyService::addShopEntry);
    ServerTickEvents.END_WORLD_TICK.register(ConscienceBomberFrenzyService::tickWorld);
    ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> clearPlayer(handler.player));
}

private static void addShopEntry(PlayerEntity player, BuildShopEntries.ShopContext context) {
    GameWorldComponent game = GameWorldComponent.KEY.get(player.getWorld());
    boolean eligible = ConscienceBomberFrenzyRules.canBuy(
            game.isRole(player, Noellesroles.BOMBER),
            EffectiveTraitService.hasConscience(player)
    );
    if (!eligible || context.getEntries().stream().anyMatch(entry -> SHOP_ID.toString().equals(entry.id()))) {
        return;
    }
    ItemStack display = Items.TNT.getDefaultStack();
    display.set(DataComponentTypes.CUSTOM_NAME,
            Text.translatable("shop.sparktraits.bomb_maniac").formatted(Formatting.RED));
    context.addEntry(new ShopEntry.Builder(
            SHOP_ID.toString(), display, ConscienceBomberFrenzyRules.PRICE, ShopEntry.Type.WEAPON
    ).cooldown(ConscienceBomberFrenzyRules.SHOP_COOLDOWN_TICKS)
            .onBuy(candidate -> candidate instanceof ServerPlayerEntity serverPlayer && startMode(serverPlayer))
            .build());
}
```

`startMode` must check eligibility again on the server, reject an already active owner, find one empty slot among hotbar slots `0..8`, and only then write state and insert the marked grenade:

```java
static boolean startMode(ServerPlayerEntity player) {
    GameWorldComponent game = GameWorldComponent.KEY.get(player.getWorld());
    if (!ConscienceBomberFrenzyRules.canBuy(
            game.isRole(player, Noellesroles.BOMBER), EffectiveTraitService.hasConscience(player))
            || ACTIVE_UNTIL.containsKey(player.getUuid())) {
        return false;
    }
    int freeSlot = IntStream.range(0, 9)
            .filter(slot -> player.getInventory().getStack(slot).isEmpty())
            .findFirst().orElse(-1);
    if (freeSlot < 0) {
        return false;
    }
    long expiresAt = player.getWorld().getTime() + ConscienceBomberFrenzyRules.MODE_DURATION_TICKS;
    ACTIVE_UNTIL.put(player.getUuid(), expiresAt);
    player.getInventory().setStack(freeSlot, markedGrenade(player.getUuid(), expiresAt));
    return true;
}
```

Because Wathe charges and starts cooldown only when `onBuy` returns `true`, a duplicate activation or full hotbar consumes neither coins nor cooldown.

- [ ] **Step 7: Add deterministic expiry and cleanup**

Implement `clearPlayer`, `clearAll`, and `tickWorld` so they remove only marked grenades from main, armour, and offhand inventory lists. `tickWorld` must also delete a marked stack held by a non-owner or with mismatched expiry, retain at most one valid marked stack for an active owner, and remove the active map entry at `currentTick >= expiresAt`.

The central removal predicate is:

```java
private static void removeMarkedGrenades(PlayerEntity player) {
    for (List<ItemStack> inventory : List.of(
            player.getInventory().main,
            player.getInventory().armor,
            player.getInventory().offHand)) {
        for (int i = 0; i < inventory.size(); i++) {
            if (isMarkedGrenade(inventory.get(i))) {
                inventory.set(i, ItemStack.EMPTY);
            }
        }
    }
}

public static void clearPlayer(PlayerEntity player) {
    if (player == null) {
        return;
    }
    ACTIVE_UNTIL.remove(player.getUuid());
    removeMarkedGrenades(player);
}
```

`clearAll(ServerWorld world)` iterates `world.getPlayers()`, calls `clearPlayer`, then clears `ACTIVE_UNTIL`. Do not scan or discard `GrenadeEntity`; launch snapshots must survive.

- [ ] **Step 8: Re-run the rule/marker tests**

Run the command from Step 2. Expected: PASS.

## Task 5: Ordinary Grenade Throw, Cooldown Coexistence, and Explosion Policy

**Files:**
- Create all common/client mixins and `BombManiacGrenadeAccess` listed in the File Map.
- Modify: `src/main/java/dev/caecorthus/sparktraits/mixin/PlayerEntityMixin.java`
- Modify: `src/main/java/dev/caecorthus/sparktraits/mixin/ItemEntityMixin.java`
- Modify: `src/main/resources/sparktraits.mixins.json`
- Modify: `src/client/resources/sparktraits.client.mixins.json`
- Create: `src/test/java/dev/caecorthus/sparktraits/impl/traits/killer/conscience/BombManiacMixinContractTest.java`

**Interfaces:**
- Consumes: `ConscienceBomberFrenzyService.canUseMarkedGrenade`, `throwMarkedGrenade`, cooldown snapshot/restore, cleanup, and `ConscienceBomberFrenzyRules.shouldKillTarget`.
- Produces: runtime-only `BombManiacGrenadeAccess.sparktraits$setBombManiac(boolean)` and `sparktraits$isBombManiac()`.

- [ ] **Step 1: Lock the current dependency descriptors before writing mixins**

```bash
JAR="$HOME/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-common/1.21.1-net.fabricmc.yarn.1_21_1.1.21.1+build.3-v2/minecraft-common-1.21.1-net.fabricmc.yarn.1_21_1.1.21.1+build.3-v2.jar"
CLIENT_JAR="$HOME/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-clientonly/1.21.1-net.fabricmc.yarn.1_21_1.1.21.1+build.3-v2/minecraft-clientonly-1.21.1-net.fabricmc.yarn.1_21_1.1.21.1+build.3-v2.jar"
javap -classpath "$JAR" -c -p net.minecraft.server.network.ServerPlayerInteractionManager \
  | sed -n '/interactItem(/,/interactBlock(/p'
javap -classpath "$CLIENT_JAR:$JAR" -c -p net.minecraft.client.network.ClientPlayerInteractionManager \
  | sed -n '/method_41929(/,/method_41933(/p'
```

Expected server descriptor: `interactItem(ServerPlayerEntity, World, ItemStack, Hand)` with one `ItemCooldownManager.isCoolingDown(Item)` before `ItemStack.use`.

Expected client descriptor: `method_41929(Hand, PlayerEntity, MutableObject, int)` with one `ItemCooldownManager.isCoolingDown(Item)` before `ItemStack.use`. If either shape differs, stop and update the approval template rather than weakening injector `require` values.

- [ ] **Step 2: Write the failing mixin contract test**

```java
@Test
void mixinsAreRegisteredAndKeepAdaptersNarrow() throws IOException {
    String common = Files.readString(Path.of("src/main/resources/sparktraits.mixins.json"));
    String client = Files.readString(Path.of("src/client/resources/sparktraits.client.mixins.json"));
    assertTrue(common.contains("ConscienceBomberGrenadeItemMixin"));
    assertTrue(common.contains("ConscienceBomberGrenadeEntityMixin"));
    assertTrue(common.contains("BombManiacServerInteractionMixin"));
    assertTrue(common.contains("ItemCooldownManagerAccessor"));
    assertTrue(common.contains("ItemCooldownEntryAccessor"));
    assertTrue(client.contains("BombManiacClientInteractionMixin"));
    assertTrue(client.contains("BombManiacDrawContextMixin"));
    assertTrue(client.contains("BombManiacCooldownRendererMixin"));

    String itemMixin = Files.readString(Path.of(
            "src/main/java/dev/caecorthus/sparktraits/mixin/ConscienceBomberGrenadeItemMixin.java"));
    assertTrue(itemMixin.contains("priority = 500"));
    assertTrue(itemMixin.contains("snapshotGrenadeCooldown"));
    assertTrue(itemMixin.contains("restoreGrenadeCooldown"));
    assertFalse(itemMixin.contains("decrementUnlessCreative"));

    String service = Files.readString(Path.of(
            "src/main/java/dev/caecorthus/sparktraits/impl/traits/killer/conscience/ConscienceBomberFrenzyService.java"));
    assertTrue(service.contains("WatheItems.GRENADE"));
    assertTrue(service.contains("DataComponentTypes.CUSTOM_DATA"));
    assertFalse(service.contains("SparkTraitsDataComponentTypes"));
}
```

- [ ] **Step 3: Add exact cooldown snapshot/restore accessors**

```java
@Mixin(ItemCooldownManager.class)
public interface ItemCooldownManagerAccessor {
    @Accessor("entries") Map<Item, Object> sparktraits$getEntries();
    @Accessor("tick") int sparktraits$getTick();
    @Invoker("onCooldownUpdate") void sparktraits$notifyCooldownSet(Item item, int duration);
    @Invoker("onCooldownUpdate") void sparktraits$notifyCooldownRemoved(Item item);
}
```

```java
@Mixin(targets = "net.minecraft.entity.player.ItemCooldownManager$Entry")
public interface ItemCooldownEntryAccessor {
    @Accessor("endTick") int sparktraits$getEndTick();
}
```

In the service, snapshot the exact old map entry before throwing, then restore that object after all other `RETURN` injectors have run:

```java
public record CooldownSnapshot(@Nullable Object entry, int remainingTicks) {
}

public static CooldownSnapshot snapshotGrenadeCooldown(PlayerEntity player) {
    ItemCooldownManager manager = player.getItemCooldownManager();
    ItemCooldownManagerAccessor access = (ItemCooldownManagerAccessor) manager;
    Object entry = access.sparktraits$getEntries().get(WatheItems.GRENADE);
    int remaining = entry instanceof ItemCooldownEntryAccessor entryAccess
            ? Math.max(0, entryAccess.sparktraits$getEndTick() - access.sparktraits$getTick())
            : 0;
    return new CooldownSnapshot(entry, remaining);
}

public static void restoreGrenadeCooldown(PlayerEntity player, CooldownSnapshot snapshot) {
    ItemCooldownManager manager = player.getItemCooldownManager();
    ItemCooldownManagerAccessor access = (ItemCooldownManagerAccessor) manager;
    if (snapshot.entry() == null || snapshot.remainingTicks() <= 0) {
        access.sparktraits$getEntries().remove(WatheItems.GRENADE);
        access.sparktraits$notifyCooldownRemoved(WatheItems.GRENADE);
        return;
    }
    access.sparktraits$getEntries().put(WatheItems.GRENADE, snapshot.entry());
    access.sparktraits$notifyCooldownSet(WatheItems.GRENADE, snapshot.remainingTicks());
}
```

This preserves an already-running ordinary grenade cooldown without calling `set` again, so Fast Hands cannot shorten it a second time.

- [ ] **Step 4: Reproduce Wathe's ordinary throw with only approved differences**

`ConscienceBomberFrenzyService.throwMarkedGrenade` must copy Wathe's sound, entity owner/position, skin propagation, spawn, item-use recording, and stat increment. Its only behavioral differences are speed `0.75F`, projectile marker, no decrement, and no cooldown mutation:

```java
public static TypedActionResult<ItemStack> throwMarkedGrenade(World world, PlayerEntity user, Hand hand) {
    ItemStack stack = user.getStackInHand(hand);
    world.playSound(null, user.getX(), user.getY(), user.getZ(),
            WatheSounds.ITEM_GRENADE_THROW, SoundCategory.NEUTRAL,
            0.5F, 1F + (world.random.nextFloat() - 0.5F) / 10F);
    if (!world.isClient) {
        GrenadeEntity grenade = new GrenadeEntity(WatheEntities.GRENADE, world);
        grenade.setOwner(user);
        grenade.setPos(user.getX(), user.getEyeY() - 0.1, user.getZ());
        grenade.setVelocity(user, user.getPitch(), user.getYaw(), 0.0F,
                ConscienceBomberFrenzyRules.THROW_SPEED, 1.0F);
        ((BombManiacGrenadeAccess) grenade).sparktraits$setBombManiac(true);
        CosmeticComponent skin = stack.get(WatheDataComponentTypes.SKIN);
        if (skin != null && !"default".equals(skin.cosmeticId())) {
            ItemStack thrownStack = WatheItems.THROWN_GRENADE.getDefaultStack();
            thrownStack.set(WatheDataComponentTypes.SKIN, skin);
            grenade.setItem(thrownStack);
        }
        world.spawnEntity(grenade);
        if (user instanceof ServerPlayerEntity serverPlayer) {
            GameRecordManager.recordItemUse(serverPlayer,
                    Registries.ITEM.getId(WatheItems.GRENADE), null, null);
        }
    }
    user.incrementStat(Stats.USED.getOrCreateStat(WatheItems.GRENADE));
    return TypedActionResult.success(stack, world.isClient());
}
```

- [ ] **Step 5: Intercept only the marked grenade and restore cooldown after NoellesRoles**

```java
@Mixin(value = GrenadeItem.class, priority = 500)
public abstract class ConscienceBomberGrenadeItemMixin {
    @Unique
    private static final ThreadLocal<Deque<ConscienceBomberFrenzyService.CooldownSnapshot>>
            sparktraits$cooldownSnapshots = ThreadLocal.withInitial(ArrayDeque::new);

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void sparktraits$throwBombManiacGrenade(
            World world, PlayerEntity user, Hand hand,
            CallbackInfoReturnable<TypedActionResult<ItemStack>> cir
    ) {
        ItemStack stack = user.getStackInHand(hand);
        if (!ConscienceBomberFrenzyService.canUseMarkedGrenade(user, stack)) {
            return;
        }
        sparktraits$cooldownSnapshots.get().push(
                ConscienceBomberFrenzyService.snapshotGrenadeCooldown(user));
        cir.setReturnValue(ConscienceBomberFrenzyService.throwMarkedGrenade(world, user, hand));
    }

    @Inject(method = "use", at = @At("RETURN"))
    private void sparktraits$restoreOrdinaryGrenadeCooldown(
            World world, PlayerEntity user, Hand hand,
            CallbackInfoReturnable<TypedActionResult<ItemStack>> cir
    ) {
        Deque<ConscienceBomberFrenzyService.CooldownSnapshot> snapshots = sparktraits$cooldownSnapshots.get();
        if (snapshots.isEmpty()) {
            return;
        }
        ConscienceBomberFrenzyService.restoreGrenadeCooldown(user, snapshots.pop());
        if (snapshots.isEmpty()) {
            sparktraits$cooldownSnapshots.remove();
        }
    }
}
```

Priority `500` is intentionally lower than NoellesRoles' default `1000`, so this `RETURN` handler restores the prior state after its Bomber 90-second setter.

- [ ] **Step 6: Bypass item-wide cooldown gates with the exact interacted stack**

Server adapter:

```java
@Mixin(ServerPlayerInteractionManager.class)
public abstract class BombManiacServerInteractionMixin {
    @Redirect(
            method = "interactItem",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/ItemCooldownManager;isCoolingDown(Lnet/minecraft/item/Item;)Z")
    )
    private boolean sparktraits$allowMarkedGrenade(
            ItemCooldownManager manager, Item item,
            ServerPlayerEntity player, World world, ItemStack stack, Hand hand
    ) {
        return !ConscienceBomberFrenzyService.canUseMarkedGrenade(player, stack)
                && manager.isCoolingDown(item);
    }
}
```

Client prediction adapter, targeting the descriptor verified in Step 1:

```java
@Mixin(ClientPlayerInteractionManager.class)
public abstract class BombManiacClientInteractionMixin {
    @Redirect(
            method = "method_41929(Lnet/minecraft/util/Hand;Lnet/minecraft/entity/player/PlayerEntity;Lorg/apache/commons/lang3/mutable/MutableObject;I)Lnet/minecraft/network/packet/Packet;",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/ItemCooldownManager;isCoolingDown(Lnet/minecraft/item/Item;)Z")
    )
    private boolean sparktraits$allowMarkedGrenadePrediction(
            ItemCooldownManager manager, Item item,
            Hand hand, PlayerEntity player, MutableObject<?> result, int sequence
    ) {
        ItemStack stack = player.getStackInHand(hand);
        return !ConscienceBomberFrenzyService.canUseMarkedGrenade(player, stack)
                && manager.isCoolingDown(item);
    }
}
```

- [ ] **Step 7: Hide cooldown UI only for the marked stack**

In `BombManiacDrawContextMixin`, redirect `getCooldownProgress` inside the five-argument `drawItemInSlot` overload and return `0.0F` only when its `ItemStack stack` is a usable marked grenade.

In `BombManiacCooldownRendererMixin`, target Wathe `CooldownRenderer` with `remap = false`, redirect its single `isCoolingDown` call, read the selected `heldStack`, and return `false` only for a usable marked grenade. Delegate to the original manager for every other stack.

The exact shared branch for both adapters is:

```java
if (player != null && ConscienceBomberFrenzyService.canUseMarkedGrenade(player, stack)) {
    return 0.0F; // DrawContext progress redirect
    // or return false; for CooldownRenderer.isCoolingDown
}
```

Do not globally override `ItemCooldownManager.isCoolingDown`; an ordinary grenade in the other hand must remain blocked.

- [ ] **Step 8: Snapshot explosion behavior on the entity**

```java
public interface BombManiacGrenadeAccess {
    void sparktraits$setBombManiac(boolean bombManiac);
    boolean sparktraits$isBombManiac();
}
```

`ConscienceBomberGrenadeEntityMixin` implements that interface with a `@Unique boolean` and redirects the one `GameFunctions.killPlayer` invocation in `GrenadeEntity.onCollision`:

```java
Role role = GameWorldComponent.KEY.get(target.getWorld()).getRole(target);
boolean effectiveCivilian = EffectiveTraitService.isEffectiveCivilian(
        role, TraitPlayerComponent.KEY.get(target).getActiveTraitIds());
boolean shouldKill = !sparktraits$isBombManiac()
        || ConscienceBomberFrenzyRules.shouldKillTarget(
                role == null ? null : role.identifier(), effectiveCivilian);
if (shouldKill) {
    GameFunctions.killPlayer(target, spawnBody, killer, deathReason);
}
```

The boolean is written before `world.spawnEntity(grenade)` and is never cleared on mode expiry. Do not add entity NBT or a data tracker.

- [ ] **Step 9: Enforce owner-bound drop and ground-item cleanup**

Extend the existing `PlayerEntityMixin.dropItem` HEAD handler in this order:

```java
if ((Object) this instanceof ServerPlayerEntity player
        && (LastStandService.isPending(player) || DepressionTraitService.shouldBlockDrops(player))) {
    cir.setReturnValue(null);
    return;
}
if ((Object) this instanceof ServerPlayerEntity player
        && ConscienceBomberFrenzyService.isMarkedGrenade(stack)) {
    ConscienceBomberFrenzyService.clearPlayer(player);
    cir.setReturnValue(null);
}
```

This preserves the grenade through a Last Stand false-death drop attempt but makes a normal manual drop end the mode.

At the start of `ItemEntityMixin.onPlayerCollision`, discard and cancel any marked grenade before ordinary pickup logic:

```java
if (ConscienceBomberFrenzyService.isMarkedGrenade(getStack())) {
    ((ItemEntity) (Object) this).discard();
    ci.cancel();
    return;
}
```

- [ ] **Step 10: Register every new mixin and run focused tests**

Add common mixins to `sparktraits.mixins.json` and client mixins to `sparktraits.client.mixins.json`, retaining `"defaultRequire": 1`.

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew test \
  --tests dev.caecorthus.sparktraits.impl.traits.killer.conscience.ConscienceBomberFrenzyRulesTest \
  --tests dev.caecorthus.sparktraits.impl.traits.killer.conscience.BombManiacMixinContractTest
```

Expected: PASS. `./gradlew classes clientClasses` must also pass before moving on, proving all descriptors and signatures compile.

## Task 6: Lifecycle, Language, and Governance Documentation

**Files:**
- Modify: `src/main/java/dev/caecorthus/sparktraits/impl/lifecycle/TraitGameHooks.java`
- Modify: `src/main/resources/assets/sparktraits/lang/en_us.json`
- Modify: `src/main/resources/assets/sparktraits/lang/zh_cn.json`
- Modify: `ARCHITECTURE.md`
- Review: `CONTEXT.md`

**Interfaces:**
- Consumes: `ConscienceBomberFrenzyService.register`, `clearPlayer`, and `clearAll`.
- Preserves: Last Stand false-death return and the established real-death consequence order.

- [ ] **Step 1: Complete lifecycle registration and cleanup**

Register the service next to other Conscience services:

```java
ConscienceSerialKillerService.register();
ConsciencePoisonerService.register();
ConscienceBomberFrenzyService.register();
```

Reset cleanup:

```java
ConscienceBombService.clearTimedBomb(player);
ConscienceBomberFrenzyService.clearPlayer(player);
```

True-death cleanup must remain after the Last Stand early return and after the death dividend/Killer consequences:

```java
ConscienceEconomyService.rewardAfterConfirmedRealDeath(victim);
KillerTraitService.handleAfterRealKill(victim, killer, deathReason);
ImpostorBodyguardService.handleAfterKill(victim);
ConscienceSerialKillerService.handleAfterKill(victim, killer, deathReason);
ConscienceBomberFrenzyService.clearPlayer(victim);
```

Round finalization:

```java
ConscienceBombService.clearAll();
ConscienceBomberFrenzyService.clearAll(serverWorld);
ConscienceSerialKillerService.clearAll();
```

- [ ] **Step 2: Add exact English and Chinese copy**

Add:

```json
"shop.sparktraits.bomb_maniac": "Bomb Maniac Mode"
```

```json
"shop.sparktraits.bomb_maniac": "炸弹狂模式"
```

Replace the affected trait descriptions with accurate behavior:

```json
"trait.sparktraits.bloodthirsty.description": "Each real kill shortens knife cooldown by 5% per counted stack, capped by one third of the round player count.",
"trait.sparktraits.the_showman.description": "Real kills grant 8 money per other living player within 8 blocks of the victim, up to 10 players.",
"trait.sparktraits.plunderer.description": "Real kills steal one third of the victim's money, rounded down.",
"trait.sparktraits.paranoid.description": "Psycho mode lasts 20 seconds longer and starts with one extra armour layer.",
"trait.sparktraits.cornered.description": "Gain 75 money when a teammate dies, or 200 total when that death leaves you alone."
```

```json
"trait.sparktraits.bloodthirsty.description": "每层真实击杀使刀冷却缩短 5%，计入层数上限为本局人数的三分之一。",
"trait.sparktraits.the_showman.description": "真实击杀时，死者 8 格内每名其他存活玩家提供 8 金币，最多计算 10 人。",
"trait.sparktraits.plunderer.description": "真实击杀会偷取死者三分之一的金币，向下取整。",
"trait.sparktraits.paranoid.description": "疯魔模式延长 20 秒，并在开启时额外获得一层当前护盾。",
"trait.sparktraits.cornered.description": "同伙死亡时获得 75 金币；若该次死亡使你成为唯一同伙，则本次共获得 200 金币。"
```

Keep Conscience's general description concise; the role-specific shop entry itself carries the Bomb Maniac name.

- [ ] **Step 3: Update the architecture lifecycle paragraph**

Amend only the kill-order paragraph in `ARCHITECTURE.md` to state:

```text
A Last Stand transition syncs new spectator views and returns. A real death then
runs the independent Conscience death dividend, killer consequences, Impostor
Bodyguard, Conscience Serial Killer consequences, and Bomb Maniac owner cleanup
before trait/player-state cleanup and spectator resync. Round finalization clears
both Conscience timed services before player traits.
```

- [ ] **Step 4: Reconcile `CONTEXT.md` against implementation**

Confirm its existing definitions still say all of the following: 20 seconds; TNT shop display; ordinary Wathe grenade; no cooldown/unlimited use; 1.5 throw distance; exact three witch exceptions; apprentice follows alignment; launch snapshot; other-player-only dividend; Wathe passive cadence/cap; combined Cornered 200; no Demon Hunter police traits; random-weight-only 1.5 multiplier.

If all remain accurate, make no further `CONTEXT.md` edit.

- [ ] **Step 5: Validate JSON and focused lifecycle tests**

```bash
jq empty src/main/resources/assets/sparktraits/lang/en_us.json
jq empty src/main/resources/assets/sparktraits/lang/zh_cn.json
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew test \
  --tests dev.caecorthus.sparktraits.impl.lifecycle.TraitGameHooksDeathOrderContractTest
```

Expected: both JSON files parse and lifecycle contract passes.

## Task 7: Full Verification and Manual Gameplay Smoke Test

**Files:**
- Review all files in the File Map; do not edit outside them unless a compile error proves the plan's exact signature is stale, in which case update the approval template first.

- [ ] **Step 1: Run all focused suites together**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew test \
  --tests dev.caecorthus.sparktraits.impl.selection.TraitSelectorTest \
  --tests dev.caecorthus.sparktraits.impl.traits.killer.KillerTraitServiceTest \
  --tests dev.caecorthus.sparktraits.impl.traits.killer.conscience.ConscienceEconomyServiceTest \
  --tests dev.caecorthus.sparktraits.impl.traits.killer.conscience.ConscienceBomberFrenzyRulesTest \
  --tests dev.caecorthus.sparktraits.impl.traits.killer.conscience.BombManiacMixinContractTest \
  --tests dev.caecorthus.sparktraits.impl.lifecycle.TraitGameHooksDeathOrderContractTest
```

Expected: all focused tests PASS.

- [ ] **Step 2: Run the repository verification gates**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew test
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew verifyArchitecture
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew clean build
git diff --check
```

Expected: all commands exit 0. Use `clean` before judging failures because stale numbered `.class` files are a known false-failure source.

- [ ] **Step 3: Inspect the built jar and protocol boundary**

```bash
JAR=$(find build/libs -maxdepth 1 -name 'sparktraits-*.jar' ! -name '*-sources.jar' | sort | tail -n 1)
unzip -p "$JAR" fabric.mod.json | jq '{id,version,depends}'
jar tf "$JAR" | rg 'ConscienceBomberFrenzy|ConscienceEconomy|BombManiac|mixins.json'
rg -n "bomb_maniac" src/main/java/dev/caecorthus/sparktraits/component \
  src/main/java/dev/caecorthus/sparktraits/net src/main/resources/fabric.mod.json || true
```

Expected: new implementation/mixin classes are packaged; no new component id, packet, player/world NBT field, or dependency appears.

- [ ] **Step 4: Start a development client for mixin and gameplay smoke testing**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew runClient
```

Verify these exact cases in a local round:

1. A Conscience Bomber sees one TNT-icon `炸弹狂模式` entry at 350 coins; a normal Bomber and non-Bomber Conscience player do not.
2. Full hotbar or already-active purchase fails without charging coins or starting the 3-minute shop cooldown.
3. Successful purchase starts 3:00 shop cooldown and grants exactly one ordinary grenade. At the 20-second mode expiry, the shop cooldown has about 2:40 remaining.
4. The marked grenade throws repeatedly without decrement or item cooldown at 1.5 times ordinary distance.
5. If an ordinary grenade cooldown already exists, the marked grenade remains usable, while switching to an ordinary grenade stack still shows and enforces the original remaining cooldown.
6. Ordinary grenades still decrement and receive NoellesRoles Bomber's 90-second cooldown. The existing transferred timed bomb remains unchanged.
7. Bomb Maniac explosions spare effective civilians and the Conscience Bomber, kill effective killers/Impostors/neutrals, kill the exact three specified witch roles, and spare an effectively civilian apprentice witch.
8. Throw at approximately tick 399, wait for the mode to expire, then let the projectile collide: it still has 1.5-speed launch and protected-target behavior.
9. Manual drop deletes the marked grenade and ends the mode. True death, reset, reconnect, and round end also remove it. Last Stand false death does not.
10. On another player's confirmed death, every living Conscience killer receives 50, the dead player does not pay themself, and existing 100/150/200 rewards produce 150/200/250 totals where applicable.

Stop the client cleanly after the smoke test; do not leave the Gradle session running.

- [ ] **Step 5: Audit scope against the dirty baseline**

```bash
git status --short
git diff --name-only
git diff -- src/main/java/dev/caecorthus/sparktraits/impl/traits/killer/KillerTraitService.java
rg -n "DEMON_HUNTER|DemonHunter|demonHunter" \
  src/main/java/dev/caecorthus/sparktraits/impl/selection \
  src/main/java/dev/caecorthus/sparktraits/impl/traits/killer/conscience \
  src/main/java/dev/caecorthus/sparktraits/impl/traits/killer/KillerTraitService.java
```

Expected: no Demon Hunter implementation change; no unrelated dirty file is reverted; every new hunk maps to this plan.

- [ ] **Step 6: Final optional commit checkpoint**

Only if the owner explicitly asks for a commit, stage the exact plan file list with explicit `git add <path>` arguments, review `git diff --cached`, then commit. Never use `git add .` in this dirty worktree.
