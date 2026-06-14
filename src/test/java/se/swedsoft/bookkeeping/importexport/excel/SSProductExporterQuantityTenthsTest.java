package se.swedsoft.bookkeeping.importexport.excel;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that {@link SSProductExporter#tenthsToDecimalString(Integer)} converts
 * stored tenths to UI-decimal strings before writing to export files.
 */
class SSProductExporterQuantityTenthsTest {

    @Test
    void shouldConvertWholeUnitsToDecimalString() {
        assertThat(SSProductExporter.tenthsToDecimalString(10)).isEqualTo("1.0");
        assertThat(SSProductExporter.tenthsToDecimalString(70)).isEqualTo("7.0");
    }

    @Test
    void shouldConvertHalfUnitsToDecimalString() {
        assertThat(SSProductExporter.tenthsToDecimalString(25)).isEqualTo("2.5");
        assertThat(SSProductExporter.tenthsToDecimalString(5)).isEqualTo("0.5");
    }

    @Test
    void shouldHandleZeroAndNegativeValues() {
        assertThat(SSProductExporter.tenthsToDecimalString(0)).isEqualTo("0.0");
        assertThat(SSProductExporter.tenthsToDecimalString(-5)).isEqualTo("-0.5");
    }

    @Test
    void shouldReturnEmptyStringForNullTenths() {
        assertThat(SSProductExporter.tenthsToDecimalString(null)).isEqualTo("");
    }

    @Test
    void shouldRoundTripWithImporter() {
        // Export 2.5 UI → importer must parse back to 25 tenths
        String exported = SSProductExporter.tenthsToDecimalString(25);
        assertThat(SSProductImporter.parseQuantityToTenths(exported)).isEqualTo(25);
    }
}
