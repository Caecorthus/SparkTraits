package dev.caecorthus.sparktraits.api;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SparkTraitsApiContractTest {
    @Test
    void publicFacadeKeepsDownstreamReflectionDescriptors() throws NoSuchMethodException {
        assertPublicStaticBooleanMethod("hasActiveTrait", PlayerEntity.class, Identifier.class);
        assertPublicStaticBooleanMethod("hasLastStandTriggeredThisRound", ServerWorld.class, UUID.class);
        assertPublicStaticBooleanMethod("isFinalMomentActive", World.class);
        assertPublicStaticBooleanMethod("isFakeDeathBody", Entity.class);
        assertPublicStaticBooleanMethod("isInstinctHidden", PlayerEntity.class, PlayerEntity.class);
    }

    @Test
    void publicFacadeQueriesAreNullSafe() {
        assertFalse(SparkTraitsApi.hasActiveTrait(null, null));
        assertFalse(SparkTraitsApi.hasLastStandTriggeredThisRound(null, null));
        assertFalse(SparkTraitsApi.isFinalMomentActive(null));
        assertFalse(SparkTraitsApi.isFakeDeathBody(null));
        assertFalse(SparkTraitsApi.isInstinctHidden(null, null));
    }

    private static void assertPublicStaticBooleanMethod(String name, Class<?>... parameterTypes) {
        Method method = findMethod(name, parameterTypes);

        assertNotNull(method);
        assertEquals(boolean.class, method.getReturnType());
        assertTrue(Modifier.isPublic(method.getModifiers()));
        assertTrue(Modifier.isStatic(method.getModifiers()));
    }

    private static Method findMethod(String name, Class<?>... parameterTypes) {
        try {
            return SparkTraitsApi.class.getDeclaredMethod(name, parameterTypes);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }
}
