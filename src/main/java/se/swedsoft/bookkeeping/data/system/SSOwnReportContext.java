package se.swedsoft.bookkeeping.data.system;

import se.swedsoft.bookkeeping.data.SSOwnReport;
import se.swedsoft.bookkeeping.persistence.Repositories;

import java.util.List;
import java.util.Optional;

/**
 * OwnReport domain facade.
 */
public final class SSOwnReportContext {

    private SSOwnReportContext() {}

    public static List<SSOwnReport> getOwnReports() {
        return Repositories.ownReports().findAll();
    }

    public static Optional<SSOwnReport> getOwnReport(SSOwnReport pOwnReport) {
        return Repositories.ownReports().findByOwnReport(pOwnReport);
    }

    public static void addOwnReport(SSOwnReport pOwnReport) {
        Repositories.ownReports().add(pOwnReport);
    }

    public static void updateOwnReport(SSOwnReport pOwnReport) {
        Repositories.ownReports().update(pOwnReport);
    }

    public static void deleteOwnReport(SSOwnReport pOwnReport) {
        Repositories.ownReports().delete(pOwnReport);
    }
}
