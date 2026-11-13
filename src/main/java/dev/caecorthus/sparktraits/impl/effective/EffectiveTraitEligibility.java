package dev.caecorthus.sparktraits.impl.effective;

import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.impl.traits.civilian.laststand.LastStandTrait;
import dev.caecorthus.sparktraits.impl.traits.civilian.police.PoliceRoleCategory;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

import java.util.Collection;
import java.util.UUID;

/** Whether Conscience or Impostor may be rolled or admin-locked for a role, and the killer counts that round
 *  assignment balances them against.
 *  善良与内鬼能否在开局抽取或被管理员锁定到某身份，以及开局分配据以平衡它们的杀手人数。 */
public final class EffectiveTraitEligibility {
    private static final Identifier SPARKWITCH_PIG_GOD_ID = Identifier.of("sparkwitch", "pig_god");
    private static final Identifier SPARKWITCH_SAINT_ID = Identifier.of("sparkwitch", "saint");
    // SparkWitch's Blind sees only through sounds, so Impostor instinct and night vision would void the role.
    // SparkWitch 盲人只能靠声音感知，内鬼的本能透视与夜视会让该职业失效。
    private static final Identifier SPARKWITCH_BLIND_ID = Identifier.of("sparkwitch", "blind");
    // Owner 2026-11-04: SparkWitch's Noa paints from her real Wathe mood, which Impostor's fake mood would pin at 1.0.
    // 所有者 2026-11-04：SparkWitch 城崎诺亚按真实 Wathe 心情作画，内鬼的虚假心情会把它固定在 1.0。
    private static final Identifier SPARKWITCH_NOA_ID = Identifier.of("sparkwitch", "noa");
    // PR SparkWitch #206: SparkWitch's Sherry is the passenger-side detective (real sanity, killer deduction), so she must never roll Impostor.
    // PR SparkWitch #206：SparkWitch 橘雪莉是乘客阵营的侦探（真实理智、推演凶手），不能被抽成内鬼。
    private static final Identifier SPARKWITCH_SHERRY_ID = Identifier.of("sparkwitch", "sherry");
    private static final Identifier SPARKWITCH_BELL_RINGER_ID = Identifier.of("sparkwitch", "bell_ringer");

    private EffectiveTraitEligibility() {
    }

    public static boolean canSelectConscience(Role role, GameWorldComponent gameComponent, Collection<Identifier> selectedTraits) {
        return canSelectConscience(
                role,
                originalKillerCount(gameComponent),
                isRoleEnabled(gameComponent, role),
                selectedTraits
        );
    }

    public static boolean canSelectConscience(Role role, int originalKillerCount, Collection<Identifier> selectedTraits) {
        return canSelectConscience(role, originalKillerCount, true, selectedTraits);
    }

    public static boolean canSelectConscience(
            Role role,
            int originalKillerCount,
            boolean roleEnabled,
            Collection<Identifier> selectedTraits
    ) {
        return originalKillerCount >= 2
                && roleEnabled
                && EffectiveTraitService.isOriginalKiller(role)
                && !isConscienceBlockedRole(role)
                && !EffectiveTraitService.hasImpostor(selectedTraits);
    }

    /** Owner rule: SparkWitch's Bell Ringer never receives Conscience; other killer traits stay eligible.
     *  所有者规则：SparkWitch 的敲钟人永不获得善良；其他杀手词条不受影响。 */
    public static boolean isConscienceBlockedRole(Role role) {
        return role != null && role.identifier().equals(SPARKWITCH_BELL_RINGER_ID);
    }

    public static boolean canSelectImpostor(Role role, GameWorldComponent gameComponent, Collection<Identifier> selectedTraits) {
        return canSelectImpostor(role, originalKillerCount(gameComponent), selectedTraits);
    }

    public static boolean canSelectImpostor(Role role, int originalKillerCount, Collection<Identifier> selectedTraits) {
        return originalKillerCount >= 2
                && EffectiveTraitService.isOriginalCivilian(role)
                // Every role-only refusal lives in this one predicate, which admin lock validation asks too.
                // 所有仅取决于身份的拒绝都在这一个谓词里，管理员锁定校验问的也是它。
                && !isImpostorPermanentlyExcludedRole(role)
                && !selectedTraits.contains(LastStandTrait.ID)
                && !EffectiveTraitService.hasConscience(selectedTraits);
    }

    public static int originalKillerCount(GameWorldComponent gameComponent) {
        if (gameComponent == null) {
            return 0;
        }
        int count = 0;
        for (UUID uuid : gameComponent.getAllPlayers()) {
            if (EffectiveTraitService.isOriginalKiller(gameComponent.getRole(uuid))) {
                count++;
            }
        }
        return count;
    }

    private static boolean isRoleEnabled(GameWorldComponent gameComponent, Role role) {
        return gameComponent != null && role != null && gameComponent.isRoleEnabled(role);
    }

    /** Keeps high-agency innocent roles, including every police-category role, from being converted into Impostor.
     *  Permanent Impostor role exclusions shared by round selection and admin lock validation: the one role-only
     *  predicate both ask, Survival Master and Undercover included, so a lock selection would drop is refused when
     *  it is made. Only {@link #canSelectImpostor}'s round-state conditions (original killer count, the traits
     *  already selected) stay out of the command check.
     *  防止强机制无辜者角色（含所有警职类别身份）被转换成内鬼。
     *  开局抽取与管理员锁定校验共用的内鬼永久身份排除：两者询问的唯一仅看身份的谓词，包含生存大师与卧底，
     *  因此开局会被丢弃的锁定在下达时即被拒绝。只有 {@link #canSelectImpostor} 中依赖对局状态的条件
     *  （原始杀手人数、已选天赋）不进入命令校验。 */
    public static boolean isImpostorPermanentlyExcludedRole(Role role) {
        return role != null
                && (PoliceRoleCategory.isPolice(role)
                || EffectiveTraitService.isUndercover(role)
                || role.identifier().equals(Noellesroles.SURVIVAL_MASTER_ID)
                // Toxicologist owns SparkStrength's blue-poison kit, which must stay on the innocent side.
                // 毒理学家持有 SparkStrength 的蓝毒道具，必须保持好人身份。
                || role.identifier().equals(Noellesroles.TOXICOLOGIST_ID)
                || role.identifier().equals(SPARKWITCH_PIG_GOD_ID)
                || role.identifier().equals(SPARKWITCH_SAINT_ID)
                || role.identifier().equals(SPARKWITCH_BLIND_ID)
                || role.identifier().equals(SPARKWITCH_NOA_ID)
                || role.identifier().equals(SPARKWITCH_SHERRY_ID));
    }

    public static boolean countsAsPublicKiller(Role role, Collection<Identifier> traits) {
        return EffectiveTraitService.isOriginalKiller(role) && !EffectiveTraitService.hasConscience(traits);
    }

    public static int publicKillerCount(GameWorldComponent gameComponent, Collection<ServerPlayerEntity> players) {
        int count = 0;
        for (ServerPlayerEntity player : players) {
            if (countsAsPublicKiller(gameComponent.getRole(player), TraitPlayerComponent.KEY.get(player).getActiveTraitIds())) {
                count++;
            }
        }
        return count;
    }

    public static int requiredExtraKillersForConscience(int intendedPublicKillerCount, int originalKillerRoleCount, int conscienceCount) {
        return Math.max(0, intendedPublicKillerCount + conscienceCount - originalKillerRoleCount);
    }
}
