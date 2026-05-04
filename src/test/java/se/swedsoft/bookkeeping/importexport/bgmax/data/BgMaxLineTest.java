package se.swedsoft.bookkeeping.importexport.bgmax.data;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link BgMaxLine} and {@link BgMaxBetalning}.
 *
 * Both classes are pure data classes with no database or file dependencies.
 */
class BgMaxLineTest {

    /** Builds a minimal 80-character line padded with spaces. */
    private String pad(String prefix) {
        StringBuilder sb = new StringBuilder(prefix);
        while (sb.length() < 80) {
            sb.append(' ');
        }
        return sb.toString();
    }

    // ---- BgMaxLine constructor ----

    @Test
    void constructorAcceptsExactly80Characters() {
        String line = pad("01BGMAX            01201406011235350102                                        ");
        // No exception expected
        new BgMaxLine(line);
    }

    @Test
    void constructorThrowsForLineShorterThan80Chars() {
        assertThatThrownBy(() -> new BgMaxLine("tooshort"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("lengt mismatch");
    }

    @Test
    void constructorThrowsForLineLongerThan80Chars() {
        String longLine = "X".repeat(81);
        assertThatThrownBy(() -> new BgMaxLine(longLine))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("lengt mismatch");
    }

    // ---- getTransaktionsKod ----

    @Test
    void getTransaktionsKodReturnsFirstTwoChars() {
        // Record 20 starts with "20"
        String line = pad("20" + "1234567890");
        BgMaxLine bgLine = new BgMaxLine(line);

        assertThat(bgLine.getTransaktionsKod()).isEqualTo("20");
    }

    @Test
    void getTransaktionsKodForRecord01() {
        String line = pad("01BGMAX            01");
        BgMaxLine bgLine = new BgMaxLine(line);

        assertThat(bgLine.getTransaktionsKod()).isEqualTo("01");
    }

    // ---- getField(int) ----

    @Test
    void getFieldSingleCharReturnsCorrectChar() {
        // Build a line where position 5 (1-indexed) is 'Z'
        char[] chars = new char[80];
        java.util.Arrays.fill(chars, ' ');
        chars[4] = 'Z';  // 0-indexed position 4 = 1-indexed position 5
        BgMaxLine bgLine = new BgMaxLine(new String(chars));

        assertThat(bgLine.getField(5)).isEqualTo("Z");
    }

    // ---- getField(int, int) ----

    @Test
    void getFieldRangeTrimmed() {
        // positions 3-7 (1-indexed) contain "HELLO" (rest is spaces)
        char[] chars = new char[80];
        java.util.Arrays.fill(chars, ' ');
        chars[2] = 'H'; // pos 3
        chars[3] = 'I'; // pos 4
        BgMaxLine bgLine = new BgMaxLine(new String(chars));

        assertThat(bgLine.getField(3, 4)).isEqualTo("HI");
    }

    @Test
    void getFieldRangeTrimsTrailingSpaces() {
        char[] chars = new char[80];
        java.util.Arrays.fill(chars, ' ');
        chars[2] = 'A'; // pos 3
        // positions 4-9 remain spaces
        BgMaxLine bgLine = new BgMaxLine(new String(chars));

        assertThat(bgLine.getField(3, 9)).isEqualTo("A");
    }

    // ---- BgMaxBetalning.getBelopp() ----

    @Test
    void getBeloppScalesEighteenDigitStringByMinusTwoPowerOfTen() {
        BgMaxBetalning bet = new BgMaxBetalning();
        // "000000000000018000" represents 180.00 SEK (amounts in öre/cents)
        bet.setBeloppRaw("000000000000018000");

        BigDecimal expected = new BigDecimal("180.00");
        assertThat(bet.getBelopp()).isEqualByComparingTo(expected);
    }

    @Test
    void getBeloppForZeroAmount() {
        BgMaxBetalning bet = new BgMaxBetalning();
        bet.setBeloppRaw("000000000000000000");

        assertThat(bet.getBelopp()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getBeloppForSmallAmountOneOre() {
        BgMaxBetalning bet = new BgMaxBetalning();
        bet.setBeloppRaw("000000000000000001");

        assertThat(bet.getBelopp()).isEqualByComparingTo(new BigDecimal("0.01"));
    }

    @Test
    void getBeloppForLargeAmount() {
        BgMaxBetalning bet = new BgMaxBetalning();
        // 1 000 000.00 SEK = 100 000 000 öre
        bet.setBeloppRaw("000000000010000000");

        assertThat(bet.getBelopp()).isEqualByComparingTo(new BigDecimal("100000.00"));
    }

    // ---- BgMaxBetalning default constructor ----

    @Test
    void defaultConstructorInitialisesReferenser() {
        BgMaxBetalning bet = new BgMaxBetalning();

        assertThat(bet.getReferenser()).isNotNull().isEmpty();
    }

    // ---- BgMaxFile parse invalid input ----

    @Test
    void parseShouldThrowImportExceptionWhenFirstLineIsInvalid() {
        BgMaxFile file = new BgMaxFile();
        java.util.List<String> lines = new java.util.ArrayList<>();
        // Valid length but does NOT start with "01BGMAX"
        lines.add(pad("99INVALID         "));

        assertThatThrownBy(() -> file.parse(lines))
                .isInstanceOf(se.swedsoft.bookkeeping.importexport.util.SSImportException.class);
    }

    @Test
    void parseThrowsImportExceptionForLineTooShort() {
        BgMaxFile file = new BgMaxFile();
        java.util.List<String> lines = new java.util.ArrayList<>();
        // First line must be 80 chars; this is shorter
        lines.add("01BGMAX");

        assertThatThrownBy(() -> file.parse(lines))
                .isInstanceOf(se.swedsoft.bookkeeping.importexport.util.SSImportException.class);
    }
}
