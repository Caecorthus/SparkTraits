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
        assertTrue(buildScript.contains("dev/caecorthus/sparktraits/net/SparkTraitsServerConnection.class"));
        assertTrue(buildScript.contains("dev/caecorthus/sparktraits/net/SparkTraitsPackets.class"));
        assertTrue(buildScript.contains("dev/caecorthus/sparktraits/net/SparkTraitsServerConfirmS2CPacket.class"));
        assertTrue(buildScript.contains(
                "dev/caecorthus/sparktraits/client/net/SparkTraitsClientVersionHandshake.class"
        ));
    }

    @Test
    void playStageConfirmationIsWiredToClientConfirmationGate() throws IOException {
        String packetsSource = Files.readString(Path.of(
                "src/main/java/dev/caecorthus/sparktraits/net/SparkTraitsPackets.java"
        ));
        String clientHandshakeSource = Files.readString(Path.of(
                "src/client/java/dev/caecorthus/sparktraits/client/net/SparkTraitsClientVersionHandshake.java"
        ));

        assertTrue(packetsSource.contains("ServerPlayConnectionEvents.JOIN"));
        assertTrue(packetsSource.contains("SparkTraitsServerConfirmS2CPacket"));
        assertTrue(clientHandshakeSource.contains("ClientPlayNetworking.registerGlobalReceiver"));
        assertTrue(clientHandshakeSource.contains("SparkTraitsVersionCheck.isCompatible"));
        assertTrue(clientHandshakeSource.contains("SparkTraitsServerConnection.confirmServer()"));
    }
}
