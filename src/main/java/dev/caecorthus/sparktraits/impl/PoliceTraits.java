package dev.caecorthus.sparktraits.impl;

import dev.caecorthus.sparktraits.SparkTraits;
import dev.caecorthus.sparktraits.api.TraitAudience;
import dev.caecorthus.sparktraits.api.TraitDefinition;
import dev.caecorthus.sparktraits.api.TraitRegistry;
import net.minecraft.util.Identifier;

/**
 * Registers traits reserved for Wathe's original Vigilante and Veteran roles.
 * 注册仅限 Wathe 原始义警与老兵身份使用的警类天赋。
 */
public final class PoliceTraits {
    public static final Identifier MARKSMAN = SparkTraits.id("marksman");
    public static final Identifier FAST_RELOAD = SparkTraits.id("fast_reload");
    public static final Identifier HEAVY_ARTILLERY = SparkTraits.id("heavy_artillery");
    public static final Identifier NIKO = SparkTraits.id("niko");
    public static final Identifier WELL_TRAINED = SparkTraits.id("well_trained");
    public static final Identifier GOING_DARK = SparkTraits.id("going_dark");

    private PoliceTraits() {
    }

    public static void register() {
        TraitRegistry.register(vigilante(MARKSMAN, 0xD6C27A).build());
        TraitRegistry.register(vigilante(FAST_RELOAD, 0xF4A261).build());
        TraitRegistry.register(vigilante(HEAVY_ARTILLERY, 0xD94F30).build());
        TraitRegistry.register(vigilante(NIKO, 0x39FF14).build());
        TraitRegistry.register(veteran(WELL_TRAINED, 0x4A90E2).build());
        TraitRegistry.register(veteran(GOING_DARK, 0x2E4057).build());
    }

    private static TraitDefinition.Builder vigilante(Identifier id, int color) {
        return TraitDefinition.builder(id, color)
                .audience(TraitAudience.INNOCENT_ONLY)
                .predicate(context -> VigilanteVeteranTraitService.canSelectVigilanteTrait(context.role()));
    }

    private static TraitDefinition.Builder veteran(Identifier id, int color) {
        return TraitDefinition.builder(id, color)
                .audience(TraitAudience.INNOCENT_ONLY)
                .predicate(context -> VigilanteVeteranTraitService.canSelectVeteranTrait(context.role()));
    }
}
