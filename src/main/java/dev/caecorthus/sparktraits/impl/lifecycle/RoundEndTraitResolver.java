package dev.caecorthus.sparktraits.impl.lifecycle;

import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.component.TraitWorldComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Single source of the traits a participant ended the round with, shared by round-end win data and replay tooltips.
 * Only valid before Wathe's finalize resets players and SparkTraits clears round state.
 * 回合结束数据与回放提示共用的"本局最终词条"唯一来源；仅在 Wathe 结算重置玩家、SparkTraits 清理本局状态之前有效。
 */
public final class RoundEndTraitResolver {
    private RoundEndTraitResolver() {
    }

    public static List<Identifier> resolve(ServerWorld world, TraitWorldComponent traitWorld, UUID playerUuid) {
        PlayerEntity player = world.getPlayerByUuid(playerUuid);
        List<Identifier> onlineActiveTraits = player == null
                ? null
                : TraitPlayerComponent.KEY.get(player).getActiveTraitIds();
        return choose(
                onlineActiveTraits,
                traitWorld.getDeathTraitSnapshot(playerUuid),
                traitWorld.getRoundTraitSnapshot(playerUuid)
        );
    }

    /**
     * Priority: online active traits, then the latest death snapshot, then the assignment-time round snapshot.
     * Empty never wins over a later source; a null active list means the participant is offline.
     * 优先级：在线玩家当前词条 > 最近一次死亡快照 > 分配时的本局快照；空列表不会压过后续来源，null 表示玩家离线。
     */
    static List<Identifier> choose(
            @Nullable List<Identifier> onlineActiveTraits,
            List<Identifier> deathTraits,
            List<Identifier> roundTraits
    ) {
        if (onlineActiveTraits != null && !onlineActiveTraits.isEmpty()) {
            return onlineActiveTraits;
        }
        if (!deathTraits.isEmpty()) {
            return deathTraits;
        }
        return roundTraits;
    }
}
