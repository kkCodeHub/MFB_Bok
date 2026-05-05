package se.swedsoft.bookkeeping.persistence;

import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.persistence.legacy.SSDBAccountPlanRepository;
import se.swedsoft.bookkeeping.persistence.legacy.SSDBAccountingYearRepository;
import se.swedsoft.bookkeeping.persistence.legacy.SSDBCreditInvoiceRepository;
import se.swedsoft.bookkeeping.persistence.legacy.SSDBCustomerRepository;
import se.swedsoft.bookkeeping.persistence.legacy.SSDBInvoiceRepository;
import se.swedsoft.bookkeeping.persistence.legacy.SSDBOrderRepository;
import se.swedsoft.bookkeeping.persistence.legacy.SSDBPeriodicInvoiceRepository;
import se.swedsoft.bookkeeping.persistence.legacy.SSDBProductRepository;
import se.swedsoft.bookkeeping.persistence.legacy.SSDBPurchaseOrderRepository;
import se.swedsoft.bookkeeping.persistence.legacy.SSDBSupplierRepository;
import se.swedsoft.bookkeeping.persistence.legacy.SSDBSupplierCreditInvoiceRepository;
import se.swedsoft.bookkeeping.persistence.legacy.SSDBSupplierInvoiceRepository;
import se.swedsoft.bookkeeping.persistence.legacy.SSDBTenderRepository;
import se.swedsoft.bookkeeping.persistence.legacy.SSDBVoucherRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2AccountPlanRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2AccountingYearRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2CreditInvoiceRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2CustomerRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2InvoiceRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2OrderRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2PeriodicInvoiceRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2ProductRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2PurchaseOrderRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2SupplierRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2SupplierCreditInvoiceRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2SupplierInvoiceRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2TenderRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2VoucherRepository;

/**
 * Central access point for repository instances.
 *
 * <p>All repository instances are created once via {@link #init(SSDB)} and
 * then available through static getters.  Call sites should obtain
 * repositories through this class rather than instantiating implementations
 * directly.</p>
 *
 * <p>When {@code fribok.schema.version=v2} the factory wires in the
 * {@link V2CustomerRepository}, {@link V2ProductRepository} and
 * {@link V2SupplierRepository} implementations.  Otherwise the legacy
 * SSDB-delegating implementations are used.</p>
 *
 * <p>Usage during application startup:</p>
 * <pre>{@code
 *   Repositories.init(SSDB.getInstance());
 * }</pre>
 *
 * <p>Usage in application code:</p>
 * <pre>{@code
 *   List<SSCustomer> customers = Repositories.customers().findAll();
 * }</pre>
 */
public final class Repositories {

    private static final String SCHEMA_PROPERTY = "fribok.schema.version";

    private static CustomerRepository customerRepository;
    private static CreditInvoiceRepository creditInvoiceRepository;
    private static InvoiceRepository invoiceRepository;
    private static OrderRepository orderRepository;
    private static PeriodicInvoiceRepository periodicInvoiceRepository;
    private static ProductRepository productRepository;
    private static PurchaseOrderRepository purchaseOrderRepository;
    private static SupplierRepository supplierRepository;
    private static SupplierInvoiceRepository supplierInvoiceRepository;
    private static SupplierCreditInvoiceRepository supplierCreditInvoiceRepository;
    private static TenderRepository tenderRepository;
    private static AccountPlanRepository accountPlanRepository;
    private static VoucherRepository voucherRepository;
    private static AccountingYearRepository accountingYearRepository;

    private Repositories() {
        // utility class
    }

    /**
     * Returns {@code true} when the V2 schema is active.
     *
     * @return {@code true} if {@code fribok.schema.version=v2}
     */
    public static boolean isSchemaV2() {
        return "v2".equalsIgnoreCase(System.getProperty(SCHEMA_PROPERTY, "v1"));
    }

    /**
     * Initialises all repository instances backed by the given {@link SSDB}.
     *
     * <p>When {@code fribok.schema.version=v2} the V2 repository implementations
     * are used; otherwise the legacy SSDB-delegating ones are created.
     * Must be called once before any getter is used.
     * Calling again replaces the existing instances.</p>
     *
     * @param db the SSDB instance; must not be {@code null}
     */
    public static void init(SSDB db) {
        if (db == null) {
            throw new NullPointerException("db must not be null");
        }
        if (isSchemaV2()) {
            customerRepository = new V2CustomerRepository(db);
            creditInvoiceRepository = new V2CreditInvoiceRepository(db);
            invoiceRepository = new V2InvoiceRepository(db);
            orderRepository = new V2OrderRepository(db);
            periodicInvoiceRepository = new V2PeriodicInvoiceRepository(db);
            productRepository = new V2ProductRepository(db);
            purchaseOrderRepository = new V2PurchaseOrderRepository(db);
            supplierRepository = new V2SupplierRepository(db);
            supplierInvoiceRepository = new V2SupplierInvoiceRepository(db);
            supplierCreditInvoiceRepository = new V2SupplierCreditInvoiceRepository(db);
            tenderRepository = new V2TenderRepository(db);
            accountPlanRepository = new V2AccountPlanRepository(db);
            voucherRepository = new V2VoucherRepository(db);
            accountingYearRepository = new V2AccountingYearRepository(db);
        } else {
            customerRepository = new SSDBCustomerRepository(db);
            creditInvoiceRepository = new SSDBCreditInvoiceRepository(db);
            invoiceRepository = new SSDBInvoiceRepository(db);
            orderRepository = new SSDBOrderRepository(db);
            periodicInvoiceRepository = new SSDBPeriodicInvoiceRepository(db);
            productRepository = new SSDBProductRepository(db);
            purchaseOrderRepository = new SSDBPurchaseOrderRepository(db);
            supplierRepository = new SSDBSupplierRepository(db);
            supplierInvoiceRepository = new SSDBSupplierInvoiceRepository(db);
            supplierCreditInvoiceRepository = new SSDBSupplierCreditInvoiceRepository(db);
            tenderRepository = new SSDBTenderRepository(db);
            accountPlanRepository = new SSDBAccountPlanRepository(db);
            voucherRepository = new SSDBVoucherRepository(db);
            accountingYearRepository = new SSDBAccountingYearRepository(db);
        }
    }

    /**
     * Returns the {@link CustomerRepository}.
     *
     * @return the customer repository; never {@code null} after {@link #init}
     * @throws IllegalStateException if {@link #init} has not been called
     */
    public static CustomerRepository customers() {
        if (customerRepository == null) {
            throw new IllegalStateException("Repositories.init() has not been called");
        }
        return customerRepository;
    }

    /**
     * Returns the {@link CreditInvoiceRepository}.
     *
     * @return the credit-invoice repository; never {@code null} after {@link #init}
     * @throws IllegalStateException if {@link #init} has not been called
     */
    public static CreditInvoiceRepository creditInvoices() {
        if (creditInvoiceRepository == null) {
            throw new IllegalStateException("Repositories.init() has not been called");
        }
        return creditInvoiceRepository;
    }

    /**
     * Returns the {@link InvoiceRepository}.
     *
     * @return the invoice repository; never {@code null} after {@link #init}
     * @throws IllegalStateException if {@link #init} has not been called
     */
    public static InvoiceRepository invoices() {
        if (invoiceRepository == null) {
            throw new IllegalStateException("Repositories.init() has not been called");
        }
        return invoiceRepository;
    }

    /**
     * Returns the {@link OrderRepository}.
     *
     * @return the order repository; never {@code null} after {@link #init}
     * @throws IllegalStateException if {@link #init} has not been called
     */
    public static OrderRepository orders() {
        if (orderRepository == null) {
            throw new IllegalStateException("Repositories.init() has not been called");
        }
        return orderRepository;
    }

    /**
     * Returns the {@link PeriodicInvoiceRepository}.
     *
     * @return the periodic-invoice repository; never {@code null} after {@link #init}
     * @throws IllegalStateException if {@link #init} has not been called
     */
    public static PeriodicInvoiceRepository periodicInvoices() {
        if (periodicInvoiceRepository == null) {
            throw new IllegalStateException("Repositories.init() has not been called");
        }
        return periodicInvoiceRepository;
    }

    /**
     * Returns the {@link ProductRepository}.
     *
     * @return the product repository; never {@code null} after {@link #init}
     * @throws IllegalStateException if {@link #init} has not been called
     */
    public static ProductRepository products() {
        if (productRepository == null) {
            throw new IllegalStateException("Repositories.init() has not been called");
        }
        return productRepository;
    }

    /**
     * Returns the {@link PurchaseOrderRepository}.
     *
     * @return the purchase-order repository; never {@code null} after {@link #init}
     * @throws IllegalStateException if {@link #init} has not been called
     */
    public static PurchaseOrderRepository purchaseOrders() {
        if (purchaseOrderRepository == null) {
            throw new IllegalStateException("Repositories.init() has not been called");
        }
        return purchaseOrderRepository;
    }

    /**
     * Returns the {@link SupplierRepository}.
     *
     * @return the supplier repository; never {@code null} after {@link #init}
     * @throws IllegalStateException if {@link #init} has not been called
     */
    public static SupplierRepository suppliers() {
        if (supplierRepository == null) {
            throw new IllegalStateException("Repositories.init() has not been called");
        }
        return supplierRepository;
    }

    /**
     * Returns the {@link SupplierInvoiceRepository}.
     *
     * @return the supplier-invoice repository; never {@code null} after {@link #init}
     * @throws IllegalStateException if {@link #init} has not been called
     */
    public static SupplierInvoiceRepository supplierInvoices() {
        if (supplierInvoiceRepository == null) {
            throw new IllegalStateException("Repositories.init() has not been called");
        }
        return supplierInvoiceRepository;
    }

    /**
     * Returns the {@link SupplierCreditInvoiceRepository}.
     *
     * @return the supplier-credit-invoice repository; never {@code null} after {@link #init}
     * @throws IllegalStateException if {@link #init} has not been called
     */
    public static SupplierCreditInvoiceRepository supplierCreditInvoices() {
        if (supplierCreditInvoiceRepository == null) {
            throw new IllegalStateException("Repositories.init() has not been called");
        }
        return supplierCreditInvoiceRepository;
    }

    /**
     * Returns the {@link TenderRepository}.
     *
     * @return the tender repository; never {@code null} after {@link #init}
     * @throws IllegalStateException if {@link #init} has not been called
     */
    public static TenderRepository tenders() {
        if (tenderRepository == null) {
            throw new IllegalStateException("Repositories.init() has not been called");
        }
        return tenderRepository;
    }

    /**
     * Returns the {@link AccountPlanRepository}.
     *
     * @return the account-plan repository; never {@code null} after {@link #init}
     * @throws IllegalStateException if {@link #init} has not been called
     */
    public static AccountPlanRepository accountPlans() {
        if (accountPlanRepository == null) {
            throw new IllegalStateException("Repositories.init() has not been called");
        }
        return accountPlanRepository;
    }

    /**
     * Returns the {@link VoucherRepository}.
     *
     * @return the voucher repository; never {@code null} after {@link #init}
     * @throws IllegalStateException if {@link #init} has not been called
     */
    public static VoucherRepository vouchers() {
        if (voucherRepository == null) {
            throw new IllegalStateException("Repositories.init() has not been called");
        }
        return voucherRepository;
    }

    /**
     * Returns the {@link AccountingYearRepository}.
     *
     * @return the accounting-year repository; never {@code null} after {@link #init}
     * @throws IllegalStateException if {@link #init} has not been called
     */
    public static AccountingYearRepository accountingYears() {
        if (accountingYearRepository == null) {
            throw new IllegalStateException("Repositories.init() has not been called");
        }
        return accountingYearRepository;
    }
}

