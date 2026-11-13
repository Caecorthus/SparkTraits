package dev.caecorthus.sparktraits.impl.effective;

import dev.caecorthus.sparktraits.SparkTraits;
import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparkfactionapi.api.settlement.SettlementExclusions;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.event.CheckWinCondition;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.jester.JesterPlayerComponent;
import org.agmas.noellesroles.shadowjester.ShadowJesterPlayerComponent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Round-end resolution by effective alignment: the team-win listener, team-win deferrals for blocking neutrals and
 *  NoellesRoles' Jester and Shadow Jester, and the self-realisation of Impostors left without real killer support.
 *  按有效阵营结算回合：队伍胜利监听、阻塞中立与 NoellesRoles 小丑/双影小丑的队伍胜利延后，以及失去真实杀手支持的内鬼自我觉悟。 */
public final class EffectiveWinService {
    public static final Identifier SELF_REALIZATION = SparkTraits.id("self_realization");
    private static final Identifier SPARKWITCH_GRAND_WITCH_ID = Identifier.of("sparkwitch", "grand_witch");
    private static final Identifier SPARKWITCH_ACCOMPLICE_ID = Identifier.of("sparkwitch", "accomplice");
    // SparkWitch special accomplices block team wins exactly like the Accomplice.
    // SparkWitch 特殊共犯与共犯一样阻止队伍胜利。
    private static final Identifier SPARKWITCH_ABYSS_LISTENER_ID = Identifier.of("sparkwitch", "abyss_listener");
    private static final Identifier SPARKWITCH_POTION_GUNNER_ID = Identifier.of("sparkwitch", "potion_gunner");
    private static final Identifier SPARKWITCH_RIFTWALKER_ID = Identifier.of("sparkwitch", "riftwalker");
    // SparkWitch's Bewitched (魔化使) is an accomplice before promotion.
    // SparkWitch 魔化使是晋升前的共犯。
    private static final Identifier SPARKWITCH_BEWITCHED_ID = Identifier.of("sparkwitch", "bewitched");
    private static final Identifier SPARKWITCH_MURDEROUS_WITCH_ID = Identifier.of("sparkwitch", "murderous_witch");
    // SparkWitch's Insider shares the Corrupt Cop's team and keeps its win rule after the Corrupt Cop dies.
    // SparkWitch 内应与黑警同属一个阵营，黑警死后由内应继续承担该阵营的胜利规则。
    private static final Identifier SPARKWITCH_INSIDER_ID = Identifier.of("sparkwitch", "insider");
    private static final Identifier NOELLES_SHADOW_JESTER_ID = Identifier.of("noellesroles", "shadow_jester");

    private EffectiveWinService() {
    }

    static void register() {
        CheckWinCondition.EVENT.register(EffectiveWinService::checkWin);
    }

    public static boolean isRealOriginalKiller(PlayerEntity player, GameWorldComponent gameComponent) {
        return player != null
                && EffectiveTraitService.isOriginalKiller(gameComponent.getRole(player))
                && !EffectiveTraitService.hasConscience(player);
    }

    /** Resolves round-end winner membership through effective alignment.
     *  通过有效阵营判断回合结束时玩家是否属于胜利方。 */
    public static boolean didEffectiveTeamWin(
            GameFunctions.WinStatus winStatus,
            Role role,
            Collection<Identifier> traits
    ) {
        return switch (winStatus) {
            case KILLERS -> EffectiveTraitService.isEffectiveKiller(role, traits);
            case PASSENGERS, TIME -> EffectiveTraitService.isEffectiveCivilian(role, traits);
            default -> false;
        };
    }

    /** Defers ordinary team wins so downstream blockers and custom faction wins can resolve their hooks.
     *  延后普通队伍胜利，让后续阻塞角色与自定义阵营胜利用自己的钩子结算。 */
    public static boolean shouldDeferTeamWinForBlockingNeutral(
            GameFunctions.WinStatus proposedWinStatus,
            Collection<Role> livingRoles
    ) {
        return shouldDeferTeamWinForBlockingNeutral(proposedWinStatus, livingRoles, false);
    }

    public static boolean shouldDeferTeamWinForBlockingNeutral(
            GameFunctions.WinStatus proposedWinStatus,
            Collection<Role> livingRoles,
            boolean noellesJesterBlocksTeamWin
    ) {
        if (proposedWinStatus != GameFunctions.WinStatus.KILLERS
                && proposedWinStatus != GameFunctions.WinStatus.PASSENGERS) {
            return false;
        }
        if (noellesJesterBlocksTeamWin) {
            return true;
        }
        if (livingRoles == null) {
            return false;
        }
        for (Role role : livingRoles) {
            if (isBlockingTeamWinNeutral(role)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBlockingTeamWinNeutral(Role role) {
        return role != null
                && (SPARKWITCH_GRAND_WITCH_ID.equals(role.identifier())
                || SPARKWITCH_ACCOMPLICE_ID.equals(role.identifier())
                || SPARKWITCH_ABYSS_LISTENER_ID.equals(role.identifier())
                || SPARKWITCH_POTION_GUNNER_ID.equals(role.identifier())
                || SPARKWITCH_RIFTWALKER_ID.equals(role.identifier())
                || SPARKWITCH_BEWITCHED_ID.equals(role.identifier())
                || SPARKWITCH_MURDEROUS_WITCH_ID.equals(role.identifier())
                || Noellesroles.CORRUPT_COP_ID.equals(role.identifier())
                || SPARKWITCH_INSIDER_ID.equals(role.identifier())
                || Noellesroles.TAOTIE_ID.equals(role.identifier()));
    }

    /** Mirrors NoellesRoles' Jester blocker without making ordinary Jester block team wins.
     *  同步 NoellesRoles 的小丑阻止规则，但普通小丑不会阻止队伍胜利。 */
    public static boolean shouldDeferTeamWinForNoellesJester(
            Role role,
            boolean inPsychoMode,
            boolean transitioning
    ) {
        return role != null
                && Noellesroles.JESTER_ID.equals(role.identifier())
                && (inPsychoMode || transitioning);
    }

    /** Defers ordinary team wins while NoellesRoles' bound Shadow Jesters need showdown routing.
     *  当 NoellesRoles 命运绑定双影小丑需要进入/维持谢幕时，延后普通队伍胜利。 */
    public static boolean shouldDeferTeamWinForNoellesShadowJester(
            Role role,
            boolean allied,
            boolean showdownActive
    ) {
        return shouldDeferTeamWinForNoellesShadowJester(
                GameFunctions.WinStatus.KILLERS,
                GameFunctions.WinStatus.KILLERS,
                role,
                allied,
                showdownActive
        ) || shouldDeferTeamWinForNoellesShadowJester(
                GameFunctions.WinStatus.PASSENGERS,
                GameFunctions.WinStatus.PASSENGERS,
                role,
                allied,
                showdownActive
        );
    }

    /** Routes only Shadow Jester's killer-win takeover before showdown; active showdown keeps all ordinary wins deferred.
     *  双影谢幕开始前只接管杀手胜利；谢幕已开始后继续延后所有普通队伍胜利。 */
    public static boolean shouldDeferTeamWinForNoellesShadowJester(
            GameFunctions.WinStatus currentStatus,
            GameFunctions.WinStatus proposedWinStatus,
            Role role,
            boolean allied,
            boolean showdownActive
    ) {
        if (proposedWinStatus != GameFunctions.WinStatus.KILLERS
                && proposedWinStatus != GameFunctions.WinStatus.PASSENGERS) {
            return false;
        }
        if (role == null || !NOELLES_SHADOW_JESTER_ID.equals(role.identifier())) {
            return false;
        }
        if (showdownActive) {
            return true;
        }
        return allied
                && (currentStatus == GameFunctions.WinStatus.KILLERS
                || proposedWinStatus == GameFunctions.WinStatus.KILLERS);
    }

    static boolean preservesResolvedWinStatus(GameFunctions.WinStatus currentStatus) {
        return currentStatus == GameFunctions.WinStatus.TIME
                || currentStatus == GameFunctions.WinStatus.NEUTRAL;
    }

    private static CheckWinCondition.WinResult checkWin(
            ServerWorld world,
            GameWorldComponent gameComponent,
            GameFunctions.WinStatus currentStatus
    ) {
        if (preservesResolvedWinStatus(currentStatus)) {
            return null;
        }
        List<ServerPlayerEntity> players = world.getPlayers();
        List<Role> livingRoles = new ArrayList<>();
        boolean realKillerAlive = false;
        boolean impostorAlive = false;
        boolean effectiveCivilianAlive = false;
        boolean noellesJesterBlocksTeamWin = false;
        boolean noellesShadowJesterDefersPassengerWin = false;
        boolean noellesShadowJesterDefersKillerWin = false;

        for (ServerPlayerEntity player : players) {
            if (!GameFunctions.isPlayerPlayingAndAlive(player) || !gameComponent.hasAnyRole(player)) {
                continue;
            }
            // SparkFactionAPI settlement-excluded roles (raw role, e.g. SparkWitch's Black Raven) take no part in this
            // census at all, so every team-win outcome reads exactly as if they were not in the round.
            // SparkFactionAPI 结算排除身份（按原始身份判断，如 SparkWitch 黑羽鸦）完全不参与本次统计，
            // 所有队伍胜负都与该玩家不在本局时完全一致。
            if (SettlementExclusions.isExcluded(gameComponent, player.getUuid())) {
                continue;
            }
            Role role = gameComponent.getRole(player);
            Collection<Identifier> traits = TraitPlayerComponent.KEY.get(player).getActiveTraitIds();
            livingRoles.add(role);
            if (role != null && Noellesroles.JESTER_ID.equals(role.identifier())) {
                JesterPlayerComponent jester = JesterPlayerComponent.KEY.get(player);
                if (shouldDeferTeamWinForNoellesJester(role, jester.inPsychoMode, jester.isTransitioning())) {
                    noellesJesterBlocksTeamWin = true;
                }
            }
            if (shouldDeferTeamWinForNoellesShadowJester(
                    player,
                    role,
                    currentStatus,
                    GameFunctions.WinStatus.PASSENGERS
            )) {
                noellesShadowJesterDefersPassengerWin = true;
            }
            if (shouldDeferTeamWinForNoellesShadowJester(
                    player,
                    role,
                    currentStatus,
                    GameFunctions.WinStatus.KILLERS
            )) {
                noellesShadowJesterDefersKillerWin = true;
            }
            if (EffectiveTraitService.isOriginalKiller(role) && !EffectiveTraitService.hasConscience(traits)) {
                realKillerAlive = true;
            }
            if (EffectiveTraitService.hasImpostor(traits)) {
                impostorAlive = true;
            }
            if (EffectiveTraitService.isEffectiveCivilian(role, traits)) {
                effectiveCivilianAlive = true;
            }
        }

        if (!realKillerAlive) {
            boolean shadowJesterShowdownStarted = hasStartedNoellesShadowJesterShowdown(players, gameComponent);
            killUnsupportedImpostors(players, gameComponent, false, shadowJesterShowdownStarted);
            // Impostors kept by the showdown are its killer-side opponents: the killer-win check below and NoellesRoles
            // settle the round instead of handing it to passengers.
            // 被谢幕保留的内鬼是杀手方对手：交给下方杀手胜利判定与 NoellesRoles 结算，而不是直接判好人胜。
            if (!(shadowJesterShowdownStarted && impostorAlive)) {
                if (shouldDeferTeamWinForBlockingNeutral(
                        GameFunctions.WinStatus.PASSENGERS,
                        livingRoles,
                        noellesJesterBlocksTeamWin || noellesShadowJesterDefersPassengerWin
                )) {
                    return null;
                }
                return CheckWinCondition.WinResult.allow(GameFunctions.WinStatus.PASSENGERS);
            }
        }
        if (!effectiveCivilianAlive) {
            if (shouldDeferTeamWinForBlockingNeutral(
                    GameFunctions.WinStatus.KILLERS,
                    livingRoles,
                    noellesJesterBlocksTeamWin || noellesShadowJesterDefersKillerWin
            )) {
                return null;
            }
            return CheckWinCondition.WinResult.allow(GameFunctions.WinStatus.KILLERS);
        }
        if (currentStatus == GameFunctions.WinStatus.PASSENGERS || currentStatus == GameFunctions.WinStatus.KILLERS) {
            return CheckWinCondition.WinResult.block();
        }
        return null;
    }

    private static boolean shouldDeferTeamWinForNoellesShadowJester(
            PlayerEntity player,
            Role role,
            GameFunctions.WinStatus currentStatus,
            GameFunctions.WinStatus proposedWinStatus
    ) {
        // Runs for every living player each tick, so only Shadow Jesters read NoellesRoles' component.
        // 每 tick 对每个存活玩家都会执行，因此只有双影小丑才读取 NoellesRoles 组件。
        if (player == null || !isNoellesShadowJester(role)) {
            return false;
        }
        ShadowJesterPlayerComponent jester = ShadowJesterPlayerComponent.KEY.get(player);
        return shouldDeferTeamWinForNoellesShadowJester(
                currentStatus,
                proposedWinStatus,
                role,
                jester.isAllied(),
                jester.isShowdownActive()
        );
    }

    private static boolean isNoellesShadowJester(Role role) {
        return role != null && NOELLES_SHADOW_JESTER_ID.equals(role.identifier());
    }

    /** Kills Impostors once their real killer-side support is gone, even if neutral blockers keep the round active,
     *  unless a Shadow Jester showdown has started and still needs them as opponents.
     *  当真实杀手支持消失时清理内鬼，即使中立阻塞者让回合继续；但双影谢幕开始后内鬼仍是谢幕对手，不再清理。 */
    public static void killUnsupportedImpostorsIfNoRealKillers(ServerWorld world, GameWorldComponent gameComponent) {
        if (world == null
                || gameComponent == null
                || gameComponent.getGameStatus() != GameWorldComponent.GameStatus.ACTIVE) {
            return;
        }
        List<ServerPlayerEntity> players = world.getPlayers();
        boolean realKillerAlive = false;
        for (ServerPlayerEntity player : players) {
            if (!GameFunctions.isPlayerPlayingAndAlive(player) || !gameComponent.hasAnyRole(player)) {
                continue;
            }
            // Same census as the team-win listener: settlement-excluded players never keep the killer side alive.
            // 与队伍胜利监听相同的统计口径：结算排除玩家永远不算作存活的杀手方。
            if (SettlementExclusions.isExcluded(gameComponent, player.getUuid())) {
                continue;
            }
            if (isRealOriginalKiller(player, gameComponent)) {
                realKillerAlive = true;
                break;
            }
        }
        killUnsupportedImpostors(
                players,
                gameComponent,
                realKillerAlive,
                !realKillerAlive && hasStartedNoellesShadowJesterShowdown(players, gameComponent)
        );
    }

    /** A started Shadow Jester showdown keeps Impostors in it as killer-side opponents, so they no longer self-realize.
     *  双影谢幕开始后内鬼作为杀手方对手留在谢幕中，不再自我觉悟。 */
    public static boolean shouldSelfRealizeUnsupportedImpostor(
            Collection<Identifier> traits,
            boolean playerAlive,
            boolean playerInRound,
            boolean realOriginalKillerAlive,
            boolean shadowJesterShowdownStarted
    ) {
        return playerAlive
                && playerInRound
                && !realOriginalKillerAlive
                && !shadowJesterShowdownStarted
                && traits != null
                && EffectiveTraitService.hasImpostor(traits);
    }

    /** NoellesRoles keeps showdownActive on fallen Shadow Jesters until the round resets, so this stays true after they die.
     *  NoellesRoles 在回合重置前不会清除阵亡双影小丑的 showdownActive，因此小丑死后仍视为谢幕已开始。 */
    private static boolean hasStartedNoellesShadowJesterShowdown(
            List<ServerPlayerEntity> players,
            GameWorldComponent gameComponent
    ) {
        for (ServerPlayerEntity player : players) {
            if (isNoellesShadowJester(gameComponent.getRole(player))
                    && ShadowJesterPlayerComponent.KEY.get(player).isShowdownActive()) {
                return true;
            }
        }
        return false;
    }

    private static void killUnsupportedImpostors(
            List<ServerPlayerEntity> players,
            GameWorldComponent gameComponent,
            boolean realOriginalKillerAlive,
            boolean shadowJesterShowdownStarted
    ) {
        // Either flag already rules out every player below; skip the per-player trait reads.
        // 任一标记成立时下方所有玩家都不会觉悟；直接跳过逐个读取天赋。
        if (realOriginalKillerAlive || shadowJesterShowdownStarted) {
            return;
        }
        for (ServerPlayerEntity player : players) {
            if (shouldSelfRealizeUnsupportedImpostor(
                    TraitPlayerComponent.KEY.get(player).getActiveTraitIds(),
                    GameFunctions.isPlayerPlayingAndAlive(player),
                    gameComponent.hasAnyRole(player),
                    realOriginalKillerAlive,
                    shadowJesterShowdownStarted
            )) {
                GameFunctions.killPlayer(player, true, null, SELF_REALIZATION, true);
            }
        }
    }
}
