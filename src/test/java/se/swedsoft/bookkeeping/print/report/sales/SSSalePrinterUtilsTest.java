package se.swedsoft.bookkeeping.print.report.sales;

import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSNewCompany;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

class SSSalePrinterUtilsTest {

    @Test
    void getPrimaryPaymentMethodPrefersBankGiro() {
        SSNewCompany company = company("5555-1111", "192837-4", "SE3550000000054910000003", "ESSESESS");

        assertThat(SSSalePrinterUtils.getPrimaryPaymentMethod(company)).isEqualTo("BG");
    }

    @Test
    void getPrimaryPaymentMethodUsesPlusGiroWhenBankGiroMissing() {
        SSNewCompany company = company("", "192837-4", "SE3550000000054910000003", "ESSESESS");

        assertThat(SSSalePrinterUtils.getPrimaryPaymentMethod(company)).isEqualTo("PG");
    }

    @Test
    void getPrimaryPaymentMethodUsesIbanWhenBicPresent() {
        SSNewCompany company = company("", "", "SE3550000000054910000003", "ESSESESS");

        assertThat(SSSalePrinterUtils.getPrimaryPaymentMethod(company)).isEqualTo("IBAN");
    }

    @Test
    void getPrimaryPaymentMethodUsesBbanWhenBicMissing() {
        SSNewCompany company = company("", "", "SE3550000000054910000003", "");

        assertThat(SSSalePrinterUtils.getPrimaryPaymentMethod(company)).isEqualTo("BBAN");
    }

    @Test
    void getPrimaryPaymentMethodFallsBackToIbanWhenNoPaymentDataExists() {
        SSNewCompany company = company("", "", "", "");

        assertThat(SSSalePrinterUtils.getPrimaryPaymentMethod(company)).isEqualTo("IBAN");
    }

    @Test
    void getPrimaryPaymentAccountPrefersBankGiro() {
        SSNewCompany company = company("5555-1111", "192837-4", "SE3550000000054910000003", "ESSESESS");

        assertThat(SSSalePrinterUtils.getPrimaryPaymentAccount(company)).isEqualTo("5555-1111");
    }

    @Test
    void getPrimaryPaymentAccountUsesPlusGiroWhenBankGiroMissing() {
        SSNewCompany company = company("", "192837-4", "SE3550000000054910000003", "ESSESESS");

        assertThat(SSSalePrinterUtils.getPrimaryPaymentAccount(company)).isEqualTo("192837-4");
    }

    @Test
    void getPrimaryPaymentAccountFallsBackToIban() {
        SSNewCompany company = company("", "", "SE3550000000054910000003", "");

        assertThat(SSSalePrinterUtils.getPrimaryPaymentAccount(company)).isEqualTo("SE3550000000054910000003");
    }

    @Test
    void getImageReturnsNullForNullFile() {
        assertThat(SSSalePrinterUtils.getImage(null)).isNull();
    }

    @Test
    void getImageReturnsNullForMissingFile() {
        assertThat(SSSalePrinterUtils.getImage(new File("does-not-exist.png"))).isNull();
    }

    private static SSNewCompany company(String bankGiro, String plusGiro, String iban, String bic) {
        SSNewCompany company = new SSNewCompany();
        company.setBankGiroNumber(bankGiro);
        company.setPlusGiroNumber(plusGiro);
        company.setIBAN(iban);
        company.setBIC(bic);
        return company;
    }
}

