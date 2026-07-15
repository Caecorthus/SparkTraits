package dev.caecorthus.sparktraits.impl.effective;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.caecorthus.sparktraits.impl.command.admin.TraitLockValidationService;
import dev.caecorthus.sparktraits.impl.traits.civilian.impostor.ImpostorTrait;
import dev.doctor4t.wathe.api.Role;
import java.util.Set;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

class ImpostorRoleEligibilityTest {
    @Test
    void saintCannotBecomeImpostor() {
        assertFalse(EffectiveTraitService.canSelectImpostor(civilian("sparkwitch", "saint"), 2, Set.of()));
    }

    @Test
    void commandLocksRejectSaintAndKeepOrdinaryCiviliansCompatible() {
        ImpostorTrait impostor = new ImpostorTrait();

        assertNotNull(TraitLockValidationService.findAudienceConflict(impostor, civilian("sparkwitch", "saint")));
        assertNull(TraitLockValidationService.findAudienceConflict(impostor, civilian("wathe", "civilian")));
    }

    @Test
    void ordinaryCivilianRemainsEligibleForImpostor() {
        assertTrue(EffectiveTraitService.canSelectImpostor(civilian("wathe", "civilian"), 2, Set.of()));
    }

    private static Role civilian(String namespace, String path) {
        return new Role(
                Identifier.of(namespace, path),
                0,
                true,
                false,
                Role.MoodType.NONE,
                -1,
                false
        );
    }
}
