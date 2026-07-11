package dev.caecorthus.sparktraits.client.audio;

import dev.caecorthus.sparktraits.SparkTraits;
import net.minecraft.entity.Entity;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;

/** Client-only, opt-in diagnostics for Cautious sound adapters. */
public final class CautiousSoundDebug {
    private static final String ENABLED_PROPERTY = "sparktraits.debugCautiousSound";

    private CautiousSoundDebug() {
    }

    public static boolean isEnabled() {
        return Boolean.getBoolean(ENABLED_PROPERTY);
    }

    public static void trace(
            String adapterPath,
            Entity source,
            boolean confirmedServer,
            boolean cautiousRule,
            String decision,
            Identifier soundId,
            SoundCategory category,
            Float volume
    ) {
        if (!isEnabled()) {
            return;
        }

        SparkTraits.LOGGER.info(
                "Cautious sound adapter={} sourceUuid={} sourceType={} confirmedServer={} cautiousRule={} decision={} soundId={} category={} volume={}",
                adapterPath,
                source == null ? "unavailable" : source.getUuid(),
                source == null ? "unavailable" : Registries.ENTITY_TYPE.getId(source.getType()),
                confirmedServer,
                cautiousRule,
                decision,
                soundId == null ? "unavailable" : soundId,
                category == null ? "unavailable" : category,
                volume == null ? "unavailable" : volume
        );
    }
}
