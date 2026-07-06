package dev.caecorthus.sparktraits.impl.traits.civilian;

import dev.caecorthus.sparktraits.SparkTraits;
import dev.caecorthus.sparktraits.api.TraitAudience;
import dev.caecorthus.sparktraits.api.TraitDefinition;
import dev.caecorthus.sparktraits.api.TraitRegistry;
import net.minecraft.util.Identifier;
import dev.caecorthus.sparktraits.impl.traits.civilian.depression.DepressionTrait;
import dev.caecorthus.sparktraits.impl.traits.global.GlobalTraitService;
import dev.caecorthus.sparktraits.impl.traits.civilian.impostor.ImpostorTrait;

/**
 * Registers traits reserved for original civilian-side roles.
 * 注册仅限原始平民阵营使用的天赋；运行时仍会排除内鬼天赋。
 */
public final class CivilianTraits {
    public static final Identifier EXTROVERTED = SparkTraits.id("extroverted");
    public static final Identifier INTROVERTED = SparkTraits.id("introverted");
    public static final Identifier MONEY_TREE = SparkTraits.id("money_tree");
    public static final Identifier FOCUS = SparkTraits.id("focus");
    public static final Identifier DEPRESSION = SparkTraits.id("depression");

    private CivilianTraits() {
    }

    public static void register() {
        TraitRegistry.register(base(EXTROVERTED, CivilianTraitService.EXTROVERTED_COLOR)
                .predicate(context -> CivilianTraitService.canSelectNonUndercoverCivilianTrait(
                        context.role(),
                        context.selectedTraitIds()
                ))
                .incompatibleWith(INTROVERTED)
                .build());
        TraitRegistry.register(base(INTROVERTED, CivilianTraitService.INTROVERTED_COLOR)
                .predicate(context -> CivilianTraitService.canSelectNonUndercoverCivilianTrait(
                        context.role(),
                        context.selectedTraitIds()
                ))
                .incompatibleWith(EXTROVERTED)
                .build());
        TraitRegistry.register(base(MONEY_TREE, CivilianTraitService.MONEY_TREE_COLOR)
                .predicate(context -> CivilianTraitService.canSelectMoneyTree(
                        context.role(),
                        context.selectedTraitIds(),
                        context.player() != null
                                && context.gameComponent() != null
                                && GlobalTraitService.canSeeMoneyForTrait(
                                        context.player(),
                                        context.gameComponent(),
                                        context.role()
                                )
                ))
                .build());
        TraitRegistry.register(base(FOCUS, CivilianTraitService.FOCUS_COLOR)
                .predicate(context -> CivilianTraitService.canSelectFocus(
                        context.role(),
                        context.selectedTraitIds()
                ))
                .build());
        TraitRegistry.register(new DepressionTrait());
    }

    private static TraitDefinition.Builder base(Identifier id, int color) {
        return TraitDefinition.builder(id, color)
                .audience(TraitAudience.INNOCENT_ONLY)
                .incompatibleWith(ImpostorTrait.ID)
                .predicate(context -> CivilianTraitService.canSelectCivilianTrait(
                        context.role(),
                        context.selectedTraitIds()
                ));
    }
}
