package dev.caecorthus.sparktraits;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WraithOwnershipRemovalSourceTest {
    private static final Path MAIN_SOURCES = Path.of("src/main/java");
    private static final Path MAIN_RESOURCES = Path.of("src/main/resources");
    private static final Path CLIENT_SOURCES = Path.of("src/client/java");
    private static final Path CLIENT_RESOURCES = Path.of("src/client/resources");
    private static final List<Path> ALLOWED_WRAITH_REFERENCES = List.of(
            Path.of("src/main/java/dev/caecorthus/sparktraits/api/SparkTraitsApi.java"),
            Path.of("src/main/java/dev/caecorthus/sparktraits/compat/SparkWitchWraithBridge.java"),
            Path.of("src/main/java/dev/caecorthus/sparktraits/component/RetiredTraitIds.java")
    );

    @Test
    void productionSourcesAndResourcesRetainOnlyTheNarrowWraithCompatibilityBridge() throws IOException {
        try (Stream<Path> paths = Stream.of(
                MAIN_SOURCES,
                MAIN_RESOURCES,
                CLIENT_SOURCES,
                CLIENT_RESOURCES
        ).flatMap(this::walkFilesUnchecked)) {
            List<Path> owners = paths
                    .filter(this::containsWraithOwnership)
                    .filter(path -> !ALLOWED_WRAITH_REFERENCES.contains(path))
                    .toList();

            assertTrue(owners.isEmpty(), () -> "Unexpected Wraith ownership: " + owners);
        }
    }

    @Test
    void voicePluginKeepsDepressionBehaviorWithoutTheWraithMicrophoneGuard() throws IOException {
        String voicePlugin = Files.readString(Path.of(
                "src/main/java/dev/caecorthus/sparktraits/voice/SparkTraitsVoiceChatPlugin.java"
        ));

        assertTrue(voicePlugin.contains("DepressionTraitService.shouldMuteVoice(speaker)"));
        assertTrue(voicePlugin.contains("DepressionTraitService.shouldMuteVoice(listener)"));
        assertFalse(voicePlugin.contains("Wraith"));
    }

    private Stream<Path> walkFiles(Path root) throws IOException {
        return Files.walk(root)
                .filter(Files::isRegularFile)
                .map(Path::normalize);
    }

    private Stream<Path> walkFilesUnchecked(Path root) {
        try {
            return walkFiles(root);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot walk " + root, exception);
        }
    }

    private boolean containsWraithOwnership(Path path) {
        String pathText = path.toString().toLowerCase();
        if (pathText.contains("wraith")) {
            return true;
        }
        if (!pathText.endsWith(".java") && !pathText.endsWith(".json")) {
            return false;
        }
        try {
            String contents = Files.readString(path);
            return contents.contains("Wraith") || contents.contains("wraith") || contents.contains("冤魂");
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot inspect " + path, exception);
        }
    }
}
