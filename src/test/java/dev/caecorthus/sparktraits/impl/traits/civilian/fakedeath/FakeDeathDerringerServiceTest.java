package dev.caecorthus.sparktraits.impl.traits.civilian.fakedeath;

import dev.caecorthus.sparktraits.impl.traits.civilian.CivilianTraits;
import dev.caecorthus.sparktraits.impl.traits.civilian.laststand.LastStandTrait;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FakeDeathDerringerServiceTest {
    private static final Identifier OTHER_TRAIT = Identifier.of("sparktraits", "other");

    @Test
    void derringerReplenishesWhenTargetHasActiveFakeDeathTrait() {
        assertTrue(FakeDeathDerringerService.shouldReplenish(
                true,
                Set.of(CivilianTraits.DEPRESSION)
        ));
        assertTrue(FakeDeathDerringerService.shouldReplenish(
                true,
                Set.of(LastStandTrait.ID)
        ));
        assertTrue(FakeDeathDerringerService.shouldReplenish(
                true,
                Set.of(CivilianTraits.DEPRESSION, LastStandTrait.ID)
        ));
    }

    @Test
    void otherWeaponsAndTargetsWithoutActiveFakeDeathTraitsDoNotReplenish() {
        assertFalse(FakeDeathDerringerService.shouldReplenish(
                false,
                Set.of(CivilianTraits.DEPRESSION)
        ));
        assertFalse(FakeDeathDerringerService.shouldReplenish(true, Set.of(OTHER_TRAIT)));
        assertFalse(FakeDeathDerringerService.shouldReplenish(true, List.of()));
        assertFalse(FakeDeathDerringerService.shouldReplenish(true, null));
    }

    @Test
    void replenishmentMutationClearsUsedOnTheFiringStack() throws IOException {
        String source = Files.readString(Path.of(
                System.getProperty("user.dir"),
                "src/main/java/dev/caecorthus/sparktraits/impl/traits/civilian/fakedeath/FakeDeathDerringerService.java"
        ));
        String mutation = section(
                source,
                "static void replenish(ItemStack weapon, Collection<Identifier> targetTraits) {",
                "    static boolean shouldReplenish("
        );

        assertTrue(mutation.contains("shouldReplenish(weapon.isOf(WatheItems.DERRINGER), targetTraits)"));
        assertTrue(mutation.contains("weapon.set(WatheDataComponentTypes.USED, false);"));
        assertFalse(mutation.contains("WatheDataComponentTypes.USED, true"));
    }

    @Test
    void targetHitResolverReplenishesBeforeApplyingGunDamage() throws IOException {
        String source = Files.readString(Path.of(
                System.getProperty("user.dir"),
                "src/main/java/dev/caecorthus/sparktraits/mixin/GunShootPayloadMixin.java"
        ));
        String hook = section(
                source,
                "private void sparktraits$applyHeavyArtilleryGunDamage(",
                "    /** Skips only the ordinary civilian-role revolver-hit mood penalty."
        );

        assertTrue(hook.contains("FakeDeathDerringerService.replenishAfterHit(shooter, victim);"));
        assertTrue(hook.indexOf("replenishAfterHit") < hook.indexOf("killPlayerWithPoliceGunTraits"));
        assertFalse(source.contains("sparktraits$replenishDerringerOnFakeDeathTraitHit"));
        assertFalse(source.contains("ShouldPunishGunShooter;shouldPunish"));
    }

    private static String section(String source, String startMarker, String endMarker) {
        int start = source.indexOf(startMarker);
        int end = source.indexOf(endMarker, start);
        assertTrue(start >= 0, "Missing source marker: " + startMarker);
        assertTrue(end > start, "Missing source marker: " + endMarker);
        return source.substring(start, end);
    }
}
