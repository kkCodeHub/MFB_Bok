package se.swedsoft.bookkeeping.importexport.xml;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SSOrderImporterQuantityTenthsTest {

    @Test
    void shouldParseWholeNumbersToTenths() {
        assertThat(SSOrderImporter.parseQuantityToTenths("1")).isEqualTo(10);
        assertThat(SSOrderImporter.parseQuantityToTenths("3")).isEqualTo(30);
    }

    @Test
    void shouldParseDecimalsWithDotAndCommaToTenths() {
        assertThat(SSOrderImporter.parseQuantityToTenths("2.5")).isEqualTo(25);
        assertThat(SSOrderImporter.parseQuantityToTenths("2,5")).isEqualTo(25);
    }

    @Test
    void shouldRoundHalfUpToTenths() {
        assertThat(SSOrderImporter.parseQuantityToTenths("2.54")).isEqualTo(25);
        assertThat(SSOrderImporter.parseQuantityToTenths("2.55")).isEqualTo(26);
    }

    @Test
    void shouldParseNegativeValuesToTenths() {
        assertThat(SSOrderImporter.parseQuantityToTenths("-1.5")).isEqualTo(-15);
    }

    @Test
    void shouldReturnZeroForNullAndBlankInput() {
        assertThat(SSOrderImporter.parseQuantityToTenths(null)).isEqualTo(0);
        assertThat(SSOrderImporter.parseQuantityToTenths(" ")).isEqualTo(0);
    }

    @Test
    void shouldThrowForNonNumericInput() {
        assertThatThrownBy(() -> SSOrderImporter.parseQuantityToTenths("abc"))
                .isInstanceOf(NumberFormatException.class);
    }
}
