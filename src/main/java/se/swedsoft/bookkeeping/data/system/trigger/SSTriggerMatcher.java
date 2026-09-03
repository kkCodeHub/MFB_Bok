package se.swedsoft.bookkeeping.data.system.trigger;

/**
 * Utility for matching trigger name strings.
 * Replaces the private {@code isAnyTrigger} helper that previously lived in SSDB.
 */
final class SSTriggerMatcher {

    private SSTriggerMatcher() {}

    /**
     * Returns {@code true} if {@code pTriggerName} equals any of {@code pTriggers}.
     *
     * @param pTriggerName the trigger name to test; may be {@code null}
     * @param pTriggers    the names to match against
     * @return {@code true} on a match, {@code false} otherwise
     */
    static boolean isAny(String pTriggerName, String... pTriggers) {
        if (pTriggerName == null) {
            return false;
        }
        for (String iTrigger : pTriggers) {
            if (pTriggerName.equals(iTrigger)) {
                return true;
            }
        }
        return false;
    }
}
