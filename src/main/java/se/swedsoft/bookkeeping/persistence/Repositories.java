package se.swedsoft.bookkeeping.persistence;

import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.data.system.SSCompanyYearContext;
import se.swedsoft.bookkeeping.data.system.SSSystemConfigContext;
import se.swedsoft.bookkeeping.persistence.v2.V2AccountPlanRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2AccountingYearRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2AutoDistRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2CreditInvoiceRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2CurrencyRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2CustomerRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2DeliveryTermRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2DeliveryWayRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2IndeliveryRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2InpaymentRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2InventoryRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2InvoiceRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2OwnReportRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2OrderRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2OutdeliveryRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2OutpaymentRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2PaymentTermRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2PeriodicInvoiceRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2ProductRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2PurchaseOrderRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2SupplierRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2SupplierCreditInvoiceRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2SupplierInvoiceRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2TenderRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2UnitRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2CompanyRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2ProjectRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2ResultUnitRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2VoucherRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2VoucherTemplateRepository;

import java.lang.reflect.Proxy;
import java.sql.Connection;

/**
 * Central access point for repository instances.
 *
 * <p>All repository instances are created once via {@link #init(SSDB)} and
 * then available through static getters.  Call sites should obtain
 * repositories through this class rather than instantiating implementations
 * directly.</p>
 *
 * <p>In V2-only mode the factory always wires V2 repository implementations
 * for all domains.</p>
 *
 * <p>Usage during application startup:</p>
 * <pre>{@code
 *   Repositories.init(SSSystemConfigContext.getDatabase());
 * }</pre>
 *
 * <p>Usage in application code:</p>
 * <pre>{@code
 *   List<SSCustomer> customers = Repositories.customers().findAll();
 * }</pre>
 */
public final class Repositories {

    private static final Connection NO_CONNECTION = createNoConnection();

    private static V2CustomerRepository customerRepository;
    private static V2AutoDistRepository autoDistRepository;
    private static V2CreditInvoiceRepository creditInvoiceRepository;
    private static V2CurrencyRepository currencyRepository;
    private static V2DeliveryTermRepository deliveryTermRepository;
    private static V2DeliveryWayRepository deliveryWayRepository;
    private static V2IndeliveryRepository indeliveryRepository;
    private static V2InpaymentRepository inpaymentRepository;
    private static V2InventoryRepository inventoryRepository;
    private static V2InvoiceRepository invoiceRepository;
    private static V2OwnReportRepository ownReportRepository;
    private static V2OrderRepository orderRepository;
    private static V2OutdeliveryRepository outdeliveryRepository;
    private static V2OutpaymentRepository outpaymentRepository;
    private static V2PaymentTermRepository paymentTermRepository;
    private static V2PeriodicInvoiceRepository periodicInvoiceRepository;
    private static V2ProductRepository productRepository;
    private static V2PurchaseOrderRepository purchaseOrderRepository;
    private static V2SupplierRepository supplierRepository;
    private static V2SupplierInvoiceRepository supplierInvoiceRepository;
    private static V2SupplierCreditInvoiceRepository supplierCreditInvoiceRepository;
    private static V2TenderRepository tenderRepository;
    private static V2UnitRepository unitRepository;
    private static V2AccountPlanRepository accountPlanRepository;
    private static V2VoucherRepository voucherRepository;
    private static V2VoucherTemplateRepository voucherTemplateRepository;
    private static V2AccountingYearRepository accountingYearRepository;
    private static V2CompanyRepository companyRepository;
    private static V2ProjectRepository projectRepository;
    private static V2ResultUnitRepository resultUnitRepository;
    private static SSDB initializedDb;

    private Repositories() {
        // utility class
    }

    /**
     * Returns {@code true} when the V2 schema is active.
     *
     * @return always {@code true} in V2-only mode
     */
    public static boolean isSchemaV2() {
        return true;
    }

    /**
     * Initialises all repository instances backed by the given {@link SSDB}.
     *
     * <p>V2 implementations are always created. Must be called once before any
     * getter is used.
     * Calling again replaces the existing instances.</p>
     *
     * @param db the SSDB instance; must not be {@code null}
     */
    public static void init(SSDB db) {
        if (db == null) {
            throw new NullPointerException("db must not be null");
        }
        initializedDb = db;
        if (db.getConnection() == null) {
            initWithoutConnection(db);
            return;
        }
        // Migrated H domains are V2-only in Slice P.
        autoDistRepository = new V2AutoDistRepository(db.getConnection(),
                () -> {
                    se.swedsoft.bookkeeping.data.SSNewCompany iCompany = SSCompanyYearContext.getCurrentCompany();
                    return iCompany != null ? iCompany : db.getCurrentCompany();
                },
                db::rollbackCurrentTransaction);
        creditInvoiceRepository = new V2CreditInvoiceRepository(db.getConnection(),
                () -> {
                    se.swedsoft.bookkeeping.data.SSNewCompany iCompany = SSCompanyYearContext.getCurrentCompany();
                    return iCompany != null ? iCompany : db.getCurrentCompany();
                },
                db::rollbackCurrentTransaction);
        // Reference data (Kategori A) â€” always V2 after cutover.
        currencyRepository = new V2CurrencyRepository(db.getConnection(), db::rollbackCurrentTransaction);
        deliveryTermRepository = new V2DeliveryTermRepository(db.getConnection(), db::rollbackCurrentTransaction);
        deliveryWayRepository = new V2DeliveryWayRepository(db.getConnection(), db::rollbackCurrentTransaction);
        paymentTermRepository = new V2PaymentTermRepository(db.getConnection(), db::rollbackCurrentTransaction);
        unitRepository = new V2UnitRepository(db.getConnection(), db::rollbackCurrentTransaction);
        inpaymentRepository = new V2InpaymentRepository(db.getConnection(),
                () -> {
                    se.swedsoft.bookkeeping.data.SSNewCompany iCompany = SSCompanyYearContext.getCurrentCompany();
                    return iCompany != null ? iCompany : db.getCurrentCompany();
                },
                db::rollbackCurrentTransaction);
        invoiceRepository = new V2InvoiceRepository(db.getConnection(),
                () -> {
                    se.swedsoft.bookkeeping.data.SSNewCompany iCompany = SSCompanyYearContext.getCurrentCompany();
                    return iCompany != null ? iCompany : db.getCurrentCompany();
                },
                db::rollbackCurrentTransaction);
        ownReportRepository = new V2OwnReportRepository(db.getConnection(),
                () -> {
                    se.swedsoft.bookkeeping.data.SSNewCompany iCompany = SSCompanyYearContext.getCurrentCompany();
                    return iCompany != null ? iCompany : db.getCurrentCompany();
                },
                db::rollbackCurrentTransaction);
        periodicInvoiceRepository = new V2PeriodicInvoiceRepository(db.getConnection(),
                () -> {
                    se.swedsoft.bookkeeping.data.SSNewCompany iCompany = SSCompanyYearContext.getCurrentCompany();
                    return iCompany != null ? iCompany : db.getCurrentCompany();
                },
                db::rollbackCurrentTransaction);
        indeliveryRepository = new V2IndeliveryRepository(db.getConnection(),
                () -> {
                    se.swedsoft.bookkeeping.data.SSNewCompany iCompany = SSCompanyYearContext.getCurrentCompany();
                    return iCompany != null ? iCompany : db.getCurrentCompany();
                },
                db::rollbackCurrentTransaction);
        inventoryRepository = new V2InventoryRepository(db.getConnection(),
                () -> {
                    se.swedsoft.bookkeeping.data.SSNewCompany iCompany = SSCompanyYearContext.getCurrentCompany();
                    return iCompany != null ? iCompany : db.getCurrentCompany();
                },
                db::rollbackCurrentTransaction);
        orderRepository = new V2OrderRepository(db.getConnection(),
                () -> {
                    se.swedsoft.bookkeeping.data.SSNewCompany iCompany = SSCompanyYearContext.getCurrentCompany();
                    return iCompany != null ? iCompany : db.getCurrentCompany();
                },
                db::rollbackCurrentTransaction);
        outdeliveryRepository = new V2OutdeliveryRepository(db.getConnection(),
                () -> {
                    se.swedsoft.bookkeeping.data.SSNewCompany iCompany = SSCompanyYearContext.getCurrentCompany();
                    return iCompany != null ? iCompany : db.getCurrentCompany();
                },
                db::rollbackCurrentTransaction);
        outpaymentRepository = new V2OutpaymentRepository(db.getConnection(),
                () -> {
                    se.swedsoft.bookkeeping.data.SSNewCompany iCompany = SSCompanyYearContext.getCurrentCompany();
                    return iCompany != null ? iCompany : db.getCurrentCompany();
                },
                db::rollbackCurrentTransaction);
        purchaseOrderRepository = new V2PurchaseOrderRepository(db.getConnection(),
                () -> {
                    se.swedsoft.bookkeeping.data.SSNewCompany iCompany = SSCompanyYearContext.getCurrentCompany();
                    return iCompany != null ? iCompany : db.getCurrentCompany();
                },
                db::rollbackCurrentTransaction);
        supplierCreditInvoiceRepository = new V2SupplierCreditInvoiceRepository(db.getConnection(),
                () -> {
                    se.swedsoft.bookkeeping.data.SSNewCompany iCompany = SSCompanyYearContext.getCurrentCompany();
                    return iCompany != null ? iCompany : db.getCurrentCompany();
                },
                db::rollbackCurrentTransaction);
        supplierInvoiceRepository = new V2SupplierInvoiceRepository(db.getConnection(),
                () -> {
                    se.swedsoft.bookkeeping.data.SSNewCompany iCompany = SSCompanyYearContext.getCurrentCompany();
                    return iCompany != null ? iCompany : db.getCurrentCompany();
                },
                db::rollbackCurrentTransaction);
        tenderRepository = new V2TenderRepository(db.getConnection(),
                () -> {
                    se.swedsoft.bookkeeping.data.SSNewCompany iCompany = SSCompanyYearContext.getCurrentCompany();
                    return iCompany != null ? iCompany : db.getCurrentCompany();
                },
                db::rollbackCurrentTransaction);
        voucherTemplateRepository = new V2VoucherTemplateRepository(db.getConnection(),
                () -> {
                    se.swedsoft.bookkeeping.data.SSNewCompany iCompany = SSCompanyYearContext.getCurrentCompany();
                    return iCompany != null ? iCompany : db.getCurrentCompany();
                },
                db::rollbackCurrentTransaction);

        customerRepository = new V2CustomerRepository(db.getConnection(),
                () -> {
                    se.swedsoft.bookkeeping.data.SSNewCompany iCompany = SSCompanyYearContext.getCurrentCompany();
                    return iCompany != null ? iCompany : db.getCurrentCompany();
                }, db::rollbackCurrentTransaction);
        productRepository = new V2ProductRepository(db.getConnection(),
                () -> {
                    se.swedsoft.bookkeeping.data.SSNewCompany iCompany = SSCompanyYearContext.getCurrentCompany();
                    return iCompany != null ? iCompany : db.getCurrentCompany();
                }, db::rollbackCurrentTransaction);
        supplierRepository = new V2SupplierRepository(db.getConnection(),
                () -> {
                    se.swedsoft.bookkeeping.data.SSNewCompany iCompany = SSCompanyYearContext.getCurrentCompany();
                    return iCompany != null ? iCompany : db.getCurrentCompany();
                }, db::rollbackCurrentTransaction);
        accountPlanRepository = new V2AccountPlanRepository(db.getConnection(), db::rollbackCurrentTransaction);
        voucherRepository = new V2VoucherRepository(db.getConnection(),
                () -> {
                    se.swedsoft.bookkeeping.data.SSNewCompany iCompany = SSCompanyYearContext.getCurrentCompany();
                    return iCompany != null ? iCompany : db.getCurrentCompany();
                },
                () -> {
                    se.swedsoft.bookkeeping.data.SSNewAccountingYear iYear = SSCompanyYearContext.getCurrentYear();
                    return iYear != null ? iYear : db.getCurrentYear();
                }, db::rollbackCurrentTransaction);
        accountingYearRepository = new V2AccountingYearRepository(
                db.getConnection(),
                () -> {
                    se.swedsoft.bookkeeping.data.SSNewCompany iCompany = SSCompanyYearContext.getCurrentCompany();
                    return iCompany != null ? iCompany : db.getCurrentCompany();
                },
                () -> {
                    se.swedsoft.bookkeeping.data.SSNewAccountingYear iYear = SSCompanyYearContext.getCurrentYear();
                    return iYear != null ? iYear : db.getCurrentYear();
                },
                iYear -> {
                    SSCompanyYearContext.applyOpenedYearFromRepository(iYear);
                    db.applyOpenedYearFromRepository(iYear);
                },
                db::rollbackCurrentTransaction);
        companyRepository = new V2CompanyRepository(db.getConnection(), db::rollbackCurrentTransaction,
                SSCompanyYearContext::getYearsForCompany, year -> Repositories.accountingYears().delete(year));

        projectRepository = new V2ProjectRepository(db.getConnection(),
                () -> {
                    se.swedsoft.bookkeeping.data.SSNewCompany iCompany = SSCompanyYearContext.getCurrentCompany();
                    return iCompany != null ? iCompany : db.getCurrentCompany();
                },
                db::rollbackCurrentTransaction);
        resultUnitRepository = new V2ResultUnitRepository(db.getConnection(),
                () -> {
                    se.swedsoft.bookkeeping.data.SSNewCompany iCompany = SSCompanyYearContext.getCurrentCompany();
                    return iCompany != null ? iCompany : db.getCurrentCompany();
                },
                db::rollbackCurrentTransaction);
    }

    private static void initWithoutConnection(SSDB db) {
        // DB exists but JDBC connection is not initialized yet (common in pure unit tests).
        customerRepository = new V2CustomerRepository(NO_CONNECTION, () -> null, () -> { });
        ownReportRepository = new V2OwnReportRepository(NO_CONNECTION, () -> null, () -> { });
        periodicInvoiceRepository = new V2PeriodicInvoiceRepository(NO_CONNECTION, () -> null, () -> { });
        indeliveryRepository = new V2IndeliveryRepository(NO_CONNECTION, () -> null, () -> { });
        inventoryRepository = new V2InventoryRepository(NO_CONNECTION, () -> null, () -> { });
        orderRepository = new V2OrderRepository(NO_CONNECTION, () -> null, () -> { });
        outdeliveryRepository = new V2OutdeliveryRepository(NO_CONNECTION, () -> null, () -> { });
        productRepository = new V2ProductRepository(NO_CONNECTION, () -> null, () -> { });
        purchaseOrderRepository = new V2PurchaseOrderRepository(NO_CONNECTION, () -> null, () -> { });
        supplierCreditInvoiceRepository = new V2SupplierCreditInvoiceRepository(NO_CONNECTION, () -> null, () -> { });
        supplierInvoiceRepository = new V2SupplierInvoiceRepository(NO_CONNECTION, () -> null, () -> { });
        voucherTemplateRepository = new V2VoucherTemplateRepository(NO_CONNECTION, () -> null, () -> { });
        accountPlanRepository = new V2AccountPlanRepository(NO_CONNECTION, () -> { });
        voucherRepository = new V2VoucherRepository(NO_CONNECTION, () -> null, () -> null, () -> { });
        accountingYearRepository = new V2AccountingYearRepository(
                NO_CONNECTION,
                () -> null,
                () -> null,
                year -> { },
                () -> { });
       companyRepository = new V2CompanyRepository(NO_CONNECTION, () -> { }, company -> java.util.Collections.emptyList(),
                year -> { });

        projectRepository = new V2ProjectRepository(NO_CONNECTION, () -> null, () -> { });
        resultUnitRepository = new V2ResultUnitRepository(NO_CONNECTION, () -> null, () -> { });
    }

    /**
     * Returns the {@link V2CustomerRepository}.
     *
     * @return the customer repository; never {@code null} after {@link #init}
     * @throws IllegalStateException if {@link #init} has not been called
     */
    public static V2CustomerRepository customers() {
        ensureInitialized();
        return customerRepository;
    }

    /**
     * Returns the {@link V2AutoDistRepository}.
     *
     * @return the auto-dist repository; never {@code null} after {@link #init}
     * @throws IllegalStateException if {@link #init} has not been called
     */
    public static V2AutoDistRepository autoDists() {
        ensureInitialized();
        return autoDistRepository;
    }

    /**
     * Returns the {@link V2CurrencyRepository}.
     *
     * @return the currency repository; never {@code null} after {@link #init}
     * @throws IllegalStateException if {@link #init} has not been called
     */
    public static V2CurrencyRepository currencies() {
        ensureInitialized();
        return currencyRepository;
    }

    /**
     * Returns the {@link V2DeliveryTermRepository}.
     *
     * @return the delivery-term repository; never {@code null} after {@link #init}
     * @throws IllegalStateException if {@link #init} has not been called
     */
    public static V2DeliveryTermRepository deliveryTerms() {
        ensureInitialized();
        return deliveryTermRepository;
    }

    /**
     * Returns the {@link V2DeliveryWayRepository}.
     *
     * @return the delivery-way repository; never {@code null} after {@link #init}
     * @throws IllegalStateException if {@link #init} has not been called
     */
    public static V2DeliveryWayRepository deliveryWays() {
        ensureInitialized();
        return deliveryWayRepository;
    }

    /**
     * Returns the {@link V2PaymentTermRepository}.
     *
     * @return the payment-term repository; never {@code null} after {@link #init}
     * @throws IllegalStateException if {@link #init} has not been called
     */
    public static V2PaymentTermRepository paymentTerms() {
        ensureInitialized();
        return paymentTermRepository;
    }

    /**
     * Returns the {@link V2UnitRepository}.
     *
     * @return the unit repository; never {@code null} after {@link #init}
     * @throws IllegalStateException if {@link #init} has not been called
     */
    public static V2UnitRepository units() {
        ensureInitialized();
        return unitRepository;
    }

    /**
     * Returns the {@link V2CreditInvoiceRepository}.
     *
     * @return the credit-invoice repository; never {@code null} after {@link #init}
     * @throws IllegalStateException if {@link #init} has not been called
     */
    public static V2CreditInvoiceRepository creditInvoices() {
        ensureInitialized();
        return creditInvoiceRepository;
    }

    /**
     * Returns the {@link V2IndeliveryRepository}.
     *
     * @return the indelivery repository; never {@code null} after {@link #init}
     * @throws IllegalStateException if {@link #init} has not been called
     */
    public static V2IndeliveryRepository indeliveries() {
        if (indeliveryRepository == null) {
            throw new IllegalStateException("Repositories.init() has not been called");
        }
        return indeliveryRepository;
    }

    /**
     * Returns the {@link V2InpaymentRepository}.
     *
     * @return the inpayment repository; never {@code null} after {@link #init}
     * @throws IllegalStateException if {@link #init} has not been called
     */
    public static V2InpaymentRepository inpayments() {
        if (inpaymentRepository == null) {
            throw new IllegalStateException("Repositories.init() has not been called");
        }
        return inpaymentRepository;
    }

    /**
     * Returns the {@link V2InventoryRepository}.
     *
     * @return the inventory repository; never {@code null} after {@link #init}
     * @throws IllegalStateException if {@link #init} has not been called
     */
    public static V2InventoryRepository inventories() {
        if (inventoryRepository == null) {
            throw new IllegalStateException("Repositories.init() has not been called");
        }
        return inventoryRepository;
    }

    /**
     * Returns the {@link V2InvoiceRepository}.
     *
     * @return the invoice repository; never {@code null} after {@link #init}
     * @throws IllegalStateException if {@link #init} has not been called
     */
    public static V2InvoiceRepository invoices() {
        ensureInitialized();
        return invoiceRepository;
    }

    /**
     * Returns the {@link V2OwnReportRepository}.
     *
     * @return the own-report repository; never {@code null} after {@link #init}
     * @throws IllegalStateException if {@link #init} has not been called
     */
    public static V2OwnReportRepository ownReports() {
        ensureInitialized();
        return ownReportRepository;
    }

    /**
     * Returns the {@link V2OrderRepository}.
     *
     * @return the order repository; never {@code null} after {@link #init}
     * @throws IllegalStateException if {@link #init} has not been called
     */
    public static V2OrderRepository orders() {
        ensureInitialized();
        return orderRepository;
    }

    /**
     * Returns the {@link V2OutdeliveryRepository}.
     *
     * @return the outdelivery repository; never {@code null} after {@link #init}
     * @throws IllegalStateException if {@link #init} has not been called
     */
    public static V2OutdeliveryRepository outdeliveries() {
        if (outdeliveryRepository == null) {
            throw new IllegalStateException("Repositories.init() has not been called");
        }
        return outdeliveryRepository;
    }

    /**
     * Returns the {@link V2OutpaymentRepository}.
     *
     * @return the outpayment repository; never {@code null} after {@link #init}
     * @throws IllegalStateException if {@link #init} has not been called
     */
    public static V2OutpaymentRepository outpayments() {
        if (outpaymentRepository == null) {
            throw new IllegalStateException("Repositories.init() has not been called");
        }
        return outpaymentRepository;
    }

    /**
     * Returns the {@link V2PeriodicInvoiceRepository}.
     *
     * @return the periodic-invoice repository; never {@code null} after {@link #init}
     * @throws IllegalStateException if {@link #init} has not been called
     */
    public static V2PeriodicInvoiceRepository periodicInvoices() {
        ensureInitialized();
        return periodicInvoiceRepository;
    }

    /**
     * Returns the {@link V2ProductRepository}.
     *
     * @return the product repository; never {@code null} after {@link #init}
     * @throws IllegalStateException if {@link #init} has not been called
     */
    public static V2ProductRepository products() {
        ensureInitialized();
        return productRepository;
    }

    /**
     * Returns the {@link V2PurchaseOrderRepository}.
     *
     * @return the purchase-order repository; never {@code null} after {@link #init}
     * @throws IllegalStateException if {@link #init} has not been called
     */
    public static V2PurchaseOrderRepository purchaseOrders() {
        ensureInitialized();
        return purchaseOrderRepository;
    }

    /**
     * Returns the {@link V2SupplierRepository}.
     *
     * @return the supplier repository; never {@code null} after {@link #init}
     * @throws IllegalStateException if {@link #init} has not been called
     */
    public static V2SupplierRepository suppliers() {
        ensureInitialized();
        return supplierRepository;
    }

    /**
     * Returns the {@link V2SupplierInvoiceRepository}.
     *
     * @return the supplier-invoice repository; never {@code null} after {@link #init}
     * @throws IllegalStateException if {@link #init} has not been called
     */
    public static V2SupplierInvoiceRepository supplierInvoices() {
        ensureInitialized();
        return supplierInvoiceRepository;
    }

    /**
     * Returns the {@link V2SupplierCreditInvoiceRepository}.
     *
     * @return the supplier-credit-invoice repository; never {@code null} after {@link #init}
     * @throws IllegalStateException if {@link #init} has not been called
     */
    public static V2SupplierCreditInvoiceRepository supplierCreditInvoices() {
        ensureInitialized();
        return supplierCreditInvoiceRepository;
    }

    /**
     * Returns the {@link V2TenderRepository}.
     *
     * @return the tender repository; never {@code null} after {@link #init}
     * @throws IllegalStateException if {@link #init} has not been called
     */
    public static V2TenderRepository tenders() {
        if (tenderRepository == null) {
            throw new IllegalStateException("Repositories.init() has not been called");
        }
        return tenderRepository;
    }

    /**
     * Returns the {@link V2AccountPlanRepository}.
     *
     * @return the account-plan repository; never {@code null} after {@link #init}
     * @throws IllegalStateException if {@link #init} has not been called
     */
    public static V2AccountPlanRepository accountPlans() {
        ensureInitialized();
        return accountPlanRepository;
    }

    /**
     * Returns the {@link V2VoucherRepository}.
     *
     * @return the voucher repository; never {@code null} after {@link #init}
     * @throws IllegalStateException if {@link #init} has not been called
     */
    public static V2VoucherRepository vouchers() {
        ensureInitialized();
        return voucherRepository;
    }

    /**
     * Returns the {@link V2VoucherTemplateRepository}.
     *
     * @return the voucher-template repository; never {@code null} after {@link #init}
     * @throws IllegalStateException if {@link #init} has not been called
     */
    public static V2VoucherTemplateRepository voucherTemplates() {
        ensureInitialized();
        return voucherTemplateRepository;
    }

    /**
     * Returns the {@link V2AccountingYearRepository}.
     *
     * @return the accounting-year repository; never {@code null} after {@link #init}
     * @throws IllegalStateException if {@link #init} has not been called
     */
    public static V2AccountingYearRepository accountingYears() {
        ensureInitialized();
        return accountingYearRepository;
    }

    /**
     * Returns the {@link V2CompanyRepository}.
     *
     * @return the company repository; never {@code null} after {@link #init}
     * @throws IllegalStateException if {@link #init} has not been called
     */
    public static V2CompanyRepository companies() {
        ensureInitialized();
        return companyRepository;
    }

    /**
     * Returns the {@link V2ProjectRepository}.
     *
     * @return the project repository; never {@code null} after {@link #init}
     * @throws IllegalStateException if {@link #init} has not been called
     */
    public static V2ProjectRepository projects() {
        ensureInitialized();
        return projectRepository;
    }

    /**
     * Returns the {@link V2ResultUnitRepository}.
     *
     * @return the result-unit repository; never {@code null} after {@link #init}
     * @throws IllegalStateException if {@link #init} has not been called
     */
    public static V2ResultUnitRepository resultUnits() {
        ensureInitialized();
        return resultUnitRepository;
    }

    private static void ensureInitialized() {
        SSDB db = initializedDb;
        if (db == null) {
            db = SSSystemConfigContext.getDatabase();
            initializedDb = db;
        }
        if (db == null) {
            return;
        }
        boolean shouldInit = customerRepository == null;
        if (!shouldInit && db.getConnection() != null && autoDistRepository == null) {
            shouldInit = true;
        }
        if (shouldInit) {
            init(db);
        }
    }

    private static Connection createNoConnection() {
        return (Connection) Proxy.newProxyInstance(
                Repositories.class.getClassLoader(),
                new Class[]{Connection.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "isClosed" -> true;
                    case "close" -> null;
                    case "toString" -> "NoConnection";
                    default -> throw new UnsupportedOperationException("No JDBC connection available");
                });
    }
}
