package dev.caecorthus.sparktraits;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Temporary guard while the old implementation is absent; delete or replace it when the rewrite starts.
 * 旧实现清空期间的临时保护；开始重写灵探时删除或替换本测试。
 */
class SpiritSleuthOldImplementationRemovalTest {
    private static final List<Path> PRODUCTION_ROOTS = List.of(
            Path.of("src/main/java"),
            Path.of("src/main/resources"),
            Path.of("src/client/java"),
            Path.of("src/client/resources")
    );

    @Test
    void productionSourcesAndResourcesContainNoOldSpiritSleuthImplementation() throws IOException {
        try (Stream<Path> paths = PRODUCTION_ROOTS.stream().flatMap(this::walkFilesUnchecked)) {
            List<Path> references = paths
                    .filter(this::containsSpiritSleuthReference)
                    .toList();

            assertTrue(references.isEmpty(), () -> "Old Spirit Sleuth implementation remains in: " + references);
        }
    }

    private Stream<Path> walkFilesUnchecked(Path root) {
        try {
            return Files.walk(root)
                    .filter(Files::isRegularFile)
                    .map(Path::normalize);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot walk " + root, exception);
        }
    }

    private boolean containsSpiritSleuthReference(Path path) {
        String pathText = path.toString().toLowerCase();
        if (pathText.contains("spiritsleuth") || pathText.contains("spirit_sleuth")) {
            return true;
        }
        if (!pathText.endsWith(".java") && !pathText.endsWith(".json")) {
            return false;
        }
        try {
            String contents = Files.readString(path);
            return contents.contains("SpiritSleuth")
                    || contents.contains("Spirit Sleuth")
                    || contents.contains("spirit_sleuth")
                    || contents.contains("灵探");
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot inspect " + path, exception);
        }
    }
}
