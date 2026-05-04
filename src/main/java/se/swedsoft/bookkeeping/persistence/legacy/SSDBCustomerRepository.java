package se.swedsoft.bookkeeping.persistence.legacy;

import se.swedsoft.bookkeeping.data.SSCustomer;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.persistence.CustomerRepository;

import java.util.List;
import java.util.Optional;

/**
 * Legacy {@link CustomerRepository} implementation that delegates all operations
 * to the existing {@link SSDB} singleton.
 *
 * <p>This class exists as a thin adapter so that callers can depend on the
 * {@code CustomerRepository} interface rather than {@code SSDB} directly.
 * It introduces no new behaviour; it is the first step toward being able
 * to swap in a different persistence back-end without touching call sites.</p>
 */
public class SSDBCustomerRepository implements CustomerRepository {

    private final SSDB db;

    /**
     * Creates a repository backed by the given {@link SSDB} instance.
     *
     * @param db the SSDB instance to delegate to; must not be {@code null}
     */
    public SSDBCustomerRepository(SSDB db) {
        if (db == null) {
            throw new NullPointerException("db must not be null");
        }
        this.db = db;
    }

    /** {@inheritDoc} */
    @Override
    public List<SSCustomer> findAll() {
        return db.getCustomers();
    }

    /** {@inheritDoc} */
    @Override
    public Optional<SSCustomer> findByNumber(String customerNumber) {
        return db.getCustomer(customerNumber);
    }

    /** {@inheritDoc} */
    @Override
    public Optional<SSCustomer> findByCustomer(SSCustomer customer) {
        return db.getCustomer(customer);
    }

    /** {@inheritDoc} */
    @Override
    public List<SSCustomer> findAll(List<SSCustomer> subset) {
        return db.getCustomers(subset);
    }

    /** {@inheritDoc} */
    @Override
    public void add(SSCustomer customer) {
        db.addCustomer(customer);
    }

    /** {@inheritDoc} */
    @Override
    public void update(SSCustomer customer) {
        db.updateCustomer(customer);
    }

    /** {@inheritDoc} */
    @Override
    public void delete(SSCustomer customer) {
        db.deleteCustomer(customer);
    }
}

