package se.swedsoft.bookkeeping.data.system.trigger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.swedsoft.bookkeeping.persistence.v2.schema.SSSchemaBuilder;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Supplier;

/**
 * Encapsulates DB-level trigger schema operations (create / drop / rebuild).
 * Decouples these operations from the SSDB facade.
 */
public final class SSTriggerSchemaService {

    private static final Logger LOG = LoggerFactory.getLogger(SSTriggerSchemaService.class);

    private final Supplier<Connection> iConnectionSupplier;

    public SSTriggerSchemaService(Supplier<Connection> pConnectionSupplier) {
        iConnectionSupplier = pConnectionSupplier;
    }

    /** Creates local HSQLDB triggers for the current schema. */
    public void createLocalTriggers() {
        try {
            new SSSchemaBuilder(iConnectionSupplier.get()).createLocalTriggers();
        } catch (SQLException e) {
            LOG.debug("createLocalTriggers encountered: {}", e.getMessage());
        }
    }

    /** Drops all local HSQLDB triggers for the current schema. */
    public void dropTriggers() {
        try {
            new SSSchemaBuilder(iConnectionSupplier.get()).dropTriggers();
        } catch (SQLException e) {
            LOG.debug("dropTriggers encountered: {}", e.getMessage());
        }
    }

    /** Drops then re-creates all local triggers. */
    public void rebuildLocalTriggers() {
        dropTriggers();
        createLocalTriggers();
    }
}
