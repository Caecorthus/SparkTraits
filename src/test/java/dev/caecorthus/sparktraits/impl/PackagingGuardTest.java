package dev.caecorthus.sparktraits.impl;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PackagingGuardTest {
    @Test
    void packagedJarGuardRequiresClientVersionHandshakeClasses() throws IOException {
        String buildScript = Files.readString(Path.of("build.gradle"));

        assertTrue(buildScript.contains("dev/caecorthus/sparktraits/client/SparkTraitsClient.class"));
        assertTrue(buildScript.contains(
                "dev/caecorthus/sparktraits/client/net/SparkTraitsClientVersionHandshake.class"
        ));
    }
}
