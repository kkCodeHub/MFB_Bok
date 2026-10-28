package se.swedsoft.bookkeeping.gui.outpayment;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.calc.math.SSOutpaymentMath;
import se.swedsoft.bookkeeping.calc.math.SSSupplierInvoiceMath;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.SSOutpayment;
import se.swedsoft.bookkeeping.data.SSOutpaymentRow;
import se.swedsoft.bookkeeping.data.SSSupplierInvoice;
import se.swedsoft.bookkeeping.data.SSSupplierInvoiceRow;
import se.swedsoft.bookkeeping.data.SSVoucher;
import se.swedsoft.bookkeeping.data.common.SSDefaultAccount;
import se.swedsoft.bookkeeping.data.system.SSCompanyYearContext;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.data.system.SSSystemConfigContext;
import se.swedsoft.bookkeeping.gui.outpayment.util.SSOutpaymentRowTableModel;
import se.swedsoft.bookkeeping.persistence.Repositories;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SSOutpaymentParityRegressionTest {
    private static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test_outpayment_parity_regression";

    private static Connection connection;

    @BeforeAll
    static void setup() throws Exception {
        System.setProperty("fribok.schema.version", "v2");
        Class.forName("org.hsqldb.jdbcDriver");
        connection = DriverManager.getConnection(JDBC_URL, "sa", "");
        SSSystemConfigContext.startupLocal(connection);

        SSNewCompany company = new SSNewCompany();
        company.setName("GUI Outpayment Parity Regression Test AB");
        SSCompanyYearContext.addCompany(company);
        SSDB.getInstance().setCurrentCompany(company);

        SSNewAccountingYear year = new SSNewAccountingYear();
        year.setLocalFrom(LocalDate.of(2025, 1, 1));
        year.setLocalTo(LocalDate.of(2025, 12, 31));
        SSCompanyYearContext.addAccountingYear(year);
        SSDB.getInstance().setCurrentYear(year);

        Repositories.init(SSDB.getInstance());
    }

    @AfterEach
    void clearSaldoMap() {
        SSSupplierInvoiceMath.iSaldoMap = null;
    }

    @AfterAll
    static void teardown() throws Exception {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } finally {
            System.clearProperty("fribok.schema.version");
        }
    }

    @Test
    void selectingSupplierInvoiceInitializesPaymentAmountFromHelper() {
        SSSupplierInvoice invoice = supplierInvoice(101, "100.00");
        SSSupplierInvoiceMath.iSaldoMap = new HashMap<>();
        SSSupplierInvoiceMath.iSaldoMap.put(101, new BigDecimal("999.00"));

        SSOutpaymentRow row = new SSOutpaymentRow();
        row.setSupplierInvoice(invoice);

        assertThat(row.getValue()).isEqualByComparingTo("100.00");
    }

    @Test
    void currencyRateDifferenceUsesSaldoMinusPaymentAmount() {
        SSSupplierInvoice invoice = new SSSupplierInvoice();
        invoice.setNumber(202);

        SSSupplierInvoiceMath.iSaldoMap = new HashMap<>();
        SSSupplierInvoiceMath.iSaldoMap.put(202, new BigDecimal("200.00"));

        SSOutpaymentRow row = new SSOutpaymentRow();
        row.setSupplierInvoice(invoice);
        row.setValue(new BigDecimal("150.00"));

        Optional<BigDecimal> difference = SSOutpaymentMath.getCurrencyRateDifference(row);
        assertThat(difference).isPresent();
        assertThat(difference.orElseThrow()).isEqualByComparingTo("50.00");
    }

    @Test
    void kursdifferensColumnShowsRemainingSaldoAfterPartialPayment() {
        SSSupplierInvoice invoice = new SSSupplierInvoice();
        invoice.setNumber(606);
        SSSupplierInvoiceMath.iSaldoMap = new HashMap<>();
        SSSupplierInvoiceMath.iSaldoMap.put(606, new BigDecimal("300.00"));

        SSOutpaymentRow row = new SSOutpaymentRow();
        row.setSupplierInvoice(invoice);
        row.setValue(new BigDecimal("120.00"));

        Object value = SSOutpaymentRowTableModel.COLUMN_CURRENCYRATEDIFFERENCE.getValue(row);
        assertThat((BigDecimal) value).isEqualByComparingTo("180.00");
    }

    @Test
    void remainingSaldoRecalculatesWhenPaymentAmountChanges() {
        SSSupplierInvoice invoice = new SSSupplierInvoice();
        invoice.setNumber(707);
        SSSupplierInvoiceMath.iSaldoMap = new HashMap<>();
        SSSupplierInvoiceMath.iSaldoMap.put(707, new BigDecimal("250.00"));

        SSOutpaymentRow row = new SSOutpaymentRow();
        row.setSupplierInvoice(invoice);
        row.setValue(new BigDecimal("100.00"));
        assertThat(SSOutpaymentMath.getCurrencyRateDifference(row).orElseThrow())
                .isEqualByComparingTo("150.00");

        row.setValue(new BigDecimal("80.00"));
        assertThat(SSOutpaymentMath.getCurrencyRateDifference(row).orElseThrow())
                .isEqualByComparingTo("170.00");
    }

    @Test
    void sumUsesPaymentAmountWithoutCurrencyConversion() {
        SSOutpayment outpayment = new SSOutpayment();
        SSOutpaymentRow row = new SSOutpaymentRow();
        row.setValue(new BigDecimal("100.00"));
        row.setCurrencyRate(new BigDecimal("2.000000"));
        outpayment.getRows().add(row);

        assertThat(SSOutpaymentMath.getSum(outpayment)).isEqualByComparingTo("100.00");
    }

    @Test
    void postableFallbackUsesInvoiceAmountWhenPaymentAmountIsZero() throws Exception {
        SSOutpayment outpayment = new SSOutpayment();
        SSOutpaymentRow row = new SSOutpaymentRow();
        row.setSupplierInvoice(supplierInvoice(303, "120.00"));
        row.setValue(BigDecimal.ZERO);
        outpayment.getRows().add(row);

        Method method = SSOutpaymentDialog.class.getDeclaredMethod(
                "hasPostableOutpaymentAmounts", SSOutpayment.class);
        method.setAccessible(true);

        boolean postable = (Boolean) method.invoke(null, outpayment);
        assertThat(postable).isTrue();
    }

    @Test
    void generateVoucherWithLegacyRatesDoesNotCreateCurrencyGainOrLossRows() {
        SSOutpayment outpayment = new SSOutpayment();
        outpayment.setNumber(909);

        SSOutpaymentRow row = new SSOutpaymentRow();
        row.setInvoiceNr(90901);
        row.setInvoiceCurrencyRate(new BigDecimal("10.25000"));
        row.setCurrencyRate(new BigDecimal("10.99000"));
        row.setValue(new BigDecimal("100.00"));
        outpayment.getRows().add(row);

        SSVoucher voucher = outpayment.generateVoucher();

        int currencyProfitAccount = SSDefaultAccount.CurrencyProfit.getDefaultAccountNumber();
        int currencyLossAccount = SSDefaultAccount.CurrencyLoss.getDefaultAccountNumber();
        assertThat(voucher.getRows())
                .noneMatch(voucherRow -> Objects.equals(voucherRow.getAccountNr(), currencyProfitAccount)
                        || Objects.equals(voucherRow.getAccountNr(), currencyLossAccount));
    }

    private static SSSupplierInvoice supplierInvoice(int number, String unitPrice) {
        SSSupplierInvoice invoice = new SSSupplierInvoice();
        invoice.setNumber(number);

        SSSupplierInvoiceRow invoiceRow = new SSSupplierInvoiceRow();
        invoiceRow.setUnitprice(new BigDecimal(unitPrice));
        invoiceRow.setQuantity(10);
        invoice.getRows().add(invoiceRow);

        return invoice;
    }
}
