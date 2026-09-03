package se.swedsoft.bookkeeping.data.system.trigger;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.swedsoft.bookkeeping.data.SSOwnReport;
import se.swedsoft.bookkeeping.data.system.SSTriggerRuntime;
import se.swedsoft.bookkeeping.gui.ownreport.SSOwnReportFrame;
import se.swedsoft.bookkeeping.persistence.Repositories;

final class ReportTriggerHandler implements SSTriggerCategoryHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ReportTriggerHandler.class);

    private final SSTriggerRuntime iRuntime;

    ReportTriggerHandler(SSTriggerRuntime pRuntime) {
        iRuntime = pRuntime;
    }

    @Override
    public boolean handle(String iTriggerName, String iTableName, String iNumber) {
        if (iTriggerName == null) {
            return false;
        }
        if (!SSTriggerMatcher.isAny(iTriggerName, "NEWOWNREPORT", "EDITOWNREPORT", "DELETEOWNREPORT")) {
            return false;
        }
        List<SSOwnReport> iOwnReports = iRuntime.getOwnReports();
        if (iOwnReports == null) {
            return true;
        }
        if (iTriggerName.equals("NEWOWNREPORT")) {
            SSOwnReport iOwnReport = new SSOwnReport();
            iOwnReport.setId(Integer.parseInt(iNumber));
            Optional<SSOwnReport> optOwnReport = Repositories.ownReports().findByOwnReport(iOwnReport);
            if (optOwnReport.isEmpty()) {
                LOG.warn("NEWOWNREPORT trigger: entity not found for number {}", iNumber);
                return true;
            }
            iOwnReport = optOwnReport.get();
            if (!iOwnReports.contains(iOwnReport) && iOwnReport.getId() != -1) {
                iOwnReports.add(iOwnReport);
            }
            if (SSOwnReportFrame.getInstance() != null) {
                SSOwnReportFrame.getInstance().updateFrame();
            }
            return true;
        }
        if (iTriggerName.equals("EDITOWNREPORT")) {
            SSOwnReport iOwnReport = new SSOwnReport();
            iOwnReport.setId(Integer.parseInt(iNumber));
            Optional<SSOwnReport> optOwnReport = Repositories.ownReports().findByOwnReport(iOwnReport);
            if (optOwnReport.isEmpty()) {
                LOG.warn("EDITOWNREPORT trigger: entity not found for number {}", iNumber);
                return true;
            }
            iOwnReport = optOwnReport.get();
            int iIndex = iOwnReports.lastIndexOf(iOwnReport);
            if (iIndex != -1) {
                iOwnReports.remove(iIndex);
                iOwnReports.add(iIndex, iOwnReport);
            } else {
                iOwnReports.add(iOwnReport);
            }
            if (SSOwnReportFrame.getInstance() != null) {
                SSOwnReportFrame.getInstance().updateFrame();
            }
            return true;
        }
        if (iTriggerName.equals("DELETEOWNREPORT")) {
            SSOwnReport iOwnReport = new SSOwnReport();
            iOwnReport.setId(Integer.parseInt(iNumber));
            iOwnReports.remove(iOwnReport);
            if (SSOwnReportFrame.getInstance() != null) {
                SSOwnReportFrame.getInstance().updateFrame();
            }
            return true;
        }
        return true;
    }
}

