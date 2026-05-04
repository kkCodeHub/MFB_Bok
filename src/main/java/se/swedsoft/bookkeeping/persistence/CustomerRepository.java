package se.swedsoft.bookkeeping.persistence;

import se.swedsoft.bookkeeping.data.SSCustomer;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for {@link SSCustomer} persistence operations.
 *
 * <p>Implementations decouple the rest of the application from the
 * underlying storage mechanism (currently HSQLDB via {@code SSDB}).</p>
 */
public interface CustomerRepository {

    /**
     * Returns all customers for the current company.
     *
     * @return mutable list of customers; never {@code null}
     */
    List<SSCustomer> findAll();

    /**
     * Looks up a customer by its customer-number field.
     *
     * @param customerNumber the customer number to search for; must not be {@code null}
     * @return an {@link Optional} containing the customer, or empty if not found
     */
    Optional<SSCustomer> findByNumber(String customerNumber);

    /**
     * Looks up a customer by object identity (uses its customer-number).
     *
     * @param customer reference customer; must not be {@code null}
     * @return an {@link Optional} containing the stored customer, or empty if not found
     */
    Optional<SSCustomer> findByCustomer(SSCustomer customer);

    /**
     * Returns only those customers from {@code subset} that exist in the repository.
     *
     * @param subset candidate list; must not be {@code null}
     * @return filtered list; never {@code null}
     */
    List<SSCustomer> findAll(List<SSCustomer> subset);

    /**
     * Persists a new customer for the current company.
     *
     * @param customer the customer to add; must not be {@code null}
     */
    void add(SSCustomer customer);

    /**
     * Updates an existing customer record.
     *
     * @param customer the customer with updated values; must not be {@code null}
     */
    void update(SSCustomer customer);

    /**
     * Deletes a customer from the current company.
     *
     * @param customer the customer to delete; must not be {@code null}
     */
    void delete(SSCustomer customer);
}

