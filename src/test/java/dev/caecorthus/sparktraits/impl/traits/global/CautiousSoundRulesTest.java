package dev.caecorthus.sparktraits.impl.traits.global;

import net.minecraft.entity.Entity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CautiousSoundRulesTest {
    @Test
    void clientEntityStepSoundRequiresCautiousPlayerStepSound() {
        Identifier stepSound = Identifier.of("minecraft", "block.stone.step");

        assertTrue(CautiousSoundRules.shouldSuppressClientEntityStepSound(
                true,
                true,
                SoundCategory.PLAYERS,
                stepSound
        ));
        assertFalse(CautiousSoundRules.shouldSuppressClientEntityStepSound(
                false,
                true,
                SoundCategory.PLAYERS,
                stepSound
        ));
        assertFalse(CautiousSoundRules.shouldSuppressClientEntityStepSound(
                true,
                false,
                SoundCategory.PLAYERS,
                stepSound
        ));
        assertFalse(CautiousSoundRules.shouldSuppressClientEntityStepSound(
                true,
                true,
                SoundCategory.BLOCKS,
                stepSound
        ));
        assertFalse(CautiousSoundRules.shouldSuppressClientEntityStepSound(
                true,
                true,
                SoundCategory.PLAYERS,
                Identifier.of("minecraft", "entity.player.hurt")
        ));
    }

    @Test
    void suppressMovementSoundsKeepsEventsAndRemovesSoundOnlyEffects() {
        assertEquals(Entity.MoveEffect.EVENTS,
                CautiousSoundRules.suppressMovementSounds(Entity.MoveEffect.ALL, true));
        assertEquals(Entity.MoveEffect.NONE,
                CautiousSoundRules.suppressMovementSounds(Entity.MoveEffect.SOUNDS, true));
        assertEquals(Entity.MoveEffect.ALL,
                CautiousSoundRules.suppressMovementSounds(Entity.MoveEffect.ALL, false));
        assertEquals(Entity.MoveEffect.EVENTS,
                CautiousSoundRules.suppressMovementSounds(Entity.MoveEffect.EVENTS, true));
    }
}
