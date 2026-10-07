package se.swedsoft.bookkeeping.data.system.trigger;

import java.util.List;
import java.util.LinkedList;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.swedsoft.bookkeeping.data.SSVoucher;
import se.swedsoft.bookkeeping.data.system.SSTriggerRuntime;
import se.swedsoft.bookkeeping.gui.voucher.SSVoucherFrame;
import se.swedsoft.bookkeeping.persistence.Repositories;

final class AccountingTriggerHandler implements SSTriggerCategoryHandler {

    private static final Logger LOG = LoggerFactory.getLogger(AccountingTriggerHandler.class);

    private final SSTriggerRuntime iRuntime;

    AccountingTriggerHandler(SSTriggerRuntime pRuntime) {
        iRuntime = pRuntime;
    }

    @Override
    public boolean handle(String iTriggerName, String iTableName, String iNumber) {
        if (iTriggerName == null) {
            return false;
        }
        if (!SSTriggerMatcher.isAny(iTriggerName, "NEWVOUCHER", "EDITVOUCHER", "DELETEVOUCHER")) {
            return false;
        }

        List<SSVoucher> iVouchers = iRuntime.getVouchers();
        if (iVouchers == null) {
            iVouchers = refreshVoucherCache();
        }

        if (iTriggerName.equals("NEWVOUCHER")) {
            VoucherIdentifier iIdentifier = parseVoucherIdentifier(iNumber);
            Optional<SSVoucher> optVoucher = Repositories.vouchers().findBySeriesAndNumber(
                    se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.getCurrentYear(),
                    iIdentifier.series,
                    iIdentifier.number);
            if (optVoucher.isEmpty()) {
                LOG.warn("NEWVOUCHER trigger: entity not found for series/number {}", iNumber);
                refreshVoucherCache();
                if (SSVoucherFrame.getInstance() != null) {
                    SSVoucherFrame.getInstance().updateFrame();
                }
                return true;
            }
            SSVoucher iVoucher = optVoucher.get();
            int iIndex = findVoucherIndex(iVouchers, iIdentifier);
            if (iIndex == -1) {
                iVouchers.add(iVoucher);
            } else {
                iVouchers.remove(iIndex);
                iVouchers.add(iIndex, iVoucher);
            }
            if (SSVoucherFrame.getInstance() != null) {
                SSVoucherFrame.getInstance().updateFrame();
            }
            return true;
        }

        if (iTriggerName.equals("EDITVOUCHER")) {
            VoucherIdentifier iIdentifier = parseVoucherIdentifier(iNumber);
            Optional<SSVoucher> optVoucher = Repositories.vouchers().findBySeriesAndNumber(
                    se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.getCurrentYear(),
                    iIdentifier.series,
                    iIdentifier.number);
            if (optVoucher.isEmpty()) {
                LOG.warn("EDITVOUCHER trigger: entity not found for series/number {}", iNumber);
                refreshVoucherCache();
                if (SSVoucherFrame.getInstance() != null) {
                    SSVoucherFrame.getInstance().updateFrame();
                }
                return true;
            }
            SSVoucher iVoucher = optVoucher.get();
            int iIndex = findVoucherIndex(iVouchers, iIdentifier);
            if (iIndex == -1) {
                return true;
            }
            iVouchers.remove(iIndex);
            iVouchers.add(iIndex, iVoucher);
            if (SSVoucherFrame.getInstance() != null) {
                SSVoucherFrame.getInstance().updateFrame();
            }
            return true;
        }

        if (iTriggerName.equals("DELETEVOUCHER")) {
            VoucherIdentifier iIdentifier = parseVoucherIdentifier(iNumber);
            iVouchers.removeIf(voucher -> matchesVoucher(voucher, iIdentifier));
            if (SSVoucherFrame.getInstance() != null) {
                SSVoucherFrame.getInstance().updateFrame();
            }
            return true;
        }
        return true;
    }

    private List<SSVoucher> refreshVoucherCache() {
        List<SSVoucher> iFresh = new LinkedList<>();
        se.swedsoft.bookkeeping.data.SSNewAccountingYear iCurrentYear =
                se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.getCurrentYear();
        if (iCurrentYear != null) {
            iFresh.addAll(Repositories.vouchers().findByYear(iCurrentYear));
        }
        iRuntime.setVouchers(iFresh);
        return iFresh;
    }

    private boolean matchesVoucher(SSVoucher pVoucher, VoucherIdentifier pIdentifier) {
        if (pVoucher == null || pIdentifier == null) {
            return false;
        }
        if (pVoucher.getNumber() != pIdentifier.number) {
            return false;
        }
        if (pIdentifier.series == null || pIdentifier.series.trim().isEmpty()) {
            return true;
        }
        String iSeries = pVoucher.getSeries();
        return iSeries != null && iSeries.equalsIgnoreCase(pIdentifier.series);
    }

    private int findVoucherIndex(List<SSVoucher> pVouchers, VoucherIdentifier pIdentifier) {
        if (pVouchers == null || pIdentifier == null) {
            return -1;
        }
        for (int i = 0; i < pVouchers.size(); i++) {
            if (matchesVoucher(pVouchers.get(i), pIdentifier)) {
                return i;
            }
        }
        return -1;
    }

    private VoucherIdentifier parseVoucherIdentifier(String pNumber) {
        if (pNumber == null) {
            throw new NumberFormatException("voucher identifier must not be null");
        }
        int iSeparator = pNumber.indexOf(':');
        if (iSeparator > 0) {
            String iSeries = pNumber.substring(0, iSeparator);
            int iNumber = Integer.parseInt(pNumber.substring(iSeparator + 1));
            return new VoucherIdentifier(iSeries, iNumber);
        }
        return new VoucherIdentifier(null, Integer.parseInt(pNumber));
    }

    private static final class VoucherIdentifier {
        private final String series;
        private final int number;

        private VoucherIdentifier(String pSeries, int pNumber) {
            series = pSeries;
            number = pNumber;
        }
    }
}
