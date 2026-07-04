package dev.caecorthus.sparktraits.net;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SparkTraitsVersionCheckTest {
    @Test
    void matchingVersionsAreCompatible() {
        assertTrue(SparkTraitsVersionCheck.isCompatible("0.1.9.1", "0.1.9.1"));
    }

    @Test
    void differentOrBlankVersionsAreRejected() {
        assertFalse(SparkTraitsVersionCheck.isCompatible("0.1.9.1", "0.1.9"));
        assertFalse(SparkTraitsVersionCheck.isCompatible("0.1.9.1", ""));
        assertFalse(SparkTraitsVersionCheck.isCompatible("0.1.9.1", null));
    }

    @Test
    void unansweredLoginQueriesAreAllowedForProxyTransfers() {
        assertFalse(SparkTraitsVersionCheck.shouldRejectUnansweredLoginQuery());
    }

    @Test
    void disconnectMessagesNameExpectedAndActualVersions() {
        assertEquals(
                "SparkTraits is required on the client with version 0.1.9.1.",
                SparkTraitsVersionCheck.missingClientMessage("0.1.9.1")
        );
        assertEquals(
                "SparkTraits version mismatch: server=0.1.9.1, client=0.1.9. "
                        + "Please install the same SparkTraits version as the server.",
                SparkTraitsVersionCheck.mismatchMessage("0.1.9.1", "0.1.9")
        );
    }
}
