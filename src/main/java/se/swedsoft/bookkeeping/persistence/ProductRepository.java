package se.swedsoft.bookkeeping.persistence;

import se.swedsoft.bookkeeping.data.SSProduct;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for {@link SSProduct} persistence operations.
 *
 * <p>Implementations decouple the rest of the application from the
 * underlying storage mechanism (currently HSQLDB via {@code SSDB}).</p>
 */
public interface ProductRepository {

    /**
     * Returns all products for the current company.
     *
     * @return mutable list of products; never {@code null}
     */
    List<SSProduct> findAll();

    /**
     * Looks up a product by its product-number field.
     *
     * @param productNumber the product number to search for; must not be {@code null}
     * @return an {@link Optional} containing the product, or empty if not found
     */
    Optional<SSProduct> findByNumber(String productNumber);

    /**
     * Looks up a product by object identity (uses its product-number).
     *
     * @param product reference product; must not be {@code null}
     * @return an {@link Optional} containing the stored product, or empty if not found
     */
    Optional<SSProduct> findByProduct(SSProduct product);

    /**
     * Returns only those products from {@code subset} that exist in the repository.
     *
     * @param subset candidate list; must not be {@code null}
     * @return filtered list; never {@code null}
     */
    List<SSProduct> findAll(List<SSProduct> subset);

    /**
     * Persists a new product for the current company.
     *
     * @param product the product to add; must not be {@code null}
     */
    void add(SSProduct product);

    /**
     * Updates an existing product record.
     *
     * @param product the product with updated values; must not be {@code null}
     */
    void update(SSProduct product);

    /**
     * Deletes a product from the current company.
     *
     * @param product the product to delete; must not be {@code null}
     */
    void delete(SSProduct product);
}

