package dev.caecorthus.sparktraits;

import dev.caecorthus.sparktraits.component.SparkTraitsDataComponentTypes;
import dev.caecorthus.sparktraits.impl.traits.civilian.laststand.LastStandFinalMomentService;
import dev.caecorthus.sparktraits.impl.command.admin.SparkTraitsCommands;
import dev.caecorthus.sparktraits.impl.traits.civilian.laststand.LastStandService;
import dev.caecorthus.sparktraits.impl.registry.SparkTraitsBuiltInTraits;
import dev.caecorthus.sparktraits.impl.resource.SparkTraitsParticles;
import dev.caecorthus.sparktraits.impl.resource.SparkTraitsSounds;
import dev.caecorthus.sparktraits.impl.lifecycle.TraitGameHooks;
import dev.caecorthus.sparktraits.net.version.SparkTraitsPackets;
import dev.caecorthus.sparktraits.net.version.SparkTraitsVersionHandshake;
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
        SparkTraitsVersionHandshake.registerServer();
        SparkTraitsPackets.register();
        SparkTraitsDataComponentTypes.init();
        SparkTraitsParticles.register();
        SparkTraitsSounds.initialize();
        SparkTraitsBuiltInTraits.register();
        LastStandService.register();
        LastStandFinalMomentService.register();
        TraitGameHooks.register();
        SparkTraitsCommands.register();
    }
}
