package se.swedsoft.bookkeeping.persistence;

import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.persistence.legacy.SSDBAccountPlanRepository;
import se.swedsoft.bookkeeping.persistence.legacy.SSDBAccountingYearRepository;
import se.swedsoft.bookkeeping.persistence.legacy.SSDBCustomerRepository;
import se.swedsoft.bookkeeping.persistence.legacy.SSDBInpaymentRepository;
import se.swedsoft.bookkeeping.persistence.legacy.SSDBInventoryRepository;
import se.swedsoft.bookkeeping.persistence.legacy.SSDBInvoiceRepository;
import se.swedsoft.bookkeeping.persistence.legacy.SSDBOutdeliveryRepository;
import se.swedsoft.bookkeeping.persistence.legacy.SSDBOutpaymentRepository;
import se.swedsoft.bookkeeping.persistence.legacy.SSDBProductRepository;
import se.swedsoft.bookkeeping.persistence.legacy.SSDBSupplierRepository;
import se.swedsoft.bookkeeping.persistence.legacy.SSDBVoucherRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2AccountPlanRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2AccountingYearRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2AutoDistRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2CreditInvoiceRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2CustomerRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2IndeliveryRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2InpaymentRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2InventoryRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2InvoiceRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2OwnReportRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2OrderRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2OutdeliveryRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2OutpaymentRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2PeriodicInvoiceRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2ProductRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2PurchaseOrderRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2SupplierRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2SupplierCreditInvoiceRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2SupplierInvoiceRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2TenderRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2VoucherRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2VoucherTemplateRepository;

/**
 * Central access point for repository instances.
 *
 * <p>All repository instances are created once via {@link #init(SSDB)} and
 * then available through static getters.  Call sites should obtain
 * repositories through this class rather than instantiating implementations
 * directly.</p>
 *
 * <p>When {@code fribok.schema.version=v2} the factory wires in the V2
 * repository implementations. For cut-over domains ({@code AutoDist},
 * {@code VoucherTemplate}, {@code OwnReport}, {@code SupplierInvoice},
 * {@code Tender}, {@code PeriodicInvoice}, {@code CreditInvoice},
 * {@code SupplierCreditInvoice}, {@code PurchaseOrder}, {@code Order} and
 * {@code Indelivery})
 * V2 repositories are always used regardless of schema flag. Other domains
 * still switch between V2 and legacy SSDB-delegating implementations.</p>
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
    private static AutoDistRepository autoDistRepository;
    private static CreditInvoiceRepository creditInvoiceRepository;
    private static IndeliveryRepository indeliveryRepository;
    private static InpaymentRepository inpaymentRepository;
    private static InventoryRepository inventoryRepository;
    private static InvoiceRepository invoiceRepository;
    private static OwnReportRepository ownReportRepository;
    private static OrderRepository orderRepository;
    private static OutdeliveryRepository outdeliveryRepository;
    private static OutpaymentRepository outpaymentRepository;
    private static PeriodicInvoiceRepository periodicInvoiceRepository;
    private static ProductRepository productRepository;
    private static PurchaseOrderRepository purchaseOrderRepository;
    private static SupplierRepository supplierRepository;
    private static SupplierInvoiceRepository supplierInvoiceRepository;
    private static SupplierCreditInvoiceRepository supplierCreditInvoiceRepository;
    private static TenderRepository tenderRepository;
    private static AccountPlanRepository accountPlanRepository;
    private static VoucherRepository voucherRepository;
    private static VoucherTemplateRepository voucherTemplateRepository;
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
     * are used. For cut-over domains ({@code AutoDist},
     * {@code VoucherTemplate}, {@code OwnReport}, {@code SupplierInvoice},
     * {@code Tender}, {@code PeriodicInvoice}, {@code CreditInvoice},
     * {@code SupplierCreditInvoice}, {@code PurchaseOrder}, {@code Order},
     * {@code Indelivery}) V2
     * implementations are always created as part of Slice P cutover; otherwise
     * legacy SSDB-delegating ones are created. Must be called once before any
     * getter is used.
     * Calling again replaces the existing instances.</p>
     *
     * @param db the SSDB instance; must not be {@code null}
     */
    public static void init(SSDB db) {
        if (db == null) {
            throw new NullPointerException("db must not be null");
        }
        // Migrated H domains are V2-only in Slice P.
        autoDistRepository = new V2AutoDistRepository(db);
        creditInvoiceRepository = new V2CreditInvoiceRepository(db);
        ownReportRepository = new V2OwnReportRepository(db);
        periodicInvoiceRepository = new V2PeriodicInvoiceRepository(db);
        indeliveryRepository = new V2IndeliveryRepository(db);
        orderRepository = new V2OrderRepository(db);
        purchaseOrderRepository = new V2PurchaseOrderRepository(db);
        supplierCreditInvoiceRepository = new V2SupplierCreditInvoiceRepository(db);
        supplierInvoiceRepository = new V2SupplierInvoiceRepository(db);
        tenderRepository = new V2TenderRepository(db);
        voucherTemplateRepository = new V2VoucherTemplateRepository(db);

        if (isSchemaV2()) {
            customerRepository = new V2CustomerRepository(db);
            inpaymentRepository = new V2InpaymentRepository(db);
            inventoryRepository = new V2InventoryRepository(db);
            invoiceRepository = new V2InvoiceRepository(db);
            outdeliveryRepository = new V2OutdeliveryRepository(db);
            outpaymentRepository = new V2OutpaymentRepository(db);
            productRepository = new V2ProductRepository(db);
            supplierRepository = new V2SupplierRepository(db);
            accountPlanRepository = new V2AccountPlanRepository(db);
            voucherRepository = new V2VoucherRepository(db);
            accountingYearRepository = new V2AccountingYearRepository(db);
        } else {
            customerRepository = new SSDBCustomerRepository(db);
            inpaymentRepository = new SSDBInpaymentRepository(db);
            inventoryRepository = new SSDBInventoryRepository(db);
            invoiceRepository = new SSDBInvoiceRepository(db);
            outdeliveryRepository = new SSDBOutdeliveryRepository(db);
            outpaymentRepository = new SSDBOutpaymentRepository(db);
            productRepository = new SSDBProductRepository(db);
            supplierRepository = new SSDBSupplierRepository(db);
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
     * Returns the {@link AutoDistRepository}.
     *
     * @return the auto-dist repository; never {@code null} after {@link #init}
     * @throws IllegalStateException if {@link #init} has not been called
     */
    public static AutoDistRepository autoDists() {
        if (autoDistRepository == null) {
            throw new IllegalStateException("Repositories.init() has not been called");
        }
        return autoDistRepository;
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
     * Returns the {@link IndeliveryRepository}.
     *
     * @return the indelivery repository; never {@code null} after {@link #init}
     * @throws IllegalStateException if {@link #init} has not been called
     */
    public static IndeliveryRepository indeliveries() {
        if (indeliveryRepository == null) {
            throw new IllegalStateException("Repositories.init() has not been called");
        }
        return indeliveryRepository;
    }

    /**
     * Returns the {@link InpaymentRepository}.
     *
     * @return the inpayment repository; never {@code null} after {@link #init}
     * @throws IllegalStateException if {@link #init} has not been called
     */
    public static InpaymentRepository inpayments() {
        if (inpaymentRepository == null) {
            throw new IllegalStateException("Repositories.init() has not been called");
        }
        return inpaymentRepository;
    }

    /**
     * Returns the {@link InventoryRepository}.
     *
     * @return the inventory repository; never {@code null} after {@link #init}
     * @throws IllegalStateException if {@link #init} has not been called
     */
    public static InventoryRepository inventories() {
        if (inventoryRepository == null) {
            throw new IllegalStateException("Repositories.init() has not been called");
        }
        return inventoryRepository;
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
     * Returns the {@link OwnReportRepository}.
     *
     * @return the own-report repository; never {@code null} after {@link #init}
     * @throws IllegalStateException if {@link #init} has not been called
     */
    public static OwnReportRepository ownReports() {
        if (ownReportRepository == null) {
            throw new IllegalStateException("Repositories.init() has not been called");
        }
        return ownReportRepository;
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
     * Returns the {@link OutdeliveryRepository}.
     *
     * @return the outdelivery repository; never {@code null} after {@link #init}
     * @throws IllegalStateException if {@link #init} has not been called
     */
    public static OutdeliveryRepository outdeliveries() {
        if (outdeliveryRepository == null) {
            throw new IllegalStateException("Repositories.init() has not been called");
        }
        return outdeliveryRepository;
    }

    /**
     * Returns the {@link OutpaymentRepository}.
     *
     * @return the outpayment repository; never {@code null} after {@link #init}
     * @throws IllegalStateException if {@link #init} has not been called
     */
    public static OutpaymentRepository outpayments() {
        if (outpaymentRepository == null) {
            throw new IllegalStateException("Repositories.init() has not been called");
        }
        return outpaymentRepository;
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
     * Returns the {@link VoucherTemplateRepository}.
     *
     * @return the voucher-template repository; never {@code null} after {@link #init}
     * @throws IllegalStateException if {@link #init} has not been called
     */
    public static VoucherTemplateRepository voucherTemplates() {
        if (voucherTemplateRepository == null) {
            throw new IllegalStateException("Repositories.init() has not been called");
        }
        return voucherTemplateRepository;
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

