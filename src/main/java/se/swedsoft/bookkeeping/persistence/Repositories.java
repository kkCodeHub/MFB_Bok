package se.swedsoft.bookkeeping.persistence;

import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.persistence.legacy.SSDBCustomerRepository;
import se.swedsoft.bookkeeping.persistence.legacy.SSDBProductRepository;
import se.swedsoft.bookkeeping.persistence.legacy.SSDBSupplierRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2CustomerRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2ProductRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2SupplierRepository;

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
    private static ProductRepository productRepository;
    private static SupplierRepository supplierRepository;

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
            productRepository = new V2ProductRepository(db);
            supplierRepository = new V2SupplierRepository(db);
        } else {
            customerRepository = new SSDBCustomerRepository(db);
            productRepository = new SSDBProductRepository(db);
            supplierRepository = new SSDBSupplierRepository(db);
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
}

