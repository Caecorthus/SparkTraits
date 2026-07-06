package dev.caecorthus.sparktraits.net.version;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SparkTraitsVersionCheckTest {
    @Test
    void matchingVersionsAreCompatible() {
        assertTrue(SparkTraitsVersionCheck.isCompatible("0.1.9.4", "0.1.9.4"));
    }

    @Test
    void differentOrBlankVersionsAreRejected() {
        assertFalse(SparkTraitsVersionCheck.isCompatible("0.1.9.4", "0.1.0"));
        assertFalse(SparkTraitsVersionCheck.isCompatible("0.1.9.4", ""));
        assertFalse(SparkTraitsVersionCheck.isCompatible("0.1.9.4", null));
    }

    @Test
    void unansweredLoginQueriesAreAllowedForProxyTransfers() {
        assertFalse(SparkTraitsVersionCheck.shouldRejectUnansweredLoginQuery());
    }

    @Test
    void disconnectMessagesNameExpectedAndActualVersions() {
        assertEquals(
                "SparkTraits is required on the client with version 0.1.9.4.",
                SparkTraitsVersionCheck.missingClientMessage("0.1.9.4")
        );
        assertEquals(
                "SparkTraits version mismatch: server=0.1.9.4, client=0.1.0. "
                        + "Please install the same SparkTraits version as the server.",
                SparkTraitsVersionCheck.mismatchMessage("0.1.9.4", "0.1.0")
        );
    }
}
