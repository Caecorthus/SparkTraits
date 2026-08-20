package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.impl.effective.EffectiveTraitService;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import org.agmas.noellesroles.Noellesroles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.UUID;

/**
 * Makes NoellesRoles' bound Shadow Jester showdown see effective killer-team players.
 * 让 NoellesRoles 的命运绑定双影谢幕识别 SparkTraits 翻转后的有效杀手阵营。
 *
 * <p>NoellesRoles 的双影谢幕逻辑本身不能修改，因此这里只重定向其胜利检查
 * lambda 内唯一用于判断“杀手是否存活”的原始杀手列表调用。这样普通好人仍
 * 不会被加入杀手列表，拥有 impostor 词条的原始好人则会被正确视为谢幕对手。</p>
 */
@Mixin(value = Noellesroles.class, remap = false)
public abstract class NoellesRolesShadowJesterWinMixin {
    @Redirect(
            // 当前锁定的 NoellesRoles 版本中，双影谢幕检查位于该合成 lambda。
            method = "lambda$registerEvents$14",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/doctor4t/wathe/cca/GameWorldComponent;getAllKillerTeamPlayers()Ljava/util/List;"
            )
    )
    private static List<UUID> sparktraits$getEffectiveKillerTeamPlayers(
            GameWorldComponent gameWorldComponent
    ) {
        return EffectiveTraitService.getEffectiveKillerTeamPlayers(gameWorldComponent);
    }
}
