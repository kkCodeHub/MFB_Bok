package se.swedsoft.bookkeeping.importexport.xml;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that {@link SSOrderExporter#tenthsToDecimalString(Integer)} converts
 * stored tenths to UI-decimal strings before writing to export files.
 */
class SSOrderExporterQuantityTenthsTest {

    @Test
    void shouldConvertWholeUnitsToDecimalString() {
        assertThat(SSOrderExporter.tenthsToDecimalString(10)).isEqualTo("1.0");
        assertThat(SSOrderExporter.tenthsToDecimalString(30)).isEqualTo("3.0");
    }

    @Test
    void shouldConvertHalfUnitsToDecimalString() {
        assertThat(SSOrderExporter.tenthsToDecimalString(15)).isEqualTo("1.5");
        assertThat(SSOrderExporter.tenthsToDecimalString(25)).isEqualTo("2.5");
    }

    @Test
    void shouldHandleZeroAndNegativeValues() {
        assertThat(SSOrderExporter.tenthsToDecimalString(0)).isEqualTo("0.0");
        assertThat(SSOrderExporter.tenthsToDecimalString(-15)).isEqualTo("-1.5");
    }

    @Test
    void shouldReturnEmptyStringForNullTenths() {
        assertThat(SSOrderExporter.tenthsToDecimalString(null)).isEqualTo("");
    }

    @Test
    void shouldRoundTripWithImporter() {
        // Export 3.0 UI → importer must parse back to 30 tenths
        String exported = SSOrderExporter.tenthsToDecimalString(30);
        assertThat(SSOrderImporter.parseQuantityToTenths(exported)).isEqualTo(30);
    }
}
