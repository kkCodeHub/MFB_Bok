package se.swedsoft.bookkeeping.data.system;

import se.swedsoft.bookkeeping.data.system.trigger.SSEventTriggerDispatcher;
import se.swedsoft.bookkeeping.data.system.trigger.SSTriggerSchemaService;

/**
 * Public facade for trigger handling and cache synchronization.
 * All external callers should use this class instead of SSDB directly.
 */
public final class SSEventTriggerSyncContext {

    private static final SSDB DB = SSSystemConfigContext.getDatabase();
    private static final SSEventTriggerDispatcher TRIGGER_DISPATCHER = new SSEventTriggerDispatcher(DB);
    private static final SSTriggerSchemaService TRIGGER_SCHEMA_SERVICE =
            new SSTriggerSchemaService(() -> DB.getConnection());

    private SSEventTriggerSyncContext() {}

    public static void clearCachedLists() {
        DB.clearCachedLists();
    }

    public static void dropTriggers() {
        TRIGGER_SCHEMA_SERVICE.dropTriggers();
    }

    public static void createTriggers() {
        TRIGGER_SCHEMA_SERVICE.createLocalTriggers();
    }

    public static void triggerAction(String pTriggerName, String pTableName, String pNumber) {
        TRIGGER_DISPATCHER.dispatchAction(pTriggerName, pTableName, pNumber);
    }
}
