package se.swedsoft.bookkeeping.importexport.excel;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SSProductImporterQuantityTenthsTest {

    @Test
    void shouldParseWholeNumbersToTenths() {
        assertThat(SSProductImporter.parseQuantityToTenths("1")).isEqualTo(10);
        assertThat(SSProductImporter.parseQuantityToTenths("7")).isEqualTo(70);
    }

    @Test
    void shouldParseDecimalsWithDotAndCommaToTenths() {
        assertThat(SSProductImporter.parseQuantityToTenths("2.5")).isEqualTo(25);
        assertThat(SSProductImporter.parseQuantityToTenths("2,5")).isEqualTo(25);
    }

    @Test
    void shouldRoundHalfUpToTenths() {
        assertThat(SSProductImporter.parseQuantityToTenths("0.04")).isEqualTo(0);
        assertThat(SSProductImporter.parseQuantityToTenths("0.05")).isEqualTo(1);
    }

    @Test
    void shouldParseNegativeValuesToTenths() {
        assertThat(SSProductImporter.parseQuantityToTenths("-3.5")).isEqualTo(-35);
    }

    @Test
    void shouldReturnNullForNullOrBlankInput() {
        assertThat(SSProductImporter.parseQuantityToTenths(null)).isNull();
        assertThat(SSProductImporter.parseQuantityToTenths(" ")).isNull();
    }

    @Test
    void shouldThrowForNonNumericInput() {
        assertThatThrownBy(() -> SSProductImporter.parseQuantityToTenths("NaN"))
                .isInstanceOf(NumberFormatException.class);
    }
}
