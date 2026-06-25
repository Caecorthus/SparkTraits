package dev.caecorthus.sparktraits.impl;

import dev.caecorthus.sparktraits.SparkTraits;
import dev.caecorthus.sparktraits.api.TraitAudience;
import dev.caecorthus.sparktraits.api.TraitDefinition;
import dev.caecorthus.sparktraits.api.TraitRegistry;
import net.minecraft.util.Identifier;

/**
 * Registers traits reserved for original good-side roles.
 * 注册仅限原始好人阵营使用的天赋；运行时仍会排除内鬼天赋。
 */
public final class GoodTraits {
    public static final Identifier EXTROVERTED = SparkTraits.id("extroverted");
    public static final Identifier INTROVERTED = SparkTraits.id("introverted");
    public static final Identifier MONEY_TREE = SparkTraits.id("money_tree");
    public static final Identifier FOCUS = SparkTraits.id("focus");

    private GoodTraits() {
    }

    public static void register() {
        TraitRegistry.register(base(EXTROVERTED, GoodTraitService.EXTROVERTED_COLOR)
                .incompatibleWith(INTROVERTED)
                .predicate(context -> GoodTraitService.canSelectSocialTrait(
                        context.role(),
                        context.selectedTraitIds()
                ))
                .build());
        TraitRegistry.register(base(INTROVERTED, GoodTraitService.INTROVERTED_COLOR)
                .incompatibleWith(EXTROVERTED)
                .predicate(context -> GoodTraitService.canSelectSocialTrait(
                        context.role(),
                        context.selectedTraitIds()
                ))
                .build());
        TraitRegistry.register(base(MONEY_TREE, GoodTraitService.MONEY_TREE_COLOR)
                .predicate(context -> GoodTraitService.canSelectMoneyTree(
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
        TraitRegistry.register(base(FOCUS, GoodTraitService.FOCUS_COLOR)
                .predicate(context -> GoodTraitService.canSelectFocus(
                        context.role(),
                        context.selectedTraitIds()
                ))
                .build());
    }

    private static TraitDefinition.Builder base(Identifier id, int color) {
        return TraitDefinition.builder(id, color)
                .audience(TraitAudience.INNOCENT_ONLY)
                .incompatibleWith(ImpostorTrait.ID)
                .predicate(context -> GoodTraitService.canSelectGoodTrait(
                        context.role(),
                        context.selectedTraitIds()
                ));
    }
}
