package dev.caecorthus.sparktraits.api;

import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.UUID;

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
        assertTrue(SparkTraitsApi.getActiveTraitIds(null).isEmpty());
        assertTrue(SparkTraitsApi.getRevealedTraitIds(null).isEmpty());
        assertNull(SparkTraitsApi.discountShopEntryForCharisma(null, null));
        SparkTraitsApi.restoreActiveTraitsForRuntime(null, null, null);
    }

    @Test
    void charismaDiscountIsNoOpWithoutAPlayerTraitContext() {
        ShopEntry entry = new ShopEntry(new ItemStack(Items.STICK), 50, ShopEntry.Type.TOOL);

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
