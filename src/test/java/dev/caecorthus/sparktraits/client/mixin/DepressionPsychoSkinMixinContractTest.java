package dev.caecorthus.sparktraits.client.mixin;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DepressionPsychoSkinMixinContractTest {
    @Test
    void fullBodyDepressionPsychoSkinOverridesTheFinalTexture() throws IOException {
        String source = Files.readString(Path.of(
                "src/client/java/dev/caecorthus/sparktraits/client/mixin/DepressionPsychoSkinMixin.java"
        ));

        assertTrue(source.contains("import com.llamalad7.mixinextras.injector.ModifyReturnValue;"));
        assertTrue(source.contains("@Mixin(value = PlayerEntityRenderer.class, priority = 500)"));
        assertTrue(source.contains("@ModifyReturnValue("));
        assertTrue(source.contains("at = @At(\"RETURN\")"));
        assertTrue(source.contains("Identifier originalTexture"));
        assertTrue(source.contains("return texture == null ? originalTexture : texture;"));
        assertFalse(source.contains("CallbackInfoReturnable<Identifier>"));
        assertFalse(source.contains("at = @At(\"HEAD\")"));
        assertFalse(source.contains("cancellable = true"));
    }
}
