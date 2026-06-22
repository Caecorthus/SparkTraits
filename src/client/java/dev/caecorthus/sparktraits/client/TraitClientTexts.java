package dev.caecorthus.sparktraits.client;

import dev.caecorthus.sparktraits.api.Trait;
import dev.caecorthus.sparktraits.api.TraitRegistry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;

public final class TraitClientTexts {
    private static final int UNKNOWN_TRAIT_COLOR = 0xAAAAAA;

    private TraitClientTexts() {
    }

    public static int color(Identifier traitId) {
        Trait trait = TraitRegistry.get(traitId);
        return trait == null ? UNKNOWN_TRAIT_COLOR : trait.color();
    }

    public static MutableText name(Identifier traitId) {
        Trait trait = TraitRegistry.get(traitId);
        return trait == null ? Text.literal(traitId.toString()) : trait.name().copy();
    }

    public static MutableText tag(Identifier traitId) {
        return Text.literal("[")
                .append(name(traitId))
                .append(Text.literal("]"));
    }

    public static List<Text> tooltip(Identifier traitId) {
        Trait trait = TraitRegistry.get(traitId);
        if (trait == null) {
            return List.of(Text.literal(traitId.toString()));
        }
        return List.of(trait.name(), trait.description());
    }
}
