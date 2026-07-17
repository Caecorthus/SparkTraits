package dev.caecorthus.sparktraits.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

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

        assertAppearsBefore(
                pig,
                "SparkTraitsApi.isWraithActive(viewer)",
                "PigTraitService.isPig(player)"
        );
        assertTrue(Pattern.compile(
                "boolean\\s+spectatorReveal\\s*=\\s*viewer\\s*!=\\s*null"
                        + "\\s*&&\\s*viewer\\.isSpectator\\(\\)"
                        + "\\s*&&\\s*SparkTraitsApi\\.isWraithActive\\(player\\)\\s*;"
        ).matcher(pig).find());

        assertAppearsBefore(
                depression,
                "SparkTraitsApi.isWraithActive(player)",
                "DepressionTraitService.depressionScreenEffectStrength"
        );

        assertTrue(roundText.contains("Identifier.of(\"sparkwitch\", \"wraith\")"));
        assertAppearsBefore(
                roundText,
                "if (transitionalWraith)",
                "EffectiveTraitService.hasConscience(player)"
        );
        assertAppearsBefore(
                roundText,
                "if (transitionalWraith)",
                "EffectiveTraitService.hasImpostor(player)"
        );

        assertFalse(combined.contains("dev.caecorthus.sparkwitch"));
        assertFalse(combined.contains("impl.traits.global.wraith"));
        assertFalse(combined.contains("WraithPlayerComponent"));
    }

    private static String read(String relativePath) throws IOException {
        return Files.readString(CLIENT.resolve(relativePath));
    }

    private static void assertAppearsBefore(String source, String earlier, String later) {
        int earlierIndex = source.indexOf(earlier);
        int laterIndex = source.indexOf(later);
        assertTrue(earlierIndex >= 0, () -> "Missing source token: " + earlier);
        assertTrue(laterIndex >= 0, () -> "Missing source token: " + later);
        assertTrue(earlierIndex < laterIndex, () -> earlier + " must precede " + later);
    }
}
