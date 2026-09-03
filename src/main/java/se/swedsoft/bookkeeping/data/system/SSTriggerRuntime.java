package se.swedsoft.bookkeeping.data.system;

import se.swedsoft.bookkeeping.calc.service.SaldoDeltaService;
import se.swedsoft.bookkeeping.data.SSAutoDist;
import se.swedsoft.bookkeeping.data.SSCreditInvoice;
import se.swedsoft.bookkeeping.data.SSCustomer;
import se.swedsoft.bookkeeping.data.SSIndelivery;
import se.swedsoft.bookkeeping.data.SSInpayment;
import se.swedsoft.bookkeeping.data.SSInventory;
import se.swedsoft.bookkeeping.data.SSInvoice;
import se.swedsoft.bookkeeping.data.SSOrder;
import se.swedsoft.bookkeeping.data.SSOwnReport;
import se.swedsoft.bookkeeping.data.SSOutdelivery;
import se.swedsoft.bookkeeping.data.SSOutpayment;
import se.swedsoft.bookkeeping.data.SSPeriodicInvoice;
import se.swedsoft.bookkeeping.data.SSProduct;
import se.swedsoft.bookkeeping.data.SSPurchaseOrder;
import se.swedsoft.bookkeeping.data.SSSupplier;
import se.swedsoft.bookkeeping.data.SSSupplierCreditInvoice;
import se.swedsoft.bookkeeping.data.SSSupplierInvoice;
import se.swedsoft.bookkeeping.data.SSTender;
import se.swedsoft.bookkeeping.data.SSVoucher;

import java.util.List;

/**
 * Provides trigger handlers with controlled access to SSDB cache lists and
 * saldo helpers, decoupling them from direct SSDB coupling.
 *
 * <p>Instances are created by SSDB and passed into {@link se.swedsoft.bookkeeping.data.system.trigger.SSEventTriggerDispatcher}.
 * All list accessors return live references that may be {@code null} when the
 * cache has not yet been populated.</p>
 */
public final class SSTriggerRuntime {

    private final SSDB iDatabase;

    public SSTriggerRuntime(SSDB pDatabase) {
        iDatabase = pDatabase;
    }

    // -------------------------------------------------------------------------
    // Cache list accessors (live references — may be null before first load)
    // -------------------------------------------------------------------------

    public List<SSProduct> getProducts() { return iDatabase.iProducts; }
    public void setProducts(List<SSProduct> pList) { iDatabase.iProducts = pList; }

    public List<SSCustomer> getCustomers() { return iDatabase.iCustomers; }
    public void setCustomers(List<SSCustomer> pList) { iDatabase.iCustomers = pList; }

    public List<SSSupplier> getSuppliers() { return iDatabase.iSuppliers; }
    public void setSuppliers(List<SSSupplier> pList) { iDatabase.iSuppliers = pList; }

    public List<SSAutoDist> getAutoDists() { return iDatabase.iAutoDists; }
    public void setAutoDists(List<SSAutoDist> pList) { iDatabase.iAutoDists = pList; }

    public List<SSTender> getTenders() { return iDatabase.iTenders; }
    public void setTenders(List<SSTender> pList) { iDatabase.iTenders = pList; }

    public List<SSOrder> getOrders() { return iDatabase.iOrders; }
    public void setOrders(List<SSOrder> pList) { iDatabase.iOrders = pList; }

    public List<SSInvoice> getInvoices() { return iDatabase.iInvoices; }
    public void setInvoices(List<SSInvoice> pList) { iDatabase.iInvoices = pList; }

    public List<SSCreditInvoice> getCreditInvoices() { return iDatabase.iCreditInvoices; }
    public void setCreditInvoices(List<SSCreditInvoice> pList) { iDatabase.iCreditInvoices = pList; }

    public List<SSPeriodicInvoice> getPeriodicInvoices() { return iDatabase.iPeriodicInvoices; }
    public void setPeriodicInvoices(List<SSPeriodicInvoice> pList) { iDatabase.iPeriodicInvoices = pList; }

    public List<SSInpayment> getInpayments() { return iDatabase.iInpayments; }
    public void setInpayments(List<SSInpayment> pList) { iDatabase.iInpayments = pList; }

    public List<SSOutpayment> getOutpayments() { return iDatabase.iOutpayments; }
    public void setOutpayments(List<SSOutpayment> pList) { iDatabase.iOutpayments = pList; }

    public List<SSPurchaseOrder> getPurchaseOrders() { return iDatabase.iPurchaseOrders; }
    public void setPurchaseOrders(List<SSPurchaseOrder> pList) { iDatabase.iPurchaseOrders = pList; }

    public List<SSSupplierInvoice> getSupplierInvoices() { return iDatabase.iSupplierInvoices; }
    public void setSupplierInvoices(List<SSSupplierInvoice> pList) { iDatabase.iSupplierInvoices = pList; }

    public List<SSSupplierCreditInvoice> getSupplierCreditInvoices() { return iDatabase.iSupplierCreditInvoices; }
    public void setSupplierCreditInvoices(List<SSSupplierCreditInvoice> pList) { iDatabase.iSupplierCreditInvoices = pList; }

    public List<SSInventory> getInventories() { return iDatabase.iInventories; }
    public void setInventories(List<SSInventory> pList) { iDatabase.iInventories = pList; }

    public List<SSIndelivery> getIndeliveries() { return iDatabase.iIndeliveries; }
    public void setIndeliveries(List<SSIndelivery> pList) { iDatabase.iIndeliveries = pList; }

    public List<SSOutdelivery> getOutdeliveries() { return iDatabase.iOutdeliveries; }
    public void setOutdeliveries(List<SSOutdelivery> pList) { iDatabase.iOutdeliveries = pList; }

    public List<SSVoucher> getVouchers() { return iDatabase.iVouchers; }
    public void setVouchers(List<SSVoucher> pList) { iDatabase.iVouchers = pList; }

    public List<SSOwnReport> getOwnReports() { return iDatabase.iOwnReports; }
    public void setOwnReports(List<SSOwnReport> pList) { iDatabase.iOwnReports = pList; }

    // -------------------------------------------------------------------------
    // Saldo helpers
    // -------------------------------------------------------------------------

    public void applyInpaymentSaldoDelta(SSInpayment pInpayment, boolean pAddToSaldo) {
        SaldoDeltaService.applyInpaymentDelta(pInpayment, pAddToSaldo);
    }

    public void applyOutpaymentSaldoDelta(SSOutpayment pOutpayment, boolean pAddToSaldo) {
        SaldoDeltaService.applyOutpaymentDelta(pOutpayment, pAddToSaldo);
    }
}
