package dev.caecorthus.sparktraits.api;

import dev.doctor4t.wathe.api.Faction;
import dev.doctor4t.wathe.api.Role;

public enum TraitAudience {
    UNIVERSAL {
        @Override
        public boolean canApply(TraitSelectionContext context) {
            return true;
        }
    },
    KILLER_ONLY {
        @Override
        public boolean canApply(TraitSelectionContext context) {
            return faction(context.role()) == Faction.KILLER;
        }
    },
    INNOCENT_ONLY {
        @Override
        public boolean canApply(TraitSelectionContext context) {
            return faction(context.role()) == Faction.CIVILIAN;
        }
    },
    NEUTRAL_ONLY {
        @Override
        public boolean canApply(TraitSelectionContext context) {
            return faction(context.role()) == Faction.NEUTRAL;
        }
    };

    public abstract boolean canApply(TraitSelectionContext context);

    private static Faction faction(Role role) {
        return role == null ? Faction.NONE : role.getFaction();
    }
}

