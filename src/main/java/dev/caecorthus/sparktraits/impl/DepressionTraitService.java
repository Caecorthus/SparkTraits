package dev.caecorthus.sparktraits.impl;

import dev.caecorthus.sparktraits.SparkTraits;
import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.api.event.DoorInteraction;
import dev.doctor4t.wathe.api.event.KillPlayer;
import dev.doctor4t.wathe.api.event.PsychoType;
import dev.doctor4t.wathe.api.event.ShopPurchase;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import dev.doctor4t.wathe.cca.PlayerPsychoComponent;
import dev.doctor4t.wathe.cca.PlayerStaminaComponent;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheEntities;
import dev.doctor4t.wathe.index.WatheAttributes;
import dev.doctor4t.wathe.index.WatheItems;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.jester.JesterPlayerComponent;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Runtime and pure rules for the Depression good-side trait.
 * 抑郁好人天赋的运行时逻辑与纯规则集中入口。
 */
public final class DepressionTraitService {
    public static final int COLOR = 0x6B7280;
    public static final int ATTACKER_HIGHLIGHT_COLOR = 0xFF0000;
    public static final int VICTIM_HIGHLIGHT_COLOR = 0xFF9F1C;
    public static final int MIN_RANDOM_PLAYERS = 24;
    public static final int SUICIDE_INTERVAL_TICKS = GameConstants.getInTicks(0, 30);
    public static final int FAKE_DEATH_TICKS = GameConstants.getInTicks(0, 5);
    public static final float MOOD_DRAIN_MULTIPLIER = 1.5f;
    public static final float STAMINA_MULTIPLIER = 0.8f;
    public static final float GUARANTEED_TRIGGER_MOOD = -0.20f;
    public static final float FULL_GRAYSCALE_MOOD = 0.0f;
    public static final float MAX_SCREEN_EFFECT_STRENGTH = 0.75f;
    public static final float POST_PSYCHO_MOOD = 0.7f;
    public static final double MIN_PSYCHO_COUNTER_CHANCE = 10.0;
    public static final int JESTER_MOMENT_PSYCHO_ARMOUR = 2;
    public static final int RAGE_LOOP_INTERVAL_TICKS = 1121;
    public static final Identifier APPRENTICE_WITCH_ID = Identifier.of("sparkwitch", "apprentice_witch");
    public static final Identifier DEPRESSION_STAMINA_MODIFIER_ID = SparkTraits.id("depression_stamina");
    public static final double DEPRESSION_STAMINA_MODIFIER_VALUE = -0.2;
    private static final float DEPRESSION_RANGE_SOUND_VOLUME = 3.0f;
    private static final float DEPRESSION_DIRECT_SOUND_VOLUME = 1.0f;
    private static final float DEPRESSION_SOUND_PITCH = 1.0f;
    private static final EntityAttributeModifier DEPRESSION_STAMINA_MODIFIER = new EntityAttributeModifier(
            DEPRESSION_STAMINA_MODIFIER_ID,
            DEPRESSION_STAMINA_MODIFIER_VALUE,
            EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
    );
    private static final Text PSYCHO_ACTIONBAR = Text.literal("我们今天都会死。一个比另一个晚一点罢了").withColor(ATTACKER_HIGHLIGHT_COLOR);
    private static final Text ATTACKER_ACTIONBAR = Text.literal("！！反击或逃跑！！").withColor(ATTACKER_HIGHLIGHT_COLOR);
    private static final Text ATTACKER_TITLE = Text.literal("跑").withColor(ATTACKER_HIGHLIGHT_COLOR);
    private static final Map<UUID, PendingState> pendingPlayers = new HashMap<>();
    private static final Map<UUID, ActiveState> activePlayers = new HashMap<>();
    private static final Set<UUID> forceMentalBreakdownDeaths = new java.util.HashSet<>();

    private DepressionTraitService() {
    }

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(DepressionTraitService::tickWorld);
        DoorInteraction.EVENT.register(DepressionTraitService::onDoorInteraction);
        ShopPurchase.BEFORE.register((player, entry, index) ->
                isPsychoActive(player) ? ShopPurchase.PurchaseResult.deny() : null);
    }

    public static boolean canSelectDepression(
            Role role,
            Collection<Identifier> selectedTraits,
            int startingPlayerCount,
            boolean enforceStartingPlayerCount
    ) {
        if (enforceStartingPlayerCount && startingPlayerCount < MIN_RANDOM_PLAYERS) {
            return false;
        }
        return GoodTraitService.canSelectNonUndercoverGoodTrait(role, selectedTraits)
                && role != WatheRoles.VIGILANTE
                && role != WatheRoles.VETERAN
                && !roleIdentifierEquals(role, Noellesroles.SURVIVAL_MASTER_ID)
                && !roleIdentifierEquals(role, APPRENTICE_WITCH_ID);
    }

    public static float depressionAdjustedMood(float currentMood, float proposedMood, Collection<Identifier> traits) {
        return depressionAdjustedMood(currentMood, proposedMood, traits, false);
    }

    public static float depressionAdjustedMood(
            float currentMood,
            float proposedMood,
            Collection<Identifier> traits,
            boolean depressionPsychoActive
    ) {
        if (traits == null
                || !traits.contains(GoodTraits.DEPRESSION)
                || proposedMood >= currentMood) {
            return proposedMood;
        }
        if (depressionPsychoActive) {
            return currentMood;
        }
        return currentMood - (currentMood - proposedMood) * MOOD_DRAIN_MULTIPLIER;
    }

    public static double triggerChance(float mood) {
        return linearChance(mood, GameConstants.MID_MOOD_THRESHOLD, GUARANTEED_TRIGGER_MOOD, 0.0, 100.0);
    }

    public static double psychoCounterChance(float mood) {
        if (mood > GameConstants.MID_MOOD_THRESHOLD) {
            return 0.0;
        }
        return linearChance(mood, GameConstants.MID_MOOD_THRESHOLD, FULL_GRAYSCALE_MOOD, MIN_PSYCHO_COUNTER_CHANCE, 100.0);
    }

    public static boolean shouldRunSuicideCountdown(float mood) {
        return triggerChance(mood) > 0.0;
    }

    public static boolean shouldPauseSuicideCountdown(
            boolean hasDepression,
            boolean playingAndAlive,
            boolean pending,
            boolean depressionPsychoActive,
            boolean wathePsychoActive
    ) {
        // Pause only Depression's own countdown during fake death or psycho windows.
        // 只暂停抑郁自己的倒计时；假死或疯魔窗口内不推进自毁判定。
        return !hasDepression || !playingAndAlive || pending || depressionPsychoActive || wathePsychoActive;
    }

    public static boolean shouldSuppressMentalBreakdown(
            boolean hasDepression,
            boolean depressionPsychoActive,
            boolean wathePsychoActive,
            Identifier deathReason
    ) {
        return hasDepression
                && (depressionPsychoActive || wathePsychoActive)
                && GameConstants.DeathReasons.MENTAL_BREAKDOWN.equals(deathReason);
    }

    public static float depressionPsychoMoodFloor(float mood) {
        return Math.max(mood, FULL_GRAYSCALE_MOOD);
    }

    public static float depressionPsychoRestoredMood() {
        return POST_PSYCHO_MOOD;
    }

    public static float depressionScreenEffectStrength(boolean hasDepression, boolean psychoActive, float mood) {
        if (!hasDepression) {
            return 0.0f;
        }
        if (psychoActive) {
            return MAX_SCREEN_EFFECT_STRENGTH;
        }
        if (mood >= GameConstants.MID_MOOD_THRESHOLD) {
            return 0.0f;
        }
        if (mood <= FULL_GRAYSCALE_MOOD) {
            return MAX_SCREEN_EFFECT_STRENGTH;
        }
        return MAX_SCREEN_EFFECT_STRENGTH
                * (GameConstants.MID_MOOD_THRESHOLD - mood)
                / (GameConstants.MID_MOOD_THRESHOLD - FULL_GRAYSCALE_MOOD);
    }

    public static float depressionScreenEffectStrength(
            boolean hasDepression,
            boolean psychoActive,
            int suicideTicks,
            float mood
    ) {
        return depressionScreenEffectStrength(hasDepression, psychoActive, mood);
    }

    public static int depressionStaminaMax(int maxSprintTime, boolean hasDepression) {
        if (!hasDepression || maxSprintTime < 0) {
            return maxSprintTime;
        }
        return Math.max(1, Math.round(maxSprintTime * STAMINA_MULTIPLIER));
    }

    public static float depressionRecoveredStamina(float previousTicks, float proposedTicks, boolean hasDepression) {
        if (!hasDepression || proposedTicks <= previousTicks) {
            return proposedTicks;
        }
        return previousTicks + (proposedTicks - previousTicks) * STAMINA_MULTIPLIER;
    }

    public static boolean shouldApplyDepressionStamina(Role role, boolean hasDepression, boolean psychoActive) {
        return hasDepression && !psychoActive && GlobalTraitService.hasFiniteStamina(role);
    }

    public static void applyDepressionStamina(ServerPlayerEntity player) {
        GameWorldComponent game = GameWorldComponent.KEY.get(player.getWorld());
        Role role = game == null ? null : game.getRole(player);
        TraitPlayerComponent traits = TraitPlayerComponent.KEY.get(player);
        if (!shouldApplyDepressionStamina(role, traits.hasActiveTrait(GoodTraits.DEPRESSION), isPsychoActive(player))) {
            return;
        }
        EntityAttributeInstance stamina = player.getAttributeInstance(WatheAttributes.MAX_SPRINT_TIME);
        if (stamina == null || stamina.hasModifier(DEPRESSION_STAMINA_MODIFIER_ID)) {
            return;
        }
        stamina.addTemporaryModifier(DEPRESSION_STAMINA_MODIFIER);
        PlayerStaminaComponent.KEY.get(player).sync();
    }

    public static void removeDepressionStamina(ServerPlayerEntity player) {
        EntityAttributeInstance stamina = player.getAttributeInstance(WatheAttributes.MAX_SPRINT_TIME);
        if (stamina == null) {
            return;
        }
        if (stamina.hasModifier(DEPRESSION_STAMINA_MODIFIER_ID)) {
            stamina.removeModifier(DEPRESSION_STAMINA_MODIFIER_ID);
        }
        PlayerStaminaComponent staminaComponent = PlayerStaminaComponent.KEY.get(player);
        if (!staminaComponent.isInfiniteStamina()) {
            int maxSprintTime = (int) stamina.getValue();
            staminaComponent.setMaxSprintTime(maxSprintTime);
            staminaComponent.setSprintingTicks(Math.min(staminaComponent.getSprintingTicks(), maxSprintTime));
            staminaComponent.sync();
        }
    }

    public static void applyFiniteStaminaPenalty(ServerPlayerEntity player, float previousSprintingTicks) {
        TraitPlayerComponent traits = TraitPlayerComponent.KEY.get(player);
        GameWorldComponent game = GameWorldComponent.KEY.get(player.getWorld());
        Role role = game == null ? null : game.getRole(player);
        if (!shouldApplyDepressionStamina(role, traits.hasActiveTrait(GoodTraits.DEPRESSION), isPsychoActive(player))) {
            return;
        }
        applyDepressionStamina(player);
        PlayerStaminaComponent stamina = PlayerStaminaComponent.KEY.get(player);
        if (stamina.isInfiniteStamina()) {
            return;
        }
        int maxSprintTime = stamina.getMaxSprintTime();
        float sprintingTicks = depressionRecoveredStamina(previousSprintingTicks, stamina.getSprintingTicks(), true);
        stamina.setSprintingTicks(Math.min(sprintingTicks, maxSprintTime));
        if (stamina.getSprintingTicks() <= 0) {
            stamina.setExhausted(true);
        }
    }

    public static boolean shouldMuteVoice(ServerPlayerEntity player) {
        return shouldMuteVoice(isPsychoActive(player));
    }

    public static boolean shouldMuteVoice(boolean psychoActive) {
        return psychoActive;
    }

    public static boolean shouldAllowLowMoodSprint(net.minecraft.entity.player.PlayerEntity player) {
        if (player == null) {
            return false;
        }
        return shouldAllowLowMoodSprint(
                TraitPlayerComponent.KEY.get(player).isDepressionPsychoActive(),
                isPsychoActive(player)
        );
    }

    public static boolean shouldAllowLowMoodSprint(boolean syncedPsychoActive, boolean runtimePsychoActive) {
        return syncedPsychoActive || runtimePsychoActive;
    }

    /**
     * Depression psycho is maintained only while both chase participants are still active players.
     * 只有疯魔者和目标都仍在存活游玩时，抑郁疯魔才继续维持。
     */
    public static boolean shouldEndActivePsycho(boolean playerAlive, boolean attackerAlive) {
        return !playerAlive || !attackerAlive;
    }

    /**
     * Restores post-psycho stamina without forcing finite-role players into exhaustion.
     * 疯魔结束后恢复体力，避免有限体力角色被重置到 0 体力并立即疲惫。
     */
    public static PostPsychoStaminaState postPsychoStaminaState(int roleMaxSprintTime, int effectiveMaxSprintTime) {
        if (roleMaxSprintTime < 0) {
            return new PostPsychoStaminaState(-1, Integer.MAX_VALUE, false);
        }
        int maxSprintTime = Math.max(1, effectiveMaxSprintTime);
        return new PostPsychoStaminaState(maxSprintTime, maxSprintTime, false);
    }

    public static boolean shouldBlockInventoryInsert(ServerPlayerEntity player, ItemStack stack) {
        return shouldBlockInventoryInsert(isPsychoActive(player), stack.isOf(WatheItems.BAT));
    }

    public static boolean shouldBlockInventoryInsert(boolean psychoActive, boolean batStack) {
        return psychoActive && !batStack;
    }

    public static boolean shouldBlockInventoryInsert(net.minecraft.entity.player.PlayerEntity player, ItemStack stack) {
        return player instanceof ServerPlayerEntity serverPlayer && shouldBlockInventoryInsert(serverPlayer, stack);
    }

    public static boolean shouldBlockDrops(ServerPlayerEntity player) {
        return isPending(player);
    }

    public static int initialPsychoArmour(Role killerRole, boolean activeJesterMoment) {
        return activeJesterMoment && roleIdentifierEquals(killerRole, Noellesroles.JESTER_ID)
                ? JESTER_MOMENT_PSYCHO_ARMOUR
                : 0;
    }

    public static int maintainedPsychoArmour(int currentArmour, int maxArmour) {
        if (maxArmour <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(currentArmour, maxArmour));
    }

    public static boolean shouldPlayRageLoop(int ticksUntilNextLoop) {
        return ticksUntilNextLoop <= 0;
    }

    public static int nextRageLoopTicks(int ticksUntilNextLoop) {
        return ticksUntilNextLoop <= 0 ? RAGE_LOOP_INTERVAL_TICKS : ticksUntilNextLoop - 1;
    }

    public static SoundEvent meleeKillSound(boolean secondVariant) {
        return secondVariant ? SparkTraitsSounds.DEPRESSION_MELEE_KILL_2 : SparkTraitsSounds.DEPRESSION_MELEE_KILL_1;
    }

    public static boolean isPending(ServerPlayerEntity player) {
        return pendingPlayers.containsKey(player.getUuid());
    }

    public static boolean isPsychoActive(net.minecraft.entity.player.PlayerEntity player) {
        return player != null && activePlayers.containsKey(player.getUuid());
    }

    public static boolean isWathePsychoActive(net.minecraft.entity.player.PlayerEntity player) {
        return player != null && PlayerPsychoComponent.KEY.get(player).getPsychoTicks() > 0;
    }

    public static boolean shouldSuppressMentalBreakdown(net.minecraft.entity.player.PlayerEntity player, Identifier deathReason) {
        return shouldSuppressMentalBreakdown(
                player != null && TraitPlayerComponent.KEY.get(player).hasActiveTrait(GoodTraits.DEPRESSION),
                isPsychoActive(player),
                isWathePsychoActive(player),
                deathReason
        );
    }

    public static KillPlayer.KillResult beforeKill(ServerPlayerEntity victim, @Nullable ServerPlayerEntity killer, Identifier deathReason) {
        if (forceMentalBreakdownDeaths.remove(victim.getUuid())) {
            return null;
        }
        if (isPending(victim)) {
            return KillPlayer.KillResult.cancel();
        }
        if (killer == null || isPsychoActive(victim) || activePlayers.containsKey(victim.getUuid())) {
            return null;
        }
        TraitPlayerComponent victimTraits = TraitPlayerComponent.KEY.get(victim);
        if (!victimTraits.hasActiveTrait(GoodTraits.DEPRESSION)) {
            return null;
        }
        float mood = PlayerMoodComponent.KEY.get(victim).getMood();
        if (mood > GameConstants.MID_MOOD_THRESHOLD) {
            return null;
        }
        GameWorldComponent game = GameWorldComponent.KEY.get(victim.getWorld());
        Role killerRole = game.getRole(killer);
        Collection<Identifier> killerTraits = TraitPlayerComponent.KEY.get(killer).getActiveTraitIds();
        if (EffectiveTraitService.isEffectiveCivilian(killerRole, killerTraits)) {
            return null;
        }
        double roll = victim.getRandom().nextDouble() * 100.0;
        double chance = psychoCounterChance(mood);
        if (roll >= chance) {
            return null;
        }
        startPending(victim, killer, deathReason, initialPsychoArmour(
                killerRole,
                JesterPlayerComponent.KEY.get(killer).inPsychoMode
        ));
        return KillPlayer.KillResult.cancel();
    }

    public static void handleAfterKill(ServerPlayerEntity victim, @Nullable ServerPlayerEntity killer) {
        if (killer != null) {
            ActiveState killerState = activePlayers.get(killer.getUuid());
            if (killerState != null) {
                playRangeSound(victim, meleeKillSound(killer.getRandom().nextBoolean()));
                if (killerState.attackerUuid().equals(victim.getUuid())) {
                    playRangeSound(killer, SparkTraitsSounds.DEPRESSION_RAGE_TO_DOCILE);
                    endPsycho(killer, true, true);
                }
            }
        }
        if (activePlayers.containsKey(victim.getUuid())) {
            playRangeSound(victim, SparkTraitsSounds.DEPRESSION_SHYGUY_KILLED);
            endPsycho(victim, false, false);
        }
        clearPending(victim);
    }

    public static void clearPlayer(ServerPlayerEntity player) {
        clearPending(player);
        if (activePlayers.containsKey(player.getUuid())) {
            endPsycho(player, false, false);
        }
        TraitPlayerComponent.KEY.get(player).setDepressionSuicideTicks(-1);
        TraitPlayerComponent.KEY.get(player).setDepressionPsychoState(false, null);
        TraitPlayerComponent.KEY.get(player).setDepressionCounterTarget(null);
    }

    public static void clearRoundState(ServerWorld world) {
        for (UUID uuid : Set.copyOf(pendingPlayers.keySet())) {
            if (world.getPlayerByUuid(uuid) instanceof ServerPlayerEntity player) {
                clearPending(player);
            }
        }
        for (UUID uuid : Set.copyOf(activePlayers.keySet())) {
            if (world.getPlayerByUuid(uuid) instanceof ServerPlayerEntity player) {
                endPsycho(player, false, false);
            }
        }
        pendingPlayers.clear();
        activePlayers.clear();
        forceMentalBreakdownDeaths.clear();
    }

    private static DoorInteraction.DoorInteractionResult onDoorInteraction(DoorInteraction.DoorInteractionContext context) {
        if (!(context.getPlayer() instanceof ServerPlayerEntity player) || !isPsychoActive(player)) {
            return DoorInteraction.DoorInteractionResult.PASS;
        }
        if (context.getHandItem().isOf(WatheItems.BAT)
                && (context.getDoorType() == DoorInteraction.DoorType.TRAIN_DOOR || context.requiresKey())) {
            return DoorInteraction.DoorInteractionResult.ALLOW;
        }
        return DoorInteraction.DoorInteractionResult.PASS;
    }

    private static void tickWorld(ServerWorld world) {
        GameWorldComponent game = GameWorldComponent.KEY.get(world);
        if (game == null || !game.isRunning()) {
            return;
        }
        tickPending(world);
        tickActive(world);
        tickSuicideCountdown(world, game);
    }

    private static void tickSuicideCountdown(ServerWorld world, GameWorldComponent game) {
        for (UUID uuid : game.getAllPlayers()) {
            if (!(world.getPlayerByUuid(uuid) instanceof ServerPlayerEntity player)) {
                continue;
            }
            TraitPlayerComponent traits = TraitPlayerComponent.KEY.get(player);
            if (shouldPauseSuicideCountdown(
                    traits.hasActiveTrait(GoodTraits.DEPRESSION),
                    GameFunctions.isPlayerPlayingAndAlive(player),
                    isPending(player),
                    isPsychoActive(player),
                    isWathePsychoActive(player))) {
                traits.setDepressionSuicideTicks(-1);
                continue;
            }
            PlayerMoodComponent mood = PlayerMoodComponent.KEY.get(player);
            if (!shouldRunSuicideCountdown(mood.getMood())) {
                traits.setDepressionSuicideTicks(-1);
                continue;
            }
            int ticks = traits.getDepressionSuicideTicks();
            if (ticks <= 0) {
                traits.setDepressionSuicideTicks(SUICIDE_INTERVAL_TICKS);
                continue;
            }
            ticks--;
            if (ticks > 0) {
                traits.setDepressionSuicideTicks(ticks);
                continue;
            }
            double chance = triggerChance(mood.getMood());
            double roll = player.getRandom().nextDouble() * 100.0;
            if (roll < chance) {
                forceMentalBreakdownDeaths.add(player.getUuid());
                GameFunctions.killPlayer(player, true, null, GameConstants.DeathReasons.MENTAL_BREAKDOWN, true);
            } else {
                traits.setDepressionSuicideTicks(SUICIDE_INTERVAL_TICKS);
            }
        }
    }

    private static void tickPending(ServerWorld world) {
        long time = world.getTime();
        for (PendingState state : Set.copyOf(pendingPlayers.values())) {
            if (!(world.getPlayerByUuid(state.playerUuid()) instanceof ServerPlayerEntity player)) {
                pendingPlayers.remove(state.playerUuid());
                continue;
            }
            holdPending(player, state);
            if (time >= state.startPsychoTime()) {
                startPsycho(player, state);
            }
        }
    }

    private static void tickActive(ServerWorld world) {
        for (ActiveState state : Set.copyOf(activePlayers.values())) {
            if (!(world.getPlayerByUuid(state.playerUuid()) instanceof ServerPlayerEntity player)) {
                activePlayers.remove(state.playerUuid());
                continue;
            }
            ServerPlayerEntity attacker = world.getServer().getPlayerManager().getPlayer(state.attackerUuid());
            boolean playerAlive = GameFunctions.isPlayerPlayingAndAlive(player);
            boolean attackerAlive = attacker != null && GameFunctions.isPlayerPlayingAndAlive(attacker);
            if (shouldEndActivePsycho(playerAlive, attackerAlive)) {
                // Dead Depression psycho players must not have their synced skin/state maintained.
                // 已死亡的抑郁疯魔玩家不能继续被同步皮肤/状态维持住。
                endPsycho(player, playerAlive, playerAlive);
                continue;
            }
            ActiveState updatedState = maintainPsycho(player, attacker, state);
            if (activePlayers.containsKey(player.getUuid())) {
                activePlayers.put(player.getUuid(), updatedState);
            }
        }
    }

    private static void startPending(ServerPlayerEntity player, ServerPlayerEntity attacker, Identifier deathReason, int initialArmour) {
        ServerWorld world = player.getServerWorld();
        UUID uuid = player.getUuid();
        PlayerBodyEntity body = WatheEntities.PLAYER_BODY.create(world);
        if (body != null) {
            body.setPlayerUuid(uuid);
            body.setDeathReason(deathReason);
            body.setDeathGameTime(world.getTime());
            Vec3d spawnPos = player.getPos().add(player.getRotationVector().normalize().multiply(1));
            body.refreshPositionAndAngles(spawnPos.x, player.getY(), spawnPos.z, player.getHeadYaw(), 0f);
            body.setYaw(player.getHeadYaw());
            body.setHeadYaw(player.getHeadYaw());
            world.spawnEntity(body);
        }
        PendingState state = new PendingState(
                uuid,
                attacker.getUuid(),
                world.getTime() + FAKE_DEATH_TICKS,
                body == null ? null : body.getUuid(),
                EffectSnapshot.capture(player),
                player.isInvulnerable(),
                player.getPos(),
                player.getYaw(),
                player.getPitch(),
                initialArmour
        );
        pendingPlayers.put(uuid, state);
        TraitPlayerComponent.KEY.get(attacker).setDepressionCounterTarget(uuid);
        attacker.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.TitleS2CPacket(ATTACKER_TITLE));
        playPairSound(player, attacker, SparkTraitsSounds.DEPRESSION_BLIND_RAGE_ENRAGE);
        playDirectSound(attacker, SparkTraitsSounds.DEPRESSION_PLAYER_WAS_SEEN);
        holdPending(player, state);
    }

    private static void holdPending(ServerPlayerEntity player, PendingState state) {
        player.changeGameMode(GameMode.SPECTATOR);
        Entity camera = pendingCameraEntity(player, state);
        if (player.getCameraEntity() != camera) {
            player.setCameraEntity(camera);
        }
        player.setInvulnerable(true);
        player.setVelocity(Vec3d.ZERO);
        player.teleport(player.getServerWorld(), state.deathPos().x, state.deathPos().y, state.deathPos().z, Set.of(), state.deathYaw(), state.deathPitch());
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 40, 0, false, false, false));
    }

    private static Entity pendingCameraEntity(ServerPlayerEntity player, PendingState state) {
        MinecraftServer server = player.getServer();
        if (server != null && state.bodyUuid() != null) {
            Entity body = server.getOverworld().getEntity(state.bodyUuid());
            if (body != null) {
                return body;
            }
            for (ServerWorld world : server.getWorlds()) {
                Entity entity = world.getEntity(state.bodyUuid());
                if (entity != null) {
                    return entity;
                }
            }
        }
        return player;
    }

    private static void startPsycho(ServerPlayerEntity player, PendingState state) {
        pendingPlayers.remove(player.getUuid());
        player.setCameraEntity(player);
        player.changeGameMode(GameMode.ADVENTURE);
        player.setInvulnerable(state.wasInvulnerable());
        state.effects().restore(player);
        player.teleport(player.getServerWorld(), state.deathPos().x, state.deathPos().y, state.deathPos().z, Set.of(), state.deathYaw(), state.deathPitch());

        InventorySnapshot inventory = InventorySnapshot.capture(player);
        clearInventory(player);
        PlayerPsychoComponent psycho = PlayerPsychoComponent.KEY.get(player);
        psycho.startPsycho(PsychoType.VISIBLE_QUIET);
        psycho.setPsychoTicks(Integer.MAX_VALUE);
        psycho.setArmour(Math.max(0, state.initialArmour()));
        PlayerStaminaComponent stamina = PlayerStaminaComponent.KEY.get(player);
        stamina.setMaxSprintTime(-1);
        stamina.setSprintingTicks(Integer.MAX_VALUE);
        stamina.setExhausted(false);
        stamina.sync();
        ActiveState activeState = new ActiveState(
                player.getUuid(),
                state.attackerUuid(),
                inventory,
                Math.max(0, state.initialArmour()),
                RAGE_LOOP_INTERVAL_TICKS
        );
        activePlayers.put(player.getUuid(), activeState);
        TraitPlayerComponent.KEY.get(player).setDepressionPsychoState(true, state.attackerUuid());
        ServerPlayerEntity attacker = player.getServer().getPlayerManager().getPlayer(state.attackerUuid());
        playPairSound(player, attacker, SparkTraitsSounds.DEPRESSION_BLIND_RAGE_CHASE);
        activePlayers.put(player.getUuid(), maintainPsycho(
                player,
                attacker,
                activeState
        ));
    }

    private static ActiveState maintainPsycho(ServerPlayerEntity player, @Nullable ServerPlayerEntity attacker, ActiveState state) {
        PlayerPsychoComponent psycho = PlayerPsychoComponent.KEY.get(player);
        if (psycho.getPsychoTicks() <= 0) {
            psycho.startPsycho(PsychoType.VISIBLE_QUIET);
        }
        psycho.setPsychoTicks(Integer.MAX_VALUE);
        // English: The Jester bonus is starting armour only; consumed armour stays consumed.
        // 中文：小丑加成只提供初始护盾；已经消耗的护盾不会被每 tick 补回。
        int maintainedArmour = maintainedPsychoArmour(psycho.getArmour(), state.maxArmour());
        if (psycho.getArmour() != maintainedArmour) {
            psycho.setArmour(maintainedArmour);
        }
        enforceBatOnly(player);
        PlayerStaminaComponent stamina = PlayerStaminaComponent.KEY.get(player);
        stamina.setMaxSprintTime(-1);
        stamina.setSprintingTicks(Integer.MAX_VALUE);
        stamina.setExhausted(false);
        stamina.sync();
        player.sendMessage(PSYCHO_ACTIONBAR, true);
        if (attacker != null) {
            attacker.sendMessage(ATTACKER_ACTIONBAR, true);
        }
        ActiveState updatedState = tickRageLoop(player, attacker, state);
        return updatedState;
    }

    private static void endPsycho(ServerPlayerEntity player, boolean restoreInventory, boolean restoreMoodIfSurvived) {
        ActiveState state = activePlayers.remove(player.getUuid());
        if (state == null) {
            return;
        }
        PlayerPsychoComponent.KEY.get(player).stopPsycho();
        if (restoreInventory) {
            state.inventory().restore(player);
        }
        restorePostPsychoStamina(player);
        restorePostPsychoMood(player, restoreMoodIfSurvived);
        TraitPlayerComponent.KEY.get(player).setDepressionPsychoState(false, null);
        ServerPlayerEntity attacker = player.getServer().getPlayerManager().getPlayer(state.attackerUuid());
        if (attacker != null) {
            TraitPlayerComponent.KEY.get(attacker).setDepressionCounterTarget(null);
        }
    }

    private static void restorePostPsychoMood(ServerPlayerEntity player, boolean restoreMoodIfSurvived) {
        if (!restoreMoodIfSurvived || !GameFunctions.isPlayerPlayingAndAlive(player)) {
            return;
        }
        // Successful Depression psycho exits at a stable sanity value instead of preserving crisis mood.
        // 抑郁疯魔成功结束后固定恢复到稳定理智值，而不是保留濒临崩溃的理智。
        PlayerMoodComponent mood = PlayerMoodComponent.KEY.get(player);
        mood.setMood(depressionPsychoRestoredMood());
        mood.sync();
    }

    private static void restorePostPsychoStamina(ServerPlayerEntity player) {
        GameWorldComponent game = GameWorldComponent.KEY.get(player.getWorld());
        Role role = game == null ? null : game.getRole(player);
        int roleMaxSprintTime = role == null ? -1 : role.getMaxSprintTime();
        EntityAttributeInstance sprintAttribute = player.getAttributeInstance(WatheAttributes.MAX_SPRINT_TIME);
        int effectiveMaxSprintTime = sprintAttribute == null ? roleMaxSprintTime : (int) sprintAttribute.getValue();
        PostPsychoStaminaState state = postPsychoStaminaState(roleMaxSprintTime, effectiveMaxSprintTime);
        PlayerStaminaComponent stamina = PlayerStaminaComponent.KEY.get(player);
        stamina.setMaxSprintTime(state.maxSprintTime());
        stamina.setSprintingTicks(state.sprintingTicks());
        stamina.setExhausted(state.exhausted());
        stamina.sync();
    }

    private static void clearPending(ServerPlayerEntity player) {
        PendingState state = pendingPlayers.remove(player.getUuid());
        if (state == null) {
            return;
        }
        player.setCameraEntity(player);
        player.setInvulnerable(state.wasInvulnerable());
        state.effects().restore(player);
        TraitPlayerComponent.KEY.get(player).setDepressionPsychoState(false, null);
        ServerPlayerEntity attacker = player.getServer().getPlayerManager().getPlayer(state.attackerUuid());
        if (attacker != null) {
            TraitPlayerComponent.KEY.get(attacker).setDepressionCounterTarget(null);
        }
    }

    private static void clearInventory(ServerPlayerEntity player) {
        PlayerInventory inventory = player.getInventory();
        inventory.clear();
        inventory.selectedSlot = 0;
    }

    private static void enforceBatOnly(ServerPlayerEntity player) {
        PlayerInventory inventory = player.getInventory();
        boolean hasBat = false;
        for (List<ItemStack> list : List.of(inventory.main, inventory.armor, inventory.offHand)) {
            for (int i = 0; i < list.size(); i++) {
                ItemStack stack = list.get(i);
                if (stack.isEmpty()) {
                    continue;
                }
                if (!hasBat && stack.isOf(WatheItems.BAT)) {
                    hasBat = true;
                    continue;
                }
                list.set(i, ItemStack.EMPTY);
            }
        }
        if (!hasBat) {
            inventory.main.set(0, new ItemStack(WatheItems.BAT));
        }
        inventory.selectedSlot = 0;
        player.currentScreenHandler.sendContentUpdates();
    }

    private static ActiveState tickRageLoop(ServerPlayerEntity player, @Nullable ServerPlayerEntity attacker, ActiveState state) {
        // English: Replay blind-rage chase on the audio-length cadence for the two chase participants only.
        // 中文：按 blind-rage chase 音频长度周期重播，并且只播放给追逐双方。
        if (shouldPlayRageLoop(state.rageLoopTicks())) {
            playPairSound(player, attacker, SparkTraitsSounds.DEPRESSION_BLIND_RAGE_CHASE);
        }
        return state.withRageLoopTicks(nextRageLoopTicks(state.rageLoopTicks()));
    }

    private static void playRangeSound(ServerPlayerEntity source, SoundEvent sound) {
        source.getWorld().playSound(
                null,
                source.getX(),
                source.getY(),
                source.getZ(),
                sound,
                SoundCategory.PLAYERS,
                DEPRESSION_RANGE_SOUND_VOLUME,
                DEPRESSION_SOUND_PITCH
        );
    }

    private static void playDirectSound(ServerPlayerEntity player, SoundEvent sound) {
        player.playSoundToPlayer(sound, SoundCategory.PLAYERS, DEPRESSION_DIRECT_SOUND_VOLUME, DEPRESSION_SOUND_PITCH);
    }

    private static void playPairSound(ServerPlayerEntity player, @Nullable ServerPlayerEntity attacker, SoundEvent sound) {
        playDirectSound(player, sound);
        if (attacker != null && attacker != player) {
            playDirectSound(attacker, sound);
        }
    }

    private static boolean roleIdentifierEquals(Role role, Identifier id) {
        return role != null && role.identifier().equals(id);
    }

    private static double linearChance(float mood, float startMood, float fullMood, double startChance, double fullChance) {
        if (mood >= startMood) {
            return startChance;
        }
        if (mood <= fullMood) {
            return fullChance;
        }
        double progress = (startMood - mood) / (double) (startMood - fullMood);
        return startChance + (fullChance - startChance) * progress;
    }

    private record PendingState(
            UUID playerUuid,
            UUID attackerUuid,
            long startPsychoTime,
            @Nullable UUID bodyUuid,
            EffectSnapshot effects,
            boolean wasInvulnerable,
            Vec3d deathPos,
            float deathYaw,
            float deathPitch,
            int initialArmour
    ) {
    }

    private record ActiveState(
            UUID playerUuid,
            UUID attackerUuid,
            InventorySnapshot inventory,
            int maxArmour,
            int rageLoopTicks
    ) {
        ActiveState withRageLoopTicks(int ticks) {
            return new ActiveState(playerUuid, attackerUuid, inventory, maxArmour, ticks);
        }
    }

    public record PostPsychoStaminaState(int maxSprintTime, float sprintingTicks, boolean exhausted) {
    }

    private record EffectSnapshot(List<StatusEffectInstance> effects) {
        static EffectSnapshot capture(ServerPlayerEntity player) {
            return new EffectSnapshot(player.getStatusEffects().stream()
                    .map(StatusEffectInstance::new)
                    .toList());
        }

        void restore(ServerPlayerEntity player) {
            player.clearStatusEffects();
            for (StatusEffectInstance effect : effects) {
                player.addStatusEffect(new StatusEffectInstance(effect));
            }
        }
    }

    private record InventorySnapshot(
            List<ItemStack> main,
            List<ItemStack> armor,
            List<ItemStack> offHand,
            int selectedSlot
    ) {
        static InventorySnapshot capture(ServerPlayerEntity player) {
            PlayerInventory inventory = player.getInventory();
            return new InventorySnapshot(copy(inventory.main), copy(inventory.armor), copy(inventory.offHand), inventory.selectedSlot);
        }

        void restore(ServerPlayerEntity player) {
            PlayerInventory inventory = player.getInventory();
            restoreList(inventory.main, main);
            restoreList(inventory.armor, armor);
            restoreList(inventory.offHand, offHand);
            inventory.selectedSlot = selectedSlot;
            player.currentScreenHandler.sendContentUpdates();
        }

        private static List<ItemStack> copy(DefaultedList<ItemStack> stacks) {
            return stacks.stream().map(ItemStack::copy).toList();
        }

        private static void restoreList(DefaultedList<ItemStack> target, List<ItemStack> source) {
            for (int i = 0; i < target.size(); i++) {
                target.set(i, i < source.size() ? source.get(i).copy() : ItemStack.EMPTY);
            }
        }
    }
}
