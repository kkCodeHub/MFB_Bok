package se.swedsoft.bookkeeping.data.system.trigger;

interface SSTriggerCategoryHandler {
    boolean handle(String pTriggerName, String pTableName, String pNumber);
}


