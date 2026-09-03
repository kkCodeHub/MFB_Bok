package se.swedsoft.bookkeeping.data.system.trigger;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.swedsoft.bookkeeping.calc.math.SSCustomerMath;
import se.swedsoft.bookkeeping.calc.math.SSInvoiceMath;
import se.swedsoft.bookkeeping.calc.service.SaldoDeltaService;
import se.swedsoft.bookkeeping.data.SSCreditInvoice;
import se.swedsoft.bookkeeping.data.SSInvoice;
import se.swedsoft.bookkeeping.data.SSOrder;
import se.swedsoft.bookkeeping.data.SSPeriodicInvoice;
import se.swedsoft.bookkeeping.data.SSTender;
import se.swedsoft.bookkeeping.data.system.SSTriggerRuntime;
import se.swedsoft.bookkeeping.gui.creditinvoice.SSCreditInvoiceFrame;
import se.swedsoft.bookkeeping.gui.customer.SSCustomerFrame;
import se.swedsoft.bookkeeping.gui.invoice.SSInvoiceFrame;
import se.swedsoft.bookkeeping.gui.order.SSOrderFrame;
import se.swedsoft.bookkeeping.gui.periodicinvoice.SSPeriodicInvoiceFrame;
import se.swedsoft.bookkeeping.gui.tender.SSTenderFrame;
import se.swedsoft.bookkeeping.persistence.Repositories;

final class SalesTriggerHandler implements SSTriggerCategoryHandler {

    private static final Logger LOG = LoggerFactory.getLogger(SalesTriggerHandler.class);

    private final SSTriggerRuntime iRuntime;

    SalesTriggerHandler(SSTriggerRuntime pRuntime) {
        iRuntime = pRuntime;
    }

    @Override
    public boolean handle(String iTriggerName, String iTableName, String iNumber) {
        if (iTriggerName == null) {
            return false;
        }
        if (!SSTriggerMatcher.isAny(iTriggerName,
                "NEWTENDER", "EDITTENDER", "DELETETENDER",
                "NEWORDER", "EDITORDER", "DELETEORDER",
                "NEWINVOICE", "EDITINVOICE", "DELETEINVOICE",
                "NEWCREDITINVOICE", "EDITCREDITINVOICE", "DELETECREDITINVOICE",
                "NEWPERIODICINVOICE", "EDITPERIODICINVOICE", "DELETEPERIODICINVOICE")) {
            return false;
        }

        if (SSTriggerMatcher.isAny(iTriggerName, "NEWTENDER", "EDITTENDER", "DELETETENDER")) {
            List<SSTender> iTenders = iRuntime.getTenders();
            if (iTenders == null) {
                return true;
            }
            SSTender iTender = new SSTender();
            iTender.setNumber(Integer.parseInt(iNumber));

            if (iTriggerName.equals("NEWTENDER")) {
                Optional<SSTender> optTender = Repositories.tenders().findByTender(iTender);
                if (optTender.isEmpty()) {
                    LOG.warn("NEWTENDER trigger: entity not found for number {}", iNumber);
                    return true;
                }
                iTender = optTender.get();
                if (!iTenders.contains(iTender)) {
                    iTenders.add(iTender);
                }
            } else if (iTriggerName.equals("EDITTENDER")) {
                Optional<SSTender> optTender = Repositories.tenders().findByTender(iTender);
                if (optTender.isEmpty()) {
                    LOG.warn("EDITTENDER trigger: entity not found for number {}", iNumber);
                    return true;
                }
                iTender = optTender.get();
                int iIndex = iTenders.lastIndexOf(iTender);
                if (iIndex == -1) {
                    return true;
                }
                iTenders.remove(iIndex);
                iTenders.add(iIndex, iTender);
            } else {
                iTenders.remove(iTender);
            }

            if (SSTenderFrame.getInstance() != null) {
                SSTenderFrame.getInstance().updateFrame();
            }
            return true;
        }

        if (SSTriggerMatcher.isAny(iTriggerName, "NEWORDER", "EDITORDER", "DELETEORDER")) {
            List<SSOrder> iOrders = iRuntime.getOrders();
            if (iOrders == null) {
                return true;
            }
            SSOrder iOrder = new SSOrder();
            iOrder.setNumber(Integer.parseInt(iNumber));

            if (iTriggerName.equals("NEWORDER")) {
                Optional<SSOrder> optOrder = Repositories.orders().findByOrder(iOrder);
                if (optOrder.isEmpty()) {
                    LOG.warn("NEWORDER trigger: entity not found for number {}", iNumber);
                    return true;
                }
                iOrder = optOrder.get();
                if (!iOrders.contains(iOrder)) {
                    iOrders.add(iOrder);
                }
            } else if (iTriggerName.equals("EDITORDER")) {
                Optional<SSOrder> optOrder = Repositories.orders().findByOrder(iOrder);
                if (optOrder.isEmpty()) {
                    LOG.warn("EDITORDER trigger: entity not found for number {}", iNumber);
                    return true;
                }
                iOrder = optOrder.get();
                int iIndex = iOrders.lastIndexOf(iOrder);
                if (iIndex == -1) {
                    return true;
                }
                iOrders.remove(iIndex);
                iOrders.add(iIndex, iOrder);
            } else {
                iOrders.remove(iOrder);
            }

            if (SSOrderFrame.getInstance() != null) {
                SSOrderFrame.getInstance().updateFrame();
            }
            return true;
        }

        if (SSTriggerMatcher.isAny(iTriggerName, "NEWINVOICE", "EDITINVOICE", "DELETEINVOICE")) {
            List<SSInvoice> iInvoices = iRuntime.getInvoices();
            if (iInvoices == null) {
                return true;
            }
            SSInvoice iInvoice = new SSInvoice();
            iInvoice.setNumber(Integer.parseInt(iNumber));

            if (iTriggerName.equals("NEWINVOICE")) {
                Optional<SSInvoice> optInvoice = Repositories.invoices().findByInvoice(iInvoice);
                if (optInvoice.isEmpty()) {
                    LOG.warn("NEWINVOICE trigger: entity not found for number {}", iNumber);
                    return true;
                }
                iInvoice = optInvoice.get();
                if (!iInvoices.contains(iInvoice)) {
                    iInvoices.add(iInvoice);
                }
                SSInvoiceMath.iSaldoMap.put(iInvoice.getNumber(), SSInvoiceMath.getSaldo(iInvoice));
                if (SSCustomerMath.iInvoicesForCustomers.containsKey(iInvoice.getCustomerNr())) {
                    SSCustomerMath.iInvoicesForCustomers.get(iInvoice.getCustomerNr()).add(iInvoice);
                } else {
                    List<SSInvoice> iNumbers = new LinkedList<>();
                    iNumbers.add(iInvoice);
                    SSCustomerMath.iInvoicesForCustomers.put(iInvoice.getCustomerNr(), iNumbers);
                }
                if (SSOrderFrame.getInstance() != null) {
                    SSOrderFrame.getInstance().updateFrame();
                }
                if (SSCustomerFrame.getInstance() != null) {
                    SSCustomerFrame.getInstance().updateFrame();
                }
                if (SSInvoiceFrame.getInstance() != null) {
                    SSInvoiceFrame.getInstance().updateFrame();
                }
                return true;
            }

            if (iTriggerName.equals("EDITINVOICE")) {
                Optional<SSInvoice> optInvoice = Repositories.invoices().findByInvoice(iInvoice);
                if (optInvoice.isEmpty()) {
                    LOG.warn("EDITINVOICE trigger: entity not found for number {}", iNumber);
                    return true;
                }
                iInvoice = optInvoice.get();
                int iIndex = iInvoices.lastIndexOf(iInvoice);
                if (iIndex == -1) {
                    return true;
                }
                iInvoices.remove(iIndex);
                iInvoices.add(iIndex, iInvoice);
                SSInvoiceMath.iSaldoMap.put(iInvoice.getNumber(), SSInvoiceMath.getSaldo(iInvoice));
                iIndex = SSCustomerMath.iInvoicesForCustomers.get(iInvoice.getCustomerNr()).indexOf(iInvoice);
                if (iIndex != -1) {
                    SSCustomerMath.iInvoicesForCustomers.get(iInvoice.getCustomerNr()).remove(iIndex);
                    SSCustomerMath.iInvoicesForCustomers.get(iInvoice.getCustomerNr()).add(iIndex, iInvoice);
                }
                if (SSOrderFrame.getInstance() != null) {
                    SSOrderFrame.getInstance().updateFrame();
                }
                if (SSCustomerFrame.getInstance() != null) {
                    SSCustomerFrame.getInstance().updateFrame();
                }
                if (SSInvoiceFrame.getInstance() != null) {
                    SSInvoiceFrame.getInstance().updateFrame();
                }
                return true;
            }

            iInvoices.remove(iInvoice);
            SSInvoiceMath.iSaldoMap.remove(iInvoice.getNumber());
            if (SSCustomerFrame.getInstance() != null) {
                SSCustomerFrame.getInstance().updateFrame();
            }
            if (SSInvoiceFrame.getInstance() != null) {
                SSInvoiceFrame.getInstance().updateFrame();
            }
            return true;
        }

        if (SSTriggerMatcher.isAny(iTriggerName, "NEWCREDITINVOICE", "EDITCREDITINVOICE",
                "DELETECREDITINVOICE")) {
            List<SSCreditInvoice> iCreditInvoices = iRuntime.getCreditInvoices();
            if (iCreditInvoices == null) {
                return true;
            }
            SSCreditInvoice iCreditInvoice = new SSCreditInvoice();
            iCreditInvoice.setNumber(Integer.parseInt(iNumber));

            if (iTriggerName.equals("NEWCREDITINVOICE")) {
                Optional<SSCreditInvoice> optCreditInvoice =
                        Repositories.creditInvoices().findByCreditInvoice(iCreditInvoice);
                if (optCreditInvoice.isEmpty()) {
                    LOG.warn("NEWCREDITINVOICE trigger: entity not found for number {}", iNumber);
                    return true;
                }
                iCreditInvoice = optCreditInvoice.get();
                if (!iCreditInvoices.contains(iCreditInvoice)) {
                    iCreditInvoices.add(iCreditInvoice);
                }
                SaldoDeltaService.applyCustomerCreditInvoiceNew(iCreditInvoice);
                if (SSCustomerFrame.getInstance() != null) {
                    SSCustomerFrame.getInstance().updateFrame();
                }
                if (SSInvoiceFrame.getInstance() != null) {
                    SSInvoiceFrame.getInstance().updateFrame();
                }
                if (SSCreditInvoiceFrame.getInstance() != null) {
                    SSCreditInvoiceFrame.getInstance().updateFrame();
                }
                return true;
            }

            if (iTriggerName.equals("EDITCREDITINVOICE")) {
                Optional<SSCreditInvoice> optCreditInvoice =
                        Repositories.creditInvoices().findByCreditInvoice(iCreditInvoice);
                if (optCreditInvoice.isEmpty()) {
                    LOG.warn("EDITCREDITINVOICE trigger: entity not found for number {}", iNumber);
                    return true;
                }
                iCreditInvoice = optCreditInvoice.get();
                int iIndex = iCreditInvoices.lastIndexOf(iCreditInvoice);
                if (iIndex == -1) {
                    return true;
                }
                SSCreditInvoice iOldCreditInvoice = iCreditInvoices.get(iIndex);
                SaldoDeltaService.applyCustomerCreditInvoiceEditRevert(iOldCreditInvoice);
                if (SSCustomerFrame.getInstance() != null) {
                    SSCustomerFrame.getInstance().updateFrame();
                }
                if (SSInvoiceFrame.getInstance() != null) {
                    SSInvoiceFrame.getInstance().updateFrame();
                }
                iCreditInvoices.remove(iIndex);
                iCreditInvoices.add(iIndex, iCreditInvoice);
                SaldoDeltaService.applyCustomerCreditInvoiceEditApply(iCreditInvoice);
                if (SSInvoiceFrame.getInstance() != null) {
                    SSInvoiceFrame.getInstance().updateFrame();
                }
                if (SSCreditInvoiceFrame.getInstance() != null) {
                    SSCreditInvoiceFrame.getInstance().updateFrame();
                }
                return true;
            }

            iCreditInvoices.remove(iCreditInvoice);
            if (SSCustomerFrame.getInstance() != null) {
                SSCustomerFrame.getInstance().updateFrame();
            }
            if (SSCreditInvoiceFrame.getInstance() != null) {
                SSCreditInvoiceFrame.getInstance().updateFrame();
            }
            return true;
        }

        if (SSTriggerMatcher.isAny(iTriggerName, "NEWPERIODICINVOICE", "EDITPERIODICINVOICE",
                "DELETEPERIODICINVOICE")) {
            List<SSPeriodicInvoice> iPeriodicInvoices = iRuntime.getPeriodicInvoices();
            if (iPeriodicInvoices == null) {
                return true;
            }
            SSPeriodicInvoice iPeriodicInvoice = new SSPeriodicInvoice();
            iPeriodicInvoice.setNumber(Integer.parseInt(iNumber));

            if (iTriggerName.equals("NEWPERIODICINVOICE")) {
                Optional<SSPeriodicInvoice> optPeriodicInvoice =
                        Repositories.periodicInvoices().findByPeriodicInvoice(iPeriodicInvoice);
                if (optPeriodicInvoice.isEmpty()) {
                    LOG.warn("NEWPERIODICINVOICE trigger: entity not found for number {}", iNumber);
                    return true;
                }
                iPeriodicInvoice = optPeriodicInvoice.get();
                if (!iPeriodicInvoices.contains(iPeriodicInvoice)) {
                    iPeriodicInvoices.add(iPeriodicInvoice);
                }
            } else if (iTriggerName.equals("EDITPERIODICINVOICE")) {
                Optional<SSPeriodicInvoice> optPeriodicInvoice =
                        Repositories.periodicInvoices().findByPeriodicInvoice(iPeriodicInvoice);
                if (optPeriodicInvoice.isEmpty()) {
                    LOG.warn("EDITPERIODICINVOICE trigger: entity not found for number {}", iNumber);
                    return true;
                }
                iPeriodicInvoice = optPeriodicInvoice.get();
                int iIndex = iPeriodicInvoices.lastIndexOf(iPeriodicInvoice);
                if (iIndex == -1) {
                    return true;
                }
                iPeriodicInvoices.remove(iIndex);
                iPeriodicInvoices.add(iIndex, iPeriodicInvoice);
            } else {
                iPeriodicInvoices.remove(iPeriodicInvoice);
            }

            if (SSPeriodicInvoiceFrame.getInstance() != null) {
                SSPeriodicInvoiceFrame.getInstance().updateFrame();
            }
            return true;
        }

        return true;
    }
}

