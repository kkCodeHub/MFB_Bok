package se.swedsoft.bookkeeping.calc.service;

import se.swedsoft.bookkeeping.calc.math.SSInvoiceMath;
import se.swedsoft.bookkeeping.calc.math.SSCreditInvoiceMath;
import se.swedsoft.bookkeeping.calc.math.SSSupplierInvoiceMath;
import se.swedsoft.bookkeeping.data.SSCreditInvoice;
import se.swedsoft.bookkeeping.data.SSInpayment;
import se.swedsoft.bookkeeping.data.SSInpaymentRow;
import se.swedsoft.bookkeeping.data.SSOutpayment;
import se.swedsoft.bookkeeping.data.SSOutpaymentRow;
import se.swedsoft.bookkeeping.data.SSSupplierCreditInvoice;

import java.math.BigDecimal;

/**
 * Centralized saldo-delta updates for payment and credit-invoice trigger flows.
 */
public final class SaldoDeltaService {

    private SaldoDeltaService() {
    }

    public static void applyInpaymentDelta(SSInpayment iInpayment, boolean iAddToSaldo) {
        if (iInpayment == null || SSInvoiceMath.iSaldoMap == null) {
            return;
        }
        for (SSInpaymentRow iRow : iInpayment.getRows()) {
            if (iRow.getValue() == null || iRow.getInvoiceNr() == null) {
                continue;
            }
            BigDecimal iCurrent = SSInvoiceMath.iSaldoMap.get(iRow.getInvoiceNr());
            if (iCurrent == null) {
                continue;
            }
            SSInvoiceMath.iSaldoMap.put(iRow.getInvoiceNr(),
                    iAddToSaldo ? iCurrent.add(iRow.getValue()) : iCurrent.subtract(iRow.getValue()));
        }
    }

    public static void applyOutpaymentDelta(SSOutpayment iOutpayment, boolean iAddToSaldo) {
        if (iOutpayment == null || SSSupplierInvoiceMath.iSaldoMap == null) {
            return;
        }
        for (SSOutpaymentRow iRow : iOutpayment.getRows()) {
            if (iRow.getValue() == null || iRow.getInvoiceNr() == null) {
                continue;
            }
            BigDecimal iCurrent = SSSupplierInvoiceMath.iSaldoMap.get(iRow.getInvoiceNr());
            if (iCurrent == null) {
                continue;
            }
            SSSupplierInvoiceMath.iSaldoMap.put(iRow.getInvoiceNr(),
                    iAddToSaldo ? iCurrent.add(iRow.getValue()) : iCurrent.subtract(iRow.getValue()));
        }
    }

    public static void applyCustomerCreditInvoiceNew(SSCreditInvoice iCreditInvoice) {
        if (SSInvoiceMath.iSaldoMap.containsKey(iCreditInvoice.getCreditingNr())) {
            SSInvoiceMath.iSaldoMap.put(iCreditInvoice.getCreditingNr(),
                    SSInvoiceMath.iSaldoMap.get(iCreditInvoice.getCreditingNr())
                            .subtract(SSCreditInvoiceMath.getTotalSum(iCreditInvoice)));
        }
    }

    public static void applyCustomerCreditInvoiceEditRevert(SSCreditInvoice iOldCreditInvoice) {
        if (SSInvoiceMath.iSaldoMap.containsKey(iOldCreditInvoice.getCreditingNr())) {
            SSInvoiceMath.iSaldoMap.put(iOldCreditInvoice.getCreditingNr(),
                    SSInvoiceMath.iSaldoMap.get(iOldCreditInvoice.getCreditingNr())
                            .add(SSCreditInvoiceMath.getTotalSum(iOldCreditInvoice)));
        }
    }

    public static void applyCustomerCreditInvoiceEditApply(SSCreditInvoice iCreditInvoice) {
        if (SSInvoiceMath.iSaldoMap.containsKey(iCreditInvoice.getCreditingNr())) {
            SSInvoiceMath.iSaldoMap.put(iCreditInvoice.getCreditingNr(),
                    SSInvoiceMath.iSaldoMap.get(iCreditInvoice.getCreditingNr())
                            .subtract(SSCreditInvoiceMath.getTotalSum(iCreditInvoice)));
        }
    }

    public static void applySupplierCreditInvoiceNew(SSSupplierCreditInvoice iSupplierCreditInvoice) {
        if (SSSupplierInvoiceMath.iSaldoMap != null
                && iSupplierCreditInvoice.getCreditingNr() != null
                && SSSupplierInvoiceMath.iSaldoMap.containsKey(iSupplierCreditInvoice.getCreditingNr())) {
            SSSupplierInvoiceMath.iSaldoMap.put(
                    iSupplierCreditInvoice.getCreditingNr(),
                    SSSupplierInvoiceMath.iSaldoMap.get(iSupplierCreditInvoice.getCreditingNr())
                            .subtract(SSSupplierInvoiceMath.getTotalSum(iSupplierCreditInvoice)));
        }
    }

    public static void applySupplierCreditInvoiceEditRevert(SSSupplierCreditInvoice iOldSupplierCreditInvoice) {
        if (SSSupplierInvoiceMath.iSaldoMap != null
                && iOldSupplierCreditInvoice.getCreditingNr() != null
                && SSSupplierInvoiceMath.iSaldoMap.containsKey(iOldSupplierCreditInvoice.getCreditingNr())) {
            SSSupplierInvoiceMath.iSaldoMap.put(
                    iOldSupplierCreditInvoice.getCreditingNr(),
                    SSSupplierInvoiceMath.iSaldoMap.get(iOldSupplierCreditInvoice.getCreditingNr())
                            .add(SSSupplierInvoiceMath.getTotalSum(iOldSupplierCreditInvoice)));
        }
    }

    public static void applySupplierCreditInvoiceEditApply(SSSupplierCreditInvoice iSupplierCreditInvoice) {
        if (SSSupplierInvoiceMath.iSaldoMap != null
                && iSupplierCreditInvoice.getCreditingNr() != null
                && SSSupplierInvoiceMath.iSaldoMap.containsKey(iSupplierCreditInvoice.getCreditingNr())) {
            SSSupplierInvoiceMath.iSaldoMap.put(
                    iSupplierCreditInvoice.getCreditingNr(),
                    SSSupplierInvoiceMath.iSaldoMap.get(iSupplierCreditInvoice.getCreditingNr())
                            .subtract(SSSupplierInvoiceMath.getTotalSum(iSupplierCreditInvoice)));
        }
    }

    public static void applySupplierCreditInvoiceDeleteRevert(SSSupplierCreditInvoice iRemovedSupplierCreditInvoice) {
        if (SSSupplierInvoiceMath.iSaldoMap != null
                && iRemovedSupplierCreditInvoice.getCreditingNr() != null
                && SSSupplierInvoiceMath.iSaldoMap.containsKey(iRemovedSupplierCreditInvoice.getCreditingNr())) {
            SSSupplierInvoiceMath.iSaldoMap.put(
                    iRemovedSupplierCreditInvoice.getCreditingNr(),
                    SSSupplierInvoiceMath.iSaldoMap.get(iRemovedSupplierCreditInvoice.getCreditingNr())
                            .add(SSSupplierInvoiceMath.getTotalSum(iRemovedSupplierCreditInvoice)));
        }
    }
}


