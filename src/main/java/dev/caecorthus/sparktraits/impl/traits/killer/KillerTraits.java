package dev.caecorthus.sparktraits.impl.traits.killer;

import dev.caecorthus.sparktraits.SparkTraits;
import dev.caecorthus.sparktraits.api.TraitAudience;
import dev.caecorthus.sparktraits.api.TraitDefinition;
import dev.caecorthus.sparktraits.api.TraitRegistry;
import dev.caecorthus.sparktraits.compat.SparkStrengthTeamEconomyBridge;
import dev.caecorthus.sparktraits.impl.traits.killer.combat.CloseQuartersService;
import net.minecraft.util.Identifier;
import dev.caecorthus.sparktraits.impl.traits.killer.conscience.ConscienceTrait;
import dev.caecorthus.sparktraits.impl.traits.civilian.impostor.ImpostorTrait;

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
    public static final Identifier TEAM_FIRST = SparkTraits.id("team_first");
    public static final Identifier EXHILARATED = SparkTraits.id("exhilarated");
    public static final Identifier CLOSE_QUARTERS = SparkTraits.id("close_quarters");
    public static final Identifier LAST_ESCAPE = SparkTraits.id("last_escape");
    public static final Identifier HERCULEAN_STRENGTH = SparkTraits.id("herculean_strength");
    public static final Identifier MASTER_SABOTEUR = SparkTraits.id("master_saboteur");
    public static final Identifier SEASONED = SparkTraits.id("seasoned");
    public static final Identifier MANIC = SparkTraits.id("manic");

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
                        KillerTraitService.hasThrustWeaponAccess(context.player())
                ))
                .build());
        TraitRegistry.register(base(SECOND_STRIKE, 0xC94B32).build());
        TraitRegistry.register(base(OPPRESSIVE, 0x3B3048)
                .uniquePerGame()
                .build());
        TraitRegistry.register(base(CORNERED, 0x6F263D).build());
        TraitRegistry.register(base(TEAM_FIRST, 0xD6A640)
                .predicate(context -> KillerTraitService.canSelectKillerTrait(context.role(), context.selectedTraitIds())
                        && SparkStrengthTeamEconomyBridge.isAvailable())
                .build());
        TraitRegistry.register(base(EXHILARATED, 0xE86B35).build());
        TraitRegistry.register(base(CLOSE_QUARTERS, 0xA93236)
                .predicate(context -> KillerTraitService.canSelectKillerTrait(context.role(), context.selectedTraitIds())
                        && CloseQuartersService.canSelect(context.player()))
                .build());
        TraitRegistry.register(base(LAST_ESCAPE, 0xAAAAB8).hiddenFromOwnerAtStart().build());
        TraitRegistry.register(base(HERCULEAN_STRENGTH, 0xC9772B)
                .predicate(context -> KillerTraitService.canSelectKillerTrait(context.role(), context.selectedTraitIds())
                        && HerculeanStrengthService.hasThrowableAccess(context.player()))
                .build());
        TraitRegistry.register(base(MASTER_SABOTEUR, 0x4A5A8C)
                .predicate(context -> KillerTraitService.canSelectKillerTrait(context.role(), context.selectedTraitIds())
                        && KillerTraitService.hasBlackoutShopEntry(context.player()))
                .build());
        // Close Quarters already owns the knife windup; stacking both would leave Seasoned without an effect.
        // 狭路相逢已接管举刀前摇，两者叠加会让老练失去效果。
        TraitRegistry.register(base(SEASONED, 0x8C9AA6)
                .incompatibleWith(CLOSE_QUARTERS)
                .predicate(context -> KillerTraitService.canSelectKillerTrait(context.role(), context.selectedTraitIds())
                        && CloseQuartersService.canSelect(context.player()))
                .build());
        TraitRegistry.register(base(MANIC, 0xD7263D)
                .predicate(context -> KillerTraitService.canSelectKillerTrait(context.role(), context.selectedTraitIds())
                        && KillerTraitService.hasPsychoModeShopEntry(context.player()))
                .build());
    }

    private static TraitDefinition.Builder base(Identifier id, int color) {
        return TraitDefinition.builder(id, color)
                .audience(TraitAudience.KILLER_ONLY)
                .incompatibleWith(ConscienceTrait.ID)
                .incompatibleWith(ImpostorTrait.ID)
                .predicate(context -> KillerTraitService.canSelectKillerTrait(context.role(), context.selectedTraitIds()));
    }
}
