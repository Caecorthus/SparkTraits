package dev.caecorthus.sparktraits.net;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SparkTraitsServerConnectionTest {
    @AfterEach
    void reset() {
        SparkTraitsServerConnection.reset();
    }

    @Test
    void ordinaryServersStartUnconfirmed() {
        SparkTraitsServerConnection.reset();

        assertFalse(SparkTraitsServerConnection.isConfirmedServer());
    }

    @Test
    void loginQueryConfirmsSparkTraitsServerUntilDisconnect() {
        SparkTraitsServerConnection.confirmServer();

        assertTrue(SparkTraitsServerConnection.isConfirmedServer());

        SparkTraitsServerConnection.reset();

        assertFalse(SparkTraitsServerConnection.isConfirmedServer());
    }
}
