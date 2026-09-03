package se.swedsoft.bookkeeping.data.system.trigger;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.swedsoft.bookkeeping.data.SSInpayment;
import se.swedsoft.bookkeeping.data.system.SSTriggerRuntime;
import se.swedsoft.bookkeeping.gui.customer.SSCustomerFrame;
import se.swedsoft.bookkeeping.gui.inpayment.SSInpaymentFrame;
import se.swedsoft.bookkeeping.gui.invoice.SSInvoiceFrame;
import se.swedsoft.bookkeeping.persistence.Repositories;

final class CustomerPaymentTriggerHandler implements SSTriggerCategoryHandler {

    private static final Logger LOG = LoggerFactory.getLogger(CustomerPaymentTriggerHandler.class);

    private final SSTriggerRuntime iRuntime;

    CustomerPaymentTriggerHandler(SSTriggerRuntime pRuntime) {
        iRuntime = pRuntime;
    }

    @Override
    public boolean handle(String iTriggerName, String iTableName, String iNumber) {
        if (iTriggerName == null) {
            return false;
        }
        if (!SSTriggerMatcher.isAny(iTriggerName, "NEWINPAYMENT", "EDITINPAYMENT", "DELETEINPAYMENT")) {
            return false;
        }

        List<SSInpayment> iInpayments = iRuntime.getInpayments();
        if (iInpayments == null) {
            return true;
        }

        if (iTriggerName.equals("NEWINPAYMENT")) {
            SSInpayment iInpayment = new SSInpayment();
            iInpayment.setNumber(Integer.parseInt(iNumber));

            Optional<SSInpayment> optInpayment = Repositories.inpayments().findByInpayment(iInpayment);
            if (optInpayment.isEmpty()) {
                LOG.warn("NEWINPAYMENT trigger: inpayment not found for number {}", iNumber);
                return true;
            }
            iInpayment = optInpayment.get();
            if (!iInpayments.contains(iInpayment)) {
                iInpayments.add(iInpayment);
                iRuntime.applyInpaymentSaldoDelta(iInpayment, false);
            }
            if (SSCustomerFrame.getInstance() != null) {
                SSCustomerFrame.getInstance().updateFrame();
            }
            if (SSInvoiceFrame.getInstance() != null) {
                SSInvoiceFrame.getInstance().updateFrame();
            }
            if (SSInpaymentFrame.getInstance() != null) {
                SSInpaymentFrame.getInstance().updateFrame();
            }
            return true;
        }

        if (iTriggerName.equals("EDITINPAYMENT")) {
            SSInpayment iInpayment = new SSInpayment();
            iInpayment.setNumber(Integer.parseInt(iNumber));

            Optional<SSInpayment> optInpayment = Repositories.inpayments().findByInpayment(iInpayment);
            if (optInpayment.isEmpty()) {
                LOG.warn("EDITINPAYMENT trigger: entity not found for number {}", iNumber);
                return true;
            }
            iInpayment = optInpayment.get();
            int iIndex = iInpayments.lastIndexOf(iInpayment);
            if (iIndex == -1) {
                return true;
            }
            SSInpayment iOldInpayment = iInpayments.get(iIndex);
            iRuntime.applyInpaymentSaldoDelta(iOldInpayment, true);
            iInpayments.remove(iIndex);
            iInpayments.add(iIndex, iInpayment);
            iRuntime.applyInpaymentSaldoDelta(iInpayment, false);
            if (SSCustomerFrame.getInstance() != null) {
                SSCustomerFrame.getInstance().updateFrame();
            }
            if (SSInvoiceFrame.getInstance() != null) {
                SSInvoiceFrame.getInstance().updateFrame();
            }
            if (SSInpaymentFrame.getInstance() != null) {
                SSInpaymentFrame.getInstance().updateFrame();
            }
            return true;
        }

        if (iTriggerName.equals("DELETEINPAYMENT")) {
            SSInpayment iInpayment = new SSInpayment();
            iInpayment.setNumber(Integer.parseInt(iNumber));

            int iIndex = iInpayments.lastIndexOf(iInpayment);
            if (iIndex != -1) {
                SSInpayment iOldInpayment = iInpayments.get(iIndex);
                iRuntime.applyInpaymentSaldoDelta(iOldInpayment, true);
                iInpayments.remove(iIndex);
            }
            if (SSCustomerFrame.getInstance() != null) {
                SSCustomerFrame.getInstance().updateFrame();
            }
            if (SSInvoiceFrame.getInstance() != null) {
                SSInvoiceFrame.getInstance().updateFrame();
            }
            if (SSInpaymentFrame.getInstance() != null) {
                SSInpaymentFrame.getInstance().updateFrame();
            }
            return true;
        }
        return false;
    }
}

