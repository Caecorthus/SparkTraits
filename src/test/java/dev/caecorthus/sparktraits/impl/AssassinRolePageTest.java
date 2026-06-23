package dev.caecorthus.sparktraits.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssassinRolePageTest {
    @Test
    void pageSizeUsesRowsThatFitBeforeControls() {
        int pageSize = AssassinRolePage.pageSizeForHeight(65, 210, 3, 20, 5);

        assertEquals(15, pageSize);
    }

    @Test
    void layoutClampsToLastAvailablePage() {
        AssassinRolePage.Layout layout = AssassinRolePage.layout(28, 99, 12);

        assertEquals(2, layout.page());
        assertEquals(3, layout.pageCount());
        assertEquals(24, layout.startIndex());
        assertEquals(28, layout.endIndex());
        assertTrue(layout.hasPrevious());
        assertFalse(layout.hasNext());
    }
}
