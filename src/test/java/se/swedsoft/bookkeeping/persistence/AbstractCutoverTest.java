package se.swedsoft.bookkeeping.persistence;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import se.swedsoft.bookkeeping.testsupport.system.SSV2DatabaseFixture;

import java.sql.Connection;

/**
 * Shared base class for Repositories cutover wiring tests.
 *
 * <p>Opens an in-memory HSQLDB V2 connection before each test so that
 * {@link Repositories#init} can instantiate repository objects that require
 * a non-null {@link java.sql.Connection}.</p>
 */
abstract class AbstractCutoverTest {

    private Connection connection;

    @BeforeEach
    void openDb() throws Exception {
        connection = SSV2DatabaseFixture.openDatabase(
                "jdbc:hsqldb:mem:cutover-" + getClass().getSimpleName() + ";shutdown=true");
    }

    @AfterEach
    void closeDb() throws Exception {
        SSV2DatabaseFixture.closeDatabase(connection);
        SSV2DatabaseFixture.clearState();
    }
}

