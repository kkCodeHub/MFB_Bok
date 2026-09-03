package se.swedsoft.bookkeeping.data.system;

import se.swedsoft.bookkeeping.data.SSProduct;
import se.swedsoft.bookkeeping.persistence.Repositories;

import java.util.List;
import java.util.Optional;

/**
 * Product domain facade.
 */
public final class SSProductContext {

    private SSProductContext() {}

    public static List<SSProduct> getProducts() {
        return Repositories.products().findAll();
    }

    public static List<SSProduct> getProducts(List<SSProduct> pProducts) {
        return Repositories.products().findAll(pProducts);
    }

    public static Optional<SSProduct> getProduct(SSProduct pProduct) {
        return Repositories.products().findByProduct(pProduct);
    }

    public static SSProduct getProduct(String pProductNumber) {
        return Repositories.products().findByNumber(pProductNumber).orElse(null);
    }

    public static void addProduct(SSProduct pProduct) {
        Repositories.products().add(pProduct);
    }

    public static void updateProduct(SSProduct pProduct) {
        Repositories.products().update(pProduct);
    }

    public static void deleteProduct(SSProduct pProduct) {
        Repositories.products().delete(pProduct);
    }
}
