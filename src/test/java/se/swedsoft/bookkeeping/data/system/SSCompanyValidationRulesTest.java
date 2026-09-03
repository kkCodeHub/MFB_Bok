package se.swedsoft.bookkeeping.data.system;

import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSAddress;
import se.swedsoft.bookkeeping.data.SSNewCompany;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SSCompanyValidationRulesTest {

    @Test
    void normalizeSwiftTrimsValue() {
        assertThat(SSCompanyValidationRules.normalizeSwift("  abcdsess  ")).isEqualTo("abcdsess");
    }

    @Test
    void normalizeSwiftRejectsTooLongValue() {
        String tooLongSwift = "123456789012345678901";

        assertThatThrownBy(() -> SSCompanyValidationRules.normalizeSwift(tooLongSwift))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SWIFT");
    }

    @Test
    void validateCompanyStringLengthsAcceptsBoundaryValues() {
        SSNewCompany company = new SSNewCompany();
        company.setName(repeat('A', SSCompanyValidationRules.NAME_MAX_LENGTH));
        company.setBIC("ABCDEFGH");
        company.setSwishImagePath("C:/tmp/" + repeat('A', SSCompanyValidationRules.NAME_MAX_LENGTH) + ".png");
        company.setAddress(new SSAddress("n", "a1", "a2", "11111", "city", "country"));
        company.setDeliveryAddress(new SSAddress("n", "a1", "a2", "11111", "city", "country"));

        assertThatCode(() -> SSCompanyValidationRules.validateCompanyStringLengths(company))
                .doesNotThrowAnyException();
    }

    @Test
    void validateCompanyStringLengthsRejectsTooLongAddressField() {
        SSNewCompany company = new SSNewCompany();
        company.setAddress(new SSAddress(repeat('A', SSCompanyValidationRules.ADDRESS_NAME_MAX_LENGTH + 1),
                "a1", "a2", "11111", "city", "country"));

        assertThatThrownBy(() -> SSCompanyValidationRules.validateCompanyStringLengths(company))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("address");
    }

    @Test
    void validateSwishImageFileNameAcceptsCompanyNameToken() {
        assertThat(SSCompanyValidationRules.validateSwishImageFileName(
                "Fribok AB", "556677-8899", "C:/tmp/2026 Fribok AB swish.png")).isEmpty();
    }

    @Test
    void validateSwishImageFileNameAcceptsCorporateIdToken() {
        assertThat(SSCompanyValidationRules.validateSwishImageFileName(
                "Fribok AB", "556677-8899", "C:/tmp/swish_5566778899.png")).isEmpty();
    }

    @Test
    void validateSwishImageFileNameRejectsMissingTokens() {
        assertThat(SSCompanyValidationRules.validateSwishImageFileName(
                "Fribok AB", "556677-8899", "C:/tmp/swish_other.png")).isPresent();
    }

    // ---- isValidEmail ----

    @Test
    void validEmailAccepted() {
        assertThat(SSCompanyValidationRules.isValidEmail("info@foretaget.se")).isTrue();
        assertThat(SSCompanyValidationRules.isValidEmail("user.name+tag@sub.domain.com")).isTrue();
    }

    @Test
    void invalidEmailRejected() {
        assertThat(SSCompanyValidationRules.isValidEmail("ingenat")).isFalse();
        assertThat(SSCompanyValidationRules.isValidEmail("@domän.se")).isFalse();
        assertThat(SSCompanyValidationRules.isValidEmail("namn@")).isFalse();
    }

    @Test
    void nullOrEmptyEmailAccepted() {
        assertThat(SSCompanyValidationRules.isValidEmail(null)).isTrue();
        assertThat(SSCompanyValidationRules.isValidEmail("")).isTrue();
        assertThat(SSCompanyValidationRules.isValidEmail("  ")).isTrue();
    }

    // ---- isValidWebAddress ----

    @Test
    void validWebAddressAccepted() {
        assertThat(SSCompanyValidationRules.isValidWebAddress("https://www.foretaget.se")).isTrue();
        assertThat(SSCompanyValidationRules.isValidWebAddress("http://example.com")).isTrue();
        assertThat(SSCompanyValidationRules.isValidWebAddress("www.foretaget.se")).isTrue();
    }

    @Test
    void invalidWebAddressRejected() {
        assertThat(SSCompanyValidationRules.isValidWebAddress("foretaget.se")).isFalse();
        assertThat(SSCompanyValidationRules.isValidWebAddress("ftp://files.example.com")).isFalse();
    }

    @Test
    void nullOrEmptyWebAddressAccepted() {
        assertThat(SSCompanyValidationRules.isValidWebAddress(null)).isTrue();
        assertThat(SSCompanyValidationRules.isValidWebAddress("")).isTrue();
    }

    // ---- isValidSwedishCorporateId ----

    @Test
    void validSwedishCorporateIdAccepted() {
        // 556021-0261 = IKEA AB: third digit 6 >= 2, Luhn checksum valid
        assertThat(SSCompanyValidationRules.isValidSwedishCorporateId("556021-0261")).isTrue();
        assertThat(SSCompanyValidationRules.isValidSwedishCorporateId("5560210261")).isTrue();
    }

    @Test
    void validSwedishCorporateIdAcceptsPersonalIdentityNumber() {
        assertThat(SSCompanyValidationRules.isValidSwedishCorporateId(buildValidPersonalIdentityNumber10())).isTrue();
        assertThat(SSCompanyValidationRules.isValidSwedishCorporateId(buildValidPersonalIdentityNumber12())).isTrue();
    }

    @Test
    void invalidSwedishCorporateIdRejected() {
        assertThat(SSCompanyValidationRules.isValidSwedishCorporateId("100000-0000")).isFalse(); // third digit < 2
        assertThat(SSCompanyValidationRules.isValidSwedishCorporateId("ABCDEF-GHIJ")).isFalse(); // non-digits
        assertThat(SSCompanyValidationRules.isValidSwedishCorporateId("123456-789")).isFalse();  // wrong length
        String validPersonalNumber = buildValidPersonalIdentityNumber10();
        String invalidPersonalNumber = validPersonalNumber.substring(0, validPersonalNumber.length() - 1) + "0";
        assertThat(SSCompanyValidationRules.isValidSwedishCorporateId(invalidPersonalNumber)).isFalse();
    }

    @Test
    void nullOrEmptyCorporateIdAccepted() {
        assertThat(SSCompanyValidationRules.isValidSwedishCorporateId(null)).isTrue();
        assertThat(SSCompanyValidationRules.isValidSwedishCorporateId("")).isTrue();
    }

    // ---- isValidSwedishVatNumber ----

    @Test
    void validSwedishVatNumberAccepted() {
        assertThat(SSCompanyValidationRules.isValidSwedishVatNumber("SE556677889901")).isTrue();
        assertThat(SSCompanyValidationRules.isValidSwedishVatNumber("se556677889901")).isTrue();
    }

    @Test
    void invalidSwedishVatNumberRejected() {
        assertThat(SSCompanyValidationRules.isValidSwedishVatNumber("556677889901")).isFalse();   // missing SE prefix
        assertThat(SSCompanyValidationRules.isValidSwedishVatNumber("SE5566778899")).isFalse();   // only 10 digits
        assertThat(SSCompanyValidationRules.isValidSwedishVatNumber("DE556677889901")).isFalse(); // wrong country code
    }

    @Test
    void nullOrEmptyVatNumberAccepted() {
        assertThat(SSCompanyValidationRules.isValidSwedishVatNumber(null)).isTrue();
        assertThat(SSCompanyValidationRules.isValidSwedishVatNumber("")).isTrue();
    }

    // ---- isValidSwedishPostalCode ----

    @Test
    void validSwedishPostalCodeAccepted() {
        assertThat(SSCompanyValidationRules.isValidSwedishPostalCode("11122")).isTrue();
        assertThat(SSCompanyValidationRules.isValidSwedishPostalCode("111 22")).isTrue();
        assertThat(SSCompanyValidationRules.isValidSwedishPostalCode("99999")).isTrue();
    }

    @Test
    void invalidSwedishPostalCodeRejected() {
        assertThat(SSCompanyValidationRules.isValidSwedishPostalCode("1234")).isFalse();   // only 4 digits
        assertThat(SSCompanyValidationRules.isValidSwedishPostalCode("123456")).isFalse(); // 6 digits
        assertThat(SSCompanyValidationRules.isValidSwedishPostalCode("1A345")).isFalse();  // non-digit
    }

    @Test
    void nullOrEmptyPostalCodeAccepted() {
        assertThat(SSCompanyValidationRules.isValidSwedishPostalCode(null)).isTrue();
        assertThat(SSCompanyValidationRules.isValidSwedishPostalCode("")).isTrue();
    }

    // ---- isValidBankgiro ----

    @Test
    void validBankgiroAccepted() {
        assertThat(SSCompanyValidationRules.isValidBankgiro("221-0425")).isTrue();
        assertThat(SSCompanyValidationRules.isValidBankgiro("5050-1055")).isTrue();
        assertThat(SSCompanyValidationRules.isValidBankgiro("50501055")).isTrue();
    }

    @Test
    void invalidBankgiroRejected() {
        assertThat(SSCompanyValidationRules.isValidBankgiro("123-456")).isFalse();    // only 6 digits
        assertThat(SSCompanyValidationRules.isValidBankgiro("123456789")).isFalse();  // 9 digits
        assertThat(SSCompanyValidationRules.isValidBankgiro("12-34567")).isFalse();   // unsupported grouping
        assertThat(SSCompanyValidationRules.isValidBankgiro("1234-567A")).isFalse();  // non-digit
    }

    @Test
    void nullOrEmptyBankgiroAccepted() {
        assertThat(SSCompanyValidationRules.isValidBankgiro(null)).isTrue();
        assertThat(SSCompanyValidationRules.isValidBankgiro("")).isTrue();
    }

    // ---- isValidPlusgiro ----

    @Test
    void validPlusgiroAccepted() {
        // Format-only validation: with and without hyphen should be accepted.
        assertThat(SSCompanyValidationRules.isValidPlusgiro("123456-9")).isTrue();
        assertThat(SSCompanyValidationRules.isValidPlusgiro("1234569")).isTrue();
        assertThat(SSCompanyValidationRules.isValidPlusgiro("123456-7")).isTrue();
    }

    @Test
    void invalidPlusgiroRejected() {
        assertThat(SSCompanyValidationRules.isValidPlusgiro("1")).isFalse();          // too short
        assertThat(SSCompanyValidationRules.isValidPlusgiro("123456789")).isFalse();  // 9 digits (> 8)
        assertThat(SSCompanyValidationRules.isValidPlusgiro("12345A-7")).isFalse();   // non-digit
    }

    @Test
    void nullOrEmptyPlusgiroAccepted() {
        assertThat(SSCompanyValidationRules.isValidPlusgiro(null)).isTrue();
        assertThat(SSCompanyValidationRules.isValidPlusgiro("")).isTrue();
    }

    private static String repeat(char c, int count) {
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append(c);
        }
        return sb.toString();
    }

    private static String buildValidPersonalIdentityNumber10() {
        String base = "800101001";
        return base + luhnCheckDigit(base);
    }

    private static String buildValidPersonalIdentityNumber12() {
        String baseDate = "19800101";
        String base = "800101001";
        return baseDate + base.substring(6) + luhnCheckDigit(base);
    }

    private static int luhnCheckDigit(String digitsWithoutCheckDigit) {
        int sum = 0;
        for (int i = 0; i < digitsWithoutCheckDigit.length(); i++) {
            int digit = Character.getNumericValue(digitsWithoutCheckDigit.charAt(i));
            if (i % 2 == 0) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
        }
        return (10 - (sum % 10)) % 10;
    }
}
