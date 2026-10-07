package dev.caecorthus.sparktraits.impl.traits.civilian.police;

import dev.caecorthus.sparktraits.SparkTraits;
import dev.caecorthus.sparktraits.api.TraitAudience;
import dev.caecorthus.sparktraits.api.TraitDefinition;
import dev.caecorthus.sparktraits.api.TraitRegistry;
import net.minecraft.util.Identifier;

/**
 * Registers police traits: gun traits for police-category roles (see {@link PoliceRoleCategory})
 * and Veteran-only traits for Wathe's Veteran.
 * 注册警类天赋：枪械天赋归警职类别身份（见 {@link PoliceRoleCategory}），老兵天赋仅限 Wathe 老兵。
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
        // Replaces the shared gun police predicate with Niko's narrower gate (no USEC).
        // 用 Niko 更窄的判定（排除 USEC）替换共享的枪械警职谓词。
        TraitRegistry.register(vigilante(NIKO, 0x39FF14)
                .predicate(context -> VigilanteVeteranTraitService.canSelectNikoTrait(context.role()))
                .build());
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
