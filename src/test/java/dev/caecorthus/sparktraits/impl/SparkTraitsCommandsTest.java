package dev.caecorthus.sparktraits.impl;

import dev.caecorthus.sparktraits.SparkTraits;
import dev.caecorthus.sparktraits.api.Trait;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SparkTraitsCommandsTest {
    private static final Trait TEST_TRAIT = new Trait() {
        @Override
        public Identifier id() {
            return SparkTraits.id("last_stand");
        }

        @Override
        public int color() {
            return 0xFFFFFF;
        }

        @Override
        public Text name() {
            return Text.literal("Last Stand");
        }
    };

    @Test
    void addTraitFeedbackNamesSingleChangedPlayerAndShortTraitId() {
        Text feedback = SparkTraitsCommands.formatPlayerTraitActionFeedback(
                TEST_TRAIT,
                SparkTraitsCommands.PlayerTraitAction.ADD,
                1,
                "Kricy"
        );

        assertEquals("Added a pending trait last_stand to Kricy.", feedback.getString());
    }

    @Test
    void addTraitFeedbackKeepsCountForMultiplePlayersAndShortTraitId() {
        Text feedback = SparkTraitsCommands.formatPlayerTraitActionFeedback(
                TEST_TRAIT,
                SparkTraitsCommands.PlayerTraitAction.ADD,
                3,
                null
        );

        assertEquals("Added a pending trait last_stand to 3 player(s).", feedback.getString());
    }

    @Test
    void addTraitFeedbackColorsTraitId() {
        Text feedback = SparkTraitsCommands.formatPlayerTraitActionFeedback(
                TEST_TRAIT,
                SparkTraitsCommands.PlayerTraitAction.ADD,
                1,
                "Kricy"
        );

        assertEquals(0xFFFFFF, feedback.getSiblings().getFirst().getStyle().getColor().getRgb());
    }

    @Test
    void traitCommandNameUsesShortIdForSparkTraitsNamespace() {
        assertEquals("last_stand", SparkTraitsCommands.formatTraitIdForCommand(SparkTraits.id("last_stand")));
    }

    @Test
    void traitSlotRollChanceFeedbackUsesWholePercent() {
        assertEquals(
                "Trait slot roll chance is 75%.",
                SparkTraitsCommands.formatTraitSlotRollChanceFeedback(0.75f).getString()
        );
        assertEquals(
                "Trait slot roll chance set to 100%.",
                SparkTraitsCommands.formatTraitSlotRollChanceSetFeedback(1.0f).getString()
        );
    }

    @Test
    void traitSlotRollChancePercentConvertsToUnitChance() {
        assertEquals(0.0f, SparkTraitsCommands.traitSlotRollChanceFromPercent(0), 0.0001f);
        assertEquals(0.75f, SparkTraitsCommands.traitSlotRollChanceFromPercent(75), 0.0001f);
        assertEquals(1.0f, SparkTraitsCommands.traitSlotRollChanceFromPercent(100), 0.0001f);
    }
}
