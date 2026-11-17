package se.swedsoft.bookkeeping.importexport.supplierpayments.data;


import se.swedsoft.bookkeeping.calc.math.SSSupplierInvoiceMath;
import se.swedsoft.bookkeeping.data.SSAddress;
import se.swedsoft.bookkeeping.data.SSSupplier;
import se.swedsoft.bookkeeping.data.SSSupplierInvoice;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.util.SSDateUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;


/**
 * User: Andreas Lago
 * Date: 2006-aug-28
 * Time: 11:58:59
 */
public class SupplierPayment {

    private SSSupplierInvoice iInvoice;

    private BigDecimal iValue;

    // betalningskonto
    private String iAccount;

    private PaymentMethod iPaymentMethod;

    private LocalDate iDate;

    private String iCurrency;

    /**
     *
     * @param iInvoice
     */
    public SupplierPayment(SSSupplierInvoice iInvoice) {
        this.iInvoice = iInvoice;
        iPaymentMethod = PaymentMethod.BANKGIRO;
        iValue = SSSupplierInvoiceMath.getTotalSum(iInvoice);
        iAccount = getBankGiro();

        iDate = iInvoice.getLocalDueDate() == null ? SSDateUtil.today() : iInvoice.getLocalDueDate();
        iCurrency = iInvoice.getCurrency() == null
                ? "SEK"
                : iInvoice.getCurrency().getName();
    }

    // //////////////////////////////////////////////////////////////////////////////////////

    /**
     *
     */
    public SSSupplierInvoice getSupplierInvoice() {
        return iInvoice;
    }

    /**
     *
     * @param iInvoice
     */
    public void setSupplierInvoice(SSSupplierInvoice iInvoice) {
        this.iInvoice = iInvoice;
    }

    // //////////////////////////////////////////////////////////////////////////////////////

    /**
     *
     */
    public BigDecimal getValue() {
        if (iValue == null) {
            iValue = new BigDecimal(0);
        }
        return iValue;
    }

    /**
     *
     * @param iValue
     */
    public void setValue(BigDecimal iValue) {
        this.iValue = iValue;
    }

    // //////////////////////////////////////////////////////////////////////////////////////

    /**
     *
     */
    public String getAccount() {
        return iAccount;
    }

    /**
     *
     * @param iAccount
     */
    public void setAccount(String iAccount) {
        this.iAccount = iAccount;
    }

    // //////////////////////////////////////////////////////////////////////////////////////

    /**
     *
     */
    public PaymentMethod getPaymentMethod() {
        return iPaymentMethod;
    }

    /**
     *
     * @param iMethod
     */
    public void setPaymentMethod(PaymentMethod iMethod) {
        iPaymentMethod = iMethod;
    }

    // //////////////////////////////////////////////////////////////////////////////////////

    public LocalDate getLocalDate() {
        return iDate;
    }

    public void setLocalDate(LocalDate iDate) {
        this.iDate = iDate;
    }

    // //////////////////////////////////////////////////////////////////////////////////////

    /**
     *
     */
    public Integer getNumber() {
        return iInvoice.getNumber();
    }

    /**
     *
     */
    public String getReference() {
        return iInvoice.getReferencenumber();
    }

    /**
     *
     */
    public String getCurrency() {
        return iCurrency;
    }

    // //////////////////////////////////////////////////////////////////////////////////////

    /**
     *
     */
    public SSSupplier getSupplier() {
        return iInvoice.getSupplier(se.swedsoft.bookkeeping.data.system.SSPurchaseContext.getSuppliers());
    }

    /**
     *
     */
    public String getBankGiro() {
        SSSupplier iSupplier = iInvoice.getSupplier(se.swedsoft.bookkeeping.data.system.SSPurchaseContext.getSuppliers());

        return iSupplier == null ? null : iSupplier.getBankgiro();
    }

    /**
     *
     */
    public String getPlusGiro() {
        SSSupplier iSupplier = iInvoice.getSupplier(se.swedsoft.bookkeeping.data.system.SSPurchaseContext.getSuppliers());

        return iSupplier == null ? null : iSupplier.getPlusgiro();
    }

    /**
     *
     */
    public String getKonto() {
        SSSupplier iSupplier = iInvoice.getSupplier(se.swedsoft.bookkeeping.data.system.SSPurchaseContext.getSuppliers());

        return iSupplier == null ? null : iSupplier.getBankgiro();
    }

    /**
     *
     */
    public Integer getOutpaymentNumber() {
        SSSupplier iSupplier = iInvoice.getSupplier(se.swedsoft.bookkeeping.data.system.SSPurchaseContext.getSuppliers());

        return iSupplier == null ? null : iSupplier.getOutpaymentNumber();
    }

    /**
     *
     */
    public SSAddress getAddress() {
        SSSupplier iSupplier = iInvoice.getSupplier(se.swedsoft.bookkeeping.data.system.SSPurchaseContext.getSuppliers());

        return iSupplier == null ? null : iSupplier.getAddress();
    }

    /**
     *
     * @param iPayments
     * @param iCurrency
     */
    public static List<SupplierPayment> getPayments(List<SupplierPayment> iPayments, String iCurrency) {
        List<SupplierPayment> iFiltered = new LinkedList<>();

        for (SupplierPayment iPayment : iPayments) {
            if (iCurrency.equals(iPayment.iCurrency)) {
                iFiltered.add(iPayment);
            }
        }
        return iFiltered;
    }

    public boolean equals(Object obj) {
        if (obj instanceof SupplierPayment) {
            SupplierPayment iPayment = (SupplierPayment) obj;

            return iPayment.iInvoice.equals(iInvoice);
        }
        return false;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append(
                "se.swedsoft.bookkeeping.importexport.supplierpayments.data.SupplierPayment");
        sb.append("{iAccount='").append(iAccount).append('\'');
        sb.append(", iCurrency='").append(iCurrency).append('\'');
        sb.append(", iDate=").append(iDate);
        sb.append(", iInvoice=").append(iInvoice);
        sb.append(", iPaymentMethod=").append(iPaymentMethod);
        sb.append(", iValue=").append(iValue);
        sb.append('}');
        return sb.toString();
    }
}
