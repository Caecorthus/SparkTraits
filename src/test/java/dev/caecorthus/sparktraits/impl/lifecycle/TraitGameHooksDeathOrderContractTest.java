package dev.caecorthus.sparktraits.impl.lifecycle;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraitGameHooksDeathOrderContractTest {
    @Test
    void bombManiacServiceRegistersExactlyOnceBesideConscienceServices() throws IOException {
        String register = block(readSource(), "public static void register()");

        assertEquals(1, occurrences(register, "ConscienceBomberFrenzyService.register();"));
        assertTrue(register.replaceAll("\\s+", " ").contains(
                "ConscienceSerialKillerService.register(); "
                        + "ConsciencePoisonerService.register(); "
                        + "ConscienceBomberFrenzyService.register();"
        ));
    }

    @Test
    void resetClearsTimedBombBeforeBombManiacOwnerState() throws IOException {
        String reset = block(readSource(), "ResetPlayer.EVENT.register(player ->");
        int timedBombCleanup = reset.indexOf("ConscienceBombService.clearTimedBomb(player);");
        int bombManiacCleanup = reset.indexOf("ConscienceBomberFrenzyService.clearPlayer(player);");
        int traitCleanup = reset.indexOf("clearActiveTraits(TraitRemovalReason.RESET)");

        assertTrue(timedBombCleanup >= 0);
        assertTrue(bombManiacCleanup > timedBombCleanup);
        assertTrue(traitCleanup > bombManiacCleanup);
    }

    @Test
    void dividendRunsOnlyAfterLastStandReturnsAndBeforeKillerConsequences() throws IOException {
        String afterKill = block(readSource(), "KillPlayer.AFTER.register((victim, killer, deathReason) ->");
        String lastStandBranch = block(afterKill, "if (lastStandStarted)");
        int branchEnd = afterKill.indexOf(lastStandBranch) + lastStandBranch.length();
        int dividend = afterKill.indexOf("ConscienceEconomyService.rewardAfterConfirmedRealDeath(victim);");
        int killerConsequences = afterKill.indexOf("KillerTraitService.handleAfterRealKill(victim, killer, deathReason);");
        int bodyguardConsequences = afterKill.indexOf("ImpostorBodyguardService.handleAfterKill(victim);");
        int serialKillerConsequences = afterKill.indexOf(
                "ConscienceSerialKillerService.handleAfterKill(victim, killer, deathReason);"
        );
        int bombManiacCleanup = afterKill.indexOf("ConscienceBomberFrenzyService.clearPlayer(victim);");
        int traitCleanup = afterKill.indexOf("playerTraits.clearActiveTraits(TraitRemovalReason.DEATH);");

        assertTrue(lastStandBranch.contains("return;"));
        assertFalse(lastStandBranch.contains("ConscienceBomberFrenzyService.clearPlayer(victim);"));
        assertTrue(dividend > branchEnd);
        assertTrue(killerConsequences > dividend);
        assertTrue(bodyguardConsequences > killerConsequences);
        assertTrue(serialKillerConsequences > bodyguardConsequences);
        assertTrue(bombManiacCleanup > serialKillerConsequences);
        assertTrue(traitCleanup > bombManiacCleanup);
    }

    @Test
    void syncsConfirmedDeathOnlyBeforeRealDeathTraits() throws IOException {
        String source = readSource();
        String afterKill = block(source, "KillPlayer.AFTER.register((victim, killer, deathReason) ->");
        String lastStandBranch = block(afterKill, "if (lastStandStarted)");
        String syncHelper = block(source, "private static void syncPlayerTraitsToNewSpectators(");

        assertEquals(2, occurrences(afterKill, "syncPlayerTraitsToNewSpectators("));
        assertFalse(lastStandBranch.contains("gameComponent.sync();"));
        assertFalse(syncHelper.contains("gameComponent.sync();"));

        int realDeathStart = afterKill.indexOf(lastStandBranch) + lastStandBranch.length();
        String realDeathPath = afterKill.substring(realDeathStart);
        int gameSync = realDeathPath.indexOf("gameComponent.sync();");
        int traitSync = realDeathPath.indexOf("syncPlayerTraitsToNewSpectators(");

        assertEquals(1, occurrences(afterKill, "gameComponent.sync();"));
        assertTrue(gameSync >= 0, "confirmed real-death state must reach clients immediately");
        assertTrue(traitSync > gameSync, "confirmed death must arrive before trait-dependent rendering state");
    }

    @Test
    void roundFinalizeClearsTimedBombsBeforePlayerTraits() throws IOException {
        String finalize = block(readSource(), "GameEvents.ON_FINISH_FINALIZE.register((world, gameComponent) ->");
        int timedBombCleanup = finalize.indexOf("ConscienceBombService.clearAll();");
        int bombManiacCleanup = finalize.indexOf("ConscienceBomberFrenzyService.clearAll(serverWorld);");
        int serialKillerCleanup = finalize.indexOf("ConscienceSerialKillerService.clearAll();");
        int lastStandCleanup = finalize.indexOf("LastStandService.clearRoundState(serverWorld);");
        int depressionCleanup = finalize.indexOf("DepressionTraitService.clearRoundState(serverWorld);");
        int traitCleanup = finalize.indexOf("clearActiveTraits(serverWorld, gameComponent);");

        assertTrue(timedBombCleanup >= 0);
        assertTrue(bombManiacCleanup > timedBombCleanup);
        assertTrue(serialKillerCleanup > bombManiacCleanup);
        assertTrue(lastStandCleanup > serialKillerCleanup);
        assertTrue(depressionCleanup > lastStandCleanup);
        assertTrue(traitCleanup > depressionCleanup);
    }

    private static String readSource() throws IOException {
        return Files.readString(Path.of(
                "src/main/java/dev/caecorthus/sparktraits/impl/lifecycle/TraitGameHooks.java"
        ));
    }

    private static String block(String source, String startNeedle) {
        int start = source.indexOf(startNeedle);
        assertTrue(start >= 0, () -> "Missing block start: " + startNeedle);
        int openBrace = source.indexOf('{', start);
        assertTrue(openBrace >= 0, () -> "Missing opening brace after: " + startNeedle);

        int depth = 0;
        for (int index = openBrace; index < source.length(); index++) {
            char current = source.charAt(index);
            if (current == '{') {
                depth++;
            } else if (current == '}' && --depth == 0) {
                return source.substring(start, index + 1);
            }
        }
        throw new AssertionError("Missing closing brace after: " + startNeedle);
    }

    private static int occurrences(String source, String needle) {
        return (source.length() - source.replace(needle, "").length()) / needle.length();
    }
}
