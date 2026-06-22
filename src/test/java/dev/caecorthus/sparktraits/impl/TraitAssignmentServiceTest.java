package dev.caecorthus.sparktraits.impl;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TraitAssignmentServiceTest {
    @Test
    void forcedConscienceCandidateUsesRandomIndex() {
        Random random = new Random() {
            @Override
            public int nextInt(int bound) {
                return 1;
            }
        };

        assertEquals(1, TraitAssignmentService.pickForcedConscienceCandidateIndex(3, random));
    }
}
