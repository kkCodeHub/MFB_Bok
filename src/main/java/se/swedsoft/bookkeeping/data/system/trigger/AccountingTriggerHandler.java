package se.swedsoft.bookkeeping.data.system.trigger;

import java.util.List;
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
            return true;
        }

        if (iTriggerName.equals("NEWVOUCHER")) {
            SSVoucher iVoucher = new SSVoucher(Integer.parseInt(iNumber));
            Optional<SSVoucher> optVoucher = Repositories.vouchers().findVoucher(iVoucher);
            if (optVoucher.isEmpty()) {
                LOG.warn("NEWVOUCHER trigger: entity not found for number {}", iNumber);
                return true;
            }
            iVoucher = optVoucher.get();
            if (!iVouchers.contains(iVoucher)) {
                iVouchers.add(iVoucher);
            }
            if (SSVoucherFrame.getInstance() != null) {
                SSVoucherFrame.getInstance().updateFrame();
            }
            return true;
        }

        if (iTriggerName.equals("EDITVOUCHER")) {
            SSVoucher iVoucher = new SSVoucher(Integer.parseInt(iNumber));
            Optional<SSVoucher> optVoucher = Repositories.vouchers().findVoucher(iVoucher);
            if (optVoucher.isEmpty()) {
                LOG.warn("EDITVOUCHER trigger: entity not found for number {}", iNumber);
                return true;
            }
            iVoucher = optVoucher.get();
            int iIndex = iVouchers.lastIndexOf(iVoucher);
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
            SSVoucher iVoucher = new SSVoucher(Integer.parseInt(iNumber));
            iVouchers.remove(iVoucher);
            if (SSVoucherFrame.getInstance() != null) {
                SSVoucherFrame.getInstance().updateFrame();
            }
            return true;
        }
        return true;
    }
}

