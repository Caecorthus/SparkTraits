package dev.caecorthus.sparktraits.impl;

import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

/**
 * Runtime helpers for the Pig trait's public shape and sound state.
 * 猪天赋的公开外形状态、碰撞体积与声音辅助入口。
 */
public final class PigTraitService {
    public static final int COLOR = 0xF4A7B9;
    public static final int WEIGHT = 13;
    public static final double ROLL_WEIGHT = 12.5D;
    public static final int RESET_AMBIENT_SOUND_CHANCE = -80;
    public static final float PIG_COLLISION_WIDTH = 0.6F;
    public static final float PIG_COLLISION_HEIGHT = 1.4F;
    public static final Identifier PIG_GOD_ROLE_ID = Identifier.of("sparkwitch", "pig_god");
    private static final int AMBIENT_SOUND_RANDOM_BOUND = 1000;

    private PigTraitService() {
    }

    public static boolean isPig(PlayerEntity player) {
        return player != null && TraitPlayerComponent.KEY.get(player).isPigActive();
    }

    public static boolean canSelectRequiredPig(Role role) {
        return role != null && PIG_GOD_ROLE_ID.equals(role.identifier());
    }

    public static boolean shouldUsePigDimensions(PlayerEntity player, EntityPose pose) {
        return isPig(player) && pose != EntityPose.SLEEPING && pose != EntityPose.DYING;
    }

    public static EntityDimensions pigDimensions() {
        return EntityDimensions.changing(PIG_COLLISION_WIDTH, PIG_COLLISION_HEIGHT);
    }

    public static SoundEvent ambientSound() {
        return SoundEvents.ENTITY_PIG_AMBIENT;
    }

    public static SoundEvent hurtSound() {
        return SoundEvents.ENTITY_PIG_HURT;
    }

    public static SoundEvent deathSound() {
        return SoundEvents.ENTITY_PIG_DEATH;
    }

    public static boolean shouldPlayAmbientSound(boolean pigActive, boolean alive, boolean spectator, int chance, int randomRoll) {
        return pigActive && alive && !spectator && randomRoll < chance;
    }

    public static int nextAmbientSoundChance(int chance, int randomRoll) {
        return randomRoll < chance ? RESET_AMBIENT_SOUND_CHANCE : chance + 1;
    }

    public static void tickAmbientSound(ServerPlayerEntity player) {
        if (player == null) {
            return;
        }
        TraitPlayerComponent component = TraitPlayerComponent.KEY.get(player);
        boolean pigActive = component.isPigActive();
        boolean alive = player.isAlive();
        boolean spectator = GameFunctions.isPlayerSpectatingOrCreative(player) || component.isLastStandPending();
        if (!pigActive || !alive || spectator) {
            component.resetPigAmbientSoundChance();
            return;
        }
        int chance = component.getPigAmbientSoundChance();
        int randomRoll = player.getRandom().nextInt(AMBIENT_SOUND_RANDOM_BOUND);
        if (shouldPlayAmbientSound(true, true, false, chance, randomRoll)) {
            player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    ambientSound(), SoundCategory.PLAYERS, 1.0f, player.getSoundPitch());
        }
        component.setPigAmbientSoundChance(nextAmbientSoundChance(chance, randomRoll));
    }

    public static void playDeathSound(ServerPlayerEntity player) {
        if (isPig(player)) {
            player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    deathSound(), SoundCategory.PLAYERS, 1.0f, player.getSoundPitch());
        }
    }

    public static void applyPigDimensions(PlayerEntity player) {
        if (player != null) {
            player.calculateDimensions();
            TraitPlayerComponent.KEY.get(player).resetPigAmbientSoundChance();
        }
    }

    public static void removePigDimensions(PlayerEntity player) {
        if (player != null) {
            player.calculateDimensions();
            TraitPlayerComponent.KEY.get(player).resetPigAmbientSoundChance();
        }
    }
}
