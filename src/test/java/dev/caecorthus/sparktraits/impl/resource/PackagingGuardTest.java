package dev.caecorthus.sparktraits.impl.resource;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import dev.caecorthus.sparktraits.client.SparkTraitsClient;
import dev.caecorthus.sparktraits.client.net.version.SparkTraitsClientVersionHandshake;
import dev.caecorthus.sparktraits.net.version.SparkTraitsPackets;
import dev.caecorthus.sparktraits.net.version.SparkTraitsServerConfirmS2CPacket;
import dev.caecorthus.sparktraits.net.version.SparkTraitsServerConnection;
import dev.caecorthus.sparktraits.net.version.SparkTraitsVersionCheck;
import dev.caecorthus.sparktraits.voice.SparkTraitsVoiceChatPlugin;

class PackagingGuardTest {
    @Test
    void packagedJarGuardRequiresClientVersionHandshakeClasses() throws IOException {
        String buildScript = Files.readString(Path.of("build.gradle"));

        assertTrue(buildScript.contains("dev/caecorthus/sparktraits/client/SparkTraitsClient.class"));
        assertTrue(buildScript.contains("dev/caecorthus/sparktraits/net/version/SparkTraitsServerConnection.class"));
        assertTrue(buildScript.contains("dev/caecorthus/sparktraits/net/version/SparkTraitsPackets.class"));
        assertTrue(buildScript.contains("dev/caecorthus/sparktraits/net/version/SparkTraitsServerConfirmS2CPacket.class"));
        assertTrue(buildScript.contains(
                "dev/caecorthus/sparktraits/client/net/version/SparkTraitsClientVersionHandshake.class"
        ));
    }

    @Test
    void playStageConfirmationIsWiredToClientConfirmationGate() throws IOException {
        String packetsSource = Files.readString(Path.of(
                "src/main/java/dev/caecorthus/sparktraits/net/version/SparkTraitsPackets.java"
        ));
        String clientHandshakeSource = Files.readString(Path.of(
                "src/client/java/dev/caecorthus/sparktraits/client/net/version/SparkTraitsClientVersionHandshake.java"
        ));

        assertTrue(packetsSource.contains("ServerPlayConnectionEvents.JOIN"));
        assertTrue(packetsSource.contains("SparkTraitsServerConfirmS2CPacket"));
        assertTrue(clientHandshakeSource.contains("ClientPlayNetworking.registerGlobalReceiver"));
        assertTrue(clientHandshakeSource.contains("SparkTraitsVersionCheck.isCompatible"));
        assertTrue(clientHandshakeSource.contains("SparkTraitsServerConnection.confirmServer()"));
    }

    @Test
    void cardinalComponentMetadataOnlyListsRegisteredKeys() throws IOException {
        String metadata = Files.readString(Path.of("src/main/resources/fabric.mod.json"));

        assertTrue(metadata.contains("\"sparktraits:traits\""));
        assertTrue(metadata.contains("\"sparktraits:world\""));
        assertFalse(metadata.contains("sparktraits:role_enhancements"));
        assertFalse(metadata.contains("sparktraits:role_enhancement_world"));
    }

    @Test
    void voiceChatPluginIsDeclaredAndPackaged() throws IOException {
        String metadata = Files.readString(Path.of("src/main/resources/fabric.mod.json"));
        String buildScript = Files.readString(Path.of("build.gradle"));

        assertTrue(metadata.contains("\"voicechat\""));
        assertTrue(metadata.contains(SparkTraitsVoiceChatPlugin.class.getName()));
        assertTrue(buildScript.contains("dev/caecorthus/sparktraits/voice/SparkTraitsVoiceChatPlugin.class"));
    }
}
