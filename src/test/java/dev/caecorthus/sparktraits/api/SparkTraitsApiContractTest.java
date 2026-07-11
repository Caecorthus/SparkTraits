package dev.caecorthus.sparktraits.api;

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
import static org.junit.jupiter.api.Assertions.assertTrue;

class SparkTraitsApiContractTest {
    @Test
    void publicFacadeKeepsDownstreamReflectionDescriptors() throws NoSuchMethodException {
        assertPublicStaticBooleanMethod("hasActiveTrait", PlayerEntity.class, Identifier.class);
        assertPublicStaticBooleanMethod("hasLastStandTriggeredThisRound", ServerWorld.class, UUID.class);
        assertPublicStaticBooleanMethod("isFinalMomentActive", World.class);
    }

    @Test
    void publicFacadeQueriesAreNullSafe() {
        assertFalse(SparkTraitsApi.hasActiveTrait(null, null));
        assertFalse(SparkTraitsApi.hasLastStandTriggeredThisRound(null, null));
        assertFalse(SparkTraitsApi.isFinalMomentActive(null));
    }

    private static void assertPublicStaticBooleanMethod(String name, Class<?>... parameterTypes)
            throws NoSuchMethodException {
        Method method = SparkTraitsApi.class.getDeclaredMethod(name, parameterTypes);

        assertEquals(boolean.class, method.getReturnType());
        assertTrue(Modifier.isPublic(method.getModifiers()));
        assertTrue(Modifier.isStatic(method.getModifiers()));
    }
}
