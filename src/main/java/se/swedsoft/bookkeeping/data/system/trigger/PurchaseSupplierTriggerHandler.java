package se.swedsoft.bookkeeping.data.system.trigger;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.swedsoft.bookkeeping.calc.math.SSSupplierInvoiceMath;
import se.swedsoft.bookkeeping.calc.math.SSSupplierMath;
import se.swedsoft.bookkeeping.calc.service.SaldoDeltaService;
import se.swedsoft.bookkeeping.data.SSOutpayment;
import se.swedsoft.bookkeeping.data.SSPurchaseOrder;
import se.swedsoft.bookkeeping.data.SSSupplierCreditInvoice;
import se.swedsoft.bookkeeping.data.SSSupplierInvoice;
import se.swedsoft.bookkeeping.data.system.SSTriggerRuntime;
import se.swedsoft.bookkeeping.gui.order.SSOrderFrame;
import se.swedsoft.bookkeeping.gui.outpayment.SSOutpaymentFrame;
import se.swedsoft.bookkeeping.gui.purchaseorder.SSPurchaseOrderFrame;
import se.swedsoft.bookkeeping.gui.supplier.SSSupplierFrame;
import se.swedsoft.bookkeeping.gui.suppliercreditinvoice.SSSupplierCreditInvoiceFrame;
import se.swedsoft.bookkeeping.gui.supplierinvoice.SSSupplierInvoiceFrame;
import se.swedsoft.bookkeeping.persistence.Repositories;

final class PurchaseSupplierTriggerHandler implements SSTriggerCategoryHandler {

    private static final Logger LOG = LoggerFactory.getLogger(PurchaseSupplierTriggerHandler.class);

    private final SSTriggerRuntime iRuntime;

    PurchaseSupplierTriggerHandler(SSTriggerRuntime pRuntime) {
        iRuntime = pRuntime;
    }

    @Override
    public boolean handle(String iTriggerName, String iTableName, String iNumber) {
        if (iTriggerName == null) {
            return false;
        }
        if (!SSTriggerMatcher.isAny(iTriggerName,
                "NEWPURCHASEORDER", "EDITPURCHASEORDER", "DELETEPURCHASEORDER",
                "NEWOUTPAYMENT", "EDITOUTPAYMENT", "DELETEOUTPAYMENT",
                "NEWSUPPLIERINVOICE", "EDITSUPPLIERINVOICE", "DELETESUPPLIERINVOICE",
                "NEWSUPPLIERCREDITINVOICE", "EDITSUPPLIERCREDITINVOICE", "DELETESUPPLIERCREDITINVOICE")) {
            return false;
        }

        if (SSTriggerMatcher.isAny(iTriggerName, "NEWPURCHASEORDER", "EDITPURCHASEORDER",
                "DELETEPURCHASEORDER")) {
            List<SSPurchaseOrder> iPurchaseOrders = iRuntime.getPurchaseOrders();
            if (iPurchaseOrders == null) {
                return true;
            }
            SSPurchaseOrder iPurchaseOrder = new SSPurchaseOrder();
            iPurchaseOrder.setNumber(Integer.parseInt(iNumber));

            if (iTriggerName.equals("NEWPURCHASEORDER")) {
                Optional<SSPurchaseOrder> optPurchaseOrder = Repositories.purchaseOrders()
                        .findByPurchaseOrder(iPurchaseOrder);
                if (optPurchaseOrder.isEmpty()) {
                    LOG.warn("NEWPURCHASEORDER trigger: entity not found for number {}", iNumber);
                    return true;
                }
                iPurchaseOrder = optPurchaseOrder.get();
                if (!iPurchaseOrders.contains(iPurchaseOrder)) {
                    iPurchaseOrders.add(iPurchaseOrder);
                }
                if (SSPurchaseOrderFrame.getInstance() != null) {
                    SSPurchaseOrderFrame.getInstance().updateFrame();
                }
            } else if (iTriggerName.equals("EDITPURCHASEORDER")) {
                Optional<SSPurchaseOrder> optPurchaseOrder = Repositories.purchaseOrders()
                        .findByPurchaseOrder(iPurchaseOrder);
                if (optPurchaseOrder.isEmpty()) {
                    LOG.warn("EDITPURCHASEORDER trigger: entity not found for number {}", iNumber);
                    return true;
                }
                iPurchaseOrder = optPurchaseOrder.get();
                int iIndex = iPurchaseOrders.lastIndexOf(iPurchaseOrder);
                if (iIndex == -1) {
                    return true;
                }
                iPurchaseOrders.remove(iIndex);
                iPurchaseOrders.add(iIndex, iPurchaseOrder);
                if (SSPurchaseOrderFrame.getInstance() != null) {
                    SSPurchaseOrderFrame.getInstance().updateFrame();
                }
            } else {
                iPurchaseOrders.remove(iPurchaseOrder);
            }

            if (SSOrderFrame.getInstance() != null) {
                SSOrderFrame.getInstance().updateFrame();
            }
            if (SSPurchaseOrderFrame.getInstance() != null) {
                SSPurchaseOrderFrame.getInstance().updateFrame();
            }
            return true;
        }

        if (SSTriggerMatcher.isAny(iTriggerName, "NEWOUTPAYMENT", "EDITOUTPAYMENT", "DELETEOUTPAYMENT")) {
            List<SSOutpayment> iOutpayments = iRuntime.getOutpayments();
            if (iOutpayments == null) {
                return true;
            }
            SSOutpayment iOutpayment = new SSOutpayment();
            iOutpayment.setNumber(Integer.parseInt(iNumber));

            if (iTriggerName.equals("NEWOUTPAYMENT")) {
                Optional<SSOutpayment> optOutpayment = Repositories.outpayments().findByOutpayment(iOutpayment);
                if (optOutpayment.isEmpty()) {
                    LOG.warn("NEWOUTPAYMENT trigger: entity not found for number {}", iNumber);
                    return true;
                }
                iOutpayment = optOutpayment.get();
                if (!iOutpayments.contains(iOutpayment)) {
                    iOutpayments.add(iOutpayment);
                    iRuntime.applyOutpaymentSaldoDelta(iOutpayment, false);
                }
            } else if (iTriggerName.equals("EDITOUTPAYMENT")) {
                Optional<SSOutpayment> optOutpayment = Repositories.outpayments().findByOutpayment(iOutpayment);
                if (optOutpayment.isEmpty()) {
                    LOG.warn("EDITOUTPAYMENT trigger: entity not found for number {}", iNumber);
                    return true;
                }
                iOutpayment = optOutpayment.get();
                int iIndex = iOutpayments.lastIndexOf(iOutpayment);
                if (iIndex == -1) {
                    return true;
                }
                SSOutpayment iOldOutpayment = iOutpayments.get(iIndex);
                iRuntime.applyOutpaymentSaldoDelta(iOldOutpayment, true);
                iOutpayments.remove(iIndex);
                iOutpayments.add(iIndex, iOutpayment);
                iRuntime.applyOutpaymentSaldoDelta(iOutpayment, false);
            } else {
                int iIndex = iOutpayments.lastIndexOf(iOutpayment);
                if (iIndex != -1) {
                    SSOutpayment iOldOutpayment = iOutpayments.get(iIndex);
                    iRuntime.applyOutpaymentSaldoDelta(iOldOutpayment, true);
                    iOutpayments.remove(iIndex);
                }
            }
            if (SSSupplierFrame.getInstance() != null) {
                SSSupplierFrame.getInstance().updateFrame();
            }
            if (SSSupplierInvoiceFrame.getInstance() != null) {
                SSSupplierInvoiceFrame.getInstance().updateFrame();
            }
            if (SSOutpaymentFrame.getInstance() != null) {
                SSOutpaymentFrame.getInstance().updateFrame();
            }
            if (SSSupplierCreditInvoiceFrame.getInstance() != null) {
                SSSupplierCreditInvoiceFrame.getInstance().updateFrame();
            }
            return true;
        }

        if (SSTriggerMatcher.isAny(iTriggerName,
                "NEWSUPPLIERINVOICE", "EDITSUPPLIERINVOICE", "DELETESUPPLIERINVOICE")) {
            List<SSSupplierInvoice> iSupplierInvoices = iRuntime.getSupplierInvoices();
            if (iSupplierInvoices == null) {
                return true;
            }
            SSSupplierInvoice iSupplierInvoice = new SSSupplierInvoice();
            iSupplierInvoice.setNumber(Integer.parseInt(iNumber));

            if (iTriggerName.equals("NEWSUPPLIERINVOICE")) {
                Optional<SSSupplierInvoice> optSupplierInvoice = Repositories.supplierInvoices()
                        .findBySupplierInvoice(iSupplierInvoice);
                if (optSupplierInvoice.isEmpty()) {
                    LOG.warn("NEWSUPPLIERINVOICE trigger: entity not found for number {}", iNumber);
                    return true;
                }
                iSupplierInvoice = optSupplierInvoice.get();
                if (!iSupplierInvoices.contains(iSupplierInvoice)) {
                    iSupplierInvoices.add(iSupplierInvoice);
                }
                if (SSSupplierInvoiceMath.iSaldoMap != null) {
                    SSSupplierInvoiceMath.iSaldoMap.put(
                            iSupplierInvoice.getNumber(),
                            SSSupplierInvoiceMath.getSaldo(iSupplierInvoice));
                }
                if (SSSupplierMath.iInvoicesForSuppliers != null) {
                    List<SSSupplierInvoice> iInvoices = SSSupplierMath.iInvoicesForSuppliers.get(
                            iSupplierInvoice.getSupplierNr());
                    if (iInvoices != null && !iInvoices.contains(iSupplierInvoice)) {
                        iInvoices.add(iSupplierInvoice);
                    }
                }
            } else if (iTriggerName.equals("EDITSUPPLIERINVOICE")) {
                Optional<SSSupplierInvoice> optSupplierInvoice = Repositories.supplierInvoices()
                        .findBySupplierInvoice(iSupplierInvoice);
                if (optSupplierInvoice.isEmpty()) {
                    LOG.warn("EDITSUPPLIERINVOICE trigger: entity not found for number {}", iNumber);
                    return true;
                }
                iSupplierInvoice = optSupplierInvoice.get();
                int iIndex = iSupplierInvoices.lastIndexOf(iSupplierInvoice);
                if (iIndex == -1) {
                    return true;
                }
                SSSupplierInvoice iOldSupplierInvoice = iSupplierInvoices.get(iIndex);
                iSupplierInvoices.remove(iIndex);
                iSupplierInvoices.add(iIndex, iSupplierInvoice);
                if (SSSupplierInvoiceMath.iSaldoMap != null) {
                    SSSupplierInvoiceMath.iSaldoMap.put(
                            iSupplierInvoice.getNumber(),
                            SSSupplierInvoiceMath.getSaldo(iSupplierInvoice));
                }
                if (SSSupplierMath.iInvoicesForSuppliers != null) {
                    List<SSSupplierInvoice> iOldSupplierInvoices = SSSupplierMath.iInvoicesForSuppliers.get(
                            iOldSupplierInvoice.getSupplierNr());
                    if (iOldSupplierInvoices != null) {
                        iOldSupplierInvoices.remove(iOldSupplierInvoice);
                    }
                    List<SSSupplierInvoice> iNewSupplierInvoices = SSSupplierMath.iInvoicesForSuppliers.get(
                            iSupplierInvoice.getSupplierNr());
                    if (iNewSupplierInvoices != null && !iNewSupplierInvoices.contains(iSupplierInvoice)) {
                        iNewSupplierInvoices.add(iSupplierInvoice);
                    }
                }
            } else {
                int iIndex = iSupplierInvoices.lastIndexOf(iSupplierInvoice);
                SSSupplierInvoice iRemovedSupplierInvoice =
                        iIndex >= 0 ? iSupplierInvoices.get(iIndex) : iSupplierInvoice;
                iSupplierInvoices.remove(iSupplierInvoice);
                if (SSSupplierInvoiceMath.iSaldoMap != null) {
                    SSSupplierInvoiceMath.iSaldoMap.remove(iSupplierInvoice.getNumber());
                }
                if (SSSupplierMath.iInvoicesForSuppliers != null) {
                    List<SSSupplierInvoice> iInvoices = SSSupplierMath.iInvoicesForSuppliers.get(
                            iRemovedSupplierInvoice.getSupplierNr());
                    if (iInvoices != null) {
                        iInvoices.remove(iRemovedSupplierInvoice);
                    }
                }
            }

            if (SSSupplierFrame.getInstance() != null) {
                SSSupplierFrame.getInstance().updateFrame();
            }
            if (SSSupplierInvoiceFrame.getInstance() != null) {
                SSSupplierInvoiceFrame.getInstance().updateFrame();
            }
            if (SSSupplierCreditInvoiceFrame.getInstance() != null) {
                SSSupplierCreditInvoiceFrame.getInstance().updateFrame();
            }
            return true;
        }

        if (SSTriggerMatcher.isAny(iTriggerName,
                "NEWSUPPLIERCREDITINVOICE", "EDITSUPPLIERCREDITINVOICE", "DELETESUPPLIERCREDITINVOICE")) {
            List<SSSupplierCreditInvoice> iSupplierCreditInvoices = iRuntime.getSupplierCreditInvoices();
            if (iSupplierCreditInvoices == null) {
                return true;
            }
            SSSupplierCreditInvoice iSupplierCreditInvoice = new SSSupplierCreditInvoice();
            iSupplierCreditInvoice.setNumber(Integer.parseInt(iNumber));

            if (iTriggerName.equals("NEWSUPPLIERCREDITINVOICE")) {
                Optional<SSSupplierCreditInvoice> optSupplierCreditInvoice = Repositories.supplierCreditInvoices()
                        .findBySupplierCreditInvoice(iSupplierCreditInvoice);
                if (optSupplierCreditInvoice.isEmpty()) {
                    LOG.warn("NEWSUPPLIERCREDITINVOICE trigger: entity not found for number {}", iNumber);
                    return true;
                }
                iSupplierCreditInvoice = optSupplierCreditInvoice.get();
                if (!iSupplierCreditInvoices.contains(iSupplierCreditInvoice)) {
                    iSupplierCreditInvoices.add(iSupplierCreditInvoice);
                }
                SaldoDeltaService.applySupplierCreditInvoiceNew(iSupplierCreditInvoice);
            } else if (iTriggerName.equals("EDITSUPPLIERCREDITINVOICE")) {
                Optional<SSSupplierCreditInvoice> optSupplierCreditInvoice = Repositories.supplierCreditInvoices()
                        .findBySupplierCreditInvoice(iSupplierCreditInvoice);
                if (optSupplierCreditInvoice.isEmpty()) {
                    LOG.warn("EDITSUPPLIERCREDITINVOICE trigger: entity not found for number {}", iNumber);
                    return true;
                }
                iSupplierCreditInvoice = optSupplierCreditInvoice.get();
                int iIndex = iSupplierCreditInvoices.lastIndexOf(iSupplierCreditInvoice);
                if (iIndex == -1) {
                    return true;
                }
                SSSupplierCreditInvoice iOldSupplierCreditInvoice = iSupplierCreditInvoices.get(iIndex);
                SaldoDeltaService.applySupplierCreditInvoiceEditRevert(iOldSupplierCreditInvoice);
                iSupplierCreditInvoices.remove(iIndex);
                iSupplierCreditInvoices.add(iIndex, iSupplierCreditInvoice);
                SaldoDeltaService.applySupplierCreditInvoiceEditApply(iSupplierCreditInvoice);
            } else {
                int iIndex = iSupplierCreditInvoices.lastIndexOf(iSupplierCreditInvoice);
                SSSupplierCreditInvoice iRemovedSupplierCreditInvoice =
                        iIndex >= 0 ? iSupplierCreditInvoices.get(iIndex) : iSupplierCreditInvoice;
                iSupplierCreditInvoices.remove(iSupplierCreditInvoice);
                SaldoDeltaService.applySupplierCreditInvoiceDeleteRevert(iRemovedSupplierCreditInvoice);
            }

            if (SSSupplierFrame.getInstance() != null) {
                SSSupplierFrame.getInstance().updateFrame();
            }
            if (SSSupplierInvoiceFrame.getInstance() != null) {
                SSSupplierInvoiceFrame.getInstance().updateFrame();
            }
            if (SSSupplierCreditInvoiceFrame.getInstance() != null) {
                SSSupplierCreditInvoiceFrame.getInstance().updateFrame();
            }
            return true;
        }

        return false;
    }
}
