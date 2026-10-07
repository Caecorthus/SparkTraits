package dev.caecorthus.sparktraits.impl.traits.civilian.laststand;

import dev.caecorthus.sparktraits.SparkTraits;
import dev.caecorthus.sparktraits.api.TraitAssignmentReason;
import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.component.TraitWorldComponent;
import dev.caecorthus.sparktraits.impl.compatibility.sparkfactionapi.SparkFactionApiEffectiveFactionBridge;
import dev.caecorthus.sparktraits.impl.replay.SparkTraitsReplayEvents;
import dev.caecorthus.sparktraits.impl.traits.civilian.impostor.ImpostorTrait;
import dev.caecorthus.sparktraits.impl.traits.killer.conscience.ConscienceTrait;
import dev.caecorthus.sparktraits.mixin.RoleHistoryComponentAccessor;
import dev.caecorthus.sparkfactionapi.api.FactionDefinition;
import dev.caecorthus.sparkfactionapi.api.FactionIds;
import dev.caecorthus.sparkfactionapi.api.SparkFactionApi;
import dev.caecorthus.sparkfactionapi.api.replay.SparkReplayApi;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.api.event.BlackoutEffect;
import dev.doctor4t.wathe.api.event.CheckWinCondition;
import dev.doctor4t.wathe.api.event.RoleAssigned;
import dev.doctor4t.wathe.cca.GameTimeComponent;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.cca.RoleHistoryComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.game.rotation.GameEntry;
import dev.doctor4t.wathe.game.rotation.RoleCategory;
import dev.doctor4t.wathe.index.WatheItems;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.professor.IronManPlayerComponent;
import org.jetbrains.annotations.Nullable;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.UUID;
import dev.caecorthus.sparktraits.impl.effective.EffectiveTraitService;

/**
 * Runs the Last Stand final moment when the civilian side has only consumed Last Stand players left.
 * 当平民阵营只剩已经消费过背水一战的玩家时，启动终局时刻。
 */
public final class LastStandFinalMomentService {
    static final int FINAL_MOMENT_TICKS = GameConstants.getInTicks(3, 0);
    private static final int FINAL_MONEY_REWARD = 1000;
    private static final int FINAL_MANA_REWARD = 1000;
    private static final int SPEED_AMPLIFIER = 2;
    private static final int TITLE_FADE_IN_TICKS = 10;
    private static final int TITLE_STAY_TICKS = 100;
    private static final int TITLE_FADE_OUT_TICKS = 10;
    private static final int UNKNOWN_FACTION_COLOR = 0xFFFFFF;
    private static final Identifier SPARKWITCH_FIEND_ID = Identifier.of("sparkwitch", "fiend");
    static final Identifier FINAL_MOMENT_TIMEOUT = SparkTraits.id("final_moment_timeout");

    private LastStandFinalMomentService() {
    }

    public static void register() {
        CheckWinCondition.EVENT.register(LastStandFinalMomentService::checkWin);
        BlackoutEffect.BEFORE.register(LastStandFinalMomentService::beforeBlackoutEffect);
    }

    private static CheckWinCondition.WinResult checkWin(
            ServerWorld world,
            GameWorldComponent gameComponent,
            GameFunctions.WinStatus currentStatus
    ) {
        if (gameComponent.getGameStatus() != GameWorldComponent.GameStatus.ACTIVE
                || currentStatus == GameFunctions.WinStatus.NEUTRAL) {
            return null;
        }
        if (TraitWorldComponent.KEY.get(world).isFinalMomentActive()) {
            return activeFinalMomentWinResult(true, currentStatus, snapshotPlayers(world, gameComponent));
        }
        return triggerFinalMomentIfEligible(world, gameComponent, currentStatus)
                ? CheckWinCondition.WinResult.block()
                : null;
    }

    static FinalMomentDecision evaluate(Collection<PlayerState> players) {
        List<UUID> finalPlayerUuids = new ArrayList<>();
        boolean livingOtherFaction = false;
        boolean blockedByOrdinaryCivilian = false;

        for (PlayerState player : players) {
            // The SparkWitch Fiend never affects a win (its own moment suspends wins in SparkWitch), so it is no opposing faction here.
            // SparkWitch 魔人从不影响胜负（魔人时刻由 SparkWitch 自行暂停胜利判定），因此这里不算作对立阵营。
            if (!player.alive() || isSparkWitchFiend(player.role())) {
                continue;
            }
            boolean effectiveCivilian = EffectiveTraitService.isEffectiveCivilian(player.role(), player.traitIds());
            if (effectiveCivilian) {
                if (player.lastStandTriggered()) {
                    finalPlayerUuids.add(player.uuid());
                } else {
                    blockedByOrdinaryCivilian = true;
                }
            } else {
                livingOtherFaction = true;
            }
        }

        return new FinalMomentDecision(
                !blockedByOrdinaryCivilian && livingOtherFaction && !finalPlayerUuids.isEmpty(),
                finalPlayerUuids
        );
    }

    static boolean shouldBlockOrdinaryWin(boolean finalMomentActive, GameFunctions.WinStatus currentStatus) {
        return finalMomentActive
                && (currentStatus == GameFunctions.WinStatus.PASSENGERS
                || currentStatus == GameFunctions.WinStatus.KILLERS);
    }

    public static boolean triggerFinalMomentIfEligible(
            ServerWorld world,
            GameWorldComponent gameComponent,
            GameFunctions.WinStatus currentStatus
    ) {
        if (world == null || gameComponent == null || currentStatus == null) {
            return false;
        }
        TraitWorldComponent traitWorld = TraitWorldComponent.KEY.get(world);
        List<PlayerState> players = snapshotPlayers(world, gameComponent);
        if (!canTriggerInactiveFinalMoment(
                gameComponent.getGameStatus() == GameWorldComponent.GameStatus.ACTIVE,
                traitWorld.isFinalMomentActive(),
                currentStatus,
                players
        )) {
            return false;
        }

        FinalMomentDecision decision = evaluate(players);
        triggerFinalMoment(world, gameComponent, traitWorld, decision.finalPlayerUuids());
        return true;
    }

    static boolean canTriggerInactiveFinalMoment(
            boolean gameActive,
            boolean finalMomentActive,
            GameFunctions.WinStatus currentStatus,
            Collection<PlayerState> players
    ) {
        if (!gameActive
                || finalMomentActive
                || currentStatus == GameFunctions.WinStatus.TIME
                || currentStatus == GameFunctions.WinStatus.NEUTRAL) {
            return false;
        }
        return evaluate(players).shouldTrigger();
    }

    static @Nullable CheckWinCondition.WinResult activeFinalMomentWinResult(
            boolean finalMomentActive,
            GameFunctions.WinStatus currentStatus,
            Collection<PlayerState> players
    ) {
        if (!finalMomentActive
                || currentStatus == GameFunctions.WinStatus.TIME
                || currentStatus == GameFunctions.WinStatus.NEUTRAL) {
            return null;
        }
        GameFunctions.WinStatus survivorWinStatus = finalMomentSurvivorWinStatus(true, players);
        if (survivorWinStatus != null) {
            return CheckWinCondition.WinResult.allow(survivorWinStatus);
        }
        // Once the Final Moment Loose End is gone, let the remaining faction resolve normally.
        // 当终局亡命徒已经阵亡时，放行剩余阵营的正常结算。
        if (!hasLivingFinalMomentLooseEnd(true, players)) {
            return null;
        }
        if (shouldBlockOrdinaryWin(true, currentStatus)) {
            return CheckWinCondition.WinResult.block();
        }
        return null;
    }

    /**
     * Final Moment never ends as a time win: when the clock runs out, every Final Moment Loose End drops dead
     * and the round resolves for whoever is left.
     * 终局时刻不会以时间胜利结束：计时归零时所有终局亡命徒当场暴毙，回合按剩余玩家结算。
     */
    public static GameFunctions.WinStatus resolveFinalMomentTimeout(
            ServerWorld world,
            GameWorldComponent gameComponent,
            GameFunctions.WinStatus currentStatus
    ) {
        if (world == null
                || gameComponent == null
                || currentStatus != GameFunctions.WinStatus.TIME
                || gameComponent.getGameStatus() != GameWorldComponent.GameStatus.ACTIVE
                || !TraitWorldComponent.KEY.get(world).isFinalMomentActive()) {
            return currentStatus;
        }
        for (ServerPlayerEntity player : livingPlayers(world, gameComponent)) {
            if (isFinalMomentLooseEnd(player)) {
                GameFunctions.killPlayer(player, true, null, FINAL_MOMENT_TIMEOUT, true);
            }
        }
        return timedOutFinalMomentStatus(snapshotPlayers(world, gameComponent));
    }

    static GameFunctions.WinStatus timedOutFinalMomentStatus(Collection<PlayerState> players) {
        for (PlayerState player : players) {
            if (player.alive() && EffectiveTraitService.isEffectiveCivilian(player.role(), player.traitIds())) {
                // A civilian who outlasted the clock keeps the ordinary time win.
                // 撑过计时的平民仍按普通时间胜利结算。
                return GameFunctions.WinStatus.TIME;
            }
        }
        // With the civilian side wiped out, KILLERS lets the effective-team and neutral listeners settle the survivors.
        // 平民阵营已全灭，交给 KILLERS 让有效阵营与中立监听器结算剩余玩家。
        return GameFunctions.WinStatus.KILLERS;
    }

    public static boolean shouldCancelRoundEndFinalization(
            ServerWorld world,
            GameWorldComponent gameComponent,
            GameFunctions.WinStatus currentStatus
    ) {
        if (world == null || gameComponent == null || currentStatus == null) {
            return false;
        }
        return shouldCancelRoundEndFinalization(
                TraitWorldComponent.KEY.get(world).isFinalMomentActive(),
                currentStatus,
                snapshotPlayers(world, gameComponent)
        );
    }

    static boolean shouldCancelRoundEndFinalization(
            boolean finalMomentActive,
            GameFunctions.WinStatus currentStatus,
            Collection<PlayerState> players
    ) {
        CheckWinCondition.WinResult result = activeFinalMomentWinResult(finalMomentActive, currentStatus, players);
        return result != null && result.status() == GameFunctions.WinStatus.NONE;
    }

    static boolean hasLivingFinalMomentLooseEnd(
            boolean finalMomentActive,
            Collection<PlayerState> players
    ) {
        if (!finalMomentActive) {
            return false;
        }
        for (PlayerState player : players) {
            if (player.alive()
                    && player.lastStandTriggered()
                    && isLooseEndRole(player.role())) {
                return true;
            }
        }
        return false;
    }

    static @Nullable GameFunctions.WinStatus finalMomentSurvivorWinStatus(
            boolean finalMomentActive,
            Collection<PlayerState> players
    ) {
        if (!finalMomentActive) {
            return null;
        }
        PlayerState onlyLivingPlayer = null;
        int livingPlayers = 0;
        for (PlayerState player : players) {
            // A living SparkWitch Fiend must not delay the survivor's win (D1).
            // 存活的 SparkWitch 魔人不得拖延幸存者的胜利（D1）。
            if (!player.alive() || isSparkWitchFiend(player.role())) {
                continue;
            }
            livingPlayers++;
            onlyLivingPlayer = player;
            if (livingPlayers > 1) {
                return null;
            }
        }
        if (onlyLivingPlayer == null
                || !onlyLivingPlayer.lastStandTriggered()
                || onlyLivingPlayer.role() == null
                || !WatheRoles.LOOSE_END.identifier().equals(onlyLivingPlayer.role().identifier())) {
            return null;
        }
        // Final Moment is a Last Stand civilian-side comeback, so the survivor resolves as passengers.
        // 终局时刻属于背水一战的平民阵营翻盘，因此最后存活者按平民阵营胜利结算。
        return GameFunctions.WinStatus.PASSENGERS;
    }

    public static int finalMomentKnifeCooldown(ServerPlayerEntity player, Item item, int duration) {
        if (player == null) {
            return duration;
        }
        ServerWorld world = player.getServerWorld();
        return finalMomentKnifeCooldown(
                duration,
                TraitWorldComponent.KEY.get(world).isFinalMomentActive(),
                GameWorldComponent.KEY.get(world).getRole(player),
                LastStandService.hasTriggeredThisRound(world, player.getUuid()),
                item == WatheItems.KNIFE
        );
    }

    static int finalMomentKnifeCooldown(
            int duration,
            boolean finalMomentActive,
            @Nullable Role role,
            boolean lastStandTriggered,
            boolean knife
    ) {
        if (finalMomentActive
                && knife
                && lastStandTriggered
                && isLooseEndRole(role)) {
            return 0;
        }
        return duration;
    }

    /**
     * Final Moment color of {@code target} as {@code viewer} sees it.
     * 终局时刻中 {@code viewer} 看到的 {@code target} 高亮颜色。
     *
     * <p>Clients get only the public Conscience/Impostor flags for other players, never their full trait list, so
     * the faction flip reads those flags. {@code killerDisguiseColor} is SparkStrength's Coroner killer disguise,
     * or null when it is absent.</p>
     * <p>客户端只会收到其他玩家公开的善良/内鬼标记，不会收到完整天赋列表，因此阵营翻转按这两个标记判断。
     * {@code killerDisguiseColor} 是 SparkStrength 验尸官的杀手伪装色，缺失时为 null。</p>
     */
    public static int finalMomentHighlightColorForViewer(
            PlayerEntity viewer,
            PlayerEntity target,
            GameWorldComponent gameComponent,
            boolean lastStandFinalMomentLooseEnd,
            @Nullable Integer killerDisguiseColor
    ) {
        Role role = gameComponent.getRole(target);
        TraitPlayerComponent targetTraits = TraitPlayerComponent.KEY.get(target);
        boolean targetHasConscience = targetTraits.isConscienceInstinctVisible();
        boolean targetHasImpostor = targetTraits.isImpostorInstinctVisible();
        Integer viewerColor = finalMomentViewerColor(
                role,
                targetHasConscience,
                targetHasImpostor,
                lastStandFinalMomentLooseEnd,
                EffectiveTraitService.isEffectiveKiller(viewer, gameComponent),
                killerDisguiseColor
        );
        if (viewerColor != null) {
            return viewerColor;
        }
        // Base faction, not the full effective chain: SparkWitch maps Murderous Witch to an ability-bridge
        // faction, but it is still a native neutral role.
        // 使用基础阵营而非完整有效阵营链：SparkWitch 把杀意魔女映射到能力桥接阵营，但它仍是原生中立职业。
        Identifier faction = finalMomentHighlightFaction(
                role,
                lastStandFinalMomentLooseEnd,
                SparkFactionApi.resolveBaseFaction(role),
                publicAlignmentTraits(targetHasConscience, targetHasImpostor)
        );
        return SparkFactionApi.getFaction(faction)
                .map(FactionDefinition::color)
                .orElse(UNKNOWN_FACTION_COLOR);
    }

    /** Viewer-specific colors that replace the faction color, or null to show the faction color.
     *  替代阵营色的观察者专属颜色；返回 null 时显示阵营色。 */
    static @Nullable Integer finalMomentViewerColor(
            @Nullable Role role,
            boolean targetHasConscience,
            boolean targetHasImpostor,
            boolean lastStandFinalMomentLooseEnd,
            boolean viewerIsEffectiveKiller,
            @Nullable Integer killerDisguiseColor
    ) {
        // A public Conscience/Impostor flag outranks the disguise, so one player never shows two clues at once.
        // 公开的善良/内鬼标记优先于伪装色，避免同一玩家同时显示两种提示。
        if (killerDisguiseColor != null && !targetHasConscience && !targetHasImpostor) {
            return killerDisguiseColor;
        }
        if (lastStandFinalMomentLooseEnd && isLooseEndRole(role)) {
            return null;
        }
        // Effective killers keep the Impostor's blue clue instead of a plain killer color.
        // 有效杀手观察者仍看到内鬼蓝色，而不是普通杀手色。
        if (viewerIsEffectiveKiller && targetHasImpostor) {
            return EffectiveTraitService.IMPOSTOR_INSTINCT_COLOR;
        }
        return null;
    }

    /** The alignment-flip traits a client can see on another player, from its public instinct flags.
     *  客户端能从公开本能标记看到的其他玩家阵营翻转天赋。 */
    static List<Identifier> publicAlignmentTraits(boolean conscience, boolean impostor) {
        List<Identifier> traits = new ArrayList<>(2);
        if (conscience) {
            traits.add(ConscienceTrait.ID);
        }
        if (impostor) {
            traits.add(ImpostorTrait.ID);
        }
        return traits;
    }

    /** Picks the faction whose registered SparkFactionAPI color Final Moment shows.
     *  选出终局时刻高亮所用的阵营，颜色取该阵营在 SparkFactionAPI 中登记的阵营色。 */
    static Identifier finalMomentHighlightFaction(
            @Nullable Role role,
            boolean lastStandFinalMomentLooseEnd,
            @Nullable Identifier baseFaction,
            @Nullable Collection<Identifier> traits
    ) {
        if (lastStandFinalMomentLooseEnd && isLooseEndRole(role)) {
            return FactionIds.CIVILIAN;
        }
        if (baseFaction == null) {
            return FactionIds.NONE;
        }
        // Impostor/Conscience flips keep flipped players from leaking their base-role faction.
        // 内鬼/善良的阵营翻转避免泄露原职业阵营。
        Identifier flipped = SparkFactionApiEffectiveFactionBridge.resolveEffectiveFaction(traits, baseFaction);
        return flipped == null ? baseFaction : flipped;
    }

    public static boolean didFinalMomentPlayerWin(
            GameFunctions.WinStatus winStatus,
            @Nullable Role role,
            boolean lastStandFinalMomentLooseEnd
    ) {
        // Running out the clock kills the Loose End, so only a passenger win counts for it.
        // 拖到计时结束会让亡命徒暴毙，因此只有平民胜利才算它获胜。
        return lastStandFinalMomentLooseEnd
                && isLooseEndRole(role)
                && winStatus == GameFunctions.WinStatus.PASSENGERS;
    }

    static boolean isFinalMomentLooseEndBlackoutImmune(
            boolean finalMomentActive,
            @Nullable Role role,
            boolean lastStandTriggered
    ) {
        return finalMomentActive
                && lastStandTriggered
                && isLooseEndRole(role);
    }

    static boolean isFinalMomentLooseEnd(
            boolean finalMomentActive,
            boolean markedFinalMomentLooseEnd,
            @Nullable Role role
    ) {
        return finalMomentActive && markedFinalMomentLooseEnd && isLooseEndRole(role);
    }

    public static boolean isFinalMomentLooseEnd(@Nullable PlayerEntity player) {
        if (player == null || player.getWorld() == null) {
            return false;
        }
        TraitWorldComponent traitWorld = TraitWorldComponent.KEY.get(player.getWorld());
        Role role = GameWorldComponent.KEY.get(player.getWorld()).getRole(player);
        return isFinalMomentLooseEnd(
                traitWorld.isFinalMomentActive(),
                traitWorld.isFinalMomentLooseEnd(player.getUuid()),
                role
        );
    }

    private static BlackoutEffect.BlackoutResult beforeBlackoutEffect(ServerPlayerEntity player, int durationTicks) {
        if (!(player.getWorld() instanceof ServerWorld world)) {
            return null;
        }
        GameWorldComponent gameComponent = GameWorldComponent.KEY.get(world);
        if (!isFinalMomentLooseEndBlackoutImmune(
                TraitWorldComponent.KEY.get(world).isFinalMomentActive(),
                gameComponent.getRole(player),
                LastStandService.hasTriggeredThisRound(world, player.getUuid())
        )) {
            return null;
        }

        // Match Wathe's killer blackout branch: cancel blindness and grant night vision.
        // 对齐 wathe 杀手熄灯分支：取消失明，并补发夜视。
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

    private static List<PlayerState> snapshotPlayers(ServerWorld world, GameWorldComponent gameComponent) {
        List<PlayerState> players = new ArrayList<>();
        for (UUID uuid : gameComponent.getAllPlayers()) {
            if (!(world.getPlayerByUuid(uuid) instanceof ServerPlayerEntity player)) {
                continue;
            }
            players.add(new PlayerState(
                    uuid,
                    gameComponent.getRole(player),
                    TraitPlayerComponent.KEY.get(player).getActiveTraitIds(),
                    gameComponent.hasAnyRole(player) && GameFunctions.isPlayerPlayingAndAlive(player),
                    LastStandService.hasTriggeredThisRound(world, uuid)
            ));
        }
        return players;
    }

    private static void triggerFinalMoment(
            ServerWorld world,
            GameWorldComponent gameComponent,
            TraitWorldComponent traitWorld,
            List<UUID> finalPlayerUuids
    ) {
        traitWorld.setFinalMomentActive(true);
        SparkTraitsReplayEvents.recordFinalMomentStarted(world);

        for (UUID uuid : finalPlayerUuids) {
            if (world.getPlayerByUuid(uuid) instanceof ServerPlayerEntity player
                    && GameFunctions.isPlayerPlayingAndAlive(player)) {
                traitWorld.markFinalMomentLooseEnd(uuid);
                convertToLooseEnd(world, gameComponent, player);
            }
        }
        gameComponent.sync();

        for (ServerPlayerEntity player : livingPlayers(world, gameComponent)) {
            if (isSparkWitchFiend(gameComponent.getRole(player))) {
                continue;
            }
            PlayerShopComponent.KEY.get(player).addToBalance(FINAL_MONEY_REWARD);
            SparkWitchManaCompatibility.addMana(player, FINAL_MANA_REWARD);
        }

        GameTimeComponent.KEY.get(world).setTime(FINAL_MOMENT_TICKS);
        broadcastFinalMomentTitle(world);
    }

    private static void convertToLooseEnd(
            ServerWorld world,
            GameWorldComponent gameComponent,
            ServerPlayerEntity player
    ) {
        // Tag the role change so SparkFactionAPI's replay line carries the Final Moment cause.
        // 为身份变化打上原因标记，使 SparkFactionAPI 的回放行附带“终局时刻”原因。
        SparkReplayApi.withRoleChangeCause(
                SparkTraits.id("loose_end_conversion"),
                (UUID) null,
                () -> gameComponent.addRole(player, WatheRoles.LOOSE_END)
        );
        replaceLatestRoleHistoryEntry(world, player.getUuid(), WatheRoles.LOOSE_END);
        RoleAssigned.EVENT.invoker().assignRole(player, WatheRoles.LOOSE_END);
        SparkTraitsReplayEvents.recordLooseEndConversion(player);

        TraitPlayerComponent playerTraits = TraitPlayerComponent.KEY.get(player);
        playerTraits.replaceActiveTraitsForRuntime(
                FinalMomentOutlawLoadout.targetTraits(),
                TraitAssignmentReason.INTERNAL
        );
        TraitWorldComponent.KEY.get(world).snapshotRoundTraits(player.getUuid(), playerTraits.getActiveTraitIds());

        player.getInventory().clear();
        giveFinalItem(player, WatheItems.KNIFE);
        giveFinalItem(player, WatheItems.DERRINGER);
        giveFinalItem(player, WatheItems.CROWBAR);
        giveFinalItem(player, WatheItems.REVOLVER);
        clearFinalMomentInitialCooldown(player, WatheItems.KNIFE);
        clearFinalMomentInitialCooldown(player, WatheItems.DERRINGER);
        clearFinalMomentInitialCooldown(player, WatheItems.CROWBAR);
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, FINAL_MOMENT_TICKS + 20, SPEED_AMPLIFIER, false, false, true));
        IronManPlayerComponent.KEY.get(player).applyBuff();
    }

    private static void clearFinalMomentInitialCooldown(ServerPlayerEntity player, Item item) {
        if (shouldClearFinalMomentInitialCooldown(item == WatheItems.KNIFE)) {
            player.getItemCooldownManager().remove(item);
        }
    }

    static boolean shouldClearFinalMomentInitialCooldown(boolean knife) {
        return knife;
    }

    private static void giveFinalItem(ServerPlayerEntity player, Item item) {
        ItemStack stack = new ItemStack(item);
        if (!player.giveItemStack(stack)) {
            player.dropItem(stack, false);
        }
    }

    private static boolean isLooseEndRole(@Nullable Role role) {
        return role != null && WatheRoles.LOOSE_END.identifier().equals(role.identifier());
    }

    static boolean isSparkWitchFiend(@Nullable Role role) {
        return role != null && SPARKWITCH_FIEND_ID.equals(role.identifier());
    }

    private static List<ServerPlayerEntity> livingPlayers(ServerWorld world, GameWorldComponent gameComponent) {
        return world.getPlayers().stream()
                .filter(gameComponent::hasAnyRole)
                .filter(GameFunctions::isPlayerPlayingAndAlive)
                .toList();
    }

    private static void replaceLatestRoleHistoryEntry(ServerWorld world, UUID playerUuid, Role role) {
        RoleHistoryComponent roleHistory = RoleHistoryComponent.KEY.get(world.getScoreboard());
        Deque<GameEntry> playerHistory = ((RoleHistoryComponentAccessor) roleHistory).sparktraits$getHistory().get(playerUuid);
        if (playerHistory == null || playerHistory.isEmpty()) {
            return;
        }
        GameEntry latestEntry = playerHistory.removeLast();
        RoleCategory category = RoleHistoryComponent.categoryOf(role);
        playerHistory.addLast(new GameEntry(
                latestEntry.killerShare(),
                latestEntry.vigilanteShare(),
                latestEntry.neutralShare(),
                category,
                role.identifier().toString()
        ));
    }

    private static void broadcastFinalMomentTitle(ServerWorld world) {
        Text title = Text.literal("终局时刻")
                .formatted(Formatting.DARK_RED, Formatting.BOLD);
        Text subtitle = Text.translatable("subtitle.sparktraits.final_moment")
                .formatted(Formatting.RED);
        for (ServerPlayerEntity player : world.getPlayers()) {
            player.networkHandler.sendPacket(new TitleS2CPacket(title));
            player.networkHandler.sendPacket(new SubtitleS2CPacket(subtitle));
            player.networkHandler.sendPacket(new TitleFadeS2CPacket(
                    TITLE_FADE_IN_TICKS,
                    TITLE_STAY_TICKS,
                    TITLE_FADE_OUT_TICKS
            ));
            player.sendMessage(title, false);
        }
    }

    record PlayerState(
            UUID uuid,
            @Nullable Role role,
            Collection<Identifier> traitIds,
            boolean alive,
            boolean lastStandTriggered
    ) {
        PlayerState {
            traitIds = traitIds == null ? List.of() : List.copyOf(traitIds);
        }
    }

    record FinalMomentDecision(boolean shouldTrigger, List<UUID> finalPlayerUuids) {
        FinalMomentDecision {
            finalPlayerUuids = List.copyOf(finalPlayerUuids);
        }
    }

    /**
     * Optional SparkWitch bridge that avoids a hard compile-time dependency.
     * 可选 SparkWitch 桥接：不让 SparkTraits 对 SparkWitch 形成硬依赖。
     */
    private static final class SparkWitchManaCompatibility {
        private static final String SPARKWITCH_MOD_ID = "sparkwitch";
        private static final Identifier WITCH_PLAYER_COMPONENT_ID = Identifier.of(SPARKWITCH_MOD_ID, "player");

        private static ComponentKey<?> key;
        private static Method hasManaSystem;
        private static Method initializeMana;
        private static Method addMana;
        private static boolean initialized;

        private SparkWitchManaCompatibility() {
        }

        static void addMana(ServerPlayerEntity player, int amount) {
            if (amount <= 0 || !FabricLoader.getInstance().isModLoaded(SPARKWITCH_MOD_ID) || !initialize()) {
                return;
            }
            try {
                Object component = key.maybeGet(player).orElse(null);
                if (component == null) {
                    return;
                }
                initializeMethods(component.getClass());
                if (!Boolean.TRUE.equals(hasManaSystem.invoke(component))) {
                    initializeMana.invoke(component);
                }
                addMana.invoke(component, amount);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | RuntimeException exception) {
                SparkTraits.LOGGER.debug("Unable to grant SparkWitch final moment mana", exception);
            }
        }

        private static boolean initialize() {
            if (initialized) {
                return key != null;
            }
            initialized = true;
            key = ComponentRegistry.get(WITCH_PLAYER_COMPONENT_ID);
            return key != null;
        }

        private static void initializeMethods(Class<?> componentClass) throws NoSuchMethodException {
            if (hasManaSystem != null && hasManaSystem.getDeclaringClass() == componentClass) {
                return;
            }
            hasManaSystem = componentClass.getMethod("hasManaSystem");
            initializeMana = componentClass.getMethod("initializeMana");
            addMana = componentClass.getMethod("addMana", int.class);
        }
    }
}
