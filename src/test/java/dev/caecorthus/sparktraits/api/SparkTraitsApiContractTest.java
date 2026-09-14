package dev.caecorthus.sparktraits.api;

import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SparkTraitsApiContractTest {

    @Test
    void publicFacadeKeepsDownstreamReflectionDescriptors() throws NoSuchMethodException {
        assertPublicStaticBooleanMethod("hasActiveTrait", PlayerEntity.class, Identifier.class);
        assertPublicStaticBooleanMethod("hasLastStandTriggeredThisRound", ServerWorld.class, UUID.class);
        assertPublicStaticBooleanMethod("isFinalMomentActive", World.class);
        assertPublicStaticBooleanMethod("isFakeDeathBody", Entity.class);
        assertPublicStaticBooleanMethod("isInstinctHidden", PlayerEntity.class, PlayerEntity.class);
        assertPublicStaticBooleanMethod("isLastStandPending", PlayerEntity.class);
        assertPublicStaticBooleanMethod("isLastStandDeathIntercepted", PlayerEntity.class);
        assertPublicStaticBooleanMethod("isLastEscapeActive", PlayerEntity.class);
        assertPublicStaticBooleanMethod("isKillerInteractionBlocked", PlayerEntity.class);
        assertPublicStaticBooleanMethod("hasLastEscapeGrayscale", PlayerEntity.class);
        assertPublicStaticBooleanMethod("shouldCancelMeleeAttack", ServerPlayerEntity.class, ServerPlayerEntity.class, ItemStack.class);
        assertEquals(float.class, SparkTraitsApi.class.getMethod("getLastEscapeDesaturation", PlayerEntity.class).getReturnType());
        assertEquals(int.class, SparkTraitsApi.class.getMethod("getForcedMeleeCooldownTicks", PlayerEntity.class, ItemStack.class).getReturnType());

        assertPublicStaticCollectionMethod("getActiveTraitIds", PlayerEntity.class);
        assertPublicStaticCollectionMethod("getRevealedTraitIds", PlayerEntity.class);
        assertPublicStaticShopEntryMethod("discountShopEntryForCharisma", PlayerEntity.class, ShopEntry.class);
        Method restore = SparkTraitsApi.class.getDeclaredMethod(
                "restoreActiveTraitsForRuntime",
                ServerPlayerEntity.class,
                Collection.class,
                Collection.class
        );
        assertEquals(void.class, restore.getReturnType());
        assertTrue(Modifier.isPublic(restore.getModifiers()));
        assertTrue(Modifier.isStatic(restore.getModifiers()));
    }

    @Test
    void publicFacadeQueriesAreNullSafe() {
        assertFalse(SparkTraitsApi.hasActiveTrait(null, null));
        assertFalse(SparkTraitsApi.hasLastStandTriggeredThisRound(null, null));
        assertFalse(SparkTraitsApi.isFinalMomentActive(null));
        assertFalse(SparkTraitsApi.isFakeDeathBody(null));
        assertFalse(SparkTraitsApi.isInstinctHidden(null, null));
        assertFalse(SparkTraitsApi.isLastStandPending(null));
        assertFalse(SparkTraitsApi.isLastStandDeathIntercepted(null));
        assertFalse(SparkTraitsApi.isLastEscapeActive(null));
        assertFalse(SparkTraitsApi.isKillerInteractionBlocked(null));
        assertFalse(SparkTraitsApi.hasLastEscapeGrayscale(null));
        assertFalse(SparkTraitsApi.shouldCancelMeleeAttack(null, null, null));
        assertEquals(0, SparkTraitsApi.getForcedMeleeCooldownTicks(null, null));
        assertEquals(0.0f, SparkTraitsApi.getLastEscapeDesaturation(null));
        assertTrue(SparkTraitsApi.getActiveTraitIds(null).isEmpty());
        assertTrue(SparkTraitsApi.getRevealedTraitIds(null).isEmpty());
        assertNull(SparkTraitsApi.discountShopEntryForCharisma(null, null));
        SparkTraitsApi.restoreActiveTraitsForRuntime(null, null, null);
    }

    @Test
    void escapeVisionCapabilityIsExplicitAndNeutralArraysAreFresh() throws Exception {
        Method version = SparkTraitsApi.class.getMethod("getLastEscapeVisionProtocolVersion");
        Method composition = SparkTraitsApi.class.getMethod("getLastEscapeComposition", PlayerEntity.class);
        assertEquals(int.class, version.getReturnType());
        assertEquals(float[].class, composition.getReturnType());
        for (Method method : new Method[] {version, composition}) {
            assertTrue(Modifier.isStatic(method.getModifiers()));
            assertTrue(Modifier.isPublic(method.getModifiers()));
        }
        assertEquals(0, SparkTraitsApi.getLastEscapeVisionProtocolVersion());
        float[] neutral = SparkTraitsApi.getLastEscapeComposition(null);
        neutral[0] = 1;
        assertArrayEquals(new float[] {0, 0, 1}, SparkTraitsApi.getLastEscapeComposition(null));
        try {
            SparkTraitsApi.installLastEscapeVisionProvider(player -> { throw new AssertionError("Null must not reach provider"); });
            assertEquals(1, SparkTraitsApi.getLastEscapeVisionProtocolVersion());
            assertArrayEquals(new float[] {0, 0, 1}, SparkTraitsApi.getLastEscapeComposition(null));
        } finally {
            SparkTraitsApi.installLastEscapeVisionProvider(null);
        }
        assertEquals(0, SparkTraitsApi.getLastEscapeVisionProtocolVersion());
    }

    @Test
    void charismaDiscountIsNoOpWithoutAPlayerTraitContext() {
        // This no-op contract must not bootstrap untransformed Minecraft registries in plain JUnit.
        ShopEntry entry = new ShopEntry.Builder("contract", null, 50, null).build();

        assertSame(entry, SparkTraitsApi.discountShopEntryForCharisma(null, entry));
    }

    private static void assertPublicStaticCollectionMethod(String name, Class<?>... parameterTypes) {
        Method method = findMethod(name, parameterTypes);

        assertNotNull(method);
        assertEquals(Collection.class, method.getReturnType());
        assertTrue(Modifier.isPublic(method.getModifiers()));
        assertTrue(Modifier.isStatic(method.getModifiers()));
    }

    private static void assertPublicStaticBooleanMethod(String name, Class<?>... parameterTypes) {
        Method method = findMethod(name, parameterTypes);

        assertNotNull(method);
        assertEquals(boolean.class, method.getReturnType());
        assertTrue(Modifier.isPublic(method.getModifiers()));
        assertTrue(Modifier.isStatic(method.getModifiers()));
    }

    private static void assertPublicStaticShopEntryMethod(String name, Class<?>... parameterTypes) {
        Method method = findMethod(name, parameterTypes);

        assertNotNull(method);
        assertEquals(ShopEntry.class, method.getReturnType());
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
