package dev.caecorthus.sparktraits.impl.traits.civilian.fakedeath;

import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.impl.traits.civilian.CivilianTraits;
import dev.caecorthus.sparktraits.impl.traits.civilian.laststand.LastStandTrait;
import dev.doctor4t.wathe.index.WatheDataComponentTypes;
import dev.doctor4t.wathe.index.WatheItems;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.Set;

public final class FakeDeathDerringerService {
    private static final Set<Identifier> FAKE_DEATH_TRAITS = Set.of(
            CivilianTraits.DEPRESSION,
            LastStandTrait.ID
    );

    private FakeDeathDerringerService() {
    }

    public static void replenishAfterHit(ServerPlayerEntity shooter, PlayerEntity target) {
        replenish(
                shooter.getMainHandStack(),
                TraitPlayerComponent.KEY.get(target).getActiveTraitIds()
        );
    }

    static void replenish(ItemStack weapon, Collection<Identifier> targetTraits) {
        if (shouldReplenish(weapon.isOf(WatheItems.DERRINGER), targetTraits)) {
            weapon.set(WatheDataComponentTypes.USED, false);
        }
    }

    static boolean shouldReplenish(boolean derringer, Collection<Identifier> targetTraits) {
        return derringer
                && targetTraits != null
                && targetTraits.stream().anyMatch(FAKE_DEATH_TRAITS::contains);
    }
}
