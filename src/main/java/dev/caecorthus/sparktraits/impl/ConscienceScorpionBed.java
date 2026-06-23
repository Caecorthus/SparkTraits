package dev.caecorthus.sparktraits.impl;

import java.util.UUID;

public interface ConscienceScorpionBed {
    boolean sparktraits$hasConscienceScorpion();

    UUID sparktraits$getConscienceScorpionPoisoner();

    default UUID sparktraits$getConsciencePoisoner() {
        return sparktraits$getConscienceScorpionPoisoner();
    }

    void sparktraits$setConscienceScorpion(boolean hasScorpion, UUID poisoner);
}
