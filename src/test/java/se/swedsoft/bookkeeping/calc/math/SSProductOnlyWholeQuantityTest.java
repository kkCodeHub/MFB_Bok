package se.swedsoft.bookkeeping.calc.math;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSProduct;
import static org.assertj.core.api.Assertions.assertThat;
/**
 * Unit tests for {@link SSProductQuantityValidator}.
 *
 * <p>Quantities are expressed in tenths: 10 = 1.0 unit, 25 = 2.5 units.</p>
 */
class SSProductOnlyWholeQuantityTest {
    @Test
    void wholeQuantityAllowedWhenFlagIsTrue() {
        SSProduct product = wholeOnlyProduct();
        assertThat(SSProductQuantityValidator.isValidQuantity(product, 10)).isTrue();
        assertThat(SSProductQuantityValidator.isValidQuantity(product, 20)).isTrue();
        assertThat(SSProductQuantityValidator.isValidQuantity(product, 0)).isTrue();
        assertThat(SSProductQuantityValidator.isValidQuantity(product, 100)).isTrue();
    }
    @Test
    void decimalQuantityRejectedWhenFlagIsTrue() {
        SSProduct product = wholeOnlyProduct();
        assertThat(SSProductQuantityValidator.isValidQuantity(product, 25)).isFalse();
        assertThat(SSProductQuantityValidator.isValidQuantity(product, 15)).isFalse();
        assertThat(SSProductQuantityValidator.isValidQuantity(product, 1)).isFalse();
        assertThat(SSProductQuantityValidator.isValidQuantity(product, 9)).isFalse();
    }
    @Test
    void anyQuantityAllowedWhenFlagIsFalse() {
        SSProduct product = decimalProduct();
        assertThat(SSProductQuantityValidator.isValidQuantity(product, 10)).isTrue();
        assertThat(SSProductQuantityValidator.isValidQuantity(product, 25)).isTrue();
        assertThat(SSProductQuantityValidator.isValidQuantity(product, 1)).isTrue();
    }
    @Test
    void nullProductIsConsideredValid() {
        assertThat(SSProductQuantityValidator.isValidQuantity(null, 25)).isTrue();
    }
    @Test
    void nullQuantityIsConsideredValid() {
        assertThat(SSProductQuantityValidator.isValidQuantity(wholeOnlyProduct(), null)).isTrue();
    }
    @Test
    void isWholeQuantityReturnsTrueForMultiplesOfTen() {
        assertThat(SSProductQuantityValidator.isWholeQuantity(0)).isTrue();
        assertThat(SSProductQuantityValidator.isWholeQuantity(10)).isTrue();
        assertThat(SSProductQuantityValidator.isWholeQuantity(30)).isTrue();
        assertThat(SSProductQuantityValidator.isWholeQuantity(100)).isTrue();
    }
    @Test
    void isWholeQuantityReturnsFalseForNonMultiplesOfTen() {
        assertThat(SSProductQuantityValidator.isWholeQuantity(5)).isFalse();
        assertThat(SSProductQuantityValidator.isWholeQuantity(25)).isFalse();
        assertThat(SSProductQuantityValidator.isWholeQuantity(11)).isFalse();
    }
    @Test
    void isWholeQuantityReturnsTrueForNull() {
        assertThat(SSProductQuantityValidator.isWholeQuantity(null)).isTrue();
    }
    @Test
    void productDefaultsToWholeQuantityOnly() {
        SSProduct product = new SSProduct();
        assertThat(product.isOnlyWholeQuantity()).isTrue();
    }
    @Test
    void productFlagRoundTrip() {
        SSProduct product = new SSProduct();
        product.setOnlyWholeQuantity(true);
        assertThat(product.isOnlyWholeQuantity()).isTrue();
        product.setOnlyWholeQuantity(false);
        assertThat(product.isOnlyWholeQuantity()).isFalse();
    }
    @Test
    void copyConstructorPreservesFlag() {
        SSProduct original = new SSProduct();
        original.setOnlyWholeQuantity(true);
        SSProduct copy = new SSProduct(original);
        assertThat(copy.isOnlyWholeQuantity()).isTrue();
    }
    private SSProduct wholeOnlyProduct() {
        SSProduct product = new SSProduct();
        product.setNumber("P-WHOLE");
        product.setDescription("Whole only product");
        product.setOnlyWholeQuantity(true);
        return product;
    }
    private SSProduct decimalProduct() {
        SSProduct product = new SSProduct();
        product.setNumber("P-DECIMAL");
        product.setDescription("Decimal allowed product");
        product.setOnlyWholeQuantity(false);
        return product;
    }
}
