package dev.caecorthus.sparktraits.component;

import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RetiredTraitIdsTest {
    private static final Identifier ARROGANT_ASF = Identifier.of("sparktraits", "arrogant_asf");
    private static final Identifier WRAITH = Identifier.of("sparktraits", "wraith");

    @Test
    void exactLegacyTraitIdsAreFilteredWithoutTouchingOtherIds() throws Exception {
        Class<?> migrations = Class.forName("dev.caecorthus.sparktraits.component.RetiredTraitIds");
        Field retiredId = migrations.getDeclaredField("ARROGANT_ASF");
        retiredId.setAccessible(true);
        Method filter = migrations.getDeclaredMethod("filter", Collection.class);
        filter.setAccessible(true);

        Identifier otherTrait = Identifier.of("sparktraits", "depression");
        Identifier samePathOtherNamespace = Identifier.of("other", "arrogant_asf");
        Identifier similarSparkTraitsId = Identifier.of("sparktraits", "arrogant_asf_legacy");
        Identifier otherWraith = Identifier.of("other", "wraith");
        Identifier legacyWraith = Identifier.of("sparktraits", "wraith_legacy");

        assertEquals(ARROGANT_ASF, retiredId.get(null));
        assertEquals(
                List.of(otherTrait, samePathOtherNamespace, similarSparkTraitsId, otherWraith, legacyWraith),
                filter.invoke(null, List.of(
                        otherTrait,
                        ARROGANT_ASF,
                        WRAITH,
                        samePathOtherNamespace,
                        similarSparkTraitsId,
                        otherWraith,
                        legacyWraith
                ))
        );
    }

    @Test
    void exactLegacyWraithIdIsRetired() throws Exception {
        Class<?> migrations = Class.forName("dev.caecorthus.sparktraits.component.RetiredTraitIds");
        Field retiredId = migrations.getDeclaredField("WRAITH");
        retiredId.setAccessible(true);

        assertEquals(WRAITH, retiredId.get(null));
    }
}
