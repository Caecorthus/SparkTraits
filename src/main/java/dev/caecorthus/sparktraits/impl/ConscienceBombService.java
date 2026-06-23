package dev.caecorthus.sparktraits.impl;

import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheParticles;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import org.agmas.noellesroles.ModSounds;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.agmas.noellesroles.ModItems.TIMED_BOMB;

/**
 * Tracks transferred timed bombs owned by Conscience bombers.
 * 追踪善良炸弹客传出去的定时炸弹，并集中处理非致死爆炸效果。
 */
public final class ConscienceBombService {
    public static final int EFFECT_DURATION_TICKS = GameConstants.getInTicks(0, 5);
    public static final double REVEAL_RADIUS = 8.0;
    public static final double REVEAL_RADIUS_SQUARED = REVEAL_RADIUS * REVEAL_RADIUS;

    private static final Map<UUID, UUID> conscienceTimedBombs = new HashMap<>();

    private ConscienceBombService() {
    }

    public static boolean shouldTrackPlacedTimedBomb(boolean bomberHasConscience) {
        return bomberHasConscience;
    }

    public static boolean shouldTrackTransferredTimedBomb(boolean sourceWasAlreadyTracked, boolean bomberHasConscience) {
        return sourceWasAlreadyTracked || bomberHasConscience;
    }

    public static boolean shouldCancelTransferredTimedBombDeath(boolean transferredByConscienceBomber) {
        return transferredByConscienceBomber;
    }

    public static boolean isWithinRevealRadius(double distanceSquared) {
        return distanceSquared <= REVEAL_RADIUS_SQUARED;
    }

    public static void markTimedBombPlaced(PlayerEntity holder, PlayerEntity bomber) {
        if (holder == null) {
            return;
        }

        if (bomber != null && shouldTrackPlacedTimedBomb(EffectiveTraitService.hasConscience(bomber))) {
            conscienceTimedBombs.put(holder.getUuid(), bomber.getUuid());
        } else {
            conscienceTimedBombs.remove(holder.getUuid());
        }
    }

    public static void markTimedBombTransferred(PlayerEntity sourceHolder, PlayerEntity targetHolder, UUID bomberUuid) {
        if (sourceHolder == null || targetHolder == null || bomberUuid == null) {
            return;
        }

        boolean sourceWasTracked = bomberUuid.equals(conscienceTimedBombs.remove(sourceHolder.getUuid()));
        PlayerEntity bomber = sourceHolder.getWorld().getPlayerByUuid(bomberUuid);
        if (shouldTrackTransferredTimedBomb(sourceWasTracked, EffectiveTraitService.hasConscience(bomber))) {
            conscienceTimedBombs.put(targetHolder.getUuid(), bomberUuid);
        } else {
            conscienceTimedBombs.remove(targetHolder.getUuid());
        }
    }

    public static boolean consumeTransferredTimedBomb(PlayerEntity holder, UUID bomberUuid) {
        if (holder == null || bomberUuid == null) {
            return false;
        }
        return shouldCancelTransferredTimedBombDeath(bomberUuid.equals(conscienceTimedBombs.remove(holder.getUuid())));
    }

    public static void clearTimedBomb(PlayerEntity holder) {
        if (holder != null) {
            conscienceTimedBombs.remove(holder.getUuid());
        }
    }

    public static void clearAll() {
        conscienceTimedBombs.clear();
    }

    /**
     * Plays the original explosion feedback, then applies Conscience-only support effects.
     * 播放原版爆炸反馈，并施加善良炸弹的夜视与高亮辅助效果。
     */
    public static void applyNonLethalExplosionEffects(ServerWorld world, PlayerEntity holder) {
        world.playSound(null, holder.getX(), holder.getY(), holder.getZ(),
                ModSounds.BOMB_EXPLODE, SoundCategory.PLAYERS, 3.0F, 1.0F);

        world.spawnParticles(WatheParticles.BIG_EXPLOSION,
                holder.getX(), holder.getY() + 0.5, holder.getZ(),
                1, 0, 0, 0, 0);

        world.spawnParticles(ParticleTypes.SMOKE,
                holder.getX(), holder.getY() + 0.5, holder.getZ(),
                100, 0, 0, 0, 0.2);

        world.spawnParticles(new ItemStackParticleEffect(ParticleTypes.ITEM, TIMED_BOMB.getDefaultStack()),
                holder.getX(), holder.getY() + 0.5, holder.getZ(),
                100, 0, 0, 0, 1.0);

        if (GameFunctions.isPlayerPlayingAndAlive(holder)) {
            holder.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, EFFECT_DURATION_TICKS, 0, false, false, true));
        }

        for (ServerPlayerEntity player : world.getPlayers(candidate ->
                GameFunctions.isPlayerPlayingAndAlive(candidate)
                        && isWithinRevealRadius(candidate.squaredDistanceTo(holder)))) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, EFFECT_DURATION_TICKS, 0, false, false, true));
        }
    }
}
