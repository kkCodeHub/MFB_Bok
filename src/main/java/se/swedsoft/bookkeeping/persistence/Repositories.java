package se.swedsoft.bookkeeping.persistence;

import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.persistence.legacy.SSDBCustomerRepository;
import se.swedsoft.bookkeeping.persistence.legacy.SSDBProductRepository;
import se.swedsoft.bookkeeping.persistence.legacy.SSDBSupplierRepository;

/**
 * Central access point for repository instances.
 *
 * <p>All repository instances are created once via {@link #init(SSDB)} and
 * then available through static getters.  Call sites should obtain
 * repositories through this class rather than instantiating implementations
 * directly.</p>
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

    private static CustomerRepository customerRepository;
    private static ProductRepository productRepository;
    private static SupplierRepository supplierRepository;

    private Repositories() {
        // utility class
    }

    /**
     * Initialises all repository instances backed by the given {@link SSDB}.
     *
     * <p>Must be called once before any getter is used.
     * Calling again replaces the existing instances.</p>
     *
     * @param db the SSDB instance; must not be {@code null}
     */
    public static void init(SSDB db) {
        if (db == null) {
            throw new NullPointerException("db must not be null");
        }
        customerRepository = new SSDBCustomerRepository(db);
        productRepository = new SSDBProductRepository(db);
        supplierRepository = new SSDBSupplierRepository(db);
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

