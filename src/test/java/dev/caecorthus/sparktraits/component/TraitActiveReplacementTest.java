package dev.caecorthus.sparktraits.component;

import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TraitActiveReplacementTest {
    @Test
    void plansOnlyTheDiffWhilePreservingRetainedRevealState() {
        Identifier alpha = Identifier.of("test", "alpha");
        Identifier beta = Identifier.of("test", "beta");
        Identifier gamma = Identifier.of("test", "gamma");
        Identifier unrelated = Identifier.of("test", "unrelated");
        List<Identifier> target = List.of(alpha, beta, gamma);

        TraitActiveReplacement.Plan plan = TraitActiveReplacement.plan(
                List.of(alpha, unrelated, beta),
                List.of(unrelated, beta),
                target
        );

        assertEquals(target, plan.target());
        assertEquals(List.of(alpha, beta), plan.retained());
        assertEquals(List.of(unrelated), plan.removed());
        assertEquals(List.of(gamma), plan.missing());
        assertEquals(List.of(beta), plan.retainedRevealed());
    }
}
