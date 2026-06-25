package dev.caecorthus.sparktraits.impl;

import dev.caecorthus.sparktraits.SparkTraits;
import dev.caecorthus.sparktraits.api.TraitAudience;
import dev.caecorthus.sparktraits.api.TraitDefinition;
import dev.caecorthus.sparktraits.api.TraitRegistry;
import net.minecraft.util.Identifier;

/**
 * Registers killer-only SparkTraits trait definitions.
 * 注册 SparkTraits 的杀手专用天赋定义。
 */
public final class KillerTraits {
    public static final Identifier BLOODTHIRSTY = SparkTraits.id("bloodthirsty");
    public static final Identifier THE_SHOWMAN = SparkTraits.id("the_showman");
    public static final Identifier PLUNDERER = SparkTraits.id("plunderer");
    public static final Identifier CHARISMA = SparkTraits.id("charisma");
    public static final Identifier PARANOID = SparkTraits.id("paranoid");
    public static final Identifier THRUST = SparkTraits.id("thrust");
    public static final Identifier SECOND_STRIKE = SparkTraits.id("second_strike");
    public static final Identifier OPPRESSIVE = SparkTraits.id("oppressive");
    public static final Identifier CORNERED = SparkTraits.id("cornered");

    private KillerTraits() {
    }

    public static void register() {
        TraitRegistry.register(base(BLOODTHIRSTY, 0x9D1B2B).build());
        TraitRegistry.register(base(THE_SHOWMAN, 0xD48B16).build());
        TraitRegistry.register(base(PLUNDERER, 0x7F5A2A).build());
        TraitRegistry.register(base(CHARISMA, 0xE6C64D).build());
        TraitRegistry.register(base(PARANOID, 0x8B3FA8)
                .predicate(context -> KillerTraitService.canSelectParanoid(
                        context.role(),
                        context.selectedTraitIds(),
                        KillerTraitService.hasPsychoModeShopEntry(context.player())
                ))
                .build());
        TraitRegistry.register(base(THRUST, 0x4E8C9E)
                .predicate(context -> KillerTraitService.canSelectThrust(
                        context.role(),
                        context.selectedTraitIds(),
                        KillerTraitService.hasThrustShopEntry(context.player())
                ))
                .build());
        TraitRegistry.register(base(SECOND_STRIKE, 0xC94B32).build());
        TraitRegistry.register(base(OPPRESSIVE, 0x3B3048)
                .uniquePerGame()
                .build());
        TraitRegistry.register(base(CORNERED, 0x6F263D).build());
    }

    private static TraitDefinition.Builder base(Identifier id, int color) {
        return TraitDefinition.builder(id, color)
                .audience(TraitAudience.KILLER_ONLY)
                .incompatibleWith(ConscienceTrait.ID)
                .incompatibleWith(ImpostorTrait.ID)
                .predicate(context -> KillerTraitService.canSelectKillerTrait(context.role(), context.selectedTraitIds()));
    }
}
