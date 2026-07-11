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
        assertTrue(component.contains("if (buf.readableBytes() > 0) {\n            buf.readBoolean();\n        }"));
        assertFalse(component.contains("arrogantAsfActive"));
    }

    @Test
    void noellesRolesPacketMixinSupportsBothBundledLambdaLayouts() throws IOException {
        String mixin = source("src/main/java/dev/caecorthus/sparktraits/mixin/NoellesRolesPacketMixin.java");

        assertTrue(mixin.contains("{\"lambda$registerPackets$31\", \"lambda$registerPackets$0\"}"));
        assertTrue(mixin.contains("{\"lambda$registerPackets$32\", \"lambda$registerPackets$1\"}"));
        assertTrue(mixin.contains("{\"lambda$registerPackets$34\", \"lambda$registerPackets$2\"}"));
        assertTrue(mixin.contains("{\"lambda$registerPackets$36\", \"lambda$registerPackets$5\"}"));
        assertTrue(mixin.contains("{\"lambda$registerPackets$37\", \"lambda$registerPackets$6\"}"));
        assertTrue(mixin.contains("{\"lambda$registerPackets$35\", \"lambda$registerPackets$4\"}"));
        assertTrue(mixin.contains("{\"lambda$registerPackets$38\", \"lambda$registerPackets$7\"}"));
        assertTrue(mixin.contains("{\"lambda$registerPackets$39\", \"lambda$registerPackets$8\"}"));
        assertTrue(mixin.contains("{\"lambda$registerPackets$40\", \"lambda$registerPackets$9\"}"));
        assertTrue(mixin.contains("{\"lambda$registerPackets$43\", \"lambda$registerPackets$12\"}"));
        assertTrue(mixin.contains("{\"lambda$registerPackets$44\", \"lambda$registerPackets$13\"}"));
        assertTrue(mixin.contains("{\"lambda$registerPackets$45\", \"lambda$registerPackets$14\"}"));
    }

    private static String source(String relativePath) throws IOException {
        return Files.readString(Path.of(System.getProperty("user.dir"), relativePath));
    }
}
