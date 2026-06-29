package dev.caecorthus.sparktraits.impl;

import dev.caecorthus.sparktraits.SparkTraits;
import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.component.TraitWorldComponent;
import dev.caecorthus.sparktraits.mixin.RoleHistoryComponentAccessor;
import dev.doctor4t.wathe.api.Faction;
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

/**
 * Runs the Last Stand final moment when the good side has only consumed Last Stand players left.
 * 当好人阵营只剩已经消费过背水一战的玩家时，启动终局时刻。
 */
public final class LastStandFinalMomentService {
    static final int FINAL_MOMENT_TICKS = GameConstants.getInTicks(3, 0);
    private static final int FINAL_MONEY_REWARD = 1000;
    private static final int FINAL_MANA_REWARD = 1000;
    private static final int SPEED_AMPLIFIER = 2;
    private static final int TITLE_FADE_IN_TICKS = 10;
    private static final int TITLE_STAY_TICKS = 100;
    private static final int TITLE_FADE_OUT_TICKS = 10;

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
        TraitWorldComponent traitWorld = TraitWorldComponent.KEY.get(world);
        if (gameComponent.getGameStatus() != GameWorldComponent.GameStatus.ACTIVE
                || currentStatus == GameFunctions.WinStatus.NEUTRAL) {
            return null;
        }
        if (traitWorld.isFinalMomentActive()) {
            return activeFinalMomentWinResult(true, currentStatus, snapshotPlayers(world, gameComponent));
        }
        if (currentStatus == GameFunctions.WinStatus.TIME) {
            return null;
        }

        FinalMomentDecision decision = evaluate(snapshotPlayers(world, gameComponent));
        if (!decision.shouldTrigger()) {
            return null;
        }

        triggerFinalMoment(world, gameComponent, traitWorld, decision.finalPlayerUuids());
        return CheckWinCondition.WinResult.block();
    }

    static FinalMomentDecision evaluate(Collection<PlayerState> players) {
        List<UUID> finalPlayerUuids = new ArrayList<>();
        boolean livingOtherFaction = false;
        boolean blockedByOrdinaryGood = false;

        for (PlayerState player : players) {
            if (!player.alive()) {
                continue;
            }
            boolean effectiveGood = EffectiveTraitService.isEffectiveCivilian(player.role(), player.traitIds());
            if (effectiveGood) {
                if (player.lastStandTriggered()) {
                    finalPlayerUuids.add(player.uuid());
                } else {
                    blockedByOrdinaryGood = true;
                }
            } else {
                livingOtherFaction = true;
            }
        }

        return new FinalMomentDecision(
                !blockedByOrdinaryGood && livingOtherFaction && !finalPlayerUuids.isEmpty(),
                finalPlayerUuids
        );
    }

    static boolean shouldBlockOrdinaryWin(boolean finalMomentActive, GameFunctions.WinStatus currentStatus) {
        return finalMomentActive
                && (currentStatus == GameFunctions.WinStatus.PASSENGERS
                || currentStatus == GameFunctions.WinStatus.KILLERS);
    }

    static @Nullable CheckWinCondition.WinResult activeFinalMomentWinResult(
            boolean finalMomentActive,
            GameFunctions.WinStatus currentStatus,
            Collection<PlayerState> players
    ) {
        if (!finalMomentActive) {
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
            if (!player.alive()) {
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
        // Final Moment is a Last Stand good-side comeback, so the survivor resolves as passengers.
        // 终局时刻属于背水一战的好人翻盘，因此最后存活者按好人胜利结算。
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

    public static int finalMomentHighlightColor(@Nullable Role role) {
        return finalMomentHighlightColor(role, false);
    }

    public static int finalMomentHighlightColor(@Nullable Role role, boolean lastStandFinalMomentLooseEnd) {
        if (lastStandFinalMomentLooseEnd && isLooseEndRole(role)) {
            return 0x36E51B;
        }
        Faction faction = role == null ? Faction.NONE : role.getFaction();
        return switch (faction) {
            case CIVILIAN -> 0x36E51B;
            case KILLER -> 0xC13838;
            case NEUTRAL -> 0xB567FF;
            case NONE -> 0xFFFFFF;
        };
    }

    public static boolean didFinalMomentPlayerWin(
            GameFunctions.WinStatus winStatus,
            @Nullable Role role,
            boolean lastStandFinalMomentLooseEnd
    ) {
        return lastStandFinalMomentLooseEnd
                && isLooseEndRole(role)
                && (winStatus == GameFunctions.WinStatus.PASSENGERS
                || winStatus == GameFunctions.WinStatus.TIME);
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

        for (UUID uuid : finalPlayerUuids) {
            if (world.getPlayerByUuid(uuid) instanceof ServerPlayerEntity player
                    && GameFunctions.isPlayerPlayingAndAlive(player)) {
                traitWorld.markFinalMomentLooseEnd(uuid);
                convertToLooseEnd(world, gameComponent, player);
            }
        }
        gameComponent.sync();

        for (ServerPlayerEntity player : livingPlayers(world, gameComponent)) {
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
        gameComponent.addRole(player, WatheRoles.LOOSE_END);
        replaceLatestRoleHistoryEntry(world, player.getUuid(), WatheRoles.LOOSE_END);
        RoleAssigned.EVENT.invoker().assignRole(player, WatheRoles.LOOSE_END);

        player.getInventory().clear();
        giveFinalItem(player, WatheItems.KNIFE);
        giveFinalItem(player, WatheItems.DERRINGER);
        giveFinalItem(player, WatheItems.CROWBAR);
        clearFinalMomentInitialCooldown(player, WatheItems.KNIFE);
        clearFinalMomentInitialCooldown(player, WatheItems.DERRINGER);
        clearFinalMomentInitialCooldown(player, WatheItems.CROWBAR);
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, FINAL_MOMENT_TICKS + 20, SPEED_AMPLIFIER, false, false, true));
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
