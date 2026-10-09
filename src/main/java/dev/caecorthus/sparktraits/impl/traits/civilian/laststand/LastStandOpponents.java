package dev.caecorthus.sparktraits.impl.traits.civilian.laststand;

import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.impl.effective.EffectiveTraitService;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Counts the living opponents of the civilian side for the {@code alive_opponents} match-record field.
 * An opponent is decided exactly as Last Stand decides a qualifying killer: any role holder who is not an
 * effective civilian (Conscience and Impostor flips included). Record-only: no gameplay reads this count.
 * 为对局记录字段 alive_opponents 统计平民阵营的存活对手数。对手判定与背水一战判定合格击杀者完全相同：
 * 任何不是有效平民的持身份玩家（含善良与内鬼的阵营翻转）。仅用于记录，玩法逻辑不读取此数值。
 */
public final class LastStandOpponents {
    private LastStandOpponents() {
    }

    /**
     * Living, online round players in this world who are not effective civilians right now.
     * 统计此世界中当前存活、在线且不是有效平民的本局玩家。
     */
    public static int countAlive(ServerWorld world) {
        GameWorldComponent game = GameWorldComponent.KEY.get(world);
        List<PlayerSide> players = new ArrayList<>();
        for (UUID uuid : game.getAllPlayers()) {
            if (!(world.getPlayerByUuid(uuid) instanceof ServerPlayerEntity player)) {
                continue;
            }
            players.add(new PlayerSide(
                    game.getRole(player),
                    TraitPlayerComponent.KEY.get(player).getActiveTraitIds(),
                    game.hasAnyRole(player) && GameFunctions.isPlayerPlayingAndAlive(player)
            ));
        }
        return count(players);
    }

    static int count(Collection<PlayerSide> players) {
        int opponents = 0;
        for (PlayerSide player : players) {
            if (player.alive() && isOpponent(player.role(), player.traitIds())) {
                opponents++;
            }
        }
        return opponents;
    }

    /**
     * The killer-side half of {@link LastStandService#canTriggerFromKill}: a role holder who is not an effective civilian.
     * 即 LastStandService.canTriggerFromKill 中击杀者一侧的判定：持有身份且不是有效平民。
     */
    static boolean isOpponent(@Nullable Role role, Collection<Identifier> traitIds) {
        return role != null && !EffectiveTraitService.isEffectiveCivilian(role, traitIds);
    }

    record PlayerSide(@Nullable Role role, Collection<Identifier> traitIds, boolean alive) {
    }
}
