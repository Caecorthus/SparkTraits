package dev.caecorthus.sparktraits;

import dev.caecorthus.sparktraits.component.SparkTraitsDataComponentTypes;
import dev.caecorthus.sparktraits.impl.SparkTraitsCommands;
import dev.caecorthus.sparktraits.impl.LastStandService;
import dev.caecorthus.sparktraits.impl.SparkTraitsBuiltInTraits;
import dev.caecorthus.sparktraits.impl.SparkTraitsParticles;
import dev.caecorthus.sparktraits.impl.TraitGameHooks;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SparkTraits implements ModInitializer {
    public static final String MOD_ID = "sparktraits";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        SparkTraitsDataComponentTypes.init();
        SparkTraitsParticles.register();
        SparkTraitsBuiltInTraits.register();
        LastStandService.register();
        TraitGameHooks.register();
        SparkTraitsCommands.register();
    }
}
