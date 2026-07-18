package dev.caecorthus.sparktraits.impl.traits.civilian.police;

import dev.caecorthus.sparktraits.impl.traits.civilian.impostor.ImpostorTrait;
import dev.caecorthus.sparktraits.impl.traits.killer.conscience.ConscienceTrait;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GoingDarkRulesTest {
    @Test
    void targetIsHiddenOnlyForLivingVeteranWithTraitDuringBlackout() {
        assertTrue(GoingDarkRules.isTargetHidden(
                true, true, WatheRoles.VETERAN, Set.of(PoliceTraits.GOING_DARK)));

        assertFalse(GoingDarkRules.isTargetHidden(
                false, true, WatheRoles.VETERAN, Set.of(PoliceTraits.GOING_DARK)));
        assertFalse(GoingDarkRules.isTargetHidden(
                true, false, WatheRoles.VETERAN, Set.of(PoliceTraits.GOING_DARK)));
        assertFalse(GoingDarkRules.isTargetHidden(
                true, true, WatheRoles.VIGILANTE, Set.of(PoliceTraits.GOING_DARK)));
        assertFalse(GoingDarkRules.isTargetHidden(
                true, true, WatheRoles.VETERAN, Set.of()));
    }

    @Test
    void requestedViewerTypesCannotRevealHiddenTarget() {
        assertSuppressed(WatheRoles.KILLER, Set.of());
        assertSuppressed(WatheRoles.CIVILIAN, Set.of(ImpostorTrait.ID));
        assertSuppressed(role("sparkwitch", "grand_witch"), Set.of());
        assertSuppressed(role("sparkwitch", "murderous_witch"), Set.of());
        assertSuppressed(role("sparkwitch", "accomplice"), Set.of());
        assertSuppressed(role("noellesroles", "corrupt_cop"), Set.of());
    }

    @Test
    void conscienceAndUnlistedViewersKeepTheirExistingHighlights() {
        assertFalse(GoingDarkRules.shouldSuppressInstinct(
                true, true, false, false, WatheRoles.KILLER, Set.of(ConscienceTrait.ID)));
        assertFalse(GoingDarkRules.shouldSuppressInstinct(
                true, true, false, false, WatheRoles.CIVILIAN, Set.of()));
        assertFalse(GoingDarkRules.shouldSuppressInstinct(
                true, true, false, false, role("noellesroles", "detective"), Set.of()));
    }

    @Test
    void inactiveTargetDeadViewerSpectatorAndFinalMomentAreNotSuppressed() {
        assertFalse(GoingDarkRules.shouldSuppressInstinct(
                false, true, false, false, WatheRoles.KILLER, Set.of()));
        assertFalse(GoingDarkRules.shouldSuppressInstinct(
                true, false, false, false, WatheRoles.KILLER, Set.of()));
        assertFalse(GoingDarkRules.shouldSuppressInstinct(
                true, true, true, false, WatheRoles.KILLER, Set.of()));
        assertFalse(GoingDarkRules.shouldSuppressInstinct(
                true, true, false, true, WatheRoles.KILLER, Set.of()));
    }

    private static void assertSuppressed(Role viewerRole, Set<Identifier> viewerTraits) {
        assertTrue(GoingDarkRules.shouldSuppressInstinct(
                true, true, false, false, viewerRole, viewerTraits));
    }

    private static Role role(String namespace, String path) {
        return new Role(Identifier.of(namespace, path), 0, false, false, Role.MoodType.FAKE, -1, true);
    }
}
