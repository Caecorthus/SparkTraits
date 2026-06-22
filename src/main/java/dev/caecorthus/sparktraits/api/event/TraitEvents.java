package dev.caecorthus.sparktraits.api.event;

import dev.caecorthus.sparktraits.api.Trait;
import dev.caecorthus.sparktraits.api.TraitAssignmentReason;
import dev.caecorthus.sparktraits.api.TraitRemovalReason;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.network.ServerPlayerEntity;

public final class TraitEvents {
    public static final Event<Assigned> ASSIGNED = EventFactory.createArrayBacked(Assigned.class, listeners -> (player, trait, reason) -> {
        for (Assigned listener : listeners) {
            listener.onTraitAssigned(player, trait, reason);
        }
    });

    public static final Event<Removed> REMOVED = EventFactory.createArrayBacked(Removed.class, listeners -> (player, trait, reason) -> {
        for (Removed listener : listeners) {
            listener.onTraitRemoved(player, trait, reason);
        }
    });

    public static final Event<Revealed> REVEALED = EventFactory.createArrayBacked(Revealed.class, listeners -> (player, trait) -> {
        for (Revealed listener : listeners) {
            listener.onTraitRevealed(player, trait);
        }
    });

    private TraitEvents() {
    }

    @FunctionalInterface
    public interface Assigned {
        void onTraitAssigned(ServerPlayerEntity player, Trait trait, TraitAssignmentReason reason);
    }

    @FunctionalInterface
    public interface Removed {
        void onTraitRemoved(ServerPlayerEntity player, Trait trait, TraitRemovalReason reason);
    }

    @FunctionalInterface
    public interface Revealed {
        void onTraitRevealed(ServerPlayerEntity player, Trait trait);
    }
}
