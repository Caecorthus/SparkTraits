package dev.caecorthus.sparktraits.impl.effective;

import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.component.TraitWorldComponent;
import dev.caecorthus.sparktraits.compat.SparkWitchWraithBridge;
import dev.caecorthus.sparktraits.impl.effective.alignment.EffectiveAlignment;
import dev.caecorthus.sparktraits.mixin.GameWorldComponentAccessor;
import dev.doctor4t.wathe.api.Faction;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.event.BlackoutEffect;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** A player's effective alignment after Conscience/Impostor flips (team membership, faction, killer-team roster) and
 *  the native side kit it implies (mood type, blackout vision and cooldown); its {@link #register()} wires every
 *  effective-team listener, including those of {@link EffectiveWinService}, {@link EffectiveConsequenceService} and
 *  {@link EffectiveInstinctService}.
 *  善良/内鬼翻转后玩家的有效阵营（所属队伍、阵营、杀手队伍名单）及其对应的原生阵营配套（心情类型、熄灯视野与冷却）；
 *  其 {@link #register()} 统一注册所有有效阵营监听，包括 {@link EffectiveWinService}、{@link EffectiveConsequenceService}
 *  与 {@link EffectiveInstinctService} 的监听。 */
public final class EffectiveTraitService {
    public static final int CONSCIENCE_COLOR = 0xFFDEF8;
    public static final int IMPOSTOR_COLOR = 0x7D0000;

    private EffectiveTraitService() {
    }

    public static void register() {
        EffectiveWinService.register();
        BlackoutEffect.BEFORE.register(EffectiveTraitService::beforeBlackoutEffect);
        EffectiveConsequenceService.register();
        EffectiveInstinctService.register();
    }

    private static BlackoutEffect.BlackoutResult beforeBlackoutEffect(ServerPlayerEntity player, int durationTicks) {
        Collection<Identifier> traits = TraitPlayerComponent.KEY.get(player).getActiveTraitIds();
        if (!shouldApplyImpostorBlackoutImmunity(traits)) {
            return null;
        }

        // Match Wathe's killer blackout branch without granting other killer features.
        // 对齐 wathe 杀手熄灯分支，但不授予其他杀手功能。
        player.removeStatusEffect(StatusEffects.BLINDNESS);
        player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.NIGHT_VISION,
                durationTicks,
                0,
                false,
                false,
                true
        ));
        return BlackoutEffect.BlackoutResult.cancel();
    }

    public static boolean hasConscience(PlayerEntity player) {
        return player != null && hasConscience(TraitPlayerComponent.KEY.get(player).getActiveTraitIds());
    }

    public static boolean hasImpostor(PlayerEntity player) {
        return player != null && hasImpostor(TraitPlayerComponent.KEY.get(player).getActiveTraitIds());
    }

    /** Keeps effective alignment stable after death listeners clear a dead player's active traits.
     *  死亡监听清空当前天赋后，回退到死亡快照以保持有效阵营判定稳定。 */
    public static Collection<Identifier> effectiveTraitIds(PlayerEntity player, GameWorldComponent game) {
        return effectiveTraitIds(
                TraitPlayerComponent.KEY.get(player).getActiveTraitIds(),
                game.isPlayerDead(player.getUuid()),
                TraitWorldComponent.KEY.get(player.getWorld()).getDeathTraitSnapshot(player.getUuid())
        );
    }

    public static Collection<Identifier> effectiveTraitIds(
            Collection<Identifier> activeTraits,
            boolean dead,
            Collection<Identifier> deathTraits
    ) {
        return activeTraits.isEmpty() && dead ? deathTraits : activeTraits;
    }

    public static boolean hasConscience(Collection<Identifier> traits) {
        return EffectiveAlignment.hasConscience(traits);
    }

    public static boolean hasImpostor(Collection<Identifier> traits) {
        return EffectiveAlignment.hasImpostor(traits);
    }

    public static boolean isOriginalKiller(Role role) {
        return EffectiveAlignment.isOriginalKiller(role);
    }

    public static boolean isOriginalCivilian(Role role) {
        return EffectiveAlignment.isOriginalCivilian(role);
    }

    public static boolean isUndercover(Role role) {
        return role != null && role.identifier().equals(Noellesroles.UNDERCOVER_ID);
    }

    public static boolean sharesBlackoutCooldown(
            Role purchaserRole,
            Collection<Identifier> purchaserTraits,
            Role targetRole,
            Collection<Identifier> targetTraits
    ) {
        if (!isOriginalKiller(purchaserRole) || !isOriginalKiller(targetRole)) {
            return false;
        }
        return hasConscience(purchaserTraits) == hasConscience(targetTraits);
    }

    /** Grants blackout vision only to active Impostor trait holders.
     *  只让当前拥有内鬼天赋的玩家获得熄灯视野免疫。 */
    public static boolean shouldApplyImpostorBlackoutImmunity(Collection<Identifier> traits) {
        return traits != null && hasImpostor(traits);
    }

    public static boolean isEffectiveKiller(PlayerEntity player, GameWorldComponent gameComponent) {
        if (player == null) {
            return false;
        }
        return isEffectiveKiller(gameComponent.getRole(player), TraitPlayerComponent.KEY.get(player).getActiveTraitIds());
    }

    public static boolean isEffectiveKiller(Role role, Collection<Identifier> traits) {
        return EffectiveAlignment.isEffectiveKiller(role, traits);
    }

    /**
     * Returns the killer-team UUIDs that NoellesRoles should use for Shadow Jester showdown checks.
     * 返回 NoellesRoles 双影谢幕判定中应使用的有效杀手阵营 UUID 列表。
     *
     * <p>Wathe 的 {@code getAllKillerTeamPlayers()} 只读取角色的原始
     * {@code canUseKiller()} 属性，因此会遗漏“原始好人 + impostor”玩家，
     * 也会把带有 conscience 的原始杀手继续算入杀手列表。这里按 SparkTraits
     * 已确定的阵营规则重新构造列表：</p>
     *
     * <ul>
     *     <li>保留原始杀手，但排除已经翻为好人阵营的 conscience；</li>
     *     <li>加入原始好人且拥有 impostor 的玩家；</li>
     *     <li>不把普通好人或其他中立角色加入杀手阵营。</li>
     * </ul>
     */
    public static List<UUID> getEffectiveKillerTeamPlayers(GameWorldComponent gameComponent) {
        if (gameComponent == null) {
            return List.of();
        }

        GameWorldComponentAccessor accessor = (GameWorldComponentAccessor) (Object) gameComponent;
        if (accessor.sparktraits$getWorld() == null) {
            return List.of();
        }

        Set<UUID> effectiveKillers = new LinkedHashSet<>();

        // 先处理 Wathe 原本认定的杀手。善良杀手不能在双影谢幕中继续充当杀手对手。
        for (UUID uuid : gameComponent.getAllKillerTeamPlayers()) {
            PlayerEntity player = accessor.sparktraits$getWorld().getPlayerByUuid(uuid);
            if (player == null || !hasConscience(player)) {
                effectiveKillers.add(uuid);
            }
        }

        // 再补入被 impostor 翻转为杀手阵营的原始好人。
        for (UUID uuid : gameComponent.getAllPlayers()) {
            Role role = gameComponent.getRole(uuid);
            PlayerEntity player = accessor.sparktraits$getWorld().getPlayerByUuid(uuid);
            if (player != null
                    && isOriginalCivilian(role)
                    && hasImpostor(player)
                    && !hasConscience(player)) {
                effectiveKillers.add(uuid);
            }
        }

        return new ArrayList<>(effectiveKillers);
    }

    public static boolean isEffectiveCivilian(PlayerEntity player, GameWorldComponent gameComponent) {
        if (player == null) {
            return false;
        }
        return isEffectiveCivilian(gameComponent.getRole(player), TraitPlayerComponent.KEY.get(player).getActiveTraitIds());
    }

    public static boolean isEffectiveCivilian(Role role, Collection<Identifier> traits) {
        return EffectiveAlignment.isEffectiveCivilian(role, traits);
    }

    /**
     * Faction after SparkTraits alignment flips: an Impostor plays for the killers and a Conscience killer for the
     * civilians; a missing or NONE role stays NONE, anything else keeps its role's faction. Deliberately not
     * SparkFactionAPI's effective-faction resolver, which would also apply other mods' registered resolvers.
     * Shared by Stands Firm and Sensitive.
     * SparkTraits 阵营翻转后的阵营：内鬼属于杀手方、善良杀手属于好人方；身份缺失或为 NONE 时仍为 NONE，其余保留身份原始阵营。
     * 有意不使用 SparkFactionAPI 的有效阵营解析器，以免引入其他模组注册的解析规则。供立场坚定与敏感共用。
     */
    public static Faction effectiveFaction(GameWorldComponent game, ServerPlayerEntity player) {
        Role role = game.getRole(player);
        if (role == null || role.getFaction() == Faction.NONE) {
            return Faction.NONE;
        }
        if (isEffectiveKiller(player, game)) {
            return Faction.KILLER;
        }
        if (isEffectiveCivilian(player, game)) {
            return Faction.CIVILIAN;
        }
        return role.getFaction();
    }

    public static Role.MoodType effectiveMoodType(PlayerEntity player, Role role) {
        if (hasConscience(player)) {
            return effectiveConscienceMoodType(SparkWitchWraithBridge.isWraithActive(player));
        }
        if (hasImpostor(player)) {
            return Role.MoodType.FAKE;
        }
        return role == null ? Role.MoodType.NONE : role.getMoodType();
    }

    /** Active Wraith state suppresses Conscience's mood HUD without removing the trait. */
    public static Role.MoodType effectiveConscienceMoodType(boolean activeWraith) {
        return activeWraith ? Role.MoodType.NONE : Role.MoodType.REAL;
    }

    public static Role.MoodType effectiveMoodType(Role role, Collection<Identifier> traits) {
        return effectiveMoodType(role, traits, false);
    }

    static Role.MoodType effectiveMoodType(Role role, Collection<Identifier> traits, boolean activeWraith) {
        if (hasConscience(traits)) {
            return effectiveConscienceMoodType(activeWraith);
        }
        if (hasImpostor(traits)) {
            return Role.MoodType.FAKE;
        }
        return role == null ? Role.MoodType.NONE : role.getMoodType();
    }
}
