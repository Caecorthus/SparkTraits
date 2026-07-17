package dev.caecorthus.sparktraits.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WraithClientCompatibilitySourceTest {
    private static final Path CLIENT = Path.of("src/client/java/dev/caecorthus/sparktraits/client");

    @Test
    void traitOwnedPresentationYieldsThroughThePublicWraithQueryOnly() throws IOException {
        String pig = read("mixin/PigPlayerRendererMixin.java");
        String depression = read("render/DepressionScreenEffects.java");
        String roundText = read("mixin/RoundTextRendererMixin.java");
        String combined = pig + depression + roundText;

        assertTrue(pig.indexOf("SparkTraitsApi.isWraithActive(viewer)")
                < pig.indexOf("PigTraitService.isPig(player)"));
        assertTrue(pig.contains("viewer.isSpectator()"));
        assertTrue(pig.contains("SparkTraitsApi.isWraithActive(player)"));

        assertTrue(depression.indexOf("SparkTraitsApi.isWraithActive(player)")
                < depression.indexOf("DepressionTraitService.depressionScreenEffectStrength"));

        assertTrue(roundText.contains("Identifier.of(\"sparkwitch\", \"wraith\")"));
        assertTrue(roundText.indexOf("if (transitionalWraith)")
                < roundText.indexOf("EffectiveTraitService.hasConscience(player)"));
        assertTrue(roundText.indexOf("if (transitionalWraith)")
                < roundText.indexOf("EffectiveTraitService.hasImpostor(player)"));

        assertFalse(combined.contains("dev.caecorthus.sparkwitch"));
        assertFalse(combined.contains("impl.traits.global.wraith"));
        assertFalse(combined.contains("WraithPlayerComponent"));
    }

    private static String read(String relativePath) throws IOException {
        return Files.readString(CLIENT.resolve(relativePath));
    }
}
