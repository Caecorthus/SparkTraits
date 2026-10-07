package dev.caecorthus.sparktraits.client.text;

import dev.caecorthus.sparkfactionapi.api.FactionDefinition;
import dev.caecorthus.sparkfactionapi.api.FactionIds;
import dev.caecorthus.sparkfactionapi.api.SparkFactionApi;
import dev.caecorthus.sparktraits.api.Trait;
import dev.caecorthus.sparktraits.api.TraitAudience;
import dev.caecorthus.sparktraits.api.TraitRegistry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class TraitClientTexts {
    private static final int UNKNOWN_TRAIT_COLOR = 0xAAAAAA;
    // Universal traits belong to no faction; neutral gray keeps their gem apart from every faction colour.
    // 通用天赋不属于任何阵营；中性灰让其宝石与各阵营色区分开。
    static final int UNIVERSAL_TRAIT_COLOR = 0x9A9A9A;

    private TraitClientTexts() {
    }

    public static int color(Identifier traitId) {
        Trait trait = TraitRegistry.get(traitId);
        return trait == null ? UNKNOWN_TRAIT_COLOR : trait.color();
    }

    /**
     * Theme colour, registered in SparkFactionAPI, of the faction the trait is reserved for (its audience, so
     * Impostor reads civilian and Conscience killer). Universal and unknown traits are gray.
     * 天赋专属阵营（按其 audience，故内鬼为平民、善良为杀手）在 SparkFactionAPI 中登记的主题色；通用与未知天赋为灰色。
     */
    public static int factionColor(Identifier traitId) {
        Trait trait = TraitRegistry.get(traitId);
        Identifier faction = trait == null ? null : audienceFaction(trait.audience());
        if (faction == null) {
            return UNIVERSAL_TRAIT_COLOR;
        }
        return SparkFactionApi.getFaction(faction)
                .map(FactionDefinition::color)
                .orElse(UNIVERSAL_TRAIT_COLOR);
    }

    static @Nullable Identifier audienceFaction(TraitAudience audience) {
        return switch (audience) {
            case KILLER_ONLY -> FactionIds.KILLER;
            case INNOCENT_ONLY -> FactionIds.CIVILIAN;
            case NEUTRAL_ONLY -> FactionIds.NEUTRAL;
            case UNIVERSAL -> null;
        };
    }

    public static MutableText name(Identifier traitId) {
        Trait trait = TraitRegistry.get(traitId);
        return trait == null ? Text.literal(traitId.toString()) : trait.name().copy();
    }

    public static List<Text> tooltip(Identifier traitId) {
        Trait trait = TraitRegistry.get(traitId);
        if (trait == null) {
            return List.of(Text.literal(traitId.toString()));
        }
        return List.of(trait.name(), trait.description());
    }
}
