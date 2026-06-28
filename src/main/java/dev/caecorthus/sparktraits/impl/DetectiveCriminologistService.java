package dev.caecorthus.sparktraits.impl;

import dev.caecorthus.sparktraits.component.RoleEnhancementPlayerComponent;
import dev.caecorthus.sparktraits.component.RoleEnhancementWorldComponent;
import dev.caecorthus.sparktraits.network.OpenCriminologistScreenS2CPacket;
import dev.caecorthus.sparktraits.network.SelectCriminologistTargetC2SPacket;
import dev.doctor4t.wathe.api.event.CanTargetBody;
import dev.doctor4t.wathe.api.event.GameEvents;
import dev.doctor4t.wathe.api.event.ResetPlayer;
import dev.doctor4t.wathe.api.event.RoleAssigned;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import dev.doctor4t.wathe.game.GameFunctions;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypeFilter;
import org.agmas.noellesroles.Noellesroles;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class DetectiveCriminologistService {
    public enum OpenResult {
        ALLOW,
        NOT_DETECTIVE,
        ON_COOLDOWN,
        NOT_ENOUGH_MONEY,
        BODY_NOT_TARGETABLE,
        TARGET_LOCKED
    }

    public enum SelectionResult {
        TRACK,
        WRONG,
        NO_KILLER_RECORD,
        KILLER_NOT_ALIVE
    }

    public static final int COST = 150;
    public static final int INITIAL_COOLDOWN_TICKS = 20 * 60;
    public static final int FAILURE_COOLDOWN_TICKS = 20 * 120;
    public static final int REVEAL_PERIOD_TICKS = 20 * 30;
    public static final int REVEAL_DURATION_TICKS = 20 * 5;
    public static final int BODY_RANGE_BLOCKS = 5;
    public static final int TRACKING_HIGHLIGHT_COLOR = 0xFF3333;

    private DetectiveCriminologistService() {
    }

    public static void register() {
        PayloadTypeRegistry.playS2C().register(OpenCriminologistScreenS2CPacket.ID, OpenCriminologistScreenS2CPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(SelectCriminologistTargetC2SPacket.ID, SelectCriminologistTargetC2SPacket.CODEC);

        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient || !(player instanceof ServerPlayerEntity detective) || !(entity instanceof PlayerBodyEntity body)) {
                return ActionResult.PASS;
            }
            return tryOpenScreen(detective, body) ? ActionResult.SUCCESS : ActionResult.PASS;
        });

        ServerPlayNetworking.registerGlobalReceiver(SelectCriminologistTargetC2SPacket.ID, (payload, context) ->
                handleSelection(context.player(), payload.bodyUuid(), payload.targetUuid()));

        RoleAssigned.EVENT.register((player, role) -> {
            if (player instanceof ServerPlayerEntity serverPlayer && Noellesroles.DETECTIVE.equals(role)) {
                RoleEnhancementPlayerComponent.KEY.get(serverPlayer).resetCriminologistForRound(INITIAL_COOLDOWN_TICKS);
            }
        });
        ResetPlayer.EVENT.register(player -> RoleEnhancementPlayerComponent.KEY.get(player).clearCriminologist());
        GameEvents.ON_FINISH_FINALIZE.register((world, gameComponent) ->
                RoleEnhancementWorldComponent.KEY.get(world).clearBodyRecords());
    }

    public static boolean isDetective(PlayerEntity player) {
        return player != null && GameWorldComponent.KEY.get(player.getWorld()).isRole(player, Noellesroles.DETECTIVE);
    }

    public static OpenResult validateOpen(
            boolean aliveDetective,
            int cooldownTicks,
            int balance,
            boolean bodyTargetable,
            boolean liveTargetLocked
    ) {
        if (!aliveDetective) {
            return OpenResult.NOT_DETECTIVE;
        }
        if (cooldownTicks > 0) {
            return OpenResult.ON_COOLDOWN;
        }
        if (balance < COST) {
            return OpenResult.NOT_ENOUGH_MONEY;
        }
        if (!bodyTargetable) {
            return OpenResult.BODY_NOT_TARGETABLE;
        }
        if (liveTargetLocked) {
            return OpenResult.TARGET_LOCKED;
        }
        return OpenResult.ALLOW;
    }

    public static SelectionResult evaluateSelection(UUID recordedKillerUuid, UUID selectedUuid, boolean killerAlive) {
        if (recordedKillerUuid == null) {
            return SelectionResult.NO_KILLER_RECORD;
        }
        if (!recordedKillerUuid.equals(selectedUuid)) {
            return SelectionResult.WRONG;
        }
        return killerAlive ? SelectionResult.TRACK : SelectionResult.KILLER_NOT_ALIVE;
    }

    public static boolean isRevealPulseActive(int trackingAgeTicks) {
        return trackingAgeTicks >= 0 && trackingAgeTicks % REVEAL_PERIOD_TICKS < REVEAL_DURATION_TICKS;
    }

    private static boolean tryOpenScreen(ServerPlayerEntity detective, PlayerBodyEntity body) {
        OpenResult result = validateOpen(detective, body);
        if (result != OpenResult.ALLOW) {
            sendOpenFailure(detective, result);
            return result != OpenResult.NOT_DETECTIVE;
        }
        List<OpenCriminologistScreenS2CPacket.Candidate> candidates = candidates(detective.getServerWorld());
        ServerPlayNetworking.send(detective, new OpenCriminologistScreenS2CPacket(body.getUuid(), candidates));
        return true;
    }

    private static OpenResult validateOpen(ServerPlayerEntity detective, PlayerBodyEntity body) {
        RoleEnhancementPlayerComponent component = RoleEnhancementPlayerComponent.KEY.get(detective);
        GameWorldComponent game = GameWorldComponent.KEY.get(detective.getWorld());
        boolean targetable = body.squaredDistanceTo(detective) <= BODY_RANGE_BLOCKS * BODY_RANGE_BLOCKS
                && CanTargetBody.EVENT.invoker().canTarget(detective, body);
        return validateOpen(
                isDetective(detective) && GameFunctions.isPlayerPlayingAndAlive(detective),
                component.getCriminologistCooldownTicks(),
                PlayerShopComponent.KEY.get(detective).getBalance(),
                targetable,
                component.hasLiveCriminologistTarget(detective.getServerWorld(), game)
        );
    }

    private static void handleSelection(ServerPlayerEntity detective, UUID bodyUuid, UUID selectedUuid) {
        PlayerBodyEntity body = findBody(detective.getServerWorld(), bodyUuid);
        if (body == null || validateOpen(detective, body) != OpenResult.ALLOW) {
            return;
        }

        PlayerShopComponent shop = PlayerShopComponent.KEY.get(detective);
        shop.setBalance(shop.getBalance() - COST);

        RoleEnhancementWorldComponent.BodyRecord record = RoleEnhancementWorldComponent.KEY
                .get(detective.getWorld())
                .getBodyRecord(bodyUuid);
        UUID killerUuid = record == null ? null : record.killerUuid();
        ServerPlayerEntity killer = killerUuid != null
                && detective.getServerWorld().getPlayerByUuid(killerUuid) instanceof ServerPlayerEntity serverKiller
                ? serverKiller
                : null;
        boolean killerAlive = killer != null && GameFunctions.isPlayerPlayingAndAlive(killer);
        SelectionResult result = evaluateSelection(killerUuid, selectedUuid, killerAlive);

        RoleEnhancementPlayerComponent component = RoleEnhancementPlayerComponent.KEY.get(detective);
        if (result == SelectionResult.TRACK) {
            component.startCriminologistTracking(killerUuid, killer.getGameProfile().getName());
            detective.sendMessage(Text.translatable("message.sparktraits.criminologist.tracking", killer.getDisplayName()), true);
            return;
        }

        component.setCriminologistCooldown(FAILURE_COOLDOWN_TICKS);
        if (result == SelectionResult.KILLER_NOT_ALIVE || result == SelectionResult.NO_KILLER_RECORD) {
            detective.sendMessage(Text.translatable("message.sparktraits.criminologist.killer_dead"), true);
        }
    }

    public static void recordBody(PlayerBodyEntity body, ServerPlayerEntity victim, ServerPlayerEntity killer, Identifier deathReason) {
        if (body == null || victim == null) {
            return;
        }
        RoleEnhancementWorldComponent.KEY.get(victim.getWorld())
                .recordBody(body.getUuid(), victim.getUuid(), killer == null ? null : killer.getUuid(), deathReason);
    }

    private static PlayerBodyEntity findBody(ServerWorld world, UUID bodyUuid) {
        List<? extends PlayerBodyEntity> bodies = world.getEntitiesByType(
                TypeFilter.equals(PlayerBodyEntity.class),
                entity -> entity.getUuid().equals(bodyUuid)
        );
        return bodies.isEmpty() ? null : bodies.getFirst();
    }

    private static List<OpenCriminologistScreenS2CPacket.Candidate> candidates(ServerWorld world) {
        GameWorldComponent game = GameWorldComponent.KEY.get(world);
        List<OpenCriminologistScreenS2CPacket.Candidate> candidates = new ArrayList<>();
        for (UUID uuid : game.getAllPlayers()) {
            String name = "Unknown";
            if (game.getGameProfiles().get(uuid) != null) {
                name = game.getGameProfiles().get(uuid).getName();
            } else if (world.getPlayerByUuid(uuid) instanceof ServerPlayerEntity player) {
                name = player.getGameProfile().getName();
            }
            candidates.add(new OpenCriminologistScreenS2CPacket.Candidate(uuid, name));
        }
        return candidates;
    }

    private static void sendOpenFailure(ServerPlayerEntity detective, OpenResult result) {
        Text message = switch (result) {
            case ON_COOLDOWN -> Text.translatable(
                    "message.sparktraits.criminologist.cooldown",
                    RoleEnhancementPlayerComponent.KEY.get(detective).getCriminologistCooldownTicks() / 20
            );
            case NOT_ENOUGH_MONEY -> Text.translatable("message.sparktraits.criminologist.not_enough_money", COST);
            case TARGET_LOCKED -> Text.translatable("message.sparktraits.criminologist.target_locked");
            default -> null;
        };
        if (message != null) {
            detective.sendMessage(message, true);
        }
    }
}
