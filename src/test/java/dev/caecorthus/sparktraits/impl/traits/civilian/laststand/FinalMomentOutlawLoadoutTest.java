package dev.caecorthus.sparktraits.impl.traits.civilian.laststand;

import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.doctor4t.wathe.api.WatheRoles;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinalMomentOutlawLoadoutTest {
    private static final String SERVICE_SOURCE =
            "src/main/java/dev/caecorthus/sparktraits/impl/traits/civilian/laststand/LastStandFinalMomentService.java";

    @Test
    void targetTraitsAreTheExactOrderedSevenTraitOutlawLoadout() {
        assertEquals(List.of(
                Identifier.of("sparktraits", "last_stand"),
                Identifier.of("sparktraits", "niko"),
                Identifier.of("sparktraits", "heavy_artillery"),
                Identifier.of("sparktraits", "fast_hands"),
                Identifier.of("sparktraits", "fast_reload"),
                Identifier.of("sparktraits", "cautious"),
                Identifier.of("sparktraits", "thrust")
        ), FinalMomentOutlawLoadout.targetTraits());
    }

    @Test
    void normalTraitLimitStaysThreeWhileTheRuntimeOutlawLoadoutHasSeven() {
        assertEquals(3, TraitPlayerComponent.MAX_TRAITS);
        assertEquals(7, FinalMomentOutlawLoadout.targetTraits().size());
    }

    @Test
    void finalMomentLooseEndRequiresActiveMomentMarkedPlayerAndLooseEndRole() {
        assertTrue(LastStandFinalMomentService.isFinalMomentLooseEnd(true, true, WatheRoles.LOOSE_END));
        assertFalse(LastStandFinalMomentService.isFinalMomentLooseEnd(false, true, WatheRoles.LOOSE_END));
        assertFalse(LastStandFinalMomentService.isFinalMomentLooseEnd(true, false, WatheRoles.LOOSE_END));
        assertFalse(LastStandFinalMomentService.isFinalMomentLooseEnd(true, true, WatheRoles.CIVILIAN));
    }

    @Test
    void conversionReplacesTraitsAfterRoleReplayBeforeGrantingTheApprovedLoadout() throws IOException {
        String source = source(SERVICE_SOURCE);
        int roleAssigned = source.indexOf("RoleAssigned.EVENT.invoker().assignRole(player, WatheRoles.LOOSE_END);");
        int replayRecorded = source.indexOf("SparkTraitsReplayEvents.recordLooseEndConversion(player);");
        int traitReplacement = source.indexOf("replaceActiveTraitsForRuntime(");
        int firstInventoryGrant = source.indexOf("giveFinalItem(player, WatheItems.KNIFE);");

        assertTrue(roleAssigned >= 0);
        assertTrue(replayRecorded > roleAssigned);
        assertTrue(traitReplacement > replayRecorded);
        assertTrue(firstInventoryGrant > traitReplacement);
        assertTrue(source.contains("giveFinalItem(player, WatheItems.REVOLVER);"));
        assertEquals(1, occurrences(source, "IronManPlayerComponent.KEY.get(player).applyBuff();"));
        assertFalse(source.contains("WhiskeyShieldEffect"));
    }

    private static int occurrences(String source, String needle) {
        return source.split(java.util.regex.Pattern.quote(needle), -1).length - 1;
    }

    private static String source(String relativePath) throws IOException {
        Path path = Path.of(System.getProperty("user.dir"), relativePath);
        assertTrue(Files.isRegularFile(path), "Missing required file: " + relativePath);
        return Files.readString(path);
    }
}
