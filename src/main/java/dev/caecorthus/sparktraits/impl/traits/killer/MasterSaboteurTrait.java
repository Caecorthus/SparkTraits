package dev.caecorthus.sparktraits.impl.traits.killer;

import dev.caecorthus.sparktraits.api.Trait;
import dev.caecorthus.sparktraits.api.TraitAudience;
import dev.caecorthus.sparktraits.api.TraitDefinition;
import dev.caecorthus.sparktraits.api.TraitSelectionContext;
import dev.caecorthus.sparktraits.compat.SparkStrengthSabotageBridge;
import net.minecraft.util.Identifier;

import java.util.Set;

/**
 * Master Saboteur is a plain killer definition whose description follows the sabotage it actually changes:
 * SparkStrength's tablet sabotage when that provider is present, otherwise the old shop blackout (50% longer).
 * 破坏大师就是普通的杀手词条定义，只是说明跟随它实际改变的破坏：存在 SparkStrength 提供方时是平板破坏，
 * 否则是旧的商店关灯（延长 50%）。
 */
final class MasterSaboteurTrait implements Trait {
    private final TraitDefinition definition;

    MasterSaboteurTrait(TraitDefinition definition) {
        this.definition = definition;
    }

    @Override
    public Identifier id() {
        return definition.id();
    }

    @Override
    public int color() {
        return definition.color();
    }

    @Override
    public int weight() {
        return definition.weight();
    }

    @Override
    public boolean uniquePerGame() {
        return definition.uniquePerGame();
    }

    @Override
    public boolean hiddenFromOwnerAtStart() {
        return definition.hiddenFromOwnerAtStart();
    }

    @Override
    public TraitAudience audience() {
        return definition.audience();
    }

    @Override
    public Set<Identifier> incompatibleTraits() {
        return definition.incompatibleTraits();
    }

    @Override
    public boolean canApply(TraitSelectionContext context) {
        return definition.canApply(context);
    }

    @Override
    public String descriptionTranslationKey() {
        String key = definition.descriptionTranslationKey();
        return SparkStrengthSabotageBridge.isAvailable() ? key : key + "_legacy";
    }
}
