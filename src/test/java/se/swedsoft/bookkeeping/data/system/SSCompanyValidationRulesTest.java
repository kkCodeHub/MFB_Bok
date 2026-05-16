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

    private static String repeat(char c, int count) {
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append(c);
        }
        return sb.toString();
    }
}

