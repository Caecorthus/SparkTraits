package dev.caecorthus.sparktraits.impl.traits.killer.conscience;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConscienceBomberFrenzyRulesTest {
    @Test
    void constantsMatchApprovedMode() {
        assertEquals(400, ConscienceBomberFrenzyRules.MODE_DURATION_TICKS);
        assertEquals(3600, ConscienceBomberFrenzyRules.SHOP_COOLDOWN_TICKS);
        assertEquals(350, ConscienceBomberFrenzyRules.PRICE);
        assertEquals(0.75F, ConscienceBomberFrenzyRules.THROW_SPEED);
    }

    @Test
    void onlyConscienceBomberCanBuy() {
        assertTrue(ConscienceBomberFrenzyRules.canBuy(true, true));
        assertFalse(ConscienceBomberFrenzyRules.canBuy(true, false));
        assertFalse(ConscienceBomberFrenzyRules.canBuy(false, true));
    }

    @Test
    void protectionUsesEffectiveCivilianWithExactWitchExceptions() {
        assertFalse(ConscienceBomberFrenzyRules.shouldKillTarget(null, true));
        assertTrue(ConscienceBomberFrenzyRules.shouldKillTarget(null, false));
        assertFalse(ConscienceBomberFrenzyRules.shouldKillTarget(
                Identifier.of("wathe", "passenger"), true));
        assertFalse(ConscienceBomberFrenzyRules.shouldKillTarget(
                Identifier.of("sparkwitch", "apprentice_witch"), true));
        assertTrue(ConscienceBomberFrenzyRules.shouldKillTarget(
                Identifier.of("sparkwitch", "grand_witch"), true));
        assertTrue(ConscienceBomberFrenzyRules.shouldKillTarget(
                Identifier.of("sparkwitch", "accomplice"), true));
        assertTrue(ConscienceBomberFrenzyRules.shouldKillTarget(
                Identifier.of("sparkwitch", "murderous_witch"), true));
        assertTrue(ConscienceBomberFrenzyRules.shouldKillTarget(
                Identifier.of("wathe", "killer"), false));
        assertTrue(ConscienceBomberFrenzyRules.shouldKillTarget(
                Identifier.of("noellesroles", "neutral"), false));
    }

    @Test
    void grenadeCooldownGateBlocksInvalidMarkersWithoutChangingOrdinaryGrenades() {
        assertFalse(ConscienceBomberFrenzyRules.shouldBlockGrenadeUse(true, true, true));
        assertTrue(ConscienceBomberFrenzyRules.shouldBlockGrenadeUse(true, false, false));
        assertFalse(ConscienceBomberFrenzyRules.shouldBlockGrenadeUse(false, false, false));
        assertTrue(ConscienceBomberFrenzyRules.shouldBlockGrenadeUse(false, false, true));
    }

    @Test
    void expiryIsExclusiveAtFourHundredTicks() {
        assertTrue(ConscienceBomberFrenzyRules.isActive(999L, 1000L));
        assertFalse(ConscienceBomberFrenzyRules.isActive(1000L, 1000L));
    }

    @Test
    void markerRoundTripsOwnerAndExpiry() {
        UUID owner = UUID.fromString("00000000-0000-0000-0000-000000000123");
        NbtCompound nbt = new NbtCompound();

        ConscienceBomberFrenzyService.writeMarker(nbt, owner, 400L);

        assertEquals(new ConscienceBomberFrenzyService.Marker(owner, 400L),
                ConscienceBomberFrenzyService.readMarker(nbt));
    }

    @Test
    void unrelatedCustomDataIsNotRecognized() {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("other_mod", "keep");

        assertNull(ConscienceBomberFrenzyService.readMarker(nbt));
    }

    @Test
    void roundCleanupScansAllOnlinePlayersBeforeClearingGlobalState() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/dev/caecorthus/sparktraits/impl/traits/killer/conscience/ConscienceBomberFrenzyService.java"
        ));
        int clearAllStart = source.indexOf("public static void clearAll(ServerWorld world)");
        int tickWorldStart = source.indexOf("public static void tickWorld", clearAllStart);

        assertTrue(clearAllStart >= 0);
        assertTrue(tickWorldStart > clearAllStart);
        String clearAll = source.substring(clearAllStart, tickWorldStart);
        assertTrue(clearAll.contains("world.getServer().getPlayerManager().getPlayerList()"));
        assertFalse(clearAll.contains("world.getPlayers()"));
        int clearPlayerCall = clearAll.indexOf("clearPlayer(player)");
        int clearMapCall = clearAll.indexOf("ACTIVE_UNTIL.clear()");
        assertTrue(clearPlayerCall >= 0);
        assertTrue(clearMapCall > clearPlayerCall);
    }
}
