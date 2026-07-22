package dev.caecorthus.sparktraits.component;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArrogantAsfRetirementContractTest {
    @Test
    void retiredTraitIsNeitherRegisteredNorForcedDuringAssignment() throws IOException {
        String builtIns = source("src/main/java/dev/caecorthus/sparktraits/impl/registry/SparkTraitsBuiltInTraits.java");
        String assignment = source("src/main/java/dev/caecorthus/sparktraits/impl/assignment/TraitAssignmentService.java");

        assertFalse(builtIns.contains("ArrogantAsf"));
        assertFalse(builtIns.contains("arrogant_asf"));
        assertFalse(assignment.contains("ArrogantAsf"));
        assertFalse(assignment.contains("arrogant_asf"));
    }

    @Test
    void retiredTrailingSyncFieldRemainsWrittenFalseAndOptionallyDiscarded() throws IOException {
        String component = source("src/main/java/dev/caecorthus/sparktraits/component/TraitPlayerComponent.java");

        assertTrue(component.contains("buf.writeBoolean(false);"));
        assertTrue(component.contains("if (buf.readableBytes() > 0) {"));
        assertTrue(component.contains("buf.readBoolean();"));
        assertFalse(component.contains("arrogantAsfActive"));
    }

    private static String source(String relativePath) throws IOException {
        return Files.readString(Path.of(System.getProperty("user.dir"), relativePath));
    }
}
