package dev.caecorthus.sparktraits.client.audio;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DepressionRageLoopContractTest {
    private static final Path INSTANCE_SOURCE = Path.of(
            "src/client/java/dev/caecorthus/sparktraits/client/audio/DepressionRageLoopSoundInstance.java"
    );
    private static final Path CONTROLLER_SOURCE = Path.of(
            "src/client/java/dev/caecorthus/sparktraits/client/audio/DepressionRageLoopController.java"
    );

    @Test
    void rageLoopRepeatsLocallyAndFollowsEachPsychoPlayer() throws IOException {
        assertTrue(Files.isRegularFile(INSTANCE_SOURCE), "client rage-loop sound instance must exist");
        assertTrue(Files.isRegularFile(CONTROLLER_SOURCE), "client rage-loop controller must exist");

        String instance = Files.readString(INSTANCE_SOURCE);
        assertTrue(instance.contains("extends AbstractSoundInstance implements TickableSoundInstance"));
        assertTrue(instance.contains("this.repeat = true;"));
        assertTrue(instance.contains("this.repeatDelay = 0;"));
        assertTrue(instance.contains("this.attenuationType = SoundInstance.AttenuationType.LINEAR;"));
        assertTrue(instance.contains("this.relative = false;"));
        assertTrue(instance.contains("public boolean shouldAlwaysPlay()"));
        assertTrue(instance.contains("return true;"));
        assertTrue(instance.contains("this.x = player.getX();"));
        assertTrue(instance.contains("this.y = player.getY();"));
        assertTrue(instance.contains("this.z = player.getZ();"));
        int tickMethod = instance.indexOf("public void tick()");
        assertTrue(tickMethod >= 0);
        assertTrue(instance.indexOf("updatePosition();", tickMethod) > tickMethod);

        String controller = Files.readString(CONTROLLER_SOURCE);
        assertTrue(controller.contains("Map<UUID, DepressionRageLoopSoundInstance>"));
        assertTrue(controller.contains("client.world.getPlayers()"));
        assertTrue(controller.contains("isDepressionPsychoActive()"));
        assertTrue(controller.contains("soundManager.play(instance);"));
        assertTrue(controller.contains("soundManager.stop(instance);"));
        assertFalse(controller.contains("stopSounds("));
        assertFalse(controller.contains("StopSoundS2CPacket"));

        String client = Files.readString(Path.of(
                "src/client/java/dev/caecorthus/sparktraits/client/SparkTraitsClient.java"
        ));
        assertTrue(client.contains("DepressionRageLoopController.tick(client);"));

        String build = Files.readString(Path.of("build.gradle"));
        assertTrue(build.contains(
                "dev/caecorthus/sparktraits/client/audio/DepressionRageLoopController.class"
        ));
        assertTrue(build.contains(
                "dev/caecorthus/sparktraits/client/audio/DepressionRageLoopSoundInstance.class"
        ));
    }

    @Test
    void serverDoesNotReplayOrGloballyStopTheRageLoop() throws IOException {
        Path servicePath = Path.of(
                "src/main/java/dev/caecorthus/sparktraits/impl/traits/civilian/depression/DepressionTraitService.java"
        );
        String service = Files.readString(servicePath);

        assertFalse(service.contains("RAGE_LOOP_INTERVAL_TICKS"));
        assertFalse(service.contains("shouldPlayRageLoop"));
        assertFalse(service.contains("nextRageLoopTicks"));
        assertFalse(service.contains("SparkTraitsSounds.DEPRESSION_RAGE_LOOP"));
        assertTrue(service.contains("CHASE_LOOP_INTERVAL_TICKS"));
        assertTrue(service.contains("playPairMusicSound(player, attacker, SparkTraitsSounds.DEPRESSION_BLIND_RAGE_CHASE)"));

        Path soundRegistry = Path.of(
                "src/main/java/dev/caecorthus/sparktraits/impl/resource/SparkTraitsSounds.java"
        );
        try (Stream<Path> sources = Files.walk(Path.of("src/main/java"))) {
            for (Path source : sources.filter(path -> path.toString().endsWith(".java")).toList()) {
                if (source.equals(soundRegistry)) {
                    continue;
                }
                String productionSource = Files.readString(source);
                assertFalse(productionSource.contains("DEPRESSION_RAGE_LOOP"), source.toString());
                assertFalse(productionSource.contains("depression.rage_loop"), source.toString());
            }
        }
    }
}
