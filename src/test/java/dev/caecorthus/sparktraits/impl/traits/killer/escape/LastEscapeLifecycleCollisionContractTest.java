package dev.caecorthus.sparktraits.impl.traits.killer.escape;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Source and mapped-bytecode contracts; not a substitute for transformed multiplayer tests. */
class LastEscapeLifecycleCollisionContractTest {
    private static final Path JAVA = Path.of("src/main/java/dev/caecorthus/sparktraits");

    private static String source(String file) throws Exception {
        return Files.readString(JAVA.resolve(file));
    }

    @Test void deferredDeathIsPersistedOnlyAfterConfirmationWithTwoOnlineGuards() throws Exception {
        String service = source("impl/traits/killer/escape/LastEscapeService.java");
        String tick = service.substring(service.indexOf("private static void tick("),
                service.indexOf("private static void removeSpeed("));
        int onlineGuard = tick.indexOf("if (manager.getPlayer(player.getUuid()) != null)");
        int deadlineGuard = tick.indexOf("if (isActive(player)) continue;");
        int death = tick.indexOf("GameFunctions.killPlayer(player, true, null, GameConstants.DeathReasons.ESCAPED, true)");
        int confirmation = tick.indexOf("if (!GameWorldComponent.KEY.get(world).isPlayerDead(player.getUuid())) continue;");
        int saveGuard = tick.indexOf("if (manager.getPlayer(player.getUuid()) == null)");
        int save = tick.indexOf("sparktraits$saveLastEscapePlayerData(player)");
        assertTrue(onlineGuard >= 0 && onlineGuard < deadlineGuard);
        assertTrue(deadlineGuard < death && death < confirmation);
        assertTrue(confirmation < saveGuard && saveGuard < save);
        // A cancelled terminal call remains pending rather than silently losing cleanup.
        assertFalse(tick.substring(deadlineGuard, confirmation).contains("offline.remove"));
        assertTrue(tick.substring(onlineGuard, deadlineGuard).contains("offline.remove(player.getUuid());\n                continue;"));
        assertTrue(source("mixin/LastEscapePlayerManagerAccessor.java").contains("@Invoker(\"savePlayerData\")"));
        MethodNode method = method("net/minecraft/server/PlayerManager", "savePlayerData",
                "(Lnet/minecraft/server/network/ServerPlayerEntity;)V");
        assertEquals(Opcodes.ACC_PROTECTED, method.access & Opcodes.ACC_PROTECTED);
    }

    @Test void collisionExemptionIsBilateralPlayerPhaseOnlyAndLeavesNormalBehaviorUntouched() throws Exception {
        String service = source("impl/traits/killer/escape/LastEscapeService.java");
        String predicate = service.substring(service.indexOf("public static boolean blocksEntityCollision("),
                service.indexOf("public static boolean hasGrayscale("));
        assertTrue(predicate.contains("first instanceof PlayerEntity firstPlayer && isActive(firstPlayer)"));
        assertTrue(predicate.contains("|| (second instanceof PlayerEntity secondPlayer && isActive(secondPlayer))"));
        assertFalse(predicate.contains("hasGrayscale"));
        assertFalse(predicate.contains("hasActiveTrait"));
        for (String mixin : List.of("LastEscapeEntityPushMixin", "LastEscapeMinecartPushMixin",
                "LastEscapeVehicleCollisionMixin")) {
            String adapter = source("mixin/" + mixin + ".java");
            assertTrue(adapter.contains("if (LastEscapeService.blocksEntityCollision("));
            assertTrue(adapter.contains("at = @At(\"HEAD\"), cancellable = true"));
            assertFalse(adapter.contains("require = 0"));
            assertFalse(adapter.contains("noClip"));
            assertFalse(adapter.contains("setVelocity"));
            assertFalse(adapter.contains("else"));
        }
    }

    @Test void mappedVehicleOverridesReachOurExactRequiredSelectors() throws Exception {
        String entity = "net/minecraft/entity/Entity";
        String boat = "net/minecraft/entity/vehicle/BoatEntity";
        String cart = "net/minecraft/entity/vehicle/AbstractMinecartEntity";
        String collision = "(Lnet/minecraft/entity/Entity;)Z";
        String push = "(Lnet/minecraft/entity/Entity;)V";
        String pair = "(Lnet/minecraft/entity/Entity;Lnet/minecraft/entity/Entity;)Z";
        assertTrue(calls(method(boat, "collidesWith", collision), boat, "canCollide", pair));
        assertTrue(calls(method(cart, "collidesWith", collision), boat, "canCollide", pair));
        assertTrue((method(boat, "canCollide", pair).access & Opcodes.ACC_STATIC) != 0);
        // Boat delegates to VehicleEntity, which inherits Entity's push; minecart does not.
        assertTrue(calls(method(boat, "pushAwayFrom", push), "net/minecraft/entity/vehicle/VehicleEntity", "pushAwayFrom", push));
        assertTrue(readClass("net/minecraft/entity/vehicle/VehicleEntity").methods.stream()
                .noneMatch(m -> m.name.equals("pushAwayFrom") && m.desc.equals(push)));
        method(entity, "pushAwayFrom", push);
        method(cart, "pushAwayFrom", push);
        assertTrue(source("mixin/LastEscapeVehicleCollisionMixin.java").contains("canCollide" + pair));
        assertTrue(source("mixin/LastEscapeEntityPushMixin.java").contains("pushAwayFrom" + push));
        assertTrue(source("mixin/LastEscapeMinecartPushMixin.java").contains("pushAwayFrom" + push));
    }

    @Test void allAdaptersAreRegisteredExactlyOnceOnBothLogicalSides() throws Exception {
        String json = Files.readString(Path.of("src/main/resources/sparktraits.mixins.json"));
        for (String mixin : List.of("LastEscapeEntityPushMixin", "LastEscapeMinecartPushMixin",
                "LastEscapeVehicleCollisionMixin", "LastEscapePlayerManagerAccessor")) {
            String entry = "\"" + mixin + "\"";
            assertTrue(json.indexOf(entry) >= 0);
            assertEquals(json.indexOf(entry), json.lastIndexOf(entry));
            assertTrue(json.indexOf(entry) > json.indexOf("\"mixins\""));
        }
        assertTrue(json.contains("\"defaultRequire\": 1"));
    }

    private static ClassNode readClass(String owner) throws Exception {
        try (var stream = LastEscapeLifecycleCollisionContractTest.class.getClassLoader()
                .getResourceAsStream(owner + ".class")) {
            assertNotNull(stream, "Missing mapped dependency: " + owner);
            ClassNode node = new ClassNode();
            new ClassReader(stream).accept(node, 0);
            return node;
        }
    }

    private static MethodNode method(String owner, String name, String descriptor) throws Exception {
        return readClass(owner).methods.stream().filter(m -> m.name.equals(name) && m.desc.equals(descriptor))
                .findFirst().orElseThrow(() -> new AssertionError(owner + "." + name + descriptor));
    }

    private static boolean calls(MethodNode method, String owner, String name, String descriptor) {
        for (var instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call && call.owner.equals(owner)
                    && call.name.equals(name) && call.desc.equals(descriptor)) return true;
        }
        return false;
    }
}
