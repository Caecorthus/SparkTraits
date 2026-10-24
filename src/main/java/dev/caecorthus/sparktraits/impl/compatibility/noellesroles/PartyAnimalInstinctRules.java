package dev.caecorthus.sparktraits.impl.compatibility.noellesroles;

import org.jetbrains.annotations.Nullable;

/**
 * The Party Animal's keyless outlines under SparkTraits' instinct hook: NoellesRoles' helium pink by buzz level and
 * SparkStrength's Party Balloon yellow. Both are its own ability, so Conscience must not hide them, and both are
 * keyless overlays, so they must not repaint the instinct outline while the key is held.
 * 派对狂的免按键描边在 SparkTraits 本能钩子中的规则：NoellesRoles 按变声等级的氦气粉色与 SparkStrength 派对气球黄色。
 * 两者都是派对狂自己的能力，因此善良不能隐藏它们；两者都是免按键叠加层，因此按住本能键时不能改写本能描边的颜色。
 */
public final class PartyAnimalInstinctRules {
    private PartyAnimalInstinctRules() {
    }

    /**
     * NoellesRoles' helium colour for a buzz amplifier (its Party Animal instinct listener: level 1, 2, then 3+).
     * NoellesRoles 按变声放大等级给出的氦气颜色（其派对狂本能监听器：1 级、2 级、3 级及以上）。
     */
    public static int heliumColor(int amplifier) {
        return switch (amplifier + 1) {
            case 1 -> 0xFFB3D9;
            case 2 -> 0xFF3FBF;
            default -> 0x8B0050;
        };
    }

    /**
     * Whether Wathe's event verdict is the Party Animal's own keyless outline: a keyless answer in a balloon or helium
     * colour the target carries right now. A skip, a keyed answer or another colour means something outranked it, or
     * its listener declined (a hidden Survival Master, a dead viewer), so the outline is not shown.
     * Wathe 事件裁决是否正是派对狂自己的免按键描边：免按键、且颜色等于目标当前所带的气球色或氦气色。skip、需按键的结果或其他颜色
     * 说明有更高优先级的结果压过了它，或其监听器未作答（被遮挡的生存大师、观察者已死亡），因此不显示该描边。
     */
    public static boolean isOwnKeylessVerdict(
            boolean verdictPresent,
            boolean verdictRequiresKeybind,
            int verdictColor,
            @Nullable Integer balloonColor,
            @Nullable Integer heliumColor
    ) {
        if (!verdictPresent || verdictRequiresKeybind || verdictColor == -1) {
            return false;
        }
        return (balloonColor != null && balloonColor == verdictColor)
                || (heliumColor != null && heliumColor == verdictColor);
    }
}
