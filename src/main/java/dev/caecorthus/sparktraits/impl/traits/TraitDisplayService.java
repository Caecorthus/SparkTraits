package dev.caecorthus.sparktraits.impl.traits;

import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.List;

/**
 * Shared trait-display rules for spectator HUD paths.
 * 旁观者 HUD 路径共用的词条显示规则。
 */
public final class TraitDisplayService {
    private TraitDisplayService() {
    }

    public static List<Identifier> spectatorPlayerTraits(
            boolean spectator,
            boolean targetDead,
            Collection<Identifier> activeTraits,
            Collection<Identifier> deathTraits
    ) {
        if (!spectator) {
            return List.of();
        }
        /*
         * 死亡玩家的世界级快照是死亡时刻的权威词条集合。近距离实体可能已经
         * 收到过普通玩家同步，其中 activeTraits 只包含本人可见的部分；如果先
         * 返回那份非空缓存，就会遮蔽完整死亡快照，造成“近处不显示、远处显示”
         * 的距离相关问题。调试 deadPlayers 和真实死亡都必须优先使用快照。
         */
        if (targetDead && deathTraits != null && !deathTraits.isEmpty()) {
            return List.copyOf(deathTraits);
        }
        if (activeTraits != null && !activeTraits.isEmpty()) {
            return List.copyOf(activeTraits);
        }
        return List.of();
    }
}
