package dev.caecorthus.sparktraits.impl.traits.civilian.police;

import dev.caecorthus.sparkfactionapi.api.PoliceRoles;
import dev.caecorthus.sparktraits.impl.effective.alignment.EffectiveAlignment;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import net.minecraft.util.Identifier;

/**
 * SparkTraits' police category: an originally innocent role listed in SparkFactionAPI {@link PoliceRoles}.
 * PoliceRoles is the single identity source, so downstream police roles (for example SparkWitch's)
 * opt in by registering there instead of being hard-coded here. The innocence guard keeps
 * non-innocent registrations such as SparkStrength's neutral Corrupt Cop out of police traits.
 * Pure role checks with no world access, so they are safe on both client and server.
 * SparkTraits 的警职类别：在 SparkFactionAPI {@link PoliceRoles} 中登记、且原始阵营为好人的身份。
 * PoliceRoles 是唯一的身份来源，下游警职（如 SparkWitch）在该处注册即可加入，不在此硬编码。
 * 好人校验把 SparkStrength 的中立贪污警察等非好人登记排除在警类天赋之外。
 * 仅做身份判断、不读取世界状态，客户端与服务端均可安全调用。
 */
public final class PoliceRoleCategory {
    // SparkWitch's USEC carries only a sniper rifle, never a revolver; matched by full id so SparkTraits keeps no
    // SparkWitch dependency. SparkWitch 的 USEC 只带狙击枪、从不持左轮；按完整身份 ID 匹配，SparkTraits 不依赖 SparkWitch。
    private static final Identifier SPARKWITCH_USEC_ID = Identifier.of("sparkwitch", "usec");

    private PoliceRoleCategory() {
    }

    /** Any originally innocent PoliceRoles member; null is never police.
     *  任何原始阵营为好人的 PoliceRoles 成员；null 永不属于警职。 */
    public static boolean isPolice(Role role) {
        return EffectiveAlignment.isOriginalCivilian(role) && PoliceRoles.contains(role);
    }

    /**
     * Police roles that may roll and use Marksman, Fast Reload and Heavy Artillery; Niko narrows this further
     * ({@link #canReceiveNikoTrait}). Veteran starts with a knife, so it keeps its own trait pool, matching
     * SparkStrength's police tablet.
     * 可抽取并使用精确枪手、快速装填与重炮的警职；Niko 在此基础上进一步收窄（见 {@link #canReceiveNikoTrait}）。
     * 老兵开局持刀，因此保留其专属天赋池，与 SparkStrength 警用平板的排除规则一致。
     */
    public static boolean canReceiveGunPoliceTraits(Role role) {
        return isPolice(role) && !WatheRoles.VETERAN.identifier().equals(role.identifier());
    }

    /**
     * Gun police roles that may roll and use Niko, a revolver/derringer trait. USEC never carries a revolver, so it
     * never rolls Niko but keeps Marksman, Fast Reload and Heavy Artillery (owner decision 2026-10-07).
     * 可抽取并使用 Niko（左轮/德林加天赋）的枪械警职。USEC 从不持左轮，因此永不抽到 Niko，
     * 但保留精确枪手、快速装填与重炮（所有者 2026-10-07 决定）。
     */
    public static boolean canReceiveNikoTrait(Role role) {
        return canReceiveGunPoliceTraits(role) && !SPARKWITCH_USEC_ID.equals(role.identifier());
    }
}
