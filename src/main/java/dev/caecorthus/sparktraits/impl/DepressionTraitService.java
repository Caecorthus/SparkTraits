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
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import org.agmas.noellesroles.Noellesroles;
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
    public static final double MIN_PSYCHO_COUNTER_CHANCE = 10.0;
    public static final Identifier APPRENTICE_WITCH_ID = Identifier.of("sparkwitch", "apprentice_witch");
    public static final Identifier DEPRESSION_STAMINA_MODIFIER_ID = SparkTraits.id("depression_stamina");
    public static final double DEPRESSION_STAMINA_MODIFIER_VALUE = -0.2;
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
        if (traits == null || !traits.contains(GoodTraits.DEPRESSION) || proposedMood >= currentMood) {
            return proposedMood;
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

    public static float depressionScreenEffectStrength(boolean hasDepression, boolean psychoActive, float mood) {
        if (!hasDepression) {
            return 0.0f;
        }
        if (psychoActive) {
            return 1.0f;
        }
        if (mood >= GameConstants.MID_MOOD_THRESHOLD) {
            return 0.0f;
        }
        if (mood <= FULL_GRAYSCALE_MOOD) {
            return 1.0f;
        }
        return (GameConstants.MID_MOOD_THRESHOLD - mood) / (GameConstants.MID_MOOD_THRESHOLD - FULL_GRAYSCALE_MOOD);
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
        return isPsychoActive(player);
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

    public static boolean isPending(ServerPlayerEntity player) {
        return pendingPlayers.containsKey(player.getUuid());
    }

    public static boolean isPsychoActive(net.minecraft.entity.player.PlayerEntity player) {
        return player != null && activePlayers.containsKey(player.getUuid());
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
        startPending(victim, killer, deathReason);
        return KillPlayer.KillResult.cancel();
    }

    public static void handleAfterKill(ServerPlayerEntity victim, @Nullable ServerPlayerEntity killer) {
        if (killer != null) {
            ActiveState killerState = activePlayers.get(killer.getUuid());
            if (killerState != null && killerState.attackerUuid().equals(victim.getUuid())) {
                endPsycho(killer, true);
            }
        }
        if (activePlayers.containsKey(victim.getUuid())) {
            endPsycho(victim, false);
        }
        clearPending(victim);
    }

    public static void clearPlayer(ServerPlayerEntity player) {
        clearPending(player);
        if (activePlayers.containsKey(player.getUuid())) {
            endPsycho(player, false);
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
                endPsycho(player, false);
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
            if (!traits.hasActiveTrait(GoodTraits.DEPRESSION)
                    || !GameFunctions.isPlayerPlayingAndAlive(player)
                    || isPending(player)
                    || isPsychoActive(player)) {
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
            if (attacker == null || !GameFunctions.isPlayerPlayingAndAlive(attacker)) {
                endPsycho(player, true);
                continue;
            }
            maintainPsycho(player, attacker);
        }
    }

    private static void startPending(ServerPlayerEntity player, ServerPlayerEntity attacker, Identifier deathReason) {
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
                player.getPitch()
        );
        pendingPlayers.put(uuid, state);
        TraitPlayerComponent.KEY.get(attacker).setDepressionCounterTarget(uuid);
        attacker.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.TitleS2CPacket(ATTACKER_TITLE));
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
        psycho.setArmour(0);
        PlayerStaminaComponent stamina = PlayerStaminaComponent.KEY.get(player);
        stamina.setMaxSprintTime(-1);
        stamina.setSprintingTicks(Integer.MAX_VALUE);
        stamina.setExhausted(false);
        stamina.sync();
        ActiveState activeState = new ActiveState(player.getUuid(), state.attackerUuid(), inventory);
        activePlayers.put(player.getUuid(), activeState);
        TraitPlayerComponent.KEY.get(player).setDepressionPsychoState(true, state.attackerUuid());
        maintainPsycho(player, player.getServer().getPlayerManager().getPlayer(state.attackerUuid()));
    }

    private static void maintainPsycho(ServerPlayerEntity player, @Nullable ServerPlayerEntity attacker) {
        PlayerPsychoComponent psycho = PlayerPsychoComponent.KEY.get(player);
        if (psycho.getPsychoTicks() <= 0) {
            psycho.startPsycho(PsychoType.VISIBLE_QUIET);
        }
        psycho.setPsychoTicks(Integer.MAX_VALUE);
        psycho.setArmour(0);
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
    }

    private static void endPsycho(ServerPlayerEntity player, boolean restoreInventory) {
        ActiveState state = activePlayers.remove(player.getUuid());
        if (state == null) {
            return;
        }
        PlayerPsychoComponent.KEY.get(player).stopPsycho();
        if (restoreInventory) {
            state.inventory().restore(player);
        }
        PlayerStaminaComponent.KEY.get(player).reset();
        TraitPlayerComponent.KEY.get(player).setDepressionPsychoState(false, null);
        ServerPlayerEntity attacker = player.getServer().getPlayerManager().getPlayer(state.attackerUuid());
        if (attacker != null) {
            TraitPlayerComponent.KEY.get(attacker).setDepressionCounterTarget(null);
        }
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
            float deathPitch
    ) {
    }

    private record ActiveState(UUID playerUuid, UUID attackerUuid, InventorySnapshot inventory) {
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
