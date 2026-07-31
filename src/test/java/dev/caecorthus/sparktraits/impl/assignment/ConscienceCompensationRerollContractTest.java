package dev.caecorthus.sparktraits.impl.assignment;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ConscienceCompensationRerollContractTest {
    @Test
    void compensationRerollsAfterRoleConversionBeforeFinalCommit() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/dev/caecorthus/sparktraits/impl/assignment/TraitAssignmentService.java"
        )).replaceAll("\\s+", " ");

        int compensationCall = source.indexOf("addExtraKillersForConscience( world,");
        int pigPostprocessing = source.indexOf("forcePigOntoPigGod(gameComponent, traitWorld, plans);");
        int finalCommit = source.indexOf("playerTraits.setActiveTraits(plan.traits(), reason);");
        int conversion = source.indexOf("gameComponent.addRole(extraKiller.player(), compensationRole);");
        int rebuildReservations = source.indexOf("rebuildUniqueTraitReservations(randomUniqueTraitReservations, plans);");
        int reroll = source.indexOf("List<Identifier> rerolledTraits = TraitSelector.selectRandomTraits(");

        assertTrue(compensationCall >= 0 && compensationCall < pigPostprocessing);
        assertTrue(pigPostprocessing < finalCommit);
        assertTrue(conversion >= 0 && conversion < rebuildReservations);
        assertTrue(rebuildReservations < reroll);
        assertTrue(source.contains("Set.of(ConscienceTrait.ID, ImpostorTrait.ID)"));
        assertTrue(source.contains("extraKiller.lockedTraits(), randomUniqueTraitReservations, CONSCIENCE_COMPENSATION_REROLL_EXCLUSIONS"));
        assertTrue(source.contains("rebuildUniqueTraitReservations(randomUniqueTraitReservations, plans);"));
        assertTrue(source.contains("extraKiller.replaceRandomTraits(rerolledTraits);"));
    }
}
