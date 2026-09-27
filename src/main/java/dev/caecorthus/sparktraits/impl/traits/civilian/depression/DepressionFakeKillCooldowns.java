package dev.caecorthus.sparktraits.impl.traits.civilian.depression;

import dev.caecorthus.sparktraits.SparkTraits;
import dev.caecorthus.sparktraits.impl.effective.death.EffectiveDeathConsequenceRules;
import dev.caecorthus.sparktraits.impl.traits.killer.combat.ExactItemCooldowns;
import dev.caecorthus.sparktraits.mixin.ItemCooldownEntryAccessor;
import dev.caecorthus.sparktraits.mixin.ItemCooldownManagerAccessor;
import dev.doctor4t.wathe.game.GameConstants;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * A weapon whose hit starts a Depression fake death keeps at most 20% of its original cooldown.
 * Only item cooldowns count; skill cooldowns are out of scope. Weapons with their own cooldown state
 * (SparkWitch's ceremonial sword) apply the same rule through SparkTraitsApi.
 * 击中后触发抑郁假死的武器最多只保留原本冷却的 20%。只处理物品冷却，技能冷却不算；
 * 自带冷却状态的武器（SparkWitch 仪礼剑）通过 SparkTraitsApi 套用同一规则。
 */
public final class DepressionFakeKillCooldowns {
    public static final int COOLDOWN_PERCENT = 20;
    private static final Identifier FLUSH_PHASE = SparkTraits.id("depression_fake_kill_cooldowns");
    /**
     * Weapons whose cooldown started before a tick-time kill (thrown, marked, pushed or injected earlier), by death reason.
     * Wathe grenades, Niko repeat shots and SparkWitch fire-poker falls name their weapon through {@link #withWeaponHint};
     * a fall alone names nothing, since Wathe credits any last attacker.
     * tick 中发生的击杀所对应、冷却更早就已开始的武器（投掷、标记、推击或注射），按死亡原因查找；
     * Wathe 手雷、Niko 连射和 SparkWitch 烧火棍坠车通过 {@link #withWeaponHint} 直接指明武器；
     * 单纯坠车不指明武器，因为 Wathe 会把任意最后攻击者记为凶手。
     */
    private static final Map<Identifier, List<Identifier>> DELAYED_WEAPONS = Map.of(
            GameConstants.DeathReasons.GRENADE, List.of(Identifier.of("sparkstrength", "m67")),
            GameConstants.DeathReasons.KNIFE, List.of(Identifier.of("sparkwitch", "feather_blade")),
            Identifier.of("noellesroles", "bodyguard_sacrifice"), List.of(Identifier.of("sparkwitch", "feather_blade")),
            Identifier.of("sparkwitch", "ninja_shuriken_kill"), List.of(Identifier.of("sparkwitch", "ninja_shuriken"))
    );
    private static final Identifier POISON_NEEDLE = Identifier.of("noellesroles", "poison_needle");
    /** Keeps its own kill cooldown through the API; its item cooldown is the dash. 通过 API 自管击杀冷却；其物品冷却是冲刺。 */
    private static final Identifier CEREMONIAL_SWORD = Identifier.of("sparkwitch", "ceremonial_sword");
    private static final ThreadLocal<Item> WEAPON_HINT = new ThreadLocal<>();
    private static final Map<UUID, FakeKills> pendingFakeKills = new HashMap<>();
    /** Length of each item's latest ordinary cooldown write; exact rewrites keep it. 每件物品最近一次普通冷却写入的时长；精确改写不改变它。 */
    private static final Map<UUID, Map<Item, OriginalCooldown>> originalCooldowns = new HashMap<>();
    /**
     * Server phase: the world/player tick and every server task (one packet) each get their own number,
     * so a write only counts when it happens after the fake death within the same phase.
     * 服务器阶段编号：世界/玩家 tick 与每个服务器任务（一个数据包）各自独立，
     * 只有在同一阶段内、假死之后发生的冷却写入才会被计入。
     */
    private static long phase;
    private static int taskDepth;
    private static boolean registered;

    private DepressionFakeKillCooldowns() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        ServerTickEvents.START_SERVER_TICK.register(server -> phase++);
        // After every default-phase listener, so END-tick kills (e.g. SparkWitch dashes) are applied in the same tick.
        // 排在所有默认阶段监听之后，使 END tick 中的击杀（如 SparkWitch 冲刺）在同一 tick 内生效。
        ServerTickEvents.END_SERVER_TICK.addPhaseOrdering(Event.DEFAULT_PHASE, FLUSH_PHASE);
        ServerTickEvents.END_SERVER_TICK.register(FLUSH_PHASE, server -> {
            flushAll(server);
            phase++;
        });
    }

    /** Rounds up so any real cooldown keeps at least one tick. 向上取整，原本有冷却时至少保留 1 tick。 */
    public static int discountedTicks(int originalTicks) {
        if (originalTicks <= 0) {
            return 0;
        }
        return (int) (((long) originalTicks * COOLDOWN_PERCENT + 99L) / 100L);
    }

    /**
     * The weapon may be ready at most 20% of its original cooldown after the kill tick; never lengthened.
     * 武器最迟在击杀 tick 后原冷却的 20% 时就绪；只缩短，不延长。
     */
    public static int shortenedEndTick(int startTick, int endTick, int killTick, int originalTicks) {
        return Math.min(endTick, Math.max(startTick, killTick) + discountedTicks(originalTicks));
    }

    /**
     * A kill site that names its weapon wins. A kill from the attacker's own action counts only the cooldown that
     * action wrote to a held item; a kill during the tick used its weapon earlier, so the death reason names it.
     * 击杀处直接指明的武器优先；攻击者自身操作造成的击杀只计该操作写给手持物品的冷却；
     * tick 中发生的击杀说明武器更早已使用，按死亡原因确定。
     */
    public static <T> Set<T> weaponsForKill(@Nullable T hint, boolean direct, Set<T> written, Set<T> held, Set<T> delayed) {
        if (hint != null) {
            return Set.of(hint);
        }
        if (!direct) {
            return delayed;
        }
        Set<T> struck = new LinkedHashSet<>(written);
        struck.retainAll(held);
        return struck;
    }

    /**
     * The original length survives exact rewrites (Last Escape halving, earlier discounts) of the same cooldown.
     * 同一段冷却经过精确改写（绝处逢生减半、先前的缩短）后仍保留原时长。
     */
    public static int originalTicks(int startTick, int endTick, int recordedStart, int recordedLength) {
        return isWithin(new OriginalCooldown(recordedStart, recordedLength), startTick, endTick)
                ? recordedLength : endTick - startTick;
    }

    private static boolean isWithin(OriginalCooldown original, int startTick, int endTick) {
        return original.lengthTicks() > 0
                && original.startTick() <= startTick
                && endTick <= original.startTick() + original.lengthTicks();
    }

    /**
     * Names the weapon for a kill that happens away from the attacker's hands (a landing grenade, a scheduled shot).
     * 为不在攻击者手中发生的击杀（手雷落地、延迟射击）指明武器。
     */
    public static void withWeaponHint(Item weapon, Runnable kill) {
        Item previous = WEAPON_HINT.get();
        WEAPON_HINT.set(weapon);
        try {
            kill.run();
        } finally {
            if (previous == null) WEAPON_HINT.remove();
            else WEAPON_HINT.set(previous);
        }
    }

    public static void onFakeDeathStarted(ServerPlayerEntity attacker, ServerPlayerEntity victim, Identifier deathReason) {
        FakeKills fakeKills = pendingFakeKills.get(attacker.getUuid());
        if (fakeKills != null && fakeKills.phase != phase) {
            pendingFakeKills.remove(attacker.getUuid());
            apply(attacker, fakeKills);
            fakeKills = null;
        }
        if (fakeKills == null) {
            fakeKills = new FakeKills(phase);
            pendingFakeKills.put(attacker.getUuid(), fakeKills);
        }
        // Only a needle-applied poison makes the needle the weapon; vials, scorpions and gas bombs have no cooldown.
        // 只有毒针施加的毒才算毒针击杀；毒药瓶、蝎子、毒气弹没有冷却。
        boolean poisonedByNeedle = GameConstants.DeathReasons.POISON.equals(deathReason)
                && Noellesroles.POISON_SOURCE_NEEDLE.equals(EffectiveDeathConsequenceRules.peekPoisonSource(victim.getUuid()));
        fakeKills.kills.add(new FakeKill(
                deathReason, WEAPON_HINT.get(), heldItems(attacker), poisonedByNeedle, taskDepth > 0, managerTick(attacker)));
    }

    /**
     * Called for every ordinary server cooldown write (exact rewrites excluded).
     * 每次普通的服务端冷却写入都会调用（不含精确改写）。
     */
    public static void recordWrite(ServerPlayerEntity owner, Item item) {
        ItemCooldownManagerAccessor manager = (ItemCooldownManagerAccessor) owner.getItemCooldownManager();
        if (manager.sparktraits$getEntries().get(item) instanceof ItemCooldownEntryAccessor cooldown) {
            int start = cooldown.sparktraits$getStartTick();
            int end = cooldown.sparktraits$getEndTick();
            Map<Item, OriginalCooldown> originals = originalCooldowns.computeIfAbsent(owner.getUuid(), uuid -> new HashMap<>());
            OriginalCooldown previous = originals.get(item);
            // A write that only shortens the running cooldown (e.g. NoellesRoles vodka) keeps its original length.
            // 仅缩短当前冷却的写入（如 NoellesRoles 伏特加）保留原冷却时长。
            if (previous == null || !isWithin(previous, start, end)) {
                originals.put(item, new OriginalCooldown(start, end - start));
            }
        }
        FakeKills fakeKills = pendingFakeKills.get(owner.getUuid());
        if (fakeKills != null && fakeKills.phase == phase) {
            fakeKills.written.add(item);
        }
    }

    /** Each server task (one received packet) is its own phase. 每个服务器任务（一个收到的数据包）是独立阶段。 */
    public static void beginServerTask() {
        taskDepth++;
        phase++;
    }

    /**
     * Applies the fake deaths this packet caused as soon as it has been handled, before anything else can write.
     * 数据包处理完毕后立即处理其造成的假死，避免之后的其他写入混入。
     */
    public static void endServerTask(MinecraftServer server) {
        try {
            for (UUID uuid : Set.copyOf(pendingFakeKills.keySet())) {
                FakeKills fakeKills = pendingFakeKills.get(uuid);
                if (fakeKills.phase == phase) {
                    pendingFakeKills.remove(uuid);
                    ServerPlayerEntity attacker = server.getPlayerManager().getPlayer(uuid);
                    if (attacker != null) {
                        safelyApply(attacker, fakeKills);
                    }
                }
            }
        } finally {
            phase++;
            taskDepth = Math.max(0, taskDepth - 1);
        }
    }

    /** Opaque marker of the attacker's pending fake kills, taken before a known attack handler runs. */
    public static @Nullable Object pendingFakeKill(ServerPlayerEntity attacker) {
        return pendingFakeKills.get(attacker.getUuid());
    }

    /**
     * Applies right after the gun handler when it started a fake death, so gun-cycle listeners see the discount.
     * 枪械处理触发假死时在其结束后立即生效，使枪械循环监听者读到缩短后的冷却。
     */
    public static void flushStartedSince(ServerPlayerEntity attacker, @Nullable Object before) {
        FakeKills fakeKills = pendingFakeKills.get(attacker.getUuid());
        if (fakeKills != null && fakeKills != before) {
            pendingFakeKills.remove(attacker.getUuid());
            apply(attacker, fakeKills);
        }
    }

    public static void flushAll(MinecraftServer server) {
        for (UUID uuid : Set.copyOf(pendingFakeKills.keySet())) {
            FakeKills fakeKills = pendingFakeKills.remove(uuid);
            ServerPlayerEntity attacker = server.getPlayerManager().getPlayer(uuid);
            if (attacker != null) {
                safelyApply(attacker, fakeKills);
            }
        }
    }

    public static void clear() {
        pendingFakeKills.clear();
        originalCooldowns.clear();
    }

    /**
     * Runs outside vanilla's per-task error handling, so a failure must not take the server tick down with it.
     * 在原版逐任务的异常处理之外运行，失败不能拖垮服务器 tick。
     */
    private static void safelyApply(ServerPlayerEntity attacker, FakeKills fakeKills) {
        try {
            apply(attacker, fakeKills);
        } catch (RuntimeException | LinkageError exception) {
            SparkTraits.LOGGER.warn("Failed to shorten Depression fake-kill weapon cooldowns", exception);
        }
    }

    private static void apply(ServerPlayerEntity attacker, FakeKills fakeKills) {
        Map<Item, Integer> killTicks = new LinkedHashMap<>();
        for (FakeKill kill : fakeKills.kills) {
            for (Item weapon : weaponsForKill(kill.hint(), kill.direct(), fakeKills.written, kill.held(), delayedWeapons(kill))) {
                killTicks.merge(weapon, kill.killTick(), Math::min);
            }
        }
        Registries.ITEM.getOrEmpty(CEREMONIAL_SWORD).ifPresent(killTicks::remove);
        ItemCooldownManagerAccessor manager = (ItemCooldownManagerAccessor) attacker.getItemCooldownManager();
        Map<Item, OriginalCooldown> originals = originalCooldowns.getOrDefault(attacker.getUuid(), Map.of());
        int now = manager.sparktraits$getTick();
        for (Map.Entry<Item, Integer> weapon : killTicks.entrySet()) {
            if (!(manager.sparktraits$getEntries().get(weapon.getKey()) instanceof ItemCooldownEntryAccessor cooldown)) {
                continue;
            }
            int startTick = cooldown.sparktraits$getStartTick();
            int endTick = cooldown.sparktraits$getEndTick();
            OriginalCooldown original = originals.get(weapon.getKey());
            int originalTicks = original == null
                    ? endTick - startTick
                    : originalTicks(startTick, endTick, original.startTick(), original.lengthTicks());
            int shortenedEnd = shortenedEndTick(startTick, endTick, weapon.getValue(), originalTicks);
            if (shortenedEnd < endTick) {
                ExactItemCooldowns.setExact(attacker, weapon.getKey(), Math.max(0, shortenedEnd - now));
            }
        }
    }

    private static Set<Item> delayedWeapons(FakeKill kill) {
        Set<Item> weapons = new LinkedHashSet<>();
        for (Identifier itemId : DELAYED_WEAPONS.getOrDefault(kill.deathReason(), List.of())) {
            // Optional mods may be absent; unknown ids are simply skipped.
            // 可选模组可能未安装；未注册的物品直接跳过。
            Registries.ITEM.getOrEmpty(itemId).ifPresent(weapons::add);
        }
        if (kill.poisonedByNeedle()) {
            Registries.ITEM.getOrEmpty(POISON_NEEDLE).ifPresent(weapons::add);
        }
        return weapons;
    }

    private static Set<Item> heldItems(ServerPlayerEntity player) {
        Set<Item> held = new LinkedHashSet<>();
        for (ItemStack stack : List.of(player.getMainHandStack(), player.getOffHandStack())) {
            if (!stack.isEmpty()) {
                held.add(stack.getItem());
            }
        }
        return held;
    }

    private static int managerTick(ServerPlayerEntity player) {
        return ((ItemCooldownManagerAccessor) player.getItemCooldownManager()).sparktraits$getTick();
    }

    /** One attacker's fake deaths within a single phase. 同一攻击者在同一阶段内造成的假死。 */
    private static final class FakeKills {
        private final long phase;
        private final List<FakeKill> kills = new ArrayList<>();
        private final Set<Item> written = new LinkedHashSet<>();

        private FakeKills(long phase) {
            this.phase = phase;
        }
    }

    private record FakeKill(
            Identifier deathReason,
            @Nullable Item hint,
            Set<Item> held,
            boolean poisonedByNeedle,
            boolean direct,
            int killTick
    ) {
    }

    private record OriginalCooldown(int startTick, int lengthTicks) {
    }
}
